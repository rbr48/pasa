package com.izhaanintellect.pasa.commands

import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.detection.TrapManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrapCommand @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val trapManager: TrapManager
) : Command {

    override val name = "/trap"
    override val description = "Configure and arm autonomous sensor defense traps"
    override val usage = "/trap [status|on|off|snatch on/off|charger on/off|pocket on/off]"

    companion object {
        private const val TAG = "PASA_TrapCommand"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (args.isEmpty() || args[0].equals("status", ignoreCase = true)) {
            val master = preferencesManager.isTrapEnabled
            val snatch = preferencesManager.isSnatchTrapEnabled
            val charger = preferencesManager.isChargerTrapEnabled
            val pocket = preferencesManager.isPocketTrapEnabled

            return CommandResult(
                success = true,
                message = "🛡️ <b>Autonomous Edge Traps Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Master Trap Armed: ${if (master) "🟢 <b>ARMED</b>" else "🔴 <b>DISARMED</b>"}\n" +
                        "• Snatch & Grab Trap (>2.6G): ${if (snatch) "✅ Enabled" else "❌ Disabled"}\n" +
                        "• Charger Disconnect Trap: ${if (charger) "✅ Enabled" else "❌ Disabled"}\n" +
                        "• Pocket / Bag Extraction Trap: ${if (pocket) "✅ Enabled (5s Grace)" else "❌ Disabled"}\n\n" +
                        "<b>Commands:</b>\n" +
                        "• <code>/trap on</code> — Arm all autonomous traps\n" +
                        "• <code>/trap off</code> — Disarm all traps\n" +
                        "• <code>/trap snatch on|off</code> — Toggle snatch trap\n" +
                        "• <code>/trap charger on|off</code> — Toggle charger trap\n" +
                        "• <code>/trap pocket on|off</code> — Toggle pocket extraction trap"
            )
        }

        val action = args[0].lowercase()

        when (action) {
            "on", "arm", "enable" -> {
                preferencesManager.isTrapEnabled = true
                trapManager.startMonitoring()
                Log.i(TAG, "Master traps armed")
                return CommandResult(
                    success = true,
                    message = "🟢 <b>Autonomous Traps ARMED!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "The device will now autonomously lock and take photos if violent snatch acceleration is detected, charger is unplugged while locked, or device is pulled from pocket without unlock."
                )
            }
            "off", "disarm", "disable" -> {
                preferencesManager.isTrapEnabled = false
                trapManager.stopMonitoring()
                Log.i(TAG, "Master traps disarmed")
                return CommandResult(
                    success = true,
                    message = "🔴 <b>Autonomous Traps DISARMED.</b>"
                )
            }
            "snatch" -> {
                val state = if (args.size > 1) args[1].lowercase() == "on" else !preferencesManager.isSnatchTrapEnabled
                preferencesManager.isSnatchTrapEnabled = state
                return CommandResult(
                    success = true,
                    message = "🏃 <b>Snatch Trap:</b> ${if (state) "✅ Enabled" else "❌ Disabled"}"
                )
            }
            "charger" -> {
                val state = if (args.size > 1) args[1].lowercase() == "on" else !preferencesManager.isChargerTrapEnabled
                preferencesManager.isChargerTrapEnabled = state
                return CommandResult(
                    success = true,
                    message = "🔌 <b>Charger Disconnect Trap:</b> ${if (state) "✅ Enabled" else "❌ Disabled"}"
                )
            }
            "pocket", "bag", "pickpocket" -> {
                val state = if (args.size > 1) args[1].lowercase() == "on" else !preferencesManager.isPocketTrapEnabled
                preferencesManager.isPocketTrapEnabled = state
                return CommandResult(
                    success = true,
                    message = "👖 <b>Pocket/Bag Extraction Trap:</b> ${if (state) "✅ Enabled (5s Grace)" else "❌ Disabled"}"
                )
            }
            else -> {
                return CommandResult(
                    success = false,
                    message = "⚠️ Unknown action <code>$action</code>. Use <code>/trap on</code>, <code>/trap off</code>, <code>/trap pocket on|off</code>, or <code>/trap status</code>."
                )
            }
        }
    }
}
