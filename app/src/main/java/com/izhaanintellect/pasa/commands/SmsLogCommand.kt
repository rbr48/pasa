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
    override val usage = "/sms_log [count]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val limit = args.firstOrNull()?.toIntOrNull()?.coerceIn(1, 20) ?: 10

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
                    val date = dateFormat.format(Date(it.getLong(dateIdx)))

                    // Truncate long messages and hide PASA commands
                    val preview = if (body.startsWith("PASA ", ignoreCase = true)) {
                        "<i>[PASA command — hidden]</i>"
                    } else {
                        body.take(80).replace("<", "&lt;").replace(">", "&gt;") +
                                if (body.length > 80) "…" else ""
                    }

                    messages.add("${count + 1}. 📱 <code>$address</code>\n   $preview\n   <i>$date</i>")
                    count++
                }
            }

            if (messages.isEmpty()) {
                return CommandResult(
                    success = true,
                    message = "💬 <b>SMS Inbox</b>\n━━━━━━━━━━━━━━━━━━━━\n<i>No messages found.</i>"
                )
            }

            val message = "💬 <b>SMS Inbox</b> (last $limit)\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    messages.joinToString("\n\n")

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
