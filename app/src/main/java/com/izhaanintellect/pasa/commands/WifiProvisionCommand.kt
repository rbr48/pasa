package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emergency Wi-Fi auto-provisioning command.
 * Allows owner to connect locked/isolated device to a nearby Wi-Fi network remotely.
 */
@Singleton
class WifiProvisionCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/wifi_connect"
    override val description = "Emergency Wi-Fi auto-provisioning while locked"
    override val usage = "/wifi_connect <ssid> [password]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (args.isEmpty()) {
            return CommandResult(
                success = false,
                message = "❌ Missing parameters. Usage: <code>/wifi_connect &lt;ssid&gt; [password]</code>"
            )
        }

        val ssid = args[0].trim()
        val password = if (args.size > 1) args[1].trim() else ""

        val (ok, text) = PasaDeviceAdmin.connectWifi(context, ssid, password)
        return CommandResult(success = ok, message = text)
    }
}
