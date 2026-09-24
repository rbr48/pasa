package com.izhaanintellect.pasa.commands

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves SMS messages from the device inbox.
 *
 * Commands:
 *   /sms_log               — Paginated list (page 1, 10 per page)
 *   /sms_log <page>        — View specific page (e.g. /sms_log 3)
 *   /sms_log export        — Download full inbox as .txt document
 *   /sms_log search <q>    — Search by number or message body
 */
@Singleton
class SmsLogCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/sms_log"
    override val description = "View SMS messages with pagination, export, and search"
    override val usage = "/sms_log [page] | /sms_log export | /sms_log search <keyword>"

    companion object {
        private const val PAGE_SIZE = 10
        private const val MAX_FETCH = 1000
    }

    data class SmsEntry(val address: String, val body: String, val date: String)

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val firstArg = args.firstOrNull()?.lowercase()?.trim()

        return try {
            // Full document export
            if (firstArg == "export" || firstArg == "all" || firstArg == "full") {
                return exportSms()
            }

            // Keyword search
            if (firstArg == "search" || firstArg == "find") {
                val query = args.drop(1).joinToString(" ").lowercase().trim()
                if (query.isBlank()) {
                    return CommandResult(
                        success = false,
                        message = "❌ Please specify a search term.\nUsage: <code>/sms_log search &lt;keyword&gt;</code>"
                    )
                }
                val matches = loadSms(query = query, limit = MAX_FETCH)
                return renderPage(matches, page = 1, isSearch = true, query = query)
            }

            // Paginated list
            val page = firstArg?.toIntOrNull() ?: 1
            val all = loadSms(query = null, limit = MAX_FETCH)
            renderPage(all, page = page, isSearch = false)

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

    private fun loadSms(query: String?, limit: Int): List<SmsEntry> {
        val messages = mutableListOf<SmsEntry>()
        val projection = arrayOf("address", "body", "date", "type")
        val cursor = context.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            projection, null, null, "date DESC"
        )
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)
        cursor?.use {
            val addrIdx = it.getColumnIndex("address")
            val bodyIdx = it.getColumnIndex("body")
            val dateIdx = it.getColumnIndex("date")
            while (it.moveToNext() && messages.size < limit) {
                val address = it.getString(addrIdx) ?: "Unknown"
                val body = it.getString(bodyIdx) ?: ""
                if (query != null &&
                    !address.lowercase().contains(query) &&
                    !body.lowercase().contains(query)
                ) continue
                val date = dateFormat.format(Date(it.getLong(dateIdx)))
                messages.add(SmsEntry(address, body, date))
            }
        }
        return messages
    }

    private fun renderPage(
        msgs: List<SmsEntry>,
        page: Int,
        isSearch: Boolean,
        query: String = ""
    ): CommandResult {
        if (msgs.isEmpty()) {
            val note = if (isSearch) " matching \"<b>$query</b>\"" else ""
            return CommandResult(
                success = true,
                message = "💬 <b>SMS Inbox</b>\n━━━━━━━━━━━━━━━━━━━━\n<i>No messages found$note.</i>"
            )
        }

        val totalPages = maxOf(1, (msgs.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val validPage = page.coerceIn(1, totalPages)
        val startIdx = (validPage - 1) * PAGE_SIZE
        val pageMsgs = msgs.drop(startIdx).take(PAGE_SIZE)

        val sb = StringBuilder()
        if (isSearch) {
            sb.appendLine("💬 <b>SMS Search: \"$query\" (${msgs.size} results)</b>")
        } else {
            sb.appendLine("💬 <b>SMS Inbox (${msgs.size} messages)</b> — Page $validPage of $totalPages")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        pageMsgs.forEachIndexed { idx, msg ->
            val num = startIdx + idx + 1
            val preview = if (msg.body.startsWith("PASA ", ignoreCase = true)) {
                "<i>[PASA command — hidden]</i>"
            } else {
                msg.body.take(150)
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;") +
                        if (msg.body.length > 150) "…" else ""
            }
            sb.appendLine("$num. 📱 <code>${msg.address}</code>")
            sb.appendLine("   $preview")
            sb.appendLine("   <i>${msg.date}</i>")
            if (idx < pageMsgs.size - 1) sb.appendLine()
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        val navItems = mutableListOf<String>()
        if (validPage > 1) navItems.add("👈 <code>/sms_log ${validPage - 1}</code>")
        if (validPage < totalPages) navItems.add("👉 <code>/sms_log ${validPage + 1}</code>")
        if (navItems.isNotEmpty()) sb.appendLine("📄 <b>Page $validPage/$totalPages:</b> ${navItems.joinToString(" • ")}")
        sb.append("💾 Export: <code>/sms_log export</code> • 🔍 Search: <code>/sms_log search &lt;keyword&gt;</code>")

        return CommandResult(success = true, message = sb.toString())
    }

    private fun exportSms(): CommandResult {
        return try {
            val all = loadSms(null, 5000)
            val file = File(context.cacheDir, "pasa_sms_${System.currentTimeMillis()}.txt")
            file.printWriter().use { out ->
                out.println("================================================================================")
                out.println("PASA Sentinel — SMS Inbox Export")
                out.println("Total Messages: ${all.size}")
                out.println("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                out.println("================================================================================")
                out.println()
                all.forEachIndexed { i, msg ->
                    out.println("${i + 1}. From: ${msg.address}")
                    out.println("   Date: ${msg.date}")
                    if (msg.body.startsWith("PASA ", ignoreCase = true)) {
                        out.println("   [PASA command — hidden]")
                    } else {
                        out.println("   ${msg.body}")
                    }
                    out.println()
                }
            }
            CommandResult(
                success = true,
                message = "💬 <b>SMS Export (${all.size} messages)</b>\n━━━━━━━━━━━━━━━━━━━━\nFull inbox document attached below.",
                documentFile = file
            )
        } catch (e: Exception) {
            CommandResult(success = false, message = "❌ Failed to export SMS log: ${e.message}")
        }
    }
}
