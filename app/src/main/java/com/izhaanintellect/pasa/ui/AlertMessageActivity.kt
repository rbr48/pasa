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
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.databinding.ActivityAlertMessageBinding
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.network.PasaBackendApi
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.util.SecurityActivityLauncher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * Features custom PIN entry, tamper photo capture on failed PIN attempts, and Device Owner Kiosk Mode.
 */
@AndroidEntryPoint
class AlertMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertMessageBinding

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var pasaBackendApi: PasaBackendApi

    private var enteredPin: StringBuilder = StringBuilder()
    private var failedPinAttempts = 0
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
                // If lost mode is active, prevent back button
                if (preferencesManager.isLostModeActive) {
                    Toast.makeText(this@AlertMessageActivity, "Enter PIN to unlock", Toast.LENGTH_SHORT).show()
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
                startLockTask()
                isKioskActive = true
                Log.i(TAG, "Device Owner Kiosk Mode (Lock Task) started successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Could not start lock task: ${e.message}")
            }
        }

        val messageText = intent.getStringExtra(EXTRA_MESSAGE)
            ?: preferencesManager.lostModeMessage.ifBlank { "Please return this device to its owner." }
        val explicitPhone = intent.getStringExtra(EXTRA_PHONE)
        val enforcePin = intent.getBooleanExtra(EXTRA_ENFORCE_PIN, false) || preferencesManager.isLostModeActive

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

        if (enforcePin || !preferencesManager.activeLockPin.isNullOrBlank()) {
            binding.llPinSection.visibility = View.VISIBLE
            binding.btnDismiss.visibility = View.GONE
            setupKeypad()
        } else {
            binding.llPinSection.visibility = View.GONE
            binding.btnDismiss.visibility = View.VISIBLE
            binding.btnDismiss.setOnClickListener {
                exitLostMode()
            }
        }

        playAlertChime()
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error in AlertMessageActivity.onCreate", e)
            finish()
        }
    }

    private fun setupKeypad() {
        updatePinDisplay()

        val numButtons = listOf(
            binding.btnKey0 to "0",
            binding.btnKey1 to "1",
            binding.btnKey2 to "2",
            binding.btnKey3 to "3",
            binding.btnKey4 to "4",
            binding.btnKey5 to "5",
            binding.btnKey6 to "6",
            binding.btnKey7 to "7",
            binding.btnKey8 to "8",
            binding.btnKey9 to "9"
        )

        for ((btn, digit) in numButtons) {
            btn.setOnClickListener {
                if (enteredPin.length < 8) {
                    enteredPin.append(digit)
                    updatePinDisplay()
                    binding.tvPinError.visibility = View.INVISIBLE
                }
            }
        }

        binding.btnKeyDelete.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updatePinDisplay()
                binding.tvPinError.visibility = View.INVISIBLE
            }
        }

        binding.btnKeyUnlock.setOnClickListener {
            verifyEnteredPin()
        }
    }

    private fun updatePinDisplay() {
        if (enteredPin.isEmpty()) {
            binding.tvPinDisplay.text = "• • • •"
            binding.tvPinDisplay.setTextColor(android.graphics.Color.parseColor("#475569"))
        } else {
            val dots = "• ".repeat(enteredPin.length).trim()
            binding.tvPinDisplay.text = dots
            binding.tvPinDisplay.setTextColor(android.graphics.Color.parseColor("#38BDF8"))
        }
    }

    private fun verifyEnteredPin() {
        val pin = enteredPin.toString()
        val activePin = preferencesManager.activeLockPin
        val duressPin = preferencesManager.duressPin

        // 1. Check Anti-Coercion Duress PIN
        if (!duressPin.isNullOrBlank() && pin == duressPin) {
            Log.w(TAG, "DURESS PIN ENTERED! Simulating normal unlock and launching silent SOS beacon.")
            Toast.makeText(this, "✅ Device Unlocked", Toast.LENGTH_SHORT).show()
            preferencesManager.isDuressActive = true

            // Trigger Duress SOS in application scope so activity finish does NOT cancel network/camera
            CoroutineScope(Dispatchers.IO).launch {
                triggerDuressSos()
            }
            exitLostMode()
            return
        }

        binding.btnKeyUnlock.isEnabled = false

        lifecycleScope.launch {
            // 2. Standard Lock PIN or Master Password (run off-thread to avoid ANR from 600k PBKDF2 iterations)
            val isPinCorrect = (!activePin.isNullOrBlank() && pin == activePin) ||
                    withContext(Dispatchers.Default) {
                        authManager.verifyMasterPassword(pin)
                    }

            binding.btnKeyUnlock.isEnabled = true

            if (isPinCorrect) {
                Log.i(TAG, "PIN verified successfully. Unlocking Lost Mode.")
                Toast.makeText(this@AlertMessageActivity, "✅ Device Unlocked", Toast.LENGTH_SHORT).show()
                preferencesManager.isDuressActive = false
                exitLostMode()
            } else {
                failedPinAttempts++
                binding.tvPinError.visibility = View.VISIBLE
                binding.tvPinError.text = "❌ Incorrect PIN ($failedPinAttempts/3 attempts)"
                enteredPin.clear()
                updatePinDisplay()
                playAlertChime()

                if (failedPinAttempts >= 3) {
                    CoroutineScope(Dispatchers.IO).launch {
                        triggerFailedPinDeterrent()
                    }
                    failedPinAttempts = 0
                }
            }
        }
    }

    private suspend fun triggerFailedPinDeterrent() {
        Log.w(TAG, "3 failed PIN attempts entered! Triggering stealth front-camera capture.")
        try {
            val loc = locationTracker.getCurrentLocation()
            val locMsg = if (loc != null) {
                "\n📍 <b>Location:</b> <a href=\"https://www.google.com/maps?q=${loc.latitude},${loc.longitude}\">${loc.latitude}, ${loc.longitude}</a>"
            } else ""

            val alertText = "An unauthorized user attempted 3 incorrect PINs on the Lost Mode screen.$locMsg"
            val captureResult = StealthCaptureBridge.capturePhoto(applicationContext, useFront = true, timeoutMs = 8000L)

            dispatchSecurityAlert(
                alertType = "FAILED_PIN_ATTEMPT",
                alertMessage = alertText,
                latVal = loc?.latitude,
                lngVal = loc?.longitude,
                photoFile = captureResult.file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling failed PIN deterrent alert", e)
        }
    }

    private suspend fun triggerDuressSos() {
        Log.w(TAG, "Triggering Duress SOS beacon in background")
        try {
            val loc = locationTracker.getCurrentLocation()
            val locMsg = if (loc != null) {
                "\n📍 <b>Live Coercion Pin:</b> <a href=\"https://www.google.com/maps?q=${loc.latitude},${loc.longitude}\">${loc.latitude}, ${loc.longitude}</a>"
            } else ""

            val alertText = "⚠️ <b>The Anti-Coercion Duress PIN was entered on this device!</b>\n" +
                    "The owner was forced to unlock under threat or duress.\n" +
                    "The lockscreen overlay unlocked cleanly to protect the owner's safety.$locMsg\n\n" +
                    "📡 <i>Covert front-camera capture and live telemetry active.</i>"

            val captureResult = StealthCaptureBridge.capturePhoto(applicationContext, useFront = true, timeoutMs = 8000L)

            dispatchSecurityAlert(
                alertType = "COERCION_DURESS_PIN",
                alertMessage = alertText,
                latVal = loc?.latitude,
                lngVal = loc?.longitude,
                photoFile = captureResult.file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in triggerDuressSos", e)
        }
    }

    private suspend fun dispatchSecurityAlert(
        alertType: String,
        alertMessage: String,
        latVal: Double?,
        lngVal: Double?,
        photoFile: File?
    ) {
        var relayedViaBackend = false

        if (preferencesManager.useBackendServer) {
            try {
                val deviceIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                val alertTypeBody = alertType.toRequestBody("text/plain".toMediaTypeOrNull())
                val msgBody = alertMessage.toRequestBody("text/plain".toMediaTypeOrNull())
                val photoPart = photoFile?.let {
                    if (it.exists() && it.length() > 0) {
                        val reqFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("photo", it.name, reqFile)
                    } else null
                }
                val latBody = latVal?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val lngBody = lngVal?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                val res = pasaBackendApi.sendDeviceAlert(
                    deviceId = deviceIdBody,
                    alertType = alertTypeBody,
                    message = msgBody,
                    photo = photoPart,
                    latitude = latBody,
                    longitude = lngBody
                )
                relayedViaBackend = res.ok
                Log.i(TAG, "Security alert ($alertType) relayed via VPS backend: ok=${res.ok}")
            } catch (e: Exception) {
                Log.w(TAG, "VPS alert relay failed, falling back to direct Telegram: ${e.message}")
            }
        }

        if (!relayedViaBackend && !preferencesManager.botToken.isNullOrBlank()) {
            try {
                val locMsg = if (latVal != null && lngVal != null) {
                    "\n📍 <b>Location:</b> <a href=\"https://www.google.com/maps?q=$latVal,$lngVal\">$latVal, $lngVal</a>"
                } else ""

                telegramApi.sendMessage(
                    token = preferencesManager.botToken,
                    request = SendMessageRequest(
                        chatId = preferencesManager.ownerChatIdLong,
                        text = "🚨 <b>$alertType</b>\n━━━━━━━━━━━━━━━━━━━━\n$alertMessage$locMsg"
                    )
                )

                photoFile?.let { file ->
                    if (file.exists() && file.length() > 0) {
                        val chatIdBody = preferencesManager.ownerChatId.toRequestBody("text/plain".toMediaTypeOrNull())
                        val captionBody = "📸 Forensic capture ($alertType)".toRequestBody("text/plain".toMediaTypeOrNull())
                        val fileBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("photo", file.name, fileBody)

                        telegramApi.sendPhoto(
                            token = preferencesManager.botToken,
                            chatId = chatIdBody,
                            photo = part,
                            caption = captionBody
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct Telegram dispatch failed: ${e.message}")
            }
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
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Error configuring lock screen flags: ${e.message}")
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
