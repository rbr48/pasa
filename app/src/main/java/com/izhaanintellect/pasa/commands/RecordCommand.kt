package com.izhaanintellect.pasa.commands

import android.util.Log
import com.izhaanintellect.pasa.audio.AudioRecorderManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles ambient audio recording.
 */
@Singleton
class RecordCommand @Inject constructor(
    private val audioRecorderManager: AudioRecorderManager
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
        if (args.firstOrNull()?.lowercase() == "stop") {
            audioRecorderManager.stopRecording()
            return CommandResult(success = true, message = "⏹️ Audio recording stopped.")
        }

        val durationSeconds = (args.firstOrNull()?.toIntOrNull() ?: DEFAULT_DURATION)
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
