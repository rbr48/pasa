package com.izhaanintellect.pasa.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class StealthVideoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PASA_Video"
    }

    private val isRecording = AtomicBoolean(false)

    suspend fun recordVideo(useFrontCamera: Boolean = true, durationSeconds: Int = 30): File? {
        if (!hasCameraPermission()) {
            Log.w(TAG, "Camera permission not granted")
            return null
        }

        if (!isRecording.compareAndSet(false, true)) {
            Log.w(TAG, "Already recording video")
            return null
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    var lifecycleOwner: ServiceLifecycleOwner? = null
                    var cameraProvider: ProcessCameraProvider? = null

                    try {
                        cameraProvider = cameraProviderFuture.get()
                        lifecycleOwner = ServiceLifecycleOwner()

                        val cameraSelector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        if (!cameraProvider.hasCamera(cameraSelector)) {
                            Log.e(TAG, "Device lacks requested camera (front=$useFrontCamera)")
                            isRecording.set(false)
                            continuation.resume(null)
                            return@addListener
                        }

                        val qualitySelector = QualitySelector.from(
                            Quality.HD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
                        )
                        val recorder = Recorder.Builder()
                            .setQualitySelector(qualitySelector)
                            .build()
                        val videoCapture = VideoCapture.withOutput(recorder)

                        cameraProvider.unbindAll()
                        lifecycleOwner.start()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            videoCapture
                        )

                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        val videoFile = File(context.cacheDir, "pasa_video_${timestamp}.mp4")
                        val outputOptions = FileOutputOptions.Builder(videoFile).build()

                        val recordingBuilder = videoCapture.output.prepareRecording(context, outputOptions)

                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            recordingBuilder.withAudioEnabled()
                        }

                        var activeRecording: Recording? = null
                        var stopJob: Job? = null

                        activeRecording = recordingBuilder.start(ContextCompat.getMainExecutor(context)) { event ->
                            when (event) {
                                is VideoRecordEvent.Start -> {
                                    Log.i(TAG, "Video recording started")
                                    stopJob = CoroutineScope(Dispatchers.Main).launch {
                                        delay(durationSeconds * 1000L)
                                        activeRecording?.stop()
                                    }
                                }
                                is VideoRecordEvent.Finalize -> {
                                    stopJob?.cancel()
                                    isRecording.set(false)
                                    if (event.hasError()) {
                                        Log.e(TAG, "Video recording failed with error: ${event.error}")
                                        cleanup(lifecycleOwner, cameraProvider)
                                        continuation.resume(null)
                                    } else {
                                        Log.i(TAG, "Video recording finalized: ${videoFile.absolutePath}")
                                        cleanup(lifecycleOwner, cameraProvider)
                                        continuation.resume(videoFile)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to initialize CameraX for video", e)
                        isRecording.set(false)
                        cleanup(lifecycleOwner, cameraProvider)
                        continuation.resume(null)
                    }
                }, ContextCompat.getMainExecutor(context))

                continuation.invokeOnCancellation {
                    isRecording.set(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Video recording error", e)
            isRecording.set(false)
            null
        }
    }

    private fun cleanup(lifecycleOwner: ServiceLifecycleOwner?, provider: ProcessCameraProvider?) {
        try {
            lifecycleOwner?.stop()
            provider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up video camera resources", e)
        }
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private class ServiceLifecycleOwner : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)

        init {
            lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        }

        fun start() {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry
    }
}
