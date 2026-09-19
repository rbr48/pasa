package com.izhaanintellect.pasa.commands

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects and returns network connectivity, IP address, and signal metrics.
 */
@Singleton
class NetworkCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/network"
    override val description = "Network connection and IP diagnostics"
    override val usage = "/network"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val sb = StringBuilder()
        sb.appendLine("🌐 <b>Network Connection Report</b>")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }

        if (caps == null) {
            sb.appendLine("❌ Device has no active internet connection.")
            return CommandResult(success = true, message = sb.toString())
        }

        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "📶 WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "📱 Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "🔌 Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "🔒 VPN Connection"
            else -> "Connected"
        }
        sb.appendLine("📡 <b>Primary Link:</b> $type")

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val info = wm.connectionInfo
                sb.appendLine("📶 <b>SSID:</b> ${info.ssid}")
                sb.appendLine("⚡ <b>Link Speed:</b> ${info.linkSpeed} Mbps")
                sb.appendLine("📶 <b>Frequency:</b> ${info.frequency} MHz")
            } catch (e: Exception) {
                // WiFi details omitted if permissions restricted
            }
        }

        val ip = getLocalIpAddress()
        sb.appendLine("🔢 <b>Internal IP:</b> ${ip ?: "Unavailable"}")

        // Bandwidth
        val down = caps.linkDownstreamBandwidthKbps
        val up = caps.linkUpstreamBandwidthKbps
        if (down > 0 || up > 0) {
            sb.appendLine("⬇️ <b>Downstream:</b> ${down / 1000} Mbps")
            sb.appendLine("⬆️ <b>Upstream:</b> ${up / 1000} Mbps")
        }

        sb.appendLine()
        sb.appendLine("<b>Capabilities:</b>")
        sb.appendLine(if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) "  ✅ Internet Reachable" else "  ❌ No Internet")
        sb.appendLine(if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "  ✅ Validated" else "  ⚠️ Unvalidated")
        sb.appendLine(if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) "  ✅ Unmetered" else "  ⚠️ Metered Data")

        return CommandResult(success = true, message = sb.toString())
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap {
                it.inetAddresses.toList()
            }?.find { !it.isLoopbackAddress && it is Inet4Address }?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
