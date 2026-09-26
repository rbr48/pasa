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
        val rawText: String,
        val callbackQueryId: String? = null,
        val messageId: Long? = null
    )

    fun parse(update: Update): ParsedCommand? {
        var callbackQueryId: String? = null
        val messageId = update.message?.messageId ?: update.callbackQuery?.message?.messageId
        val (text, chatId, senderName) = when {
            update.message != null -> {
                val message = update.message
                val raw = message.text?.trim() ?: return null
                val name = listOfNotNull(message.from?.firstName, message.from?.lastName)
                    .joinToString(" ")
                    .ifEmpty { message.from?.username ?: "Unknown" }
                Triple(raw, message.chat.id, name)
            }
            update.callbackQuery != null -> {
                val cb = update.callbackQuery
                callbackQueryId = cb.id
                var raw = cb.data?.trim() ?: return null
                if (raw.startsWith("dev_cmd:")) {
                    raw = "/" + raw.removePrefix("dev_cmd:").replace(":", " ")
                } else if (raw.startsWith("cmd:")) {
                    raw = "/" + raw.removePrefix("cmd:").replace(":", " ")
                } else if (raw.startsWith("menu:") || raw.startsWith("wizard:")) {
                    // Preserve interactive hub/wizard callback routing
                } else if (!raw.startsWith("/")) {
                    raw = "/$raw"
                }
                val name = listOfNotNull(cb.from.firstName, cb.from.lastName)
                    .joinToString(" ")
                    .ifEmpty { cb.from.username ?: "Unknown" }
                val cId = cb.message?.chat?.id ?: cb.from.id
                Triple(raw, cId, name)
            }
            else -> return null
        }

        val parts = text.split("\\s+".toRegex())
        val firstWord = parts[0].lowercase()

        val (command, args) = if (text.startsWith("menu:") || text.startsWith("wizard:")) {
            Pair(text, emptyList())
        } else if (text.startsWith("/")) {
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
                clean.contains("screen") && (clean.contains("record") || clean.contains("video")) -> Pair("/screenrecord", listOf("15"))
                clean.contains("screen") || clean.contains("screenshot") -> Pair("/screenshot", emptyList())
                clean.contains("livestream") || clean.contains("stream") -> {
                    if (clean.contains("diag") || clean.contains("debug")) {
                        Pair("/livestream_diag", emptyList())
                    } else {
                        Pair("/livestream", emptyList())
                    }
                }
                clean == "stopstream" || clean == "stop stream" -> Pair("/stopstream", emptyList())
                clean.contains("video") -> Pair("/video", listOf("front", "15"))
                clean.contains("audio") || clean.contains("mic") || clean.contains("record") -> Pair("/record", listOf("30"))
                clean.startsWith("lock app ") || (clean.startsWith("lock ") && (clean.contains("gallery") || clean.contains("phone") || clean.contains("files"))) -> Pair("/lock_app", parts.drop(1).filter { it != "app" })
                clean.startsWith("unlock app ") || (clean.startsWith("unlock ") && (clean.contains("gallery") || clean.contains("phone") || clean.contains("files"))) -> Pair("/unlock_app", parts.drop(1).filter { it != "app" })
                clean == "lock" || clean == "🔒 lock" || clean == "🔒 instant lock" || clean == "instant lock" || clean == "lock instant" || clean == "lock now" -> Pair("/lock", listOf("instant"))
                clean.startsWith("🔒 lock ") -> Pair("/lock", parts.drop(2))
                clean.startsWith("lock ") -> Pair("/lock", parts.drop(1))
                clean == "unlock" || clean == "🔓 unlock" || clean == "unlock device" -> Pair("/unlock", emptyList())
                clean.startsWith("🔓 unlock ") -> Pair("/unlock", parts.drop(2))
                clean.startsWith("unlock ") -> Pair("/unlock", parts.drop(1))
                clean.startsWith("call ") || clean.startsWith("dial ") -> Pair("/call", parts.drop(1))
                clean == "gallery" || clean.contains("gallery latest") || clean.contains("recent photos") -> Pair("/gallery_latest", parts.drop(1).filter { it.toIntOrNull() != null })
                clean.startsWith("getfile ") || clean.startsWith("download ") -> Pair("/getfile", parts.drop(1))
                clean.startsWith("list files ") -> Pair("/list_files", parts.drop(2))
                clean.startsWith("ls ") || clean.startsWith("list_files ") -> Pair("/list_files", parts.drop(1))
                clean == "ls" || clean == "list files" || clean == "list_files" -> Pair("/list_files", emptyList())
                clean == "traps" || clean == "🛡️ traps" || clean == "trap menu" -> Pair("menu:traps_hub", emptyList())
                clean.contains("trap") -> Pair("/trap", if (parts.size > 1) parts.drop(1) else listOf("status"))
                clean.contains("send sms") || clean.contains("sendsms") -> Pair("/sendsms", parts.drop(1))
                clean.contains("notification") || clean.contains("notif") || clean.contains("hide notif") -> Pair("/notification", parts.drop(1))
                clean.contains("sim") || clean.contains("carrier") || clean.contains("sim info") -> Pair("/sim", parts.drop(1))
                clean.contains("sim lock") || clean.contains("sim swap") -> Pair("/sim_lock", parts.drop(1))
                clean.contains("vibrate") || clean.contains("pulse") || clean.contains("sos") -> Pair("/vibrate_pulse", parts.drop(1))
                clean.contains("pattern") || clean.contains("unlock attempt") || clean.contains("unlock guard") -> Pair("/pattern_guard", parts.drop(1))
                clean.contains("firewall") || clean.contains("app firewall") || clean.contains("rat block") -> Pair("/app_firewall", parts.drop(1))
                clean.contains("tamper") || clean.contains("root") || clean.contains("debug") -> Pair("/tamper_detect", parts.drop(1))
                clean.contains("dead drop") || clean.contains("vault") || clean.contains("backup") -> Pair("/dead_drop", parts.drop(1))
                clean.contains("harden boot") || clean.contains("lock recovery") -> Pair("/harden_boot", parts.drop(1))
                clean.contains("factory reset") || clean.contains("reset protection") -> Pair("/factory_reset_defense", parts.drop(1))
                clean.contains("license") || clean.contains("pro key") -> Pair("/license", parts.drop(1))
                clean.contains("sms help") || clean == "sms" || clean == "sms commands" || clean == "sms guide" -> Pair("/sms_help", emptyList())
                clean.contains("device owner") || clean.contains("device_owner") || clean.contains("knox do") -> Pair("menu:device_owner_hub", emptyList())
                clean.contains("cyber") || clean == "🛡️ cyber defense" || clean == "cyber defense" -> Pair("menu:cyber_hub", emptyList())
                clean.contains("a11y") || clean.contains("accessibility") -> Pair("/a11y_shield", parts.drop(1))
                clean.contains("anti 2g") || clean.contains("anti_2g") || clean.contains("stingray") -> Pair("/anti_2g", parts.drop(1))
                clean.contains("clipper") || clean.contains("crypto guard") -> Pair("/clipper_guard", parts.drop(1))
                clean.contains("canary") || clean.contains("ransomware") -> Pair("/canary_guard", parts.drop(1))
                clean.contains("otp guard") || clean.contains("2fa guard") || clean.contains("otp interception") -> Pair("/otp_guard", parts.drop(1))
                clean.contains("install lock") || clean.contains("sideload") -> Pair("/app_install_lock", parts.drop(1))
                clean.contains("usb autolock") -> Pair("/usb_autolock", parts.drop(1))
                clean.contains("control panel") || clean == "menu" || clean == "main menu" || clean == "dashboard" || clean.contains("hub menu") || clean.contains("console") -> Pair("menu:main", emptyList())
                clean == "message" || clean == "💬 message" || clean == "lockscreen alert" || clean == "alert message" -> Pair("menu:message", emptyList())
                clean == "help" -> Pair("/help", emptyList())
                else -> return null
            }
        }

        Log.d(TAG, "Parsed command '$command' with ${args.size} args from $senderName ($chatId)")

        return ParsedCommand(
            command = command,
            args = args,
            chatId = chatId,
            senderName = senderName,
            rawText = text,
            callbackQueryId = callbackQueryId,
            messageId = messageId
        )
    }
}
