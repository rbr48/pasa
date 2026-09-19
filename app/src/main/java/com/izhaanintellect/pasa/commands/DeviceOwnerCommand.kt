package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks and reports Android Enterprise Device Owner provisioning status
 * and provides setup instructions.
 */
@Singleton
class DeviceOwnerCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/device_owner"
    override val description = "Check Device Owner enterprise protection status"
    override val usage = "/device_owner"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val isOwner = PasaDeviceAdmin.isDeviceOwner(context)
        val isAdmin = PasaDeviceAdmin.isAdminActive(context)

        return if (isOwner) {
            CommandResult(
                success = true,
                message = "👑 <b>Android Device Owner: ACTIVE</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "✅ <b>Kiosk Lock Task:</b> Ready (Locks Home/Back/Recent/Status Bar)\n" +
                        "✅ <b>Anti-Uninstall:</b> Active (App cannot be removed from Settings)\n" +
                        "✅ <b>Lockscreen Injection:</b> Active\n\n" +
                        "🛡️ <i>Your device has maximum hardware-level enterprise protection enabled!</i>"
            )
        } else {
            CommandResult(
                success = true,
                message = "👑 <b>Android Device Owner: INACTIVE</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "Current Device Admin: ${if (isAdmin) "✅ Active (Standard)" else "❌ Inactive"}\n\n" +
                        "<b>How to activate Enterprise Device Owner:</b>\n" +
                        "1. Enable Developer Options & USB Debugging on your phone.\n" +
                        "2. Connect phone to PC via USB.\n" +
                        "3. Run this command in terminal/PowerShell:\n" +
                        "<code>adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin</code>\n\n" +
                        "<i>Tip: If prompted with an account error, temporarily remove Google accounts in Settings > Accounts, run the command, then re-add your accounts.</i>"
            )
        }
    }
}
