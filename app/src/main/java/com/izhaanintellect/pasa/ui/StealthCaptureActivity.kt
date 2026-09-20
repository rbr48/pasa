package com.izhaanintellect.pasa.ui

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.databinding.ActivityStealthCaptureBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Invisible/Stealth Activity executed over lockscreen to grant CameraX a genuine TOP process state.
 *
 * Key stealth properties:
 * - PARTIAL_WAKE_LOCK: keeps CPU + camera sensor alive WITHOUT lighting up the screen
 * - Screen brightness set to 0.01f (imperceptible) if screen does briefly illuminate
 * - Shutter sound muted via AudioManager.STREAM_SYSTEM volume suppression
 * - Ringer mode temporarily set to RINGER_MODE_SILENT to suppress any OEM camera sounds
 * - All audio state fully restored on Activity completion
 * - excluded from Recents, no window animation
 */
class StealthCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStealthCaptureBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private val isFinalized = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Audio muting state
    private var audioManager: AudioManager? = null
    private var previousSystemVolume: Int = -1
    private var previousRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    private var audioMuted = false

    // Wake lock (PARTIAL — no screen turn-on)
    private var partialWakeLock: PowerManager.WakeLock? = null

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_CAMERA_FRONT = "extra_camera_front"
        const val EXTRA_DURATION = "extra_duration"

        const val MODE_PHOTO = "photo"
        const val MODE_VIDEO = "video"
        private const val TAG = "PASA_StealthAct"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        configureWindow()
        super.onCreate(savedInstanceState)

        binding = ActivityStealthCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        acquirePartialWakeLock()
        muteAudio()

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_PHOTO
        val useFront = intent.getBooleanExtra(EXTRA_CAMERA_FRONT, true)
        val duration = intent.getIntExtra(EXTRA_DURATION, 15).coerceIn(1, 60)

        Log.i(TAG, "StealthCaptureActivity started: mode=$mode, front=$useFront, duration=${duration}s")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                if (mode == MODE_VIDEO) {
                    startVideoCapture(useFront, duration)
                } else {
                    startPhotoCapture(useFront)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CameraProvider in activity", e)
                finishWithResult(null, "Camera init failure: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ── Window Configuration ─────────────────────────────────────────────────

    private fun configureWindow() {
        // Use setShowWhenLocked API (does not forcibly wake the screen)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            // Do NOT call setTurnScreenOn(true) — we use PARTIAL_WAKE_LOCK instead
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            // FLAG_TURN_SCREEN_ON intentionally omitted — PARTIAL_WAKE_LOCK is sufficient
        )

        // Dim screen to nearly black in case it does illuminate
        val lp = window.attributes
        lp.screenBrightness = 0.01f
        window.attributes = lp

        // Dismiss keyguard if already unlocked
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(this, null)
        }
    }

    // ── Wake Lock ────────────────────────────────────────────────────────────

    /**
     * Acquires a PARTIAL_WAKE_LOCK: keeps CPU and camera ISP alive without turning on the screen.
     * This is the key to truly silent capture — the screen stays off entirely.
     */
    private fun acquirePartialWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            partialWakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "pasa:stealth_partial_wake"
            )
            // 90 second max safety release
            partialWakeLock?.acquire(90_000L)
            Log.d(TAG, "Partial wake lock acquired (screen-off capture enabled)")
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire partial wake lock: ${e.message}")
        }
    }

    // ── Audio Muting ─────────────────────────────────────────────────────────

    /**
     * Silences all audio channels that could produce shutter or camera sounds:
     * - STREAM_SYSTEM (shutter click on most OEMs)
     * - RINGER_MODE_SILENT (suppresses OEM HAL-level camera sounds on some devices)
     * All state is saved and restored in unmuteAudio().
     */
    private fun muteAudio() {
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val am = audioManager ?: return

            // Save and mute STREAM_SYSTEM (shutter sound channel)
            previousSystemVolume = am.getStreamVolume(AudioManager.STREAM_SYSTEM)
            am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)

            // Save and set ringer mode to silent for OEM HAL-level suppression
            previousRingerMode = am.ringerMode
            try {
                am.ringerMode = AudioManager.RINGER_MODE_SILENT
            } catch (e: SecurityException) {
                // On Android 6+ with DND policy, we may not always be able to set ringer mode
                Log.w(TAG, "Could not set ringer mode to silent (DND policy): ${e.message}")
            }

            audioMuted = true
            Log.d(TAG, "Audio muted for stealth capture (system vol=0, ringer=silent)")
        } catch (e: Exception) {
            Log.w(TAG, "Audio muting failed: ${e.message}")
        }
    }

    /**
     * Restores all audio state to what it was before the capture.
     */
    private fun unmuteAudio() {
        if (!audioMuted) return
        try {
            val am = audioManager ?: return
            if (previousSystemVolume >= 0) {
                am.setStreamVolume(AudioManager.STREAM_SYSTEM, previousSystemVolume, 0)
            }
            try {
                am.ringerMode = previousRingerMode
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not restore ringer mode: ${e.message}")
            }
            audioMuted = false
            Log.d(TAG, "Audio restored after stealth capture")
        } catch (e: Exception) {
            Log.w(TAG, "Audio restore failed: ${e.message}")
        }
    }

    // ── Photo Capture ────────────────────────────────────────────────────────

    private fun startPhotoCapture(useFront: Boolean) {
        val provider = cameraProvider ?: run {
            finishWithResult(null, "Camera provider not ready")
            return
        }

        val cameraSelector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        if (!provider.hasCamera(cameraSelector)) {
            finishWithResult(null, "Device lacks requested camera (front=$useFront)")
            return
        }

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val photoFile = File(cacheDir, "pasa_snap_${timestamp}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // 500ms delay: AE/AF stabilization time, then capture while still muted
            mainHandler.postDelayed({
                try {
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(this),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                Log.i(TAG, "Photo captured successfully: ${photoFile.absolutePath}")
                                finishWithResult(photoFile, null)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e(TAG, "ImageCaptureException: ${exception.message}", exception)
                                finishWithResult(null, "ImageCapture error: ${exception.message} (code ${exception.imageCaptureError})")
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "takePicture exception", e)
                    finishWithResult(null, "takePicture exception: ${e.message}")
                }
            }, 500)

        } catch (e: Exception) {
            Log.e(TAG, "bindToLifecycle failed for photo", e)
            finishWithResult(null, "Camera bind error: ${e.message}")
        }
    }

    // ── Video Capture ────────────────────────────────────────────────────────

    private fun startVideoCapture(useFront: Boolean, durationSeconds: Int) {
        val provider = cameraProvider ?: run {
            finishWithResult(null, "Camera provider not ready")
            return
        }

        val cameraSelector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        if (!provider.hasCamera(cameraSelector)) {
            finishWithResult(null, "Device lacks requested camera (front=$useFront)")
            return
        }

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        // Quality cascade: HD → SD → LOWEST (ensures it works on low-end devices)
        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.HD, Quality.SD, Quality.LOWEST),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        val videoCapture = VideoCapture.withOutput(recorder)

        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, cameraSelector, preview, videoCapture)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val videoFile = File(cacheDir, "pasa_video_${timestamp}.mp4")
            val outputOptions = FileOutputOptions.Builder(videoFile).build()

            val recordingBuilder = videoCapture.output.prepareRecording(this, outputOptions)

            // Enable audio if RECORD_AUDIO is granted (captures ambient environment sound)
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                recordingBuilder.withAudioEnabled()
                Log.d(TAG, "Video recording with audio enabled")
            }

            var activeRecording: Recording? = null

            activeRecording = recordingBuilder.start(ContextCompat.getMainExecutor(this)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Log.i(TAG, "Video recording started for ${durationSeconds}s")
                        // Restore audio AFTER recording has started so we don't mute the mic
                        // (we only needed silence during the camera open phase for shutter sounds)
                        unmuteAudio()
                        mainHandler.postDelayed({
                            try {
                                activeRecording?.stop()
                            } catch (_: Exception) {}
                        }, durationSeconds * 1000L)
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            Log.e(TAG, "Video recording finalized with error: ${event.error}")
                            finishWithResult(null, "VideoCapture error code ${event.error}")
                        } else {
                            Log.i(TAG, "Video recording complete: ${videoFile.absolutePath}")
                            finishWithResult(videoFile, null)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "bindToLifecycle failed for video", e)
            finishWithResult(null, "Video bind error: ${e.message}")
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    private fun finishWithResult(file: File?, error: String?) {
        if (isFinalized.compareAndSet(false, true)) {
            unmuteAudio()
            releaseWakeLock()
            StealthCaptureBridge.notifyResult(StealthCaptureBridge.CaptureResult(file, error))
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {}
            finish()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (partialWakeLock?.isHeld == true) {
                partialWakeLock?.release()
                Log.d(TAG, "Partial wake lock released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wake lock release error: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        finishWithResult(null, "Activity destroyed before completion")
    }
}
