package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware peripheral killswitches: Bluetooth lockdown and Master Audio Mute.
 */
@Singleton
class PeripheralLockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : Command {

    override val name = "/bluetooth_lock"
    override val description = "Hardware Bluetooth & Audio Mute controls [Device Owner]"
    override val usage = "/bluetooth_lock [on|off|status] | /mic_mute [on|off|status]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        return executeBluetooth(args)
    }

    suspend fun executeBluetooth(args: List<String>): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> Hardware Bluetooth restriction requires Device Owner.\n" +
                        "Run <code>/device_owner</code> for instructions."
            )
        }

        val param = args.firstOrNull()?.lowercase() ?: "status"

        return when (param) {
            "on", "lock", "disable" -> {
                val (ok, text) = PasaDeviceAdmin.setBluetoothDisabled(context, true)
                if (ok) {
                    prefs.isBluetoothLocked = true
                    CommandResult(
                        success = true,
                        message = "📡 <b>Hardware Bluetooth Lock: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "🚫 <b>Status:</b> BLUETOOTH & SHARING BLOCKED\n" +
                                "🔒 <i>Bluetooth radio pairing and file transfer are strictly prohibited by Device Owner policy.</i>"
                    )
                } else {
                    CommandResult(success = false, message = text)
                }
            }
            "off", "unlock", "enable" -> {
                val (ok, text) = PasaDeviceAdmin.setBluetoothDisabled(context, false)
                if (ok) {
                    prefs.isBluetoothLocked = false
                    CommandResult(
                        success = true,
                        message = "📡 <b>Hardware Bluetooth Lock: DISARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "✅ <b>Status:</b> BLUETOOTH OPERATIONAL\n" +
                                "🔓 <i>Bluetooth radio restrictions cleared.</i>"
                    )
                } else {
                    CommandResult(success = false, message = text)
                }
            }
            "status" -> {
                val disabled = PasaDeviceAdmin.isBluetoothDisabled(context)
                val statusText = if (disabled) "🚫 LOCKED (Disallowed)" else "✅ UNLOCKED (Allowed)"
                CommandResult(
                    success = true,
                    message = "📡 <b>Hardware Bluetooth Status:</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• State: $statusText\n" +
                            "• Device Owner Restriction: Active"
                )
            }
            else -> CommandResult(
                success = false,
                message = "❌ Invalid syntax. Usage: <code>/bluetooth_lock [on|off|status]</code>"
            )
        }
    }

    suspend fun executeMicMute(args: List<String>): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> Master audio mute requires Android Device Owner.\n" +
                        "Run <code>/device_owner</code> for instructions."
            )
        }

        val param = args.firstOrNull()?.lowercase() ?: "status"

        return when (param) {
            "on", "mute", "disable" -> {
                val (ok, text) = PasaDeviceAdmin.setMasterMute(context, true)
                if (ok) {
                    prefs.isMicMuted = true
                    CommandResult(
                        success = true,
                        message = "🔇 <b>Hardware Master Audio Mute: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "🚫 <b>Status:</b> SYSTEM AUDIO MUTED\n" +
                                "🔇 <i>All media, ringtones, alarms, and audio playback are muted at the hardware HAL level.</i>"
                    )
                } else {
                    CommandResult(success = false, message = text)
                }
            }
            "off", "unmute", "enable" -> {
                val (ok, text) = PasaDeviceAdmin.setMasterMute(context, false)
                if (ok) {
                    prefs.isMicMuted = false
                    CommandResult(
                        success = true,
                        message = "🔊 <b>Hardware Master Audio Mute: DISARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "✅ <b>Status:</b> AUDIO OPERATIONAL\n" +
                                "🔊 <i>Master mute cleared and volume output restored.</i>"
                    )
                } else {
                    CommandResult(success = false, message = text)
                }
            }
            "status" -> {
                val muted = PasaDeviceAdmin.isMasterMute(context)
                val statusText = if (muted) "🔇 MUTED (Audio Silenced)" else "🔊 UNMUTED (Normal Audio)"
                CommandResult(
                    success = true,
                    message = "🔇 <b>Hardware Master Audio Status:</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• State: $statusText\n" +
                            "• Device Owner Policy: Enforced"
                )
            }
            else -> CommandResult(
                success = false,
                message = "❌ Invalid syntax. Usage: <code>/mic_mute [on|off|status]</code>"
            )
        }
    }
}
