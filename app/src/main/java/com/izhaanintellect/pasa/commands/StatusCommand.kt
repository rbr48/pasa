package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates an instant telemetry status report for the device.
 */
@Singleton
class StatusCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationTracker: LocationTracker,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/status"
    override val description = "Device telemetry report (battery, network, RAM, storage, location)"
    override val usage = "/status"

    companion object {
        private const val TAG = "PASA_Status"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val sb = StringBuilder()
        sb.appendLine("🛡️ <b>PASA Device Status Report</b>")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        // Battery
        val battery = getBatteryInfo()
        sb.appendLine("🔋 <b>Battery:</b> ${battery.first}%${if (battery.second) " ⚡ Charging" else ""}")

        // Network
        sb.appendLine("🌐 <b>Network:</b> ${getNetworkType()}")

        // Location
        try {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                val lat = String.format(Locale.US, "%.5f", location.latitude)
                val lng = String.format(Locale.US, "%.5f", location.longitude)
                sb.appendLine("📍 <b>Location:</b> $lat, $lng")
                sb.appendLine("    🗺️ <a href=\"https://maps.google.com/maps?q=$lat,$lng\">Open in Google Maps</a>")
            } else {
                sb.appendLine("📍 <b>Location:</b> Unavailable")
            }
        } catch (e: Exception) {
            sb.appendLine("📍 <b>Location:</b> Error")
        }

        // Uptime
        val uptimeMs = SystemClock.elapsedRealtime()
        val hours = TimeUnit.MILLISECONDS.toHours(uptimeMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60
        sb.appendLine("⏱️ <b>Uptime:</b> ${hours}h ${minutes}m")

        // Storage & Memory
        val storage = getStorageInfo()
        sb.appendLine("💾 <b>Storage:</b> ${storage.first} free / ${storage.second} total")

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        sb.appendLine("🧠 <b>RAM:</b> ${formatBytes(memInfo.availMem)} free / ${formatBytes(memInfo.totalMem)} total")

        // Guardian State
        sb.appendLine()
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🛡️ <b>Sentinel:</b> Active ✅")
        sb.appendLine("💎 <b>Security Chip:</b> ${preferencesManager.deviceKeySecurityLevel}")
        sb.appendLine("🔐 <b>Verified Sequence:</b> #${preferencesManager.replayHighWaterMark}")
        sb.appendLine("📡 <b>Tracking:</b> ${if (preferencesManager.isTrackingActive) "Active (${preferencesManager.trackingIntervalMinutes}m)" else "Off"}")
        sb.appendLine("🔇 <b>Stealth Mode:</b> ${if (preferencesManager.isStealthMode) "Enabled" else "Disabled"}")

        return CommandResult(success = true, message = sb.toString())
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(context, null, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(null, filter)
        }

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val percentage = if (scale > 0) (level * 100) / scale else 0
        return Pair(percentage, isCharging)
    }

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "No Connection"
        val caps = cm.getNetworkCapabilities(network) ?: return "Unknown"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (Mobile Data)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Active"
            else -> "Connected"
        }
    }

    private fun getStorageInfo(): Pair<String, String> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            Pair(formatBytes(stat.availableBytes), formatBytes(stat.totalBytes))
        } catch (e: Exception) {
            Pair("Unknown", "Unknown")
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1) {
            String.format(Locale.US, "%.1f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format(Locale.US, "%.0f MB", mb)
        }
    }
}
