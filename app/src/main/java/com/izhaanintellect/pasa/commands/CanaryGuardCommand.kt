package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.RansomwareCanaryManager
import com.izhaanintellect.pasa.security.Totp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CanaryGuardCommand (/canary_guard).
 * Manages the Ransomware Canary Trap.
 * Deploys cryptographic canary tripwire files across system storage and monitors them.
 * If malware or ransomware tampers with or encrypts a canary, all apps are suspended immediately.
 */
@Singleton
class CanaryGuardCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val authManager: AuthManager,
    private val canaryManager: RansomwareCanaryManager
) : Command {

    override val name = "/canary_guard"
    override val description = "Ransomware Tripwire Canary Guard"
    override val usage = "/canary_guard [status|arm|disarm|check] | /canary_guard <master_password> reset"

    companion object {
        private val ACTION_WORDS = setOf("status", "arm", "enable", "on", "disarm", "disable", "off", "check", "audit", "reset", "restore")
    }

    private fun verifyCredentials(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        val isPass = authManager.verifyMasterPassword(candidate)
        val totpSecret = prefs.smsTotpSecret
        val isTotp = totpSecret.isNotBlank() && Totp.verify(totpSecret, candidate, window = 3)
        return isPass || isTotp
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull { it.lowercase().trim() in ACTION_WORDS }?.lowercase()?.trim() ?: "status"
        val candidate = args.firstOrNull { it.lowercase().trim() !in ACTION_WORDS }?.trim()

        if (action in setOf("reset", "restore", "disarm", "disable", "off") && authManager.hasMasterPassword()) {
            if (candidate.isNullOrBlank()) {
                return CommandResult(
                    success = false,
                    message = """
                        🔑 <b>Authentication Required</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        Resetting tripwire lockdown or disarming the canary trap requires your Master Password.

                        <b>Syntax:</b> <code>/canary_guard &lt;master_password&gt; reset</code>
                        <b>Example:</b> <code>/canary_guard MySecretPass123 reset</code>
                    """.trimIndent()
                )
            }
            if (!verifyCredentials(candidate)) {
                return CommandResult(
                    success = false,
                    message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password."
                )
            }
        }

        return when (action) {
            "arm", "enable", "on" -> {
                val (ok, text) = canaryManager.armCanaryTrap()
                CommandResult(
                    success = ok,
                    message = "🪤 <b>Ransomware Canary Guard: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "✅ <b>Tripwires Planted:</b> Documents, Downloads, DCIM\n" +
                            "🔍 <b>File Observers:</b> ACTIVE (Monitoring file modifications)\n" +
                            "🛑 <b>Lockdown Policy:</b> Automatic Device Owner App Suspension upon tampering\n\n" +
                            "💡 <i>$text</i>"
                )
            }

            "disarm", "disable", "off" -> {
                val (ok, text) = canaryManager.disarmCanaryTrap()
                CommandResult(
                    success = ok,
                    message = "🪤 <b>Ransomware Canary Guard: DISARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "⚠️ Canary file observers stopped."
                )
            }

            "reset", "restore" -> {
                val unsuspended = if (PasaDeviceAdmin.isDeviceOwner(context)) {
                    PasaDeviceAdmin.suspendAllThirdPartyApps(context, false)
                } else emptyList()

                // Re-arm canary traps with fresh files
                canaryManager.armCanaryTrap()

                CommandResult(
                    success = true,
                    message = "🔄 <b>Ransomware Lockdown Reset: SUCCESS</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "✅ Restored and un-suspended ${unsuspended.size} applications.\n" +
                            "🪤 Re-planted fresh integrity canaries in storage."
                )
            }

            "check", "audit" -> {
                val (ok, auditText) = canaryManager.auditIntegrity()
                CommandResult(
                    success = ok,
                    message = "🔍 <b>Canary File Integrity Audit</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            auditText + "\n" +
                            if (ok) "🟢 <b>All canaries intact. No ransomware activity detected.</b>"
                            else "🔴 <b>CANARY BREACH DETECTED!</b>"
                )
            }

            else -> {
                val isArmed = prefs.isCanaryGuardArmed
                CommandResult(
                    success = true,
                    message = "🪤 <b>Ransomware Canary Guard Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• <b>Tripwire Mode:</b> ${if (isArmed) "🔒 ARMED (Active)" else "🔓 DISARMED"}\n" +
                            "• <b>Knox Emergency Freeze:</b> Ready (Suspends all apps upon encryption attempt)\n\n" +
                            "💡 <i>Usage:</i>\n" +
                            "• <code>/canary_guard arm</code> — Deploy canaries & arm observer\n" +
                            "• <code>/canary_guard check</code> — Audit canary integrity\n" +
                            "• <code>/canary_guard &lt;pass&gt; reset</code> — Un-suspend apps after an incident"
                )
            }
        }
    }
}
