package com.izhaanintellect.pasa.ui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.SendLocationRequest
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.databinding.ActivityAlertMessageBinding
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.util.SecurityActivityLauncher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Full-screen lockscreen activity for displaying urgent owner messages and lost-device alerts.
 * Remote-only unlock via Telegram or SMS (no local PIN entry).
 * Features: screen-touch photo capture, Device Owner Kiosk Mode, message display.
 */
@AndroidEntryPoint
class AlertMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertMessageBinding

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var duressManager: com.izhaanintellect.pasa.detection.DuressManager
    @Inject lateinit var cameraManager: com.izhaanintellect.pasa.camera.StealthCameraManager
    @Inject lateinit var pasaBackendApi: com.izhaanintellect.pasa.network.PasaBackendApi

    private var isKioskActive = false

    companion object {
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_ENFORCE_PIN = "extra_enforce_pin"
        const val ACTION_DISMISS_LOST_MODE = "com.izhaanintellect.pasa.ACTION_DISMISS_LOST_MODE"
        const val NOTIFICATION_ID = 2001
        private const val TAG = "PASA_AlertActivity"

        fun createIntent(context: Context, message: String, phone: String? = null, enforcePin: Boolean = false): Intent {
            return Intent(context, AlertMessageActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_ENFORCE_PIN, enforcePin)
                phone?.let { putExtra(EXTRA_PHONE, it) }
            }
        }
    }

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_DISMISS_LOST_MODE) {
                Log.i(TAG, "Received remote unlock broadcast, dismissing Lost Mode Guard")
                exitLostMode()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            configureLockScreenFlags()

            binding = ActivityAlertMessageBinding.inflate(layoutInflater)
            setContentView(binding.root)

        // Prevent back button from dismissing Lost Mode
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (preferencesManager.isLostModeActive) {
                    Toast.makeText(this@AlertMessageActivity, "🔒 Locked: Send /unlock from Telegram to release", Toast.LENGTH_SHORT).show()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Register remote unlock listener
        val filter = IntentFilter(ACTION_DISMISS_LOST_MODE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, filter)
        }

        // Check if Device Owner Kiosk mode is available
        if (PasaDeviceAdmin.isDeviceOwner(this)) {
            try {
                PasaDeviceAdmin.configureLockTask(this)
                PasaDeviceAdmin.setComprehensiveLockdown(this, true)

                // Non-blocking lock task start with delayed retry for Android 14-16 keyguard synchronization
                try {
                    startLockTask()
                    isKioskActive = true
                    Log.i(TAG, "Device Owner Kiosk Mode (Lock Task) started successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Initial lock task attempt: ${e.message} — scheduling 400ms retry")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            if (!isFinishing && !isDestroyed && preferencesManager.isLostModeActive) {
                                startLockTask()
                                isKioskActive = true
                                Log.i(TAG, "Device Owner Kiosk Mode started on retry")
                            }
                        } catch (re: Exception) {
                            Log.w(TAG, "Retry lock task failed: ${re.message}")
                        }
                    }, 400L)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not configure Device Owner kiosk: ${e.message}")
            }
        }

        val messageText = intent.getStringExtra(EXTRA_MESSAGE)
            ?: preferencesManager.lostModeMessage.ifBlank { "Please return this device to its owner." }
        val explicitPhone = intent.getStringExtra(EXTRA_PHONE)

        binding.tvMessageContent.text = messageText

        val timeFormat = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
        binding.tvTimestamp.text = "Received: ${timeFormat.format(Date())}"

        // Extract phone number from message if not explicitly provided
        val phoneNumber = explicitPhone ?: extractPhoneNumber(messageText)

        if (!phoneNumber.isNullOrBlank()) {
            binding.btnCallOwner.visibility = View.VISIBLE
            binding.btnCallOwner.text = "📞 Call Owner: $phoneNumber"
            binding.btnCallOwner.setOnClickListener {
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(dialIntent)
                } catch (_: Exception) {}
            }
        } else {
            binding.btnCallOwner.visibility = View.GONE
        }


        playAlertChime()
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error in AlertMessageActivity.onCreate", e)
            finish()
        }
    }


    private var lastTouchCaptureTime = 0L

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            if (now - lastTouchCaptureTime > 4000L) { // 4 second debounce
                lastTouchCaptureTime = now
                Log.w(TAG, "Physical screen touch detected during Lost Mode! Triggering covert capture.")
                CoroutineScope(Dispatchers.IO).launch {
                    triggerTouchCapture()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private suspend fun triggerTouchCapture() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Touch detected - initiating rapid headless capture and Telegram dispatch")

            com.izhaanintellect.pasa.service.PasaService.elevateServiceToCamera()

            val photoDeferred = async {
                try {
                    val directFile = cameraManager.capturePhoto(useFrontCamera = true)
                    if (directFile != null && directFile.exists() && directFile.length() > 0) {
                        directFile
                    } else {
                        Log.w(TAG, "Headless photo capture returned null, using StealthCaptureBridge fallback")
                        val bridgeResult = StealthCaptureBridge.capturePhoto(applicationContext, useFront = true, timeoutMs = 8000L)
                        bridgeResult.file
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Headless photo capture error: ${e.message}", e)
                    try {
                        StealthCaptureBridge.capturePhoto(applicationContext, useFront = true, timeoutMs = 8000L).file
                    } catch (_: Exception) { null }
                } finally {
                    com.izhaanintellect.pasa.service.PasaService.demoteServiceFromCamera()
                }
            }

            val locDeferred = async {
                try {
                    withTimeoutOrNull(4000L) {
                        locationTracker.getCurrentLocation()
                    }
                } catch (_: Exception) {
                    null
                }
            }

            val photoFile = photoDeferred.await()
            val loc = locDeferred.await()

            if (photoFile != null && photoFile.exists()) {
                Log.i(TAG, "Mugshot captured successfully: ${photoFile.length()} bytes")
            } else {
                Log.w(TAG, "Mugshot photo capture returned null")
            }

            val locMsg = if (loc != null) {
                "\n📍 <b>Location:</b> <a href=\"https://www.google.com/maps?q=${loc.latitude},${loc.longitude}\">${loc.latitude}, ${loc.longitude}</a>"
            } else ""

            val alertText = "⚠️ <b>Physical Screen Touch Detected on Locked Device!</b>\nAn unauthorized user touched the screen while Lost Mode is active.$locMsg\n\n📸 Covert front-camera mugshot captured silently."

            dispatchSecurityAlert(
                alertType = "LOST_MODE_SCREEN_TOUCH",
                alertMessage = alertText,
                latVal = loc?.latitude,
                lngVal = loc?.longitude,
                photoFile = photoFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling screen touch capture: ${e.message}", e)
        }
    }

    private suspend fun triggerDuressSos() {
        duressManager.triggerDuressSos(applicationContext, "Lost Mode Screen")
    }

    private suspend fun dispatchSecurityAlert(
        alertType: String,
        alertMessage: String,
        latVal: Double?,
        lngVal: Double?,
        photoFile: File?
    ) {
        val token = preferencesManager.botToken
        val chatId = preferencesManager.ownerChatId.ifBlank { preferencesManager.ownerChatIdLong.toString() }
        val chatIdLong = preferencesManager.ownerChatIdLong

        if (token.isBlank() || (chatId.isBlank() && chatIdLong == 0L)) {
            Log.e(TAG, "❌ Cannot send alert - bot token or chat ID not configured")
            return
        }

        try {
            // Path A: If VPS backend is enabled, relay through VPS backend alert API
            if (preferencesManager.useBackendServer) {
                try {
                    val deviceIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                    val alertTypeBody = alertType.toRequestBody("text/plain".toMediaTypeOrNull())
                    val msgBody = alertMessage.toRequestBody("text/plain".toMediaTypeOrNull())
                    val latBody = latVal?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val lngBody = lngVal?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val photoPart = photoFile?.let { file ->
                        if (file.exists() && file.length() > 0) {
                            val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("photo", file.name, reqFile)
                        } else null
                    }

                    val resp = pasaBackendApi.sendDeviceAlert(
                        deviceId = deviceIdBody,
                        alertType = alertTypeBody,
                        message = msgBody,
                        photo = photoPart,
                        latitude = latBody,
                        longitude = lngBody
                    )
                    if (resp.ok) {
                        Log.i(TAG, "✅ Security alert relayed through VPS successfully")
                        return
                    }
                } catch (vpsErr: Exception) {
                    Log.w(TAG, "VPS alert relay failed, falling back to direct Telegram: ${vpsErr.message}")
                }
            }

            // Path B: Direct Telegram API dispatch (Sovereign Mode or fallback)
            val fullText = "🚨 <b>$alertType</b>\n━━━━━━━━━━━━━━━━━━━━\n$alertMessage"

            // 1. Send photo with caption if photo exists
            var photoSent = false
            if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
                try {
                    val chatIdBody = chatId.toRequestBody("text/plain".toMediaTypeOrNull())
                    val captionBody = fullText.take(1024).toRequestBody("text/plain".toMediaTypeOrNull())
                    val fileBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("photo", photoFile.name, fileBody)

                    val photoResp = telegramApi.sendPhoto(
                        token = token,
                        chatId = chatIdBody,
                        photo = part,
                        caption = captionBody
                    )
                    if (photoResp.ok) {
                        photoSent = true
                        Log.i(TAG, "✅ Mugshot photo & alert sent directly to Telegram")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Direct photo send error: ${e.message}")
                }
            }

            // 2. If photo was not sent or failed, send text message
            if (!photoSent) {
                try {
                    telegramApi.sendMessage(
                        token = token,
                        request = SendMessageRequest(
                            chatId = chatIdLong,
                            text = fullText
                        )
                    )
                    Log.i(TAG, "✅ Alert text sent directly to Telegram")
                } catch (e: Exception) {
                    Log.e(TAG, "Direct text alert send error: ${e.message}")
                }
            }

            // 3. Send location pin if coordinates available
            if (latVal != null && lngVal != null) {
                try {
                    telegramApi.sendLocation(
                        token = token,
                        request = SendLocationRequest(
                            chatId = chatIdLong,
                            latitude = latVal,
                            longitude = lngVal
                        )
                    )
                    Log.i(TAG, "✅ Location pin sent directly to Telegram")
                } catch (locErr: Exception) {
                    Log.w(TAG, "Direct location pin send error: ${locErr.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Security alert dispatch error: ${e.message}", e)
        } finally {
            // Immediately shred local photo after dispatch
            try {
                photoFile?.let {
                    if (it.exists()) {
                        it.delete()
                        Log.i(TAG, "Photo shredded after dispatch")
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun exitLostMode() {
        try {
            if (isKioskActive) {
                stopLockTask()
                isKioskActive = false
            }
            com.izhaanintellect.pasa.admin.PasaDeviceAdmin.setComprehensiveLockdown(this, false)
        } catch (_: Exception) {}

        preferencesManager.isLostModeActive = false
        preferencesManager.activeLockPin = null
        preferencesManager.lostModeMessage = ""

        SecurityActivityLauncher.dismissNotification(this, NOTIFICATION_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                km?.requestDismissKeyguard(this, null)
            } catch (_: Exception) {}
        }

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: Exception) {}
    }

    private fun configureLockScreenFlags() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }

            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )

            applyImmersiveBars()
        } catch (e: Throwable) {
            Log.w(TAG, "Error configuring lock screen flags: ${e.message}")
        }
    }

    private fun applyImmersiveBars() {
        try {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        } catch (_: Exception) {}
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        configureLockScreenFlags()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (preferencesManager.isLostModeActive) {
            applyImmersiveBars()
            if (!hasFocus) {
                try {
                    @Suppress("DEPRECATION")
                    sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                } catch (_: Exception) {}
                com.izhaanintellect.pasa.accessibility.AccessibilityScreenCaptureService.instance?.dismissNotificationShade()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (preferencesManager.isLostModeActive) {
            val pullBack = Intent(this, AlertMessageActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(pullBack)
        }
    }

    override fun onPause() {
        super.onPause()
        if (preferencesManager.isLostModeActive && !isFinishing) {
            val pullBack = Intent(this, AlertMessageActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(pullBack)
        }
    }

    private fun playAlertChime() {
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    private fun extractPhoneNumber(text: String): String? {
        val pattern = Pattern.compile("(\\+?[0-9]{7,15})")
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
}
