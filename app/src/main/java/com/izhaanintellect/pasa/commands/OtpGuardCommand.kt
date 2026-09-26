package com.izhaanintellect.pasa.commands

import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.OtpInterceptionGuardManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OtpGuardCommand (/otp_guard).
 * 2FA / OTP Interception Guard.
 * Monitors and polices applications holding Notification Access (BIND_NOTIFICATION_LISTENER_SERVICE).
 * Ensures legitimate banking apps work smoothly while stopping trojans from stealing 2FA codes.
 */
@Singleton
class OtpGuardCommand @Inject constructor(
    private val prefs: PreferencesManager,
    private val otpGuardManager: OtpInterceptionGuardManager
) : Command {

    override val name = "/otp_guard"
    override val description = "2FA & SMS OTP Interception Guard"
    override val usage = "/otp_guard [status|audit|whitelist <pkg>|remove <pkg>|auto_neutralize on|off]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull()?.lowercase()?.trim() ?: "status"

        return when (action) {
            "on", "enable", "arm" -> {
                prefs.isOtpGuardEnabled = true
                otpGuardManager.startMonitoring()
                CommandResult(
                    success = true,
                    message = "📬 <b>2FA / OTP Interception Guard: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "✅ <b>Status:</b> ACTIVE\n" +
                            "🔍 <b>Monitoring:</b> Real-time audit of Notification Listeners\n" +
                            "🔒 <i>Protects against SharkBot, Anatsa, and banking trojans stealing OTPs.</i>"
                )
            }

            "off", "disable", "disarm" -> {
                prefs.isOtpGuardEnabled = false
                otpGuardManager.stopMonitoring()
                CommandResult(
                    success = true,
                    message = "📬 <b>2FA / OTP Interception Guard: DISARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "⚠️ Notification listener monitoring paused."
                )
            }

            "whitelist" -> {
                val pkg = args.getOrNull(1)?.trim()
                if (pkg.isNullOrBlank()) {
                    CommandResult(false, "⚠️ Specify package to whitelist: <code>/otp_guard whitelist &lt;package_name&gt;</code>")
                } else {
                    val current = prefs.otpGuardWhitelist.toMutableSet()
                    current.add(pkg)
                    prefs.otpGuardWhitelist = current
                    CommandResult(
                        success = true,
                        message = "✅ Package <code>$pkg</code> added to OTP Guard whitelist."
                    )
                }
            }

            "remove" -> {
                val pkg = args.getOrNull(1)?.trim()
                if (pkg.isNullOrBlank()) {
                    CommandResult(false, "⚠️ Specify package to remove: <code>/otp_guard remove &lt;package_name&gt;</code>")
                } else {
                    val current = prefs.otpGuardWhitelist.toMutableSet()
                    current.remove(pkg)
                    prefs.otpGuardWhitelist = current
                    CommandResult(
                        success = true,
                        message = "🗑️ Package <code>$pkg</code> removed from OTP Guard whitelist."
                    )
                }
            }

            "auto_neutralize", "autoneutralize" -> {
                val sub = args.getOrNull(1)?.lowercase()?.trim()
                if (sub == "on" || sub == "enable") {
                    prefs.isOtpGuardAutoNeutralize = true
                    CommandResult(
                        success = true,
                        message = "🛑 <b>Auto-Neutralize: ENABLED</b>\nAny unauthorized notification listener will be automatically suspended by Knox Device Owner."
                    )
                } else if (sub == "off" || sub == "disable") {
                    prefs.isOtpGuardAutoNeutralize = false
                    CommandResult(
                        success = true,
                        message = "⚠️ <b>Auto-Neutralize: DISABLED</b>\nUnauthorized listeners will trigger Telegram alerts only without auto-suspension."
                    )
                } else {
                    CommandResult(
                        success = true,
                        message = "Auto-Neutralize is currently: <b>${if (prefs.isOtpGuardAutoNeutralize) "ENABLED" else "DISABLED"}</b>\n" +
                                "Toggle with: <code>/otp_guard auto_neutralize on|off</code>"
                    )
                }
            }

            "audit", "check" -> {
                val allListeners = otpGuardManager.getActiveNotificationListeners()
                val rogue = otpGuardManager.auditListeners(triggeredByEvent = false)
                val userWhitelist = prefs.otpGuardWhitelist

                // Classify each listener into one of three tiers
                val systemTrusted = allListeners.filter { pkg ->
                    OtpInterceptionGuardManager.TRUSTED_SYSTEM_LISTENERS.contains(pkg) ||
                    OtpInterceptionGuardManager.isSystemApp(otpGuardManager.getContext(), pkg)
                }
                val userTrusted = allListeners.filter { pkg ->
                    !systemTrusted.contains(pkg) && userWhitelist.contains(pkg)
                }

                val systemText = if (systemTrusted.isEmpty()) "<i>None</i>"
                else systemTrusted.joinToString("\n") { "  ✅ <code>$it</code>" }

                val userText = if (userTrusted.isEmpty()) "<i>None</i>"
                else userTrusted.joinToString("\n") { "  🟡 <code>$it</code>" }

                val rogueText = if (rogue.isEmpty()) "🟢 <b>Zero rogue listeners — device is clean!</b>"
                else rogue.joinToString("\n") { "  🔴 <code>$it</code>" }

                val verdict = if (rogue.isEmpty())
                    "\n\n✅ <b>VERDICT: CLEAR</b> — No unauthorized notification access detected."
                else
                    "\n\n🚨 <b>VERDICT: THREAT DETECTED</b> — ${rogue.size} unrecognized app(s) have notification access.\n" +
                    "Use <code>/otp_guard whitelist &lt;pkg&gt;</code> if trusted, or <code>/otp_guard auto_neutralize on</code> to auto-freeze."

                CommandResult(
                    success = true,
                    message = "📬 <b>Notification Access Audit Report</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "📊 <b>Total Listeners Active:</b> ${allListeners.size}\n\n" +
                            "✅ <b>System / OEM Trusted (${systemTrusted.size}):</b>\n$systemText\n\n" +
                            "🟡 <b>User-Whitelisted (${userTrusted.size}):</b>\n$userText\n\n" +
                            "🔴 <b>Rogue / Unauthorized (${rogue.size}):</b>\n$rogueText" +
                            verdict + "\n\n" +
                            "ℹ️ <i>Banking apps use SMS Retriever API, not Notification Access.\n" +
                            "Only user-installed (non-system) apps with listener access are flagged.</i>"
                )
            }

            else -> {
                val isArmed = prefs.isOtpGuardEnabled
                val isAuto = prefs.isOtpGuardAutoNeutralize
                val listeners = otpGuardManager.getActiveNotificationListeners()

                CommandResult(
                    success = true,
                    message = "📬 <b>2FA / OTP Interception Guard Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• <b>Shield State:</b> ${if (isArmed) "🔒 ARMED (Active)" else "🔓 DISARMED"}\n" +
                            "• <b>Auto-Neutralize Rogue Apps:</b> ${if (isAuto) "🛑 ON (Knox Suspend)" else "⚠️ OFF (Alert Only)"}\n" +
                            "• <b>Active Listeners on Phone:</b> ${listeners.size} packages\n" +
                            "• <b>Custom Whitelisted:</b> ${prefs.otpGuardWhitelist.size} packages\n\n" +
                            "💡 <i>Usage:</i>\n" +
                            "• <code>/otp_guard audit</code> — Run comprehensive listener audit\n" +
                            "• <code>/otp_guard auto_neutralize on</code> — Auto-freeze suspicious listeners\n" +
                            "• <code>/otp_guard whitelist &lt;pkg&gt;</code> — Whitelist trusted app\n" +
                            "• <code>/otp_guard [enable|disable]</code> — Toggle guard"
                )
            }
        }
    }
}
