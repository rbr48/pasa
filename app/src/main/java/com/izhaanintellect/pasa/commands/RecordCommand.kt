package com.izhaanintellect.pasa.commands

import android.util.Log
import com.izhaanintellect.pasa.audio.AudioRecorderManager
import com.izhaanintellect.pasa.security.AuthManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles ambient audio recording (requires authentication).
 */
@Singleton
class RecordCommand @Inject constructor(
    private val audioRecorderManager: AudioRecorderManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/record"
    override val description = "Record ambient microphone audio"
    override val usage = "/record <seconds> | /record stop"

    companion object {
        private const val TAG = "PASA_Record"
        private const val DEFAULT_DURATION = 30
        private const val MAX_DURATION = 300 // 5 min
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        // If master password was provided as first argument, consume it; otherwise parse parameters directly
        val effectiveArgs = if (args.isNotEmpty() && authManager.hasMasterPassword() && authManager.verifyMasterPassword(args[0])) {
            args.drop(1)
        } else {
            args
        }

        val sub = effectiveArgs.firstOrNull()?.lowercase()
        if (sub == "stop") {
            audioRecorderManager.stopRecording()
            return CommandResult(success = true, message = "⏹️ Audio recording stopped.")
        }

        val durationSeconds = (effectiveArgs.firstOrNull()?.toIntOrNull() ?: DEFAULT_DURATION)
            .coerceIn(1, MAX_DURATION)

        Log.i(TAG, "Recording audio for ${durationSeconds}s")

        val audioFile = audioRecorderManager.record(durationSeconds)

        return if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
            CommandResult(
                success = true,
                message = "🎙️ Audio recorded (${durationSeconds}s, ${audioFile.length() / 1024} KB)",
                audioFile = audioFile
            )
        } else {
            CommandResult(
                success = false,
                message = "❌ Failed to record audio. Microphone may be in use or permission restricted."
            )
        }
    }
}
