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
    override val usage = "/wifi_connect <ssid> [password] | status | on | off"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (args.isEmpty()) {
            val (ok, statusText) = PasaDeviceAdmin.getWifiStatus(context)
            val buttons = com.izhaanintellect.pasa.bot.InlineKeyboardMarkup(
                inlineKeyboard = listOf(
                    listOf(
                        com.izhaanintellect.pasa.bot.InlineKeyboardButton("🟢 Turn Wi-Fi ON", callbackData = "cmd:wifi_connect:on"),
                        com.izhaanintellect.pasa.bot.InlineKeyboardButton("🔴 Turn Wi-Fi OFF", callbackData = "cmd:wifi_connect:off")
                    ),
                    listOf(
                        com.izhaanintellect.pasa.bot.InlineKeyboardButton("🔄 Refresh Status", callbackData = "cmd:wifi_connect:status"),
                        com.izhaanintellect.pasa.bot.InlineKeyboardButton("🔙 Back to Hub", callbackData = "menu:device_owner_hub")
                    )
                )
            )
            return CommandResult(
                success = true,
                message = "$statusText\n\n" +
                        "📶 <b>Auto-Connect to Wi-Fi Network:</b>\n" +
                        "• <code>/wifi_connect &lt;ssid&gt; &lt;password&gt;</code>\n" +
                        "• <code>/wifi_connect &lt;open_ssid&gt;</code>\n\n" +
                        "<b>Example:</b>\n" +
                        "<code>/wifi_connect HomeNetwork MySecretPass123</code>",
                replyMarkup = buttons
            )
        }

        val firstLower = args.first().lowercase().trim()
        when (firstLower) {
            "status", "check", "info" -> {
                val (ok, text) = PasaDeviceAdmin.getWifiStatus(context)
                return CommandResult(success = ok, message = text)
            }
            "on", "enable", "start" -> {
                val (ok, text) = PasaDeviceAdmin.setWifiEnabled(context, true)
                return CommandResult(success = ok, message = text)
            }
            "off", "disable", "stop" -> {
                val (ok, text) = PasaDeviceAdmin.setWifiEnabled(context, false)
                return CommandResult(success = ok, message = text)
            }
        }

        // Smart SSID & Password Parsing:
        // Handles:
        // 1. /wifi_connect "My Network Name" SecretPass123
        // 2. /wifi_connect My Network Name SecretPass123
        // 3. /wifi_connect OpenNetwork
        val fullJoined = args.joinToString(" ").trim()
        val (ssid, password) = if (fullJoined.startsWith("\"")) {
            val closingQuote = fullJoined.indexOf("\"", 1)
            if (closingQuote != -1) {
                val s = fullJoined.substring(1, closingQuote).trim()
                val p = fullJoined.substring(closingQuote + 1).trim().removeSurrounding("\"")
                Pair(s, p)
            } else {
                Pair(fullJoined.removePrefix("\"").trim(), "")
            }
        } else if (args.size == 1) {
            Pair(args[0].trim().removeSurrounding("\""), "")
        } else if (args.size == 2) {
            Pair(args[0].trim().removeSurrounding("\""), args[1].trim().removeSurrounding("\""))
        } else {
            // Assume the last token is the password if length >= 8, otherwise join all as open SSID
            val candidatePass = args.last().trim().removeSurrounding("\"")
            if (candidatePass.length >= 8) {
                Pair(args.dropLast(1).joinToString(" ").trim().removeSurrounding("\""), candidatePass)
            } else {
                Pair(fullJoined.removeSurrounding("\""), "")
            }
        }

        if (ssid.isBlank()) {
            return CommandResult(
                success = false,
                message = "❌ Missing SSID. Usage: <code>/wifi_connect &lt;ssid&gt; [password]</code>"
            )
        }

        val (ok, text) = PasaDeviceAdmin.connectWifi(context, ssid, password)
        return CommandResult(success = ok, message = text)
    }
}
