package com.izhaanintellect.pasa.bot

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses incoming Telegram updates into structured commands and arguments.
 */
@Singleton
class CommandParser @Inject constructor() {

    companion object {
        private const val TAG = "PASA_Parser"
    }

    data class ParsedCommand(
        val command: String,
        val args: List<String>,
        val chatId: Long,
        val senderName: String,
        val rawText: String
    )

    fun parse(update: Update): ParsedCommand? {
        val message = update.message ?: return null
        val text = message.text?.trim() ?: return null
        val chatId = message.chat.id

        if (!text.startsWith("/")) return null

        val parts = text.split("\\s+".toRegex())
        val rawCommand = parts[0].lowercase()
        // Strip bot username if invoked as /command@botname
        val command = rawCommand.substringBefore("@")
        val args = parts.drop(1)

        val senderName = listOfNotNull(message.from?.firstName, message.from?.lastName)
            .joinToString(" ")
            .ifEmpty { message.from?.username ?: "Unknown" }

        Log.d(TAG, "Parsed command '$command' with ${args.size} args from $senderName ($chatId)")

        return ParsedCommand(
            command = command,
            args = args,
            chatId = chatId,
            senderName = senderName,
            rawText = text
        )
    }
}
