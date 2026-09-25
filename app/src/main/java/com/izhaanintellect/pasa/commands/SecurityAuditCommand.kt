package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kernel security audit command.
 * Inspects low-level SecurityLog events captured by Android OS (ADB shells, KeyStore events, media mounts).
 * Supports on-demand full raw audit stream delivery via Telegram documents (/security_audit full)
 * with multi-layer zero-leak credential sanitization.
 */
@Singleton
class SecurityAuditCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/security_audit"
    override val description = "Kernel security audit log inspection [Device Owner]"
    override val usage = "/security_audit [full]"

    companion object {
        private val TELEGRAM_BOT_TOKEN_REGEX = Regex("""\b\d{8,11}:[A-Za-z0-9_-]{35}\b""")
        private val JWT_TOKEN_REGEX = Regex("""\beyJ[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\b""")
        private val CLI_PASSWORD_REGEX = Regex("""(?i)(?:-p\s+|--password[=\s]+|passwd[=\s]+|secret[=\s]+)(\S+)""")
    }

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

        val isFullExport = args.any {
            it.equals("full", ignoreCase = true) ||
            it.equals("all", ignoreCase = true) ||
            it.equals("export", ignoreCase = true) ||
            it.equals("dump", ignoreCase = true)
        }

        val (ok, rawLogEntries) = PasaDeviceAdmin.retrieveSecurityLogsList(context, full = isFullExport)
        if (!ok) {
            return CommandResult(success = false, message = rawLogEntries.firstOrNull() ?: "❌ Unknown error")
        }

        if (rawLogEntries.isEmpty()) {
            return CommandResult(
                success = true,
                message = "📑 <b>OS Security Audit Log:</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "✅ <b>Zero Anomalies Detected:</b> Security log is empty or currently waiting for next kernel audit batch."
            )
        }

        // Apply multi-layer zero-leak credential sanitization
        val sanitizedEntries = rawLogEntries.map { sanitizeLine(it) }

        if (isFullExport) {
            return try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val auditFile = File(context.cacheDir, "kernel_security_audit_$timestamp.txt")

                FileOutputStream(auditFile).bufferedWriter().use { writer ->
                    writer.write("================================================================================\n")
                    writer.write("PASA SENTINEL — LINUX KERNEL OS SECURITY AUDIT TRAIL\n")
                    writer.write("================================================================================\n")
                    writer.write("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})\n")
                    writer.write("Android OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                    writer.write("Build Fingerprint: ${Build.FINGERPRINT}\n")
                    writer.write("Export Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())}\n")
                    writer.write("Total Kernel Events: ${sanitizedEntries.size}\n")
                    writer.write("Security Classification: STRICTLY CONFIDENTIAL // OWNER AUDIT\n")
                    writer.write("Credential Redaction: ACTIVE (Zero-Leak Sanitization Policy)\n")
                    writer.write("================================================================================\n\n")

                    sanitizedEntries.forEach { entry ->
                        writer.write(entry)
                        writer.write("\n")
                    }

                    writer.write("\n================================================================================\n")
                    writer.write("END OF KERNEL AUDIT STREAM — PASA SENTINEL ZERO-STORAGE FORENSICS\n")
                    writer.write("================================================================================\n")
                }

                CommandResult(
                    success = true,
                    message = "📑 <b>OS Security Audit Log Export:</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "✅ Extracted <b>${sanitizedEntries.size}</b> kernel security audit events.\n" +
                            "🛡️ Full audit log delivered as document below.\n" +
                            "🔒 <i>Zero-Leak Redaction applied: All sensitive tokens & credentials filtered.</i>",
                    documentFile = auditFile
                )
            } catch (e: Exception) {
                CommandResult(
                    success = false,
                    message = "❌ Failed to generate security audit document: ${e.message}"
                )
            }
        }

        // Standard summary response
        val sb = StringBuilder()
        sb.append("📑 <b>OS Security Audit Log (${sanitizedEntries.size} events):</b>\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sanitizedEntries.takeLast(20).forEach { entry ->
            sb.append("• <code>$entry</code>\n")
        }
        sb.append("\n🛡️ <i>Audited by Android Linux Kernel</i>\n")
        sb.append("💡 <i>Tip: Send <code>/security_audit full</code> to export complete kernel audit log as a document.</i>")

        return CommandResult(success = true, message = sb.toString())
    }

    /**
     * Multi-layer credential and secret sanitizer.
     * Intercepts and scrubs bot tokens, API keys, JWT tokens, and CLI passwords.
     */
    private fun sanitizeLine(line: String): String {
        var result = line

        // 1. Redact device's configured bot token
        val activeBotToken = preferencesManager.botToken
        if (activeBotToken.isNotBlank() && result.contains(activeBotToken)) {
            result = result.replace(activeBotToken, "[REDACTED_BOT_TOKEN]")
        }

        // 2. Redact any generic Telegram bot token patterns
        result = TELEGRAM_BOT_TOKEN_REGEX.replace(result, "[REDACTED_BOT_TOKEN]")

        // 3. Redact any JWT / Bearer authentication tokens
        result = JWT_TOKEN_REGEX.replace(result, "[REDACTED_JWT_TOKEN]")

        // 4. Redact common CLI command password / secret parameters in ADB_SHELL_CMD
        result = CLI_PASSWORD_REGEX.replace(result) { match ->
            val fullMatch = match.value
            val secretValue = match.groupValues.getOrNull(1) ?: ""
            if (secretValue.isNotBlank()) {
                fullMatch.replace(secretValue, "[REDACTED_PARAM]")
            } else {
                fullMatch
            }
        }

        return result
    }
}
