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

        val parts = text.split("\\s+".toRegex())
        val firstWord = parts[0].lowercase()

        val (command, args) = if (text.startsWith("/")) {
            val rawCommand = firstWord.substringBefore("@")
            Pair(rawCommand, parts.drop(1))
        } else {
            // Map natural keywords or persistent keyboard buttons
            val clean = text.lowercase()
            when {
                clean.contains("status") || clean.contains("battery") -> Pair("/status", emptyList())
                clean.contains("locate") || clean.contains("location") || clean.contains("gps") -> Pair("/locate", emptyList())
                clean.contains("siren") || clean.contains("alarm") || clean.contains("ring") -> Pair("/ring", if (parts.size > 1) parts.drop(1) else listOf("60"))
                clean.contains("photo") || clean.contains("snap") || clean.contains("selfie") -> Pair("/snap", listOf("front"))
                clean.contains("livestream") || clean.contains("stream") -> Pair("/livestream", emptyList())
                clean == "stopstream" || clean == "stop stream" -> Pair("/stopstream", emptyList())
                clean.contains("video") -> Pair("/video", listOf("front", "15"))
                clean.contains("audio") || clean.contains("mic") || clean.contains("record") -> Pair("/record", listOf("30"))
                clean == "lock" || clean.startsWith("🔒 lock") -> Pair("/lock", parts.drop(1))
                clean == "unlock" -> Pair("/unlock", emptyList())
                clean.contains("trap") -> Pair("/trap", listOf("status"))
                clean.contains("send sms") || clean.contains("sendsms") -> Pair("/sendsms", parts.drop(1))
                clean.contains("notification") || clean.contains("notif") || clean.contains("hide notif") -> Pair("/notification", parts.drop(1))
                clean.contains("sim") || clean.contains("carrier") || clean.contains("sim info") -> Pair("/sim", parts.drop(1))
                clean.contains("sim lock") || clean.contains("sim swap") -> Pair("/sim_lock", parts.drop(1))
                clean.contains("vibrate") || clean.contains("pulse") || clean.contains("sos") -> Pair("/vibrate_pulse", parts.drop(1))
                clean.contains("pattern") || clean.contains("unlock attempt") || clean.contains("unlock guard") -> Pair("/pattern_guard", parts.drop(1))
                clean.contains("firewall") || clean.contains("app firewall") || clean.contains("rat block") -> Pair("/app_firewall", parts.drop(1))
                clean.contains("battery") || clean.contains("charge") || clean.contains("drain") -> Pair("/battery_alert", parts.drop(1))
                clean.contains("tamper") || clean.contains("root") || clean.contains("debug") -> Pair("/tamper_detect", parts.drop(1))
                clean.contains("dead drop") || clean.contains("vault") || clean.contains("backup") -> Pair("/dead_drop", parts.drop(1))
                clean.contains("control panel") || clean == "menu" || clean == "help" -> Pair("/help", emptyList())
                else -> return null
            }
        }

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
