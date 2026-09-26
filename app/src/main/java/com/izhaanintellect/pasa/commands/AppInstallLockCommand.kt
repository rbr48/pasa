package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.Totp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppInstallLockCommand (/app_install_lock).
 * Enforces hardware-backed restrictions against sideloading and rogue application installations.
 * Uses Device Owner policies (UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES & UserManager.DISALLOW_INSTALL_APPS).
 */
@Singleton
class AppInstallLockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/app_install_lock"
    override val description = "Sideload & App Install Lockdown [Device Owner]"
    override val usage = "/app_install_lock [status|unknown_only|block_all] | /app_install_lock <master_password> allow"

    companion object {
        private val ACTION_WORDS = setOf("status", "unknown_only", "unknown", "sideload", "block_all", "all", "lock", "allow", "off", "unlock", "disable")
    }

    private fun verifyCredentials(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        val isPass = authManager.verifyMasterPassword(candidate)
        val totpSecret = prefs.smsTotpSecret
        val isTotp = totpSecret.isNotBlank() && Totp.verify(totpSecret, candidate, window = 3)
        return isPass || isTotp
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> App Install Lockdown requires Android Enterprise Device Owner privileges.\nRun <code>/device_owner</code> for activation instructions."
            )
        }

        val action = args.firstOrNull { it.lowercase().trim() in ACTION_WORDS }?.lowercase()?.trim() ?: "status"
        val candidate = args.firstOrNull { it.lowercase().trim() !in ACTION_WORDS }?.trim()

        if (action in setOf("allow", "off", "unlock", "disable") && authManager.hasMasterPassword()) {
            if (candidate.isNullOrBlank()) {
                return CommandResult(
                    success = false,
                    message = """
                        🔑 <b>Disarm App Installation Lockdown</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        Re-enabling unknown sources and app sideloading requires your Master Password.

                        <b>Syntax:</b> <code>/app_install_lock &lt;master_password&gt; allow</code>
                        <b>Example:</b> <code>/app_install_lock MySecretPass123 allow</code>
                    """.trimIndent()
                )
            }
            if (!verifyCredentials(candidate)) {
                return CommandResult(
                    success = false,
                    message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password. Lockdown change rejected."
                )
            }
        }

        return when (action) {
            "unknown_only", "unknown", "sideload" -> {
                val (ok, text) = PasaDeviceAdmin.setAppInstallRestrictions(context, "unknown_only")
                if (ok) {
                    prefs.appInstallLockMode = "unknown_only"
                    CommandResult(true, text)
                } else {
                    CommandResult(false, text)
                }
            }

            "block_all", "all", "lock" -> {
                val (ok, text) = PasaDeviceAdmin.setAppInstallRestrictions(context, "block_all")
                if (ok) {
                    prefs.appInstallLockMode = "block_all"
                    CommandResult(true, text)
                } else {
                    CommandResult(false, text)
                }
            }

            "allow", "off", "unlock", "disable" -> {
                val (ok, text) = PasaDeviceAdmin.setAppInstallRestrictions(context, "allow")
                if (ok) {
                    prefs.appInstallLockMode = "none"
                    CommandResult(true, text)
                } else {
                    CommandResult(false, text)
                }
            }

            else -> {
                val currentMode = prefs.appInstallLockMode
                val modeDesc = when (currentMode) {
                    "unknown_only" -> "🔒 Sideloading Blocked (Official stores only)"
                    "block_all" -> "🛑 COMPLETE FREEZE (All installs/updates blocked)"
                    else -> "🔓 Unrestricted (Default OS behavior)"
                }

                CommandResult(
                    success = true,
                    message = "📦 <b>App Installation Lockdown Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• <b>Active Policy:</b> $modeDesc\n" +
                            "• <b>Device Owner Enforcement:</b> ACTIVE\n\n" +
                            "💡 <i>Modes & Commands:</i>\n" +
                            "• <code>/app_install_lock unknown_only</code> — Block sideloading APKs (malware/trojans)\n" +
                            "• <code>/app_install_lock block_all</code> — Complete freeze on any new apps\n" +
                            "• <code>/app_install_lock &lt;password&gt; allow</code> — Restore install permissions"
                )
            }
        }
    }
}
