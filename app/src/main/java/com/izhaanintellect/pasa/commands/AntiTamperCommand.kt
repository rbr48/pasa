package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise Anti-Tamper Suite Command.
 * Controls hardware restrictions (Safe Boot, Airplane Mode, Factory Reset, Network Reset, OTG block).
 */
@Singleton
class AntiTamperCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : Command {

    override val name = "/antitamper"
    override val description = "Toggle Enterprise Anti-Tamper Hardening Suite [Device Owner]"
    override val usage = "/antitamper [on|off|status]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> Enterprise Anti-Tamper controls require Device Owner privileges.\n" +
                        "Run <code>/device_owner</code> for activation instructions."
            )
        }

        val action = args.firstOrNull()?.lowercase() ?: "status"

        return when (action) {
            "on", "enable", "arm" -> {
                val results = PasaDeviceAdmin.applyAntiTamperSuite(context, true)
                prefs.antiTamperEnabled = true

                val sb = StringBuilder()
                sb.append("🛡️ <b>Enterprise Anti-Tamper Suite: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n")
                results.forEach { (name, ok) ->
                    sb.append(if (ok) "✅ " else "⚠️ ")
                    sb.append("<b>$name:</b> ").append(if (ok) "Locked" else "Error").append("\n")
                }
                sb.append("\n🔒 <i>Safe boot, Airplane mode, and Factory reset are now physically blocked!</i>")

                CommandResult(success = true, message = sb.toString())
            }

            "off", "disable", "disarm" -> {
                val results = PasaDeviceAdmin.applyAntiTamperSuite(context, false)
                prefs.antiTamperEnabled = false

                val sb = StringBuilder()
                sb.append("⚠️ <b>Enterprise Anti-Tamper Suite: DISARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n")
                results.forEach { (name, ok) ->
                    sb.append(if (ok) "🔓 " else "⚠️ ")
                    sb.append("<b>$name:</b> ").append(if (ok) "Unlocked" else "Error").append("\n")
                }
                sb.append("\nℹ️ <i>Device restrictions restored to normal.</i>")

                CommandResult(success = true, message = sb.toString())
            }

            "status" -> {
                val currentState = prefs.antiTamperEnabled
                CommandResult(
                    success = true,
                    message = "🛡️ <b>Anti-Tamper Suite Status:</b> ${if (currentState) "✅ <b>ARMED</b>" else "⚠️ <b>DISARMED</b>"}\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n" +
                            "• Safe Mode Boot: ${if (currentState) "🚫 Blocked" else "Allowed"}\n" +
                            "• Airplane Mode Toggle: ${if (currentState) "🚫 Blocked" else "Allowed"}\n" +
                            "• Factory Reset in Settings: ${if (currentState) "🚫 Blocked" else "Allowed"}\n" +
                            "• Network / APN Reset: ${if (currentState) "🚫 Blocked" else "Allowed"}\n" +
                            "• OTG / External Media Mount: ${if (currentState) "🚫 Blocked" else "Allowed"}\n" +
                            "• USB File Transfer (MTP): ${if (currentState) "🚫 Blocked" else "Allowed"}\n" +
                            "• Notification Shade / Quick Settings: ${if (currentState) "🚫 Locked" else "Unlocked"}\n\n" +
                            "💡 <i>Usage: <code>/antitamper on</code> or <code>/antitamper off</code></i>"
                )
            }

            else -> CommandResult(
                success = false,
                message = "❓ <b>Invalid Parameter:</b> Use <code>/antitamper on</code>, <code>/antitamper off</code>, or <code>/antitamper status</code>."
            )
        }
    }
}
