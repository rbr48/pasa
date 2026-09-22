package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware camera killswitch using Device Admin / Device Owner.
 * Disables all device cameras system-wide to preserve privacy or prevent surveillance.
 */
@Singleton
class CameraLockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : Command {

    override val name = "/camera_lock"
    override val description = "Hardware camera killswitch [Device Admin / Owner]"
    override val usage = "/camera_lock [on|off|status]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isAdminActive(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Admin Required:</b> Camera lockout requires Android Device Admin privileges.\n" +
                        "Enable Device Admin in PASA Setup or grant Device Owner."
            )
        }

        val param = args.firstOrNull()?.lowercase() ?: "status"

        return when (param) {
            "on", "lock", "disable" -> {
                val (ok, text) = PasaDeviceAdmin.setCameraDisabled(context, true)
                if (ok) {
                    prefs.isCameraLocked = true
                    CommandResult(
                        success = true,
                        message = "📷 <b>Hardware Camera Lock: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "🚫 <b>Status:</b> ALL CAMERAS DISABLED\n" +
                                "🔒 <i>Any application attempting camera access will receive a SecurityException.</i>\n\n" +
                                "💡 <i>Restore camera anytime via <code>/camera_lock off</code>.</i>"
                    )
                } else {
                    CommandResult(success = false, message = text)
                }
            }
            "off", "unlock", "enable" -> {
                val (ok, text) = PasaDeviceAdmin.setCameraDisabled(context, false)
                if (ok) {
                    prefs.isCameraLocked = false
                    CommandResult(
                        success = true,
                        message = "📷 <b>Hardware Camera Lock: DISARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "✅ <b>Status:</b> CAMERAS OPERATIONAL\n" +
                                "🔓 <i>Normal camera access has been restored across the system.</i>"
                    )
                } else {
                    CommandResult(success = false, message = text)
                }
            }
            "status" -> {
                val disabled = PasaDeviceAdmin.isCameraDisabled(context)
                val statusText = if (disabled) "🚫 LOCKED (Cameras Disabled)" else "✅ UNLOCKED (Cameras Operational)"
                CommandResult(
                    success = true,
                    message = "📷 <b>Hardware Camera Status:</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• State: $statusText\n" +
                            "• Admin Policy: Active\n\n" +
                            "💡 <i>Toggle using <code>/camera_lock on</code> or <code>/camera_lock off</code>.</i>"
                )
            }
            else -> CommandResult(
                success = false,
                message = "❌ Invalid syntax. Usage: <code>/camera_lock [on|off|status]</code>"
            )
        }
    }
}
