package com.izhaanintellect.pasa.commands

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.izhaanintellect.pasa.camera.ScreenshotManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * `/screen_burst` — Capture 5–10 rapid screen frames and compose into a grid image.
 *
 * Shows what the thief is doing over a ~10 second window by rapidly sampling the screen.
 * Frames are downscaled and composited into a single PNG grid image.
 *
 * Usage:
 *   /screen_burst              # Capture 5 frames over 10s
 *   /screen_burst 8            # Capture 8 frames over 10s
 *
 * Output:
 *   - Single PNG grid (500 KB–2 MB typical)
 *   - Sent directly to Telegram
 *
 * Timing:
 *   - 5 frames → 2.5s interval
 *   - 8 frames → 1.4s interval
 *   - 10 frames → 1.1s interval
 */
@Singleton
class ScreenBurstCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screenshotManager: ScreenshotManager
) : Command {

    override val name = "/screen_burst"
    override val description = "Capture 5–10 rapid screen frames as a grid"
    override val usage = "/screen_burst [frames=5]"

    companion object {
        private const val TAG = "PASA_ScreenBurstCmd"
        private const val MIN_FRAMES = 3
        private const val MAX_FRAMES = 10
        private const val BURST_DURATION_MS = 10000L
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val frameCount = args.firstOrNull()?.toIntOrNull()?.coerceIn(MIN_FRAMES, MAX_FRAMES) ?: 5
        val intervalMs = (BURST_DURATION_MS / frameCount).toLong()

        Log.i(TAG, "Starting screen burst: $frameCount frames, ${intervalMs}ms interval")

        val frames = mutableListOf<Bitmap>()
        var captureErrors = 0

        try {
            // Capture frames with timed intervals
            repeat(frameCount) { i ->
                try {
                    screenshotManager.captureScreenshot()?.let { file ->
                        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            frames.add(bitmap)
                            Log.i(TAG, "Frame $i/$frameCount captured")
                            // Delete temp file after loading into memory
                            file.delete()
                        } else {
                            Log.w(TAG, "Frame $i/$frameCount: BitmapFactory returned null")
                            captureErrors++
                        }
                    } ?: run {
                        Log.w(TAG, "Frame $i/$frameCount: captureScreenshot returned null")
                        captureErrors++
                    }

                    // Delay before next capture (except after last frame)
                    if (i < frameCount - 1) {
                        delay(intervalMs)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error capturing frame $i/$frameCount", e)
                    captureErrors++
                }
            }

            if (frames.isEmpty()) {
                return CommandResult(
                    success = false,
                    message = "❌ Screen burst failed: no frames captured.\n\n" +
                            "Ensure 'PASA Screenshot Service' is enabled in Settings > Accessibility."
                )
            }

            // Compose frames into grid
            val composedBitmap = composeGrid(frames, frameCount)
            val outputFile = saveComposedImage(composedBitmap)

            // Clean up frame bitmaps
            frames.forEach { it.recycle() }

            val capturedText = if (captureErrors > 0) {
                "$frameCount frames (${frameCount - captureErrors} captured, $captureErrors skipped)"
            } else {
                "$frameCount frames"
            }

            val sizeMB = String.format("%.1f", outputFile.length() / 1024.0 / 1024.0)

            return CommandResult(
                success = true,
                message = "📹 Screen burst complete: $capturedText, ${sizeMB} MB",
                photoFile = outputFile
            )

        } catch (e: Exception) {
            Log.e(TAG, "Screen burst execution error", e)

            // Clean up any allocated bitmaps
            frames.forEach { it.recycle() }

            return CommandResult(
                success = false,
                message = "❌ Screen burst failed: ${e.localizedMessage ?: "Unknown error"}\n\n" +
                        "<i>Frames captured: ${frames.size} before error</i>"
            )
        }
    }

    /**
     * Compose a list of bitmaps into a single grid image.
     *
     * Grid layout is determined by the frame count:
     * - 3-4 frames: 2×2 grid
     * - 5-6 frames: 2×3 grid
     * - 7-9 frames: 3×3 grid
     * - 10 frames: 2×5 grid
     */
    private fun composeGrid(frames: List<Bitmap>, count: Int): Bitmap {
        if (frames.isEmpty()) {
            throw IllegalArgumentException("Cannot compose grid with zero frames")
        }

        // Determine grid dimensions
        val cols = sqrt(count.toDouble()).toInt().coerceAtLeast(2)
        val rows = (count + cols - 1) / cols

        val frameWidth = frames[0].width / cols
        val frameHeight = frames[0].height / rows

        // Create output bitmap
        val result = Bitmap.createBitmap(
            frameWidth * cols,
            frameHeight * rows,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(result)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        // Draw each frame into the grid
        frames.forEachIndexed { idx, bitmap ->
            val x = (idx % cols) * frameWidth
            val y = (idx / cols) * frameHeight

            // Scale bitmap to fit grid cell
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, frameWidth, frameHeight, true)
            canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), paint)
            scaledBitmap.recycle()
        }

        Log.i(TAG, "Grid composited: ${cols}×${rows} = ${result.width}×${result.height}")
        return result
    }

    /**
     * Save a bitmap as a PNG file to the cache directory.
     */
    private fun saveComposedImage(bitmap: Bitmap): File {
        val outputDir = File(context.cacheDir, "screenshots").apply { mkdirs() }
        val outputFile = File(outputDir, "screen_burst_${System.currentTimeMillis()}.png")

        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }

        Log.i(TAG, "Composed image saved: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
        return outputFile
    }
}
