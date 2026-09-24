package com.izhaanintellect.pasa.commands

import android.content.Context
import android.provider.CallLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves call history from the device.
 *
 * Commands:
 *   /call_log              — Paginated list (page 1, 15 per page)
 *   /call_log <page>       — View specific page (e.g. /call_log 2)
 *   /call_log export       — Download full call log as .txt document
 *   /call_log search <q>   — Search by number or contact name
 *   /call_log missed       — Show only missed calls
 */
@Singleton
class CallLogCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/call_log"
    override val description = "View call history with pagination, export, search, and filters"
    override val usage = "/call_log [page] | /call_log export | /call_log search <number> | /call_log missed"

    companion object {
        private const val PAGE_SIZE = 15
        private const val MAX_FETCH = 1000
    }

    data class CallEntry(
        val number: String,
        val name: String,
        val callType: Int,
        val date: String,
        val duration: Long
    )

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val firstArg = args.firstOrNull()?.lowercase()?.trim()

        return try {
            // Full document export
            if (firstArg == "export" || firstArg == "all" || firstArg == "full") {
                return exportCallLog()
            }

            // Missed calls filter
            if (firstArg == "missed") {
                val missed = loadCalls(query = null, limit = MAX_FETCH)
                    .filter { it.callType == CallLog.Calls.MISSED_TYPE }
                return renderPage(missed, page = 1, isSearch = true, query = "Missed")
            }

            // Keyword search
            if (firstArg == "search" || firstArg == "find") {
                val query = args.drop(1).joinToString(" ").lowercase().trim()
                if (query.isBlank()) {
                    return CommandResult(
                        success = false,
                        message = "❌ Please specify a search term.\nUsage: <code>/call_log search &lt;number&gt;</code>"
                    )
                }
                val matches = loadCalls(query = query, limit = MAX_FETCH)
                return renderPage(matches, page = 1, isSearch = true, query = query)
            }

            // Paginated list
            val page = firstArg?.toIntOrNull() ?: 1
            val all = loadCalls(query = null, limit = MAX_FETCH)
            renderPage(all, page = page, isSearch = false)

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

    private fun typeIcon(type: Int) = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "📥"
        CallLog.Calls.OUTGOING_TYPE -> "📤"
        CallLog.Calls.MISSED_TYPE  -> "📵"
        CallLog.Calls.REJECTED_TYPE -> "🚫"
        CallLog.Calls.BLOCKED_TYPE  -> "🔇"
        else -> "📞"
    }

    private fun typeName(type: Int) = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "Incoming"
        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        CallLog.Calls.MISSED_TYPE   -> "Missed"
        CallLog.Calls.REJECTED_TYPE -> "Rejected"
        CallLog.Calls.BLOCKED_TYPE  -> "Blocked"
        else -> "Unknown"
    }

    private fun loadCalls(query: String?, limit: Int): List<CallEntry> {
        val calls = mutableListOf<CallEntry>()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_NAME
        )
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, projection, null, null,
            "${CallLog.Calls.DATE} DESC"
        )
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)
        cursor?.use {
            val numIdx  = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val durIdx  = it.getColumnIndex(CallLog.Calls.DURATION)
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            while (it.moveToNext() && calls.size < limit) {
                val number = it.getString(numIdx) ?: "Unknown"
                val name   = it.getString(nameIdx) ?: ""
                if (query != null &&
                    !number.contains(query) &&
                    !name.lowercase().contains(query)
                ) continue
                val date = dateFormat.format(Date(it.getLong(dateIdx)))
                calls.add(CallEntry(number, name, it.getInt(typeIdx), date, it.getLong(durIdx)))
            }
        }
        return calls
    }

    private fun renderPage(
        calls: List<CallEntry>,
        page: Int,
        isSearch: Boolean,
        query: String = ""
    ): CommandResult {
        if (calls.isEmpty()) {
            val note = if (isSearch) " matching \"<b>$query</b>\"" else ""
            return CommandResult(
                success = true,
                message = "📞 <b>Call Log</b>\n━━━━━━━━━━━━━━━━━━━━\n<i>No call records found$note.</i>"
            )
        }

        val totalPages = maxOf(1, (calls.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val validPage  = page.coerceIn(1, totalPages)
        val startIdx   = (validPage - 1) * PAGE_SIZE
        val pageCalls  = calls.drop(startIdx).take(PAGE_SIZE)

        val sb = StringBuilder()
        if (isSearch) {
            sb.appendLine("📞 <b>Call Log: \"$query\" (${calls.size} results)</b>")
        } else {
            sb.appendLine("📞 <b>Call Log (${calls.size} total)</b> — Page $validPage of $totalPages")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        pageCalls.forEachIndexed { idx, call ->
            val num     = startIdx + idx + 1
            val durStr  = if (call.duration > 0) "${call.duration / 60}m ${call.duration % 60}s" else "—"
            val nameStr = if (call.name.isNotBlank()) "<b>${call.name}</b> " else ""
            sb.appendLine("$num. ${typeIcon(call.callType)} $nameStr<code>${call.number}</code>")
            sb.appendLine("   ${typeName(call.callType)} · $durStr · ${call.date}")
            if (idx < pageCalls.size - 1) sb.appendLine()
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        val navItems = mutableListOf<String>()
        if (validPage > 1) navItems.add("👈 <code>/call_log ${validPage - 1}</code>")
        if (validPage < totalPages) navItems.add("👉 <code>/call_log ${validPage + 1}</code>")
        if (navItems.isNotEmpty()) sb.appendLine("📄 <b>Page $validPage/$totalPages:</b> ${navItems.joinToString(" • ")}")
        sb.append("💾 Export: <code>/call_log export</code> • 📵 Missed: <code>/call_log missed</code> • 🔍 Search: <code>/call_log search &lt;number&gt;</code>")

        return CommandResult(success = true, message = sb.toString())
    }

    private fun exportCallLog(): CommandResult {
        return try {
            val all  = loadCalls(null, 5000)
            val file = File(context.cacheDir, "pasa_call_log_${System.currentTimeMillis()}.txt")
            file.printWriter().use { out ->
                out.println("================================================================================")
                out.println("PASA Sentinel — Call Log Export")
                out.println("Total Calls: ${all.size}")
                out.println("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                out.println("================================================================================")
                out.println()
                all.forEachIndexed { i, call ->
                    val displayName = if (call.name.isNotBlank()) "${call.name} (${call.number})" else call.number
                    val durStr = if (call.duration > 0) "${call.duration / 60}m ${call.duration % 60}s" else "—"
                    out.println("${i + 1}. ${typeName(call.callType)} — $displayName")
                    out.println("   Date: ${call.date} | Duration: $durStr")
                    out.println()
                }
            }
            CommandResult(
                success = true,
                message = "📞 <b>Call Log Export (${all.size} calls)</b>\n━━━━━━━━━━━━━━━━━━━━\nFull call history document attached below.",
                documentFile = file
            )
        } catch (e: Exception) {
            CommandResult(success = false, message = "❌ Failed to export call log: ${e.message}")
        }
    }
}
