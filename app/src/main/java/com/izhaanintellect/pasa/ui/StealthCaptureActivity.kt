package com.izhaanintellect.pasa.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
 * Allows silent photo capture and video recording without being blocked by Android 14-16 background restrictions.
 */
class StealthCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStealthCaptureBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private val isFinalized = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

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

    private fun configureWindow() {
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

        // Dim screen to almost completely dark to maintain stealth
        val lp = window.attributes
        lp.screenBrightness = 0.01f
        window.attributes = lp

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(this, null)
        }
    }

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

            // 400ms delay to allow camera sensor AE/AF stabilization
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
            }, 400)

        } catch (e: Exception) {
            Log.e(TAG, "bindToLifecycle failed for photo", e)
            finishWithResult(null, "Camera bind error: ${e.message}")
        }
    }

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
            var activeRecording: Recording? = null

            activeRecording = recordingBuilder.start(ContextCompat.getMainExecutor(this)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Log.i(TAG, "Video recording started for ${durationSeconds}s")
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

    private fun finishWithResult(file: File?, error: String?) {
        if (isFinalized.compareAndSet(false, true)) {
            StealthCaptureBridge.notifyResult(StealthCaptureBridge.CaptureResult(file, error))
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {}
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        finishWithResult(null, "Activity destroyed before completion")
    }
}
