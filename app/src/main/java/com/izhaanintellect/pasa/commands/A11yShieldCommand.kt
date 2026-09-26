package com.izhaanintellect.pasa.commands

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.Totp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accessibility Trojan Shield Command (/a11y_shield).
 * Uses Device Owner dpm.setPermittedAccessibilityServices to strictly lock down
 * which applications can run as Accessibility Services, instantly neutralizing
 * banking trojans (e.g. SharkBot, Hook, Godfather, PixPirate) from hijacking the UI.
 */
@Singleton
class A11yShieldCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/a11y_shield"
    override val description = "Accessibility Trojan Shield [Device Owner]"
    override val usage = "/a11y_shield [status|lock|unlock|whitelist <pkg>|remove <pkg>]"

    companion object {
        private val ACTION_WORDS = setOf("status", "lock", "arm", "enable", "unlock", "disarm", "disable", "whitelist", "remove")
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
                message = "❌ <b>Device Owner Required:</b> Accessibility Trojan Shield requires Android Enterprise Device Owner privileges.\nRun <code>/device_owner</code> for activation instructions."
            )
        }

        val action = args.firstOrNull { it.lowercase().trim() in ACTION_WORDS }?.lowercase()?.trim() ?: "status"
        val candidate = args.firstOrNull { it.lowercase().trim() !in ACTION_WORDS }?.trim()

        if (action in setOf("unlock", "disarm", "disable") && authManager.hasMasterPassword()) {
            if (candidate.isNullOrBlank()) {
                return CommandResult(
                    success = false,
                    message = """
                        🔑 <b>Disarm Accessibility Trojan Shield</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        Unlocking accessibility services allows 3rd-party applications to request accessibility access. Master Password is required.

                        <b>Syntax:</b> <code>/a11y_shield &lt;master_password&gt; unlock</code>
                        <b>Example:</b> <code>/a11y_shield MySecretPass123 unlock</code>
                    """.trimIndent()
                )
            }
            if (!verifyCredentials(candidate)) {
                return CommandResult(
                    success = false,
                    message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password. Shield unlock rejected."
                )
            }
        }

        return when (action) {
            "lock", "arm", "enable" -> {
                val allowed = (listOf(context.packageName) + prefs.a11yShieldWhitelist).distinct()
                val (ok, text) = PasaDeviceAdmin.setPermittedAccessibilityServices(context, allowed)
                if (ok) {
                    prefs.isA11yShieldEnabled = true
                    CommandResult(
                        success = true,
                        message = "🛡️ <b>Accessibility Trojan Shield: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "✅ <b>Policy:</b> STRICT RESTRICTION ACTIVE\n" +
                                "🚫 Any unauthorized app attempting to activate Accessibility will be blocked by Android OS.\n" +
                                "🔒 <b>Permitted Packages (${allowed.size}):</b>\n" +
                                allowed.joinToString("\n") { "• <code>$it</code>" } + "\n\n" +
                                "💡 <i>Banking trojans & clickjacking bots neutralized!</i>"
                    )
                } else {
                    CommandResult(false, text)
                }
            }

            "unlock", "disarm", "disable" -> {
                val (ok, text) = PasaDeviceAdmin.setPermittedAccessibilityServices(context, null)
                if (ok) {
                    prefs.isA11yShieldEnabled = false
                    CommandResult(
                        success = true,
                        message = "🛡️ <b>Accessibility Trojan Shield: UNLOCKED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "⚠️ Accessibility restrictions removed. Applications can now request Accessibility access."
                    )
                } else {
                    CommandResult(false, text)
                }
            }

            "whitelist" -> {
                val targetPkg = args.getOrNull(1)?.trim() ?: candidate
                if (targetPkg.isNullOrBlank()) {
                    CommandResult(
                        success = false,
                        message = "⚠️ Specify package to whitelist: <code>/a11y_shield whitelist &lt;package_name&gt;</code>"
                    )
                } else {
                    val current = prefs.a11yShieldWhitelist.toMutableSet()
                    current.add(targetPkg)
                    prefs.a11yShieldWhitelist = current
                    if (prefs.isA11yShieldEnabled) {
                        val allowed = (listOf(context.packageName) + current).distinct()
                        PasaDeviceAdmin.setPermittedAccessibilityServices(context, allowed)
                    }
                    CommandResult(
                        success = true,
                        message = "✅ Package <code>$targetPkg</code> added to Accessibility Shield whitelist."
                    )
                }
            }

            "remove" -> {
                val targetPkg = args.getOrNull(1)?.trim() ?: candidate
                if (targetPkg.isNullOrBlank()) {
                    CommandResult(
                        success = false,
                        message = "⚠️ Specify package to remove: <code>/a11y_shield remove &lt;package_name&gt;</code>"
                    )
                } else {
                    val current = prefs.a11yShieldWhitelist.toMutableSet()
                    current.remove(targetPkg)
                    prefs.a11yShieldWhitelist = current
                    if (prefs.isA11yShieldEnabled) {
                        val allowed = (listOf(context.packageName) + current).distinct()
                        PasaDeviceAdmin.setPermittedAccessibilityServices(context, allowed)
                    }
                    CommandResult(
                        success = true,
                        message = "🗑️ Package <code>$targetPkg</code> removed from Accessibility Shield whitelist."
                    )
                }
            }

            else -> {
                // status
                val isArmed = prefs.isA11yShieldEnabled
                val permitted = PasaDeviceAdmin.getPermittedAccessibilityServices(context)
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                val runningServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) ?: emptyList()

                val runningText = if (runningServices.isEmpty()) {
                    "<i>No active accessibility services.</i>"
                } else {
                    runningServices.joinToString("\n") { s ->
                        val pkg = s.resolveInfo?.serviceInfo?.packageName ?: "unknown"
                        val id = s.id ?: pkg
                        "• <code>$id</code>"
                    }
                }

                CommandResult(
                    success = true,
                    message = "🛡️ <b>Accessibility Trojan Shield Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• <b>Shield State:</b> ${if (isArmed) "🔒 ARMED (Strict Whitelist)" else "🔓 UNLOCKED (All Allowed)"}\n" +
                            "• <b>Permitted Services:</b> ${permitted?.size?.let { "$it packages" } ?: "All (Default)"}\n" +
                            "• <b>Whitelisted Extra:</b> ${prefs.a11yShieldWhitelist.size} packages\n\n" +
                            "⚙️ <b>Currently Active Services (${runningServices.size}):</b>\n$runningText\n\n" +
                            "💡 <i>Usage:</i>\n" +
                            "• <code>/a11y_shield lock</code> — Lock down to PASA only\n" +
                            "• <code>/a11y_shield whitelist &lt;pkg&gt;</code> — Whitelist an app\n" +
                            "• <code>/a11y_shield &lt;pass&gt; unlock</code> — Remove restrictions"
                )
            }
        }
    }
}
