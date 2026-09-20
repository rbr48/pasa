package com.izhaanintellect.pasa.commands

import android.content.Context
import android.provider.CallLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves recent call history from the device.
 */
@Singleton
class CallLogCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/call_log"
    override val description = "View recent call history"
    override val usage = "/call_log [count]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val limit = args.firstOrNull()?.toIntOrNull()?.coerceIn(1, 30) ?: 15

        return try {
            val calls = mutableListOf<String>()
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.CACHED_NAME
            )

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)
                var count = 0

                while (it.moveToNext() && count < limit) {
                    val number = it.getString(numIdx) ?: "Unknown"
                    val callType = it.getInt(typeIdx)
                    val date = dateFormat.format(Date(it.getLong(dateIdx)))
                    val duration = it.getLong(durIdx)
                    val name = it.getString(nameIdx)

                    val typeIcon = when (callType) {
                        CallLog.Calls.INCOMING_TYPE -> "📥"
                        CallLog.Calls.OUTGOING_TYPE -> "📤"
                        CallLog.Calls.MISSED_TYPE -> "📵"
                        CallLog.Calls.REJECTED_TYPE -> "🚫"
                        CallLog.Calls.BLOCKED_TYPE -> "🔇"
                        else -> "📞"
                    }

                    val typeName = when (callType) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Rejected"
                        CallLog.Calls.BLOCKED_TYPE -> "Blocked"
                        else -> "Unknown"
                    }

                    val durStr = if (duration > 0) "${duration / 60}m ${duration % 60}s" else "—"
                    val displayName = if (!name.isNullOrBlank()) "<b>$name</b> " else ""

                    calls.add("${count + 1}. $typeIcon $displayName<code>$number</code>\n   $typeName · $durStr · $date")
                    count++
                }
            }

            if (calls.isEmpty()) {
                return CommandResult(
                    success = true,
                    message = "📞 <b>Call Log</b>\n━━━━━━━━━━━━━━━━━━━━\n<i>No call records found.</i>"
                )
            }

            val message = "📞 <b>Call Log</b> (last $limit)\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    calls.joinToString("\n\n")

            CommandResult(success = true, message = message)
        } catch (e: SecurityException) {
            CommandResult(
                success = false,
                message = "📞 <b>Call Log Error</b>\n━━━━━━━━━━━━━━━━━━━━\n⚠️ READ_CALL_LOG permission not granted."
            )
        } catch (e: Exception) {
            CommandResult(
                success = false,
                message = "📞 <b>Call Log Error</b>\n━━━━━━━━━━━━━━━━━━━━\n❌ ${e.message}"
            )
        }
    }
}
