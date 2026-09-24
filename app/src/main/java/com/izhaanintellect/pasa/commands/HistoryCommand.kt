package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.data.CommandLogDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Displays command execution history from the local audit log.
 *
 * Commands:
 *   /history               — Paginated list (page 1, 15 per page)
 *   /history <page>        — View specific page (e.g. /history 2)
 *   /history export        — Download full command audit log as .txt document
 */
@Singleton
class HistoryCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commandLogDao: CommandLogDao
) : Command {

    override val name = "/history"
    override val description = "View command execution history with pagination and export"
    override val usage = "/history [page] | /history export"

    companion object {
        private const val PAGE_SIZE = 15
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val firstArg = args.firstOrNull()?.lowercase()?.trim()

        // Full document export
        if (firstArg == "export" || firstArg == "all" || firstArg == "full") {
            return exportHistory()
        }

        val allLogs = commandLogDao.getRecentLogs(10000)
        val totalCount = allLogs.size

        if (totalCount == 0) {
            return CommandResult(
                success = true,
                message = "📋 <b>Command History</b>\n━━━━━━━━━━━━━━━━━━━━\n<i>No commands recorded yet.</i>"
            )
        }

        val page = firstArg?.toIntOrNull() ?: 1
        val totalPages = maxOf(1, (totalCount + PAGE_SIZE - 1) / PAGE_SIZE)
        val validPage = page.coerceIn(1, totalPages)
        val startIdx = (validPage - 1) * PAGE_SIZE
        val pageLogs = allLogs.drop(startIdx).take(PAGE_SIZE)

        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("📋 <b>Command History ($totalCount total)</b> — Page $validPage of $totalPages")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        pageLogs.forEachIndexed { idx, log ->
            val globalNum = startIdx + idx + 1
            val time = dateFormat.format(Date(log.timestamp))
            val statusIcon = when (log.status) {
                "SUCCESS"  -> "✅"
                "FAILED"   -> "❌"
                "REJECTED" -> "⛔"
                else       -> "❓"
            }
            val argsStr = if (log.args.isNotBlank()) " ${log.args}" else ""
            sb.appendLine("$globalNum. $statusIcon <code>${log.command}$argsStr</code>")
            sb.appendLine("   <i>$time — ${log.status}</i>")
            if (idx < pageLogs.size - 1) sb.appendLine()
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        val navItems = mutableListOf<String>()
        if (validPage > 1) navItems.add("👈 <code>/history ${validPage - 1}</code>")
        if (validPage < totalPages) navItems.add("👉 <code>/history ${validPage + 1}</code>")
        if (navItems.isNotEmpty()) sb.appendLine("📄 <b>Page $validPage/$totalPages:</b> ${navItems.joinToString(" • ")}")
        sb.append("💾 Full audit log: <code>/history export</code>")

        return CommandResult(success = true, message = sb.toString())
    }

    private suspend fun exportHistory(): CommandResult {
        return try {
            val all = commandLogDao.getRecentLogs(50000)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val file = File(context.cacheDir, "pasa_command_audit_${System.currentTimeMillis()}.txt")
            file.printWriter().use { out ->
                out.println("================================================================================")
                out.println("PASA Sentinel — Command Execution Audit Log")
                out.println("Total Entries: ${all.size}")
                out.println("Generated: ${dateFormat.format(Date())}")
                out.println("================================================================================")
                out.println()
                all.forEachIndexed { i, log ->
                    val time = dateFormat.format(Date(log.timestamp))
                    val argsStr = if (log.args.isNotBlank()) " ${log.args}" else ""
                    out.println("${i + 1}. [${log.status}] ${log.command}$argsStr")
                    out.println("   Time: $time")
                    out.println()
                }
            }
            CommandResult(
                success = true,
                message = "📋 <b>Command Audit Log Export (${all.size} entries)</b>\n━━━━━━━━━━━━━━━━━━━━\nFull audit trail document attached below.",
                documentFile = file
            )
        } catch (e: Exception) {
            CommandResult(success = false, message = "❌ Failed to export history: ${e.message}")
        }
    }
}
