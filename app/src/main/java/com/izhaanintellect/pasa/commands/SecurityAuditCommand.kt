package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kernel security audit command.
 * Inspects low-level SecurityLog events captured by Android OS (ADB shells, KeyStore events, media mounts).
 */
@Singleton
class SecurityAuditCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/security_audit"
    override val description = "Kernel security audit log inspection [Device Owner]"
    override val usage = "/security_audit"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> Security log inspection requires Android Device Owner.\n" +
                        "Run <code>/device_owner</code> for instructions."
            )
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return CommandResult(
                success = false,
                message = "❌ Security log retrieval requires Android 7.0+ (API 24+)."
            )
        }

        val (ok, logEntries) = PasaDeviceAdmin.retrieveSecurityLogsList(context)
        if (!ok) {
            return CommandResult(success = false, message = logEntries.firstOrNull() ?: "❌ Unknown error")
        }

        if (logEntries.isEmpty()) {
            return CommandResult(
                success = true,
                message = "📑 <b>OS Security Audit Log:</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "✅ <b>Zero Anomalies Detected:</b> Security log is empty or currently waiting for next kernel audit batch."
            )
        }

        val sb = StringBuilder()
        sb.append("📑 <b>OS Security Audit Log (${logEntries.size} events):</b>\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        logEntries.takeLast(20).forEach { entry ->
            sb.append("• <code>$entry</code>\n")
        }
        sb.append("\n🛡️ <i>Audited by Android Linux Kernel</i>")

        return CommandResult(success = true, message = sb.toString())
    }
}
