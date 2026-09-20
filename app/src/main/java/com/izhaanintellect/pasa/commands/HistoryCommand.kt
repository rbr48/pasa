package com.izhaanintellect.pasa.commands

import com.izhaanintellect.pasa.data.CommandLogDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Displays recent command execution history from the local audit log.
 */
@Singleton
class HistoryCommand @Inject constructor(
    private val commandLogDao: CommandLogDao
) : Command {

    override val name = "/history"
    override val description = "View recent command execution history"
    override val usage = "/history [count]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val limit = args.firstOrNull()?.toIntOrNull()?.coerceIn(1, 25) ?: 10
        val logs = commandLogDao.getRecentLogs(limit)

        if (logs.isEmpty()) {
            return CommandResult(
                success = true,
                message = "📋 <b>Command History</b>\n━━━━━━━━━━━━━━━━━━━━\n<i>No commands recorded yet.</i>"
            )
        }

        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)
        val entries = logs.mapIndexed { index, log ->
            val time = dateFormat.format(Date(log.timestamp))
            val statusIcon = when (log.status) {
                "SUCCESS" -> "✅"
                "FAILED" -> "❌"
                "REJECTED" -> "⛔"
                else -> "❓"
            }
            "${index + 1}. $statusIcon <code>${log.command}</code> ${if (log.args.isNotBlank()) log.args else ""}\n   <i>$time — ${log.status}</i>"
        }

        val message = "📋 <b>Command History</b> (last $limit)\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                entries.joinToString("\n\n") +
                "\n\n<i>Total logged: ${commandLogDao.getCount()}</i>"

        return CommandResult(success = true, message = message)
    }
}
