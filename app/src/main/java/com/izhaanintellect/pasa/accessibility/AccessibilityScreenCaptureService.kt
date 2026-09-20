package com.izhaanintellect.pasa.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * AccessibilityService for covert screen capture.
 *
 * This service runs in the background (after user enables it in Accessibility Settings)
 * and captures screenshots without any popups, notifications, or user interaction.
 *
 * Usage:
 * 1. User manually enables: Settings > Accessibility > PASA Screenshot Service > ON
 * 2. Commands call ScreenshotManager.captureScreenshot()
 * 3. Service captures frame silently and delivers file
 *
 * Lifecycle:
 * - onServiceConnected() when user enables in Settings
 * - onAccessibilityEvent() triggered by system events (not used for capture)
 * - onInterrupt() when user disables or system kills service
 */
class AccessibilityScreenCaptureService : AccessibilityService() {

    companion object {
        private const val TAG = "PASA_A11yScreenCapture"
    }

    override fun onServiceConnected() {
        Log.i(TAG, "AccessibilityScreenCaptureService connected - ready for screenshot capture")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't react to accessibility events; screenshots are triggered on-demand
        // by ScreenshotManager.captureScreenshot()
    }

    override fun onInterrupt() {
        Log.w(TAG, "AccessibilityScreenCaptureService interrupted (user disabled or system killed)")
    }

    /**
     * Capture the current screen as a PNG bitmap.
     *
     * This is called by ScreenshotManager via reflection/callback pattern.
     * Returns a Bitmap if successful, null otherwise.
     */
    fun takeScreenshotInternal(outputDir: File): File? {
        return try {
            // takeScreenshot() requires API 28+
            // It's called asynchronously, so we use a blocking approach with timeout
            val bitmap = takeScreenshot() ?: return null

            // Save bitmap to PNG file
            val timestamp = System.currentTimeMillis()
            val outputFile = File(outputDir, "screenshot_$timestamp.png")

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                bitmap.recycle()
            }

            Log.i(TAG, "Screenshot captured: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screenshot", e)
            null
        }
    }
}
