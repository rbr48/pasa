package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Commands for lockscreen canvas banner customization and screen inactivity autolock policy.
 */
@Singleton
class LockscreenInfoCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : Command {

    override val name = "/lockscreen_info"
    override val description = "Set lockscreen info canvas & autolock policy [Device Owner]"
    override val usage = "/lockscreen_info [<text>|clear|status] | /autolock [<sec>|default|status]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        return executeLockScreenInfo(args)
    }

    suspend fun executeLockScreenInfo(args: List<String>): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> Custom lockscreen info banner requires Android Device Owner.\n" +
                        "Run <code>/device_owner</code> for instructions."
            )
        }

        if (args.isEmpty() || args[0].equals("status", ignoreCase = true)) {
            val current = PasaDeviceAdmin.getLockScreenInfo(context)?.toString() ?: prefs.lockScreenInfo
            val infoStr = if (current.isNotBlank()) "<code>$current</code>" else "<i>None (Default)</i>"
            return CommandResult(
                success = true,
                message = "📱 <b>Lockscreen Info Banner:</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Current: $infoStr\n\n" +
                        "💡 <i>Update using <code>/lockscreen_info &lt;message&gt;</code> or clear using <code>/lockscreen_info clear</code>.</i>"
            )
        }

        val first = args[0]
        if (first.equals("clear", ignoreCase = true) || first.equals("reset", ignoreCase = true) || first.equals("off", ignoreCase = true)) {
            val (ok, text) = PasaDeviceAdmin.setLockScreenInfo(context, null)
            if (ok) {
                prefs.lockScreenInfo = ""
                return CommandResult(success = true, message = "📱 <b>Lockscreen Banner Cleared</b>\nLockscreen restored to default display.")
            } else {
                return CommandResult(success = false, message = text)
            }
        }

        val newInfo = args.joinToString(" ").trim()
        val (ok, text) = PasaDeviceAdmin.setLockScreenInfo(context, newInfo)
        if (ok) {
            prefs.lockScreenInfo = newInfo
            return CommandResult(
                success = true,
                message = "📱 <b>Lockscreen Info Banner Updated</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Displayed Text: <b>$newInfo</b>\n" +
                        "🛡️ <i>Banner is permanently pinned to the OS lockscreen display.</i>"
            )
        } else {
            return CommandResult(success = false, message = text)
        }
    }

    suspend fun executeAutolock(args: List<String>): CommandResult {
        if (!PasaDeviceAdmin.isAdminActive(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Admin Required:</b> Autolock policy requires Android Device Admin.\n" +
                        "Run <code>/device_owner</code> for instructions."
            )
        }

        val param = args.firstOrNull()?.lowercase() ?: "status"

        if (param == "status") {
            val currentMs = PasaDeviceAdmin.getMaximumTimeToLock(context)
            val sec = currentMs / 1000L
            val desc = if (sec <= 0L) "System Default / No Restriction" else "${sec}s (${sec / 60}m)"
            return CommandResult(
                success = true,
                message = "⏱️ <b>Autolock Timeout Policy:</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Maximum Inactivity Limit: <b>$desc</b>\n\n" +
                        "💡 <i>Set limit using <code>/autolock &lt;seconds&gt;</code> (e.g. <code>/autolock 30</code>) or reset via <code>/autolock default</code>.</i>"
            )
        }

        if (param == "default" || param == "reset" || param == "off" || param == "0") {
            val (ok, text) = PasaDeviceAdmin.setMaximumTimeToLock(context, 0L)
            return CommandResult(success = ok, message = text)
        }

        val seconds = param.toLongOrNull()
        if (seconds == null || seconds < 5 || seconds > 3600) {
            return CommandResult(
                success = false,
                message = "❌ Invalid duration. Please provide a timeout between 5 and 3600 seconds (e.g. <code>/autolock 30</code>)."
            )
        }

        val timeoutMs = seconds * 1000L
        val (ok, text) = PasaDeviceAdmin.setMaximumTimeToLock(context, timeoutMs)
        return CommandResult(success = ok, message = text)
    }
}
