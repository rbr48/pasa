package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * `/screenrecord` — Record screen as silent MP4 video (Device Owner mode only).
 *
 * Uses Android's built-in `screenrecord` binary via shell command.
 * Only available when Device Owner mode is enabled via `/device_owner`.
 *
 * Usage:
 *   /screenrecord              # Record 15 seconds
 *   /screenrecord 30           # Record 30 seconds (max 60)
 *
 * Output:
 *   - MP4 video file
 *   - Bitrate: 8 Mbps (adjustable)
 *   - Size: ~8-15 MB per minute at 8 Mbps
 *
 * Requirements:
 *   - Device Owner mode enabled (/device_owner)
 *   - Android API 21+
 */
@Singleton
class ScreenRecordCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/screenrecord"
    override val description = "Record screen as MP4 (Device Owner mode)"
    override val usage = "/screenrecord [seconds=15]"

    companion object {
        private const val TAG = "PASA_ScreenRecordCmd"
        private const val MIN_SECONDS = 5
        private const val MAX_SECONDS = 60
        private const val DEFAULT_SECONDS = 15
        private const val BITRATE_KBPS = 8000  // 8 Mbps
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        // Validate Device Owner mode
        if (!isDeviceOwner()) {
            return CommandResult(
                success = false,
                message = "❌ Screen recording requires Device Owner mode.\n\n" +
                        "Enable with: <code>/device_owner</code>\n\n" +
                        "Device Owner must be provisioned via ADB:\n" +
                        "<code>adb shell dpm set-device-owner ...</code>"
            )
        }

        val durationSeconds = args.firstOrNull()?.toIntOrNull()?.coerceIn(MIN_SECONDS, MAX_SECONDS) ?: DEFAULT_SECONDS
        val outputFile = File(context.cacheDir, "screenrecord_${System.currentTimeMillis()}.mp4")

        Log.i(TAG, "Starting screen recording: ${durationSeconds}s to ${outputFile.absolutePath}")

        return withTimeoutOrNull((durationSeconds + 10) * 1000L) {
            try {
                recordScreenViaShell(outputFile, durationSeconds)

                if (outputFile.exists() && outputFile.length() > 0) {
                    val sizeMB = String.format("%.1f", outputFile.length() / 1024.0 / 1024.0)
                    Log.i(TAG, "Screen recording successful: $sizeMB MB")

                    CommandResult(
                        success = true,
                        message = "🎬 Screen recorded (${durationSeconds}s, $sizeMB MB)",
                        videoFile = outputFile
                    )
                } else {
                    Log.e(TAG, "Recording produced empty or non-existent file")

                    CommandResult(
                        success = false,
                        message = "❌ Screen recording produced an empty file.\n\n" +
                                "<i>Possible causes:</i>\n" +
                                "• Device is locked (some OEMs restrict screenrecord while locked)\n" +
                                "• Storage is full\n" +
                                "• Shell permissions issue"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Screen recording failed", e)

                CommandResult(
                    success = false,
                    message = "❌ Screen recording failed: ${e.localizedMessage ?: "Unknown error"}\n\n" +
                            "<i>Error: ${e.javaClass.simpleName}</i>"
                )
            }
        } ?: CommandResult(
            success = false,
            message = "❌ Screen recording timed out after ${durationSeconds + 10} seconds."
        )
    }

    /**
     * Execute the screenrecord shell command.
     *
     * Command format:
     *   screenrecord --size 720x1280 --bit-rate 8000000 --time-limit <seconds> <filepath>
     *
     * Throws IOException if the process exits with non-zero code.
     */
    private suspend fun recordScreenViaShell(outputFile: File, durationSeconds: Int) {
        // Construct shell command
        val command = arrayOf(
            "sh",
            "-c",
            "screenrecord --size 720x1280 --bit-rate ${BITRATE_KBPS * 1000} " +
                    "--time-limit $durationSeconds ${outputFile.absolutePath}"
        )

        Log.i(TAG, "Executing shell command: ${command.joinToString(" ")}")

        val process = Runtime.getRuntime().exec(command)

        // Wait for process completion
        val completed = process.waitFor(durationSeconds + 10L, TimeUnit.SECONDS)
        if (!completed) {
            process.destroy()
            throw TimeoutException("screenrecord did not complete within ${durationSeconds + 10} seconds")
        }

        val exitCode = process.exitValue()
        if (exitCode != 0) {
            val errorText = process.errorStream.bufferedReader().readText()
            throw RuntimeException("screenrecord exited with code $exitCode: $errorText")
        }

        Log.i(TAG, "screenrecord completed successfully (exit code 0)")
    }

    /**
     * Check if this app is provisioned as Device Owner.
     */
    private fun isDeviceOwner(): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.isDeviceOwnerApp(context.packageName) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Device Owner status", e)
            false
        }
    }
}
