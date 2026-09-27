package com.izhaanintellect.pasa.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.izhaanintellect.pasa.bot.SendLocationRequest
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCameraManager
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.commands.ScreenGuardCommand
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.databinding.ActivityScreenGuardBinding
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.network.PasaBackendApi
import com.izhaanintellect.pasa.util.SecurityActivityLauncher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

/**
 * Security Activity that activates display standby protocol,
 * blacks out the screen, silences audio, and captures silent photos upon screen contact.
 */
@AndroidEntryPoint
class ScreenGuardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScreenGuardBinding

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var pasaBackendApi: PasaBackendApi
    @Inject lateinit var cameraManager: StealthCameraManager

    private var previousRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    private var lastTouchAlertTime: Long = 0L
    private var secretTapCount: Int = 0
    private var lastSecretTapTime: Long = 0L
    private var volumeUpEscapeCount: Int = 0
    private var lastVolumeUpTime: Long = 0L

    companion object {
        const val ACTION_DISMISS_SCREEN_GUARD = "com.izhaanintellect.pasa.ACTION_DISMISS_SCREEN_GUARD"
        private const val TAG = "PASA_ScreenGuard"

        fun createIntent(context: Context): Intent {
            return Intent(context, ScreenGuardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_DISMISS_SCREEN_GUARD) {
                Log.i(TAG, "Received remote wake broadcast. Exiting screen guard mode.")
                exitScreenGuard()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            configureImmersiveBlackout()

            // Enter Lock Task mode to suppress power menu, home, and recents buttons
            try {
                startLockTask()
                Log.i(TAG, "Entered Lock Task mode for screen guard")
            } catch (e: Exception) {
                Log.w(TAG, "Could not enter Lock Task mode: ${e.message}")
            }

            binding = ActivityScreenGuardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            preferencesManager.isFakeShutdownActive = true

            // Trap back button
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Completely ignore back button during screen guard
                    Log.d(TAG, "Back button pressed during screen guard - ignored")
                }
            })

            // Register wake receiver
            val filter = IntentFilter(ACTION_DISMISS_SCREEN_GUARD)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(wakeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(wakeReceiver, filter)
            }

            // Silence ringer and media (safely without crashing if DND access is not granted)
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager != null) {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || nm?.isNotificationPolicyAccessGranted == true) {
                        previousRingerMode = audioManager.ringerMode
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    } else {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                        audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Could not set silent mode: ${e.message}")
            }

            // Phase 1: Show authentic Android Shutdown spinner for 2.2 seconds
            binding.layoutShutdownDialog.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (!isFinishing && !isDestroyed) {
                        // Phase 2: Complete Blackout
                        binding.layoutShutdownDialog.visibility = View.GONE
                        dimScreenToBlack()
                        Log.i(TAG, "Screen guard blackout sequence initiated")
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Blackout transition warning: ${e.message}")
                }
            }, 2200)

            // Touch capture listener on the screen: trigger on any physical touch contact (press, tap, swipe)
            binding.viewBlackout.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    v.performClick()
                    handleScreenTouchInteraction()
                }
                true
            }

            binding.flRoot.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    v.performClick()
                    handleScreenTouchInteraction()
                }
                true
            }

            // Emergency secret wake zone (4 taps in top right corner within 3 seconds)
            binding.viewSecretWakeTap.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastSecretTapTime > 3000) {
                    secretTapCount = 0
                }
                lastSecretTapTime = now
                secretTapCount++

                if (secretTapCount >= 4) {
                    Toast.makeText(this, "Emergency Wake Triggered", Toast.LENGTH_SHORT).show()
                    exitScreenGuard()
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error in ScreenGuardActivity.onCreate", e)
            finish()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            handleScreenTouchInteraction()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        configureImmersiveBlackout()
        if (binding.layoutShutdownDialog.visibility != View.VISIBLE) {
            dimScreenToBlack()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && preferencesManager.isFakeShutdownActive) {
            // System dialog or notification appeared — dismiss it and reclaim focus
            try {
                @Suppress("DEPRECATION")
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            } catch (_: Exception) {}
            // Re-apply immersive mode
            configureImmersiveBlackout()
        }
    }

    override fun onPause() {
        super.onPause()
        if (preferencesManager.isFakeShutdownActive && !isFinishing) {
            Log.w(TAG, "ScreenGuardActivity paused while still active — re-launching")
            try {
                val relaunchIntent = ScreenGuardActivity.createIntent(applicationContext)
                com.izhaanintellect.pasa.util.SecurityActivityLauncher.launch(
                    context = applicationContext,
                    intent = relaunchIntent,
                    notificationId = 2003,
                    notificationTitle = "System Power Management",
                    notificationText = "Display standby protocol active",
                    wakeScreen = false,
                    ongoing = true,
                    silentNotification = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-launch screen guard: ${e.message}")
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            if (now - lastVolumeUpTime > 3000L) {
                volumeUpEscapeCount = 0
            }
            lastVolumeUpTime = now
            volumeUpEscapeCount++
            Log.d(TAG, "Hardware emergency escape sequence: Volume Up pressed ($volumeUpEscapeCount/4)")
            if (volumeUpEscapeCount >= 4) {
                Log.w(TAG, "Hardware emergency escape sequence triggered (4x Volume Up). Exiting screen guard.")
                Toast.makeText(this, "Emergency Wake Triggered", Toast.LENGTH_SHORT).show()
                exitScreenGuard()
                return true
            }
            return true
        }

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                Log.d(TAG, "Suppressed hardware key event during screen guard: ${event.keyCode}")
                return true // Silently consume hardware button event without showing volume HUD
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_HEADSETHOOK -> return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun dimScreenToBlack() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 0.001f // Minimum brightness
        window.attributes = layoutParams
    }

    private fun handleScreenTouchInteraction() {
        val now = System.currentTimeMillis()
        // Rate limit forensic alerts to once every 15 seconds
        if (now - lastTouchAlertTime < 15000) return
        lastTouchAlertTime = now

        Log.w(TAG, "Screen touch detected in screen guard mode! Dispatching alert & photo.")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch GPS location immediately
                val loc = locationTracker.getCurrentLocation()
                val locMsg = if (loc != null) {
                    "📍 <b>Location:</b> <a href=\"https://www.google.com/maps?q=${loc.latitude},${loc.longitude}\">${loc.latitude}, ${loc.longitude}</a>\n\n"
                } else ""

                // 2. Capture front camera photo
                var photoFile: File? = null
                try {
                    photoFile = cameraManager.capturePhoto(useFrontCamera = true)
                } catch (e: Exception) {
                    Log.w(TAG, "Direct cameraManager capture failed: ${e.message}")
                }

                if (photoFile == null || !photoFile.exists() || photoFile.length() == 0L) {
                    Log.i(TAG, "Attempting photo capture via StealthCaptureBridge")
                    val captureResult = StealthCaptureBridge.capturePhoto(applicationContext, useFront = true, timeoutMs = 8000L)
                    photoFile = captureResult.file
                }

                // 3. Dispatch deception alert to owner
                val alertMsg = "⚠️ <b>Someone touched or tapped the phone screen while display standby was active!</b>\n" +
                        "The device screen is blacked out and appears powered off to the perpetrator.\n\n" +
                        locMsg +
                        "📸 <i>Silent front-camera mugshot attached below.</i>"

                dispatchDeceptionAlert(
                    alertType = "SCREEN_GUARD_TOUCH",
                    alertMessage = alertMsg,
                    latVal = loc?.latitude,
                    lngVal = loc?.longitude,
                    photoFile = photoFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in handleScreenTouchInteraction", e)
            }
        }
    }

    private suspend fun dispatchDeceptionAlert(
        alertType: String,
        alertMessage: String,
        latVal: Double?,
        lngVal: Double?,
        photoFile: File?
    ) {
        // Direct Telegram dispatch (Strategy 1: Zero-Storage, zero server media persistence)
        if (!preferencesManager.botToken.isNullOrBlank() && preferencesManager.ownerChatIdLong != 0L) {
            try {
                val locMsg = if (latVal != null && lngVal != null) {
                    "\n📍 <b>GPS Pin:</b> <a href=\"https://www.google.com/maps?q=$latVal,$lngVal\">$latVal, $lngVal</a>"
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

                if (latVal != null && lngVal != null) {
                    try {
                        telegramApi.sendLocation(
                            token = preferencesManager.botToken,
                            request = SendLocationRequest(
                                chatId = preferencesManager.ownerChatIdLong,
                                latitude = latVal,
                                longitude = lngVal
                            )
                        )
                    } catch (locErr: Exception) {
                        Log.w(TAG, "Failed to send direct location pin: ${locErr.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct Telegram dispatch failed: ${e.message}")
            } finally {
                // Immediately shred local forensic photo after dispatch
                try {
                    photoFile?.let { if (it.exists()) it.delete() }
                } catch (_: Exception) {}
            }
        }
    }

    private fun exitScreenGuard() {
        Log.i(TAG, "exitScreenGuard called - restoring device state")
        preferencesManager.isFakeShutdownActive = false

        // Cancel high-priority alert notification
        SecurityActivityLauncher.dismissNotification(this, ScreenGuardCommand.NOTIFICATION_ID)

        // Restore brightness
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams

        // Acquire hardware bright wake lock so physical AMOLED/LCD panel turns back on
        SecurityActivityLauncher.wakeScreen(this)

        // Restore ringer mode
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.ringerMode = previousRingerMode
        } catch (_: Exception) {}

        // If lost mode is NOT active, ensure AlertMessageActivity is dismissed as well
        if (!preferencesManager.isLostModeActive) {
            try {
                val dismissLostModeIntent = Intent(AlertMessageActivity.ACTION_DISMISS_LOST_MODE).apply {
                    setPackage(packageName)
                }
                sendBroadcast(dismissLostModeIntent)
            } catch (_: Exception) {}
        }

        // Exit Lock Task mode
        try {
            stopLockTask()
            Log.i(TAG, "Stopped Lock Task mode during exitScreenGuard")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping lock task: ${e.message}")
        }

        // Direct user back to Home launcher if lost mode is not active
        if (!preferencesManager.isLostModeActive) {
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            } catch (_: Exception) {}
        }

        finish()
    }

    private fun configureImmersiveBlackout() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }

            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )

            // Hide system bars completely
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error configuring immersive blackout: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(wakeReceiver)
        } catch (_: Exception) {}
    }
}
