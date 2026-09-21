package com.izhaanintellect.pasa.camera

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.izhaanintellect.pasa.accessibility.AccessibilityScreenCaptureService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages covert screenshot capture via AccessibilityService.
 *
 * This singleton coordinates with AccessibilityScreenCaptureService to capture
 * screenshots without any popups or notifications. Screenshots are stored in
 * the app's private files directory.
 */
@Singleton
class ScreenshotManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PASA_ScreenshotMgr"
        private const val A11Y_SERVICE_CLASS = "com.izhaanintellect.pasa.accessibility.AccessibilityScreenCaptureService"
    }

    /**
     * Capture a single screenshot.
     *
     * Returns a File if successful, null if:
     * - AccessibilityService is not enabled
     * - Service fails to capture
     * - Timeout occurs (5 seconds)
     */
    suspend fun captureScreenshot(): File? {
        // 1. Check if AccessibilityService is enabled or instance is active
        if (AccessibilityScreenCaptureService.instance == null && !isAccessibilityServiceEnabled()) {
            Log.w(TAG, "Screenshot failed: AccessibilityService not enabled")
            return null
        }

        // 2. Create output directory
        val screenshotDir = File(context.filesDir, "screenshots").apply {
            if (!exists()) mkdirs()
        }

        // 3. Attempt capture via service with timeout
        return withTimeoutOrNull(7000L) {
            try {
                // Wait briefly if service is enabled but instance is still initializing
                var a11yService = AccessibilityScreenCaptureService.instance
                var attempts = 0
                while (a11yService == null && attempts < 10) {
                    kotlinx.coroutines.delay(200L)
                    a11yService = AccessibilityScreenCaptureService.instance
                    attempts++
                }

                if (a11yService == null) {
                    Log.w(TAG, "AccessibilityScreenCaptureService instance not connected after wait")
                    return@withTimeoutOrNull null
                }

                val result = a11yService.takeScreenshotInternal(screenshotDir)
                (result)?.takeIf { it.exists() && it.length() > 0 }.also { file ->
                    if (file != null) {
                        Log.i(TAG, "Screenshot captured: ${file.absolutePath} (${file.length()} bytes)")
                    } else {
                        Log.w(TAG, "takeScreenshotInternal returned null or empty file")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing screenshot", e)
                null
            }
        }.also { file ->
            if (file == null) {
                Log.e(TAG, "captureScreenshot returned null (timeout or error)")
            }
        }
    }

    /**
     * Check if PASA's AccessibilityService is enabled in system settings.
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        // Direct instance check - if service is running in memory, it's definitely enabled!
        if (AccessibilityScreenCaptureService.instance != null) {
            return true
        }

        try {
            // Method 1: Check via AccessibilityManager getEnabledAccessibilityServiceList
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
            if (am != null) {
                val enabledServicesList = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                for (service in enabledServicesList) {
                    val serviceInfo = service.resolveInfo?.serviceInfo
                    if (serviceInfo != null &&
                        serviceInfo.packageName == context.packageName &&
                        (serviceInfo.name == AccessibilityScreenCaptureService::class.java.name ||
                         serviceInfo.name.contains("AccessibilityScreenCaptureService"))) {
                        return true
                    }
                }
            }

            // Method 2: Check Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES string
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (!enabledServices.isNullOrBlank()) {
                val expectedCn = android.content.ComponentName(context, AccessibilityScreenCaptureService::class.java)
                val fullString = expectedCn.flattenToString()
                val shortString = expectedCn.flattenToShortString()

                val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServices)
                while (colonSplitter.hasNext()) {
                    val componentNameString = colonSplitter.next()
                    if (componentNameString.equals(fullString, ignoreCase = true) ||
                        componentNameString.equals(shortString, ignoreCase = true) ||
                        componentNameString.contains(A11Y_SERVICE_CLASS, ignoreCase = true) ||
                        componentNameString.contains("AccessibilityScreenCaptureService", ignoreCase = true)) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service status", e)
        }
        return false
    }

    /**
     * Attempt to get a reference to the running AccessibilityService instance.
     * Uses reflection to access the service instance from the system.
     */
    private fun getAccessibilityServiceInstance(): Any? {
        return try {
            // This is a simplified approach; in a real app, you might use a bound service
            // or a more robust IPC mechanism. For now, we rely on the service being
            // discoverable via the AccessibilityManager.
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            if (accessibilityManager != null) {
                // In a production implementation, you would bind to the service explicitly
                // or use a callback mechanism to communicate with it.
                // For now, assume the service is running.
                accessibilityManager
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting AccessibilityService instance", e)
            null
        }
    }

    /**
     * Get the screenshots directory.
     */
    fun getScreenshotDir(): File {
        return File(context.filesDir, "screenshots").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Clean up old screenshot files (older than 24 hours).
     */
    fun cleanupOldScreenshots() {
        val screenshotDir = getScreenshotDir()
        if (!screenshotDir.exists()) return

        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        screenshotDir.listFiles()?.forEach { file ->
            if (file.lastModified() < twentyFourHoursAgo) {
                if (file.delete()) {
                    Log.i(TAG, "Cleaned up old screenshot: ${file.name}")
                }
            }
        }
    }
}
