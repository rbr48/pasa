package com.izhaanintellect.pasa.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * AccessibilityService for covert screen capture.
 *
 * This service runs in the background (after user enables it in Accessibility Settings)
 * and captures screenshots without any popups, notifications, or user interaction.
 */
class AccessibilityScreenCaptureService : AccessibilityService() {

    companion object {
        private const val TAG = "PASA_A11yScreenCapture"
        private val screenshotMutex = Mutex()

        @Volatile
        var instance: AccessibilityScreenCaptureService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "AccessibilityScreenCaptureService connected - ready for screenshot capture")
        try {
            val info = serviceInfo ?: android.accessibilityservice.AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = info.flags or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            serviceInfo = info
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure dynamic serviceInfo: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val prefs = com.izhaanintellect.pasa.data.PreferencesManager(applicationContext)
            if (prefs.isLostModeActive) {
                val pkg = event?.packageName?.toString() ?: ""
                val cls = event?.className?.toString() ?: ""
                // Intercept SystemUI / notification panel / launcher during Lost Mode
                if (pkg == "com.android.systemui" || pkg.contains("launcher") ||
                    cls.contains("NotificationShade") || cls.contains("QuickSettings") ||
                    cls.contains("StatusBar") || cls.contains("Recents")
                ) {
                    dismissNotificationShade()
                    val lostModeIntent = com.izhaanintellect.pasa.ui.AlertMessageActivity.createIntent(
                        context = applicationContext,
                        message = prefs.lostModeMessage.ifBlank { "Please return this device to its owner." },
                        enforcePin = true
                    ).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    startActivity(lostModeIntent)
                }
            }
        } catch (_: Exception) {}
    }

    fun dismissNotificationShade(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to dismiss notification shade: ${e.message}")
            false
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AccessibilityScreenCaptureService interrupted (user disabled or system killed)")
    }

    /**
     * Capture the current screen as a PNG file.
     * Requires Android 11 (API 30)+ for AccessibilityService.takeScreenshot.
     */
    fun takeScreenshotInternal(outputDir: File): File? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.w(TAG, "takeScreenshot requires Android 11 (API 30)+")
            return null
        }

        // Serialize screenshot requests to avoid ERROR_TAKE_SCREENSHOT_INTERVAL_RIGID
        return runBlocking {
            screenshotMutex.withLock {
                takeScreenshotInternalLocked(outputDir)
            }
        }
    }

    private fun takeScreenshotInternalLocked(outputDir: File): File? {
        return try {
            val latch = CountDownLatch(1)
            var capturedBitmap: Bitmap? = null

            // Use dedicated background executor to prevent main thread looper deadlocks
            val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
            try {
                // Post takeScreenshot to Main Thread to guarantee thread safety across Android 11-16
                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post {
                    try {
                        takeScreenshot(
                            Display.DEFAULT_DISPLAY,
                            executor,
                            object : TakeScreenshotCallback {
                                override fun onSuccess(screenshotResult: ScreenshotResult) {
                                    try {
                                        val hardwareBuffer = screenshotResult.hardwareBuffer
                                        val colorSpace = screenshotResult.colorSpace
                                        val hwBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                                        if (hwBitmap != null) {
                                            capturedBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                            hwBitmap.recycle()
                                        }
                                        hardwareBuffer.close()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error decoding hardware buffer to bitmap", e)
                                    } finally {
                                        latch.countDown()
                                    }
                                }

                                override fun onFailure(errorCode: Int) {
                                    Log.e(TAG, "Accessibility takeScreenshot failed with error code: $errorCode")
                                    latch.countDown()
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to invoke takeScreenshot on main thread", e)
                        latch.countDown()
                    }
                }

                val awaited = latch.await(5, TimeUnit.SECONDS)
                if (!awaited) {
                    Log.w(TAG, "Accessibility takeScreenshot timed out waiting for callback")
                    return null
                }
            } finally {
                executor.shutdown()
            }

            val bitmap = capturedBitmap ?: return null
            val timestamp = System.currentTimeMillis()
            val outputFile = File(outputDir, "screenshot_$timestamp.jpg")

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()

            Log.i(TAG, "Screenshot captured: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screenshot", e)
            null
        }
    }
}
