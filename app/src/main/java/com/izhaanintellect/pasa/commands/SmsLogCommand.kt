package com.izhaanintellect.pasa.commands

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves recent SMS messages from the device inbox.
 */
@Singleton
class SmsLogCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/sms_log"
    override val description = "View recent SMS messages"
    override val usage = "/sms_log [count] [keyword]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val countArg = args.firstOrNull { it.toIntOrNull() != null }?.toIntOrNull()
        val query = args.firstOrNull { it.toIntOrNull() == null }?.trim()?.lowercase()
        val limit = (countArg ?: if (query != null) 30 else 20).coerceIn(1, 50)

        return try {
            val messages = mutableListOf<String>()
            val projection = arrayOf("address", "body", "date", "type")

            val cursor = context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                projection,
                null,
                null,
                "date DESC"
            )

            cursor?.use {
                val addrIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")

                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)
                var count = 0

                while (it.moveToNext() && count < limit) {
                    val address = it.getString(addrIdx) ?: "Unknown"
                    val body = it.getString(bodyIdx) ?: ""

                    if (query != null && !address.lowercase().contains(query) && !body.lowercase().contains(query)) {
                        continue
                    }

                    val date = dateFormat.format(Date(it.getLong(dateIdx)))

                    // Truncate long messages and hide PASA commands
                    val preview = if (body.startsWith("PASA ", ignoreCase = true)) {
                        "<i>[PASA command — hidden]</i>"
                    } else {
                        body.take(120).replace("<", "&lt;").replace(">", "&gt;") +
                                if (body.length > 120) "…" else ""
                    }

                    messages.add("${count + 1}. 📱 <code>$address</code>\n   $preview\n   <i>$date</i>")
                    count++
                }
            }

            if (messages.isEmpty()) {
                val note = if (query != null) " matching \"$query\"" else ""
                return CommandResult(
                    success = true,
                    message = "💬 <b>SMS Inbox</b>\n━━━━━━━━━━━━━━━━━━━━\n<i>No messages found$note.</i>"
                )
            }

            val header = if (query != null) "💬 <b>SMS Search: \"$query\"</b> (found ${messages.size})" else "💬 <b>SMS Inbox</b> (last ${messages.size})"
            var message = "$header\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    messages.joinToString("\n\n")

            if (message.length > 3900) {
                message = message.take(3850) + "\n\n<i>…[Truncated to fit Telegram message limit]</i>"
            }

            CommandResult(success = true, message = message)
        } catch (e: SecurityException) {
            CommandResult(
                success = false,
                message = "💬 <b>SMS Log Error</b>\n━━━━━━━━━━━━━━━━━━━━\n⚠️ READ_SMS permission not granted."
            )
        } catch (e: Exception) {
            CommandResult(
                success = false,
                message = "💬 <b>SMS Log Error</b>\n━━━━━━━━━━━━━━━━━━━━\n❌ ${e.message}"
            )
        }
    }
}
