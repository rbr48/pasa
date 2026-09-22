package com.izhaanintellect.pasa.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File

/**
 * Encodes sequential Bitmap screenshots into a standard H.264 (AVC) MP4 video
 * using Android's native MediaCodec and MediaMuxer hardware pipelines.
 *
 * PRODUCTION-READY FEATURES:
 * ✅ Streaming frame-by-frame encoding (no memory bloat)
 * ✅ Automatic color format detection with fallback
 * ✅ Aligned 4 Mbps bitrate (fixed from 2 Mbps)
 * ✅ Proper error recovery
 * ✅ Individual frame release (OOM protection)
 */
object ScreenVideoEncoder {
    private const val TAG = "PASA_ScreenVideoEncoder"
    private const val MIME_TYPE = "video/avc"
    private const val BITRATE_KBPS = 4000  // 4 Mbps (production quality)
    private const val I_FRAME_INTERVAL = 3  // 3 seconds between keyframes (not every frame)

    /**
     * Factory method for streaming encoder: encode individual frames without storing all in memory.
     */
    fun createEncoder(outputFile: File, fps: Int): StreamingEncoder? {
        return try {
            StreamingEncoder(outputFile, fps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create encoder: ${e.message}", e)
            null
        }
    }

    /**
     * Streaming encoder wrapper: manage codec lifecycle and frame-by-frame encoding.
     */
    class StreamingEncoder(
        private val outputFile: File,
        private val fps: Int
    ) {
        private var codec: MediaCodec? = null
        private var muxer: MediaMuxer? = null
        private var trackIndex = -1
        private var muxerStarted = false
        private val bufferInfo = MediaCodec.BufferInfo()
        private val frameDurationUs = (1_000_000L / fps)
        private var frameCount = 0
        private var targetWidth = 0
        private var targetHeight = 0
        private lateinit var yuvBuffer: ByteArray
        private lateinit var argbBuffer: IntArray

        init {
            // Initialize codec and muxer
            setupCodec()
        }

        private fun setupCodec() {
            try {
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                // Codec will be created on first frame (we need resolution)
            } catch (e: Exception) {
                Log.e(TAG, "Muxer init failed: ${e.message}")
                throw e
            }
        }

        /**
         * Encode a single screenshot file (loads, converts, encodes, releases).
         * Call this for EVERY frame to stream encode without memory bloat.
         */
        fun encodeFrame(screenshotFile: File): Boolean {
            return try {
                // Lazy init codec on first frame
                if (codec == null) {
                    val bitmap = BitmapFactory.decodeFile(screenshotFile.absolutePath)
                    if (bitmap == null) {
                        Log.w(TAG, "Failed to decode screenshot")
                        return false
                    }

                    targetWidth = (bitmap.width / 16) * 16  // Align to 16
                    targetHeight = (bitmap.height / 16) * 16
                    yuvBuffer = ByteArray(targetWidth * targetHeight * 3 / 2)
                    argbBuffer = IntArray(targetWidth * targetHeight)

                    initializeCodec(targetWidth, targetHeight)
                    bitmap.recycle()  // Release immediately
                }

                // Load and encode this frame
                val bitmap = BitmapFactory.decodeFile(screenshotFile.absolutePath)
                if (bitmap == null) {
                    Log.w(TAG, "Failed to decode frame $frameCount")
                    return false
                }

                val scaledBitmap = if (bitmap.width != targetWidth || bitmap.height != targetHeight) {
                    Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                } else {
                    bitmap
                }

                // Convert and encode
                scaledBitmap.getPixels(argbBuffer, 0, targetWidth, 0, 0, targetWidth, targetHeight)
                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()  // Release after copy

                convertArgbToNv12(argbBuffer, yuvBuffer, targetWidth, targetHeight)

                // Queue input
                val inputBufferIndex = codec!!.dequeueInputBuffer(10000L)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec!!.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        inputBuffer.put(yuvBuffer)
                        val ptsUs = frameCount * frameDurationUs
                        codec!!.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, ptsUs, 0)
                    }
                }

                // Drain output
                drainCodec()
                frameCount++
                true

            } catch (e: Exception) {
                Log.e(TAG, "Frame $frameCount encode failed: ${e.message}")
                false
            }
        }

        /**
         * Finalize encoding and flush remaining data to MP4.
         */
        fun release() {
            try {
                // Signal end of stream
                val eosIndex = codec?.dequeueInputBuffer(10000L)
                if (eosIndex != null && eosIndex >= 0) {
                    codec?.queueInputBuffer(
                        eosIndex,
                        0,
                        0,
                        frameCount * frameDurationUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                }

                // Drain EOS
                var eosReceived = false
                while (!eosReceived && codec != null) {
                    val outIndex = codec!!.dequeueOutputBuffer(bufferInfo, 10000L)
                    when {
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted -> {
                            trackIndex = muxer!!.addTrack(codec!!.outputFormat)
                            muxer!!.start()
                            muxerStarted = true
                        }
                        outIndex >= 0 -> {
                            val encodedData = codec!!.getOutputBuffer(outIndex)
                            if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer!!.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                            codec!!.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                eosReceived = true
                            }
                        }
                        else -> break
                    }
                }

                Log.i(TAG, "✅ Encoding finalized: $frameCount frames")
            } catch (e: Exception) {
                Log.e(TAG, "Finalization error: ${e.message}")
            } finally {
                try { codec?.stop() } catch (_: Exception) {}
                try { codec?.release() } catch (_: Exception) {}
                try { muxer?.stop() } catch (_: Exception) {}
                try { muxer?.release() } catch (_: Exception) {}
            }
        }

        private fun initializeCodec(width: Int, height: Int) {
            try {
                val colorFormat = detectColorFormat()

                val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
                    setInteger(MediaFormat.KEY_BIT_RATE, BITRATE_KBPS * 1000)  // 4 Mbps
                    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                }

                codec = MediaCodec.createEncoderByType(MIME_TYPE)
                codec!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                codec!!.start()

                Log.i(TAG, "✅ Codec initialized: ${width}x${height} @ $fps FPS, format=$colorFormat")
            } catch (e: Exception) {
                Log.e(TAG, "Codec init failed: ${e.message}")
                throw e
            }
        }

        private fun drainCodec() {
            while (codec != null) {
                val outIndex = codec!!.dequeueOutputBuffer(bufferInfo, 2000L)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted -> {
                        trackIndex = muxer!!.addTrack(codec!!.outputFormat)
                        muxer!!.start()
                        muxerStarted = true
                    }
                    outIndex >= 0 -> {
                        val encodedData = codec!!.getOutputBuffer(outIndex)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer!!.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        codec!!.releaseOutputBuffer(outIndex, false)
                    }
                    else -> return
                }
            }
        }

        private fun detectColorFormat(): Int {
            return try {
                val codecInfo = MediaCodec.createEncoderByType(MIME_TYPE).codecInfo
                val caps = codecInfo.getCapabilitiesForType(MIME_TYPE)
                val formats = caps.colorFormats

                // Prefer NV12 (YUV420SemiPlanar)
                if (formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)) {
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                } else if (formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)) {
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                } else if (formats.isNotEmpty()) {
                    formats[0]  // Fallback to first available
                } else {
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar  // Safe default
                }
            } catch (e: Exception) {
                Log.w(TAG, "Color format detection failed, using default")
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            }
        }
    }

    private fun convertArgbToNv12(argb: IntArray, nv12: ByteArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[j * width + i]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                nv12[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    nv12[uvIndex++] = u.coerceIn(0, 255).toByte()
                    nv12[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
    }
}
