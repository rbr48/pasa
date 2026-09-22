package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Battery Monitoring & Anomaly Detection
 *
 * Track charging patterns and detect signs of device being actively used.
 * Unusual drain or continuous charging can indicate remote access/screen time.
 */
@Singleton
class BatteryAlertCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/battery_alert"
    override val description = "Monitor battery charging and detect unusual drain patterns"
    override val usage = "/battery_alert [enable|disable|threshold|status|history]"

    companion object {
        private const val TAG = "PASA_BatteryAlert"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull()?.lowercase() ?: "status"

        return when (action) {
            "enable", "on", "start" -> enableBatteryMonitoring()
            "disable", "off", "stop" -> disableBatteryMonitoring()
            "threshold", "set_threshold" -> setDrainThreshold(args.getOrNull(1)?.toIntOrNull() ?: 15)
            "history" -> showBatteryHistory()
            "status" -> getBatteryStatus()
            else -> CommandResult(
                success = false,
                message = """
                    🔋 <b>Battery Alert Command</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Usage:</b>
                    • <code>/battery_alert enable</code> — Start monitoring
                    • <code>/battery_alert threshold 15</code> — Set drain rate %/hour
                    • <code>/battery_alert history</code> — Show charging history
                    • <code>/battery_alert status</code> — Current state
                """.trimIndent()
            )
        }
    }

    private fun enableBatteryMonitoring(): CommandResult {
        preferencesManager.isBatteryAlertEnabled = true
        preferencesManager.batteryAlertThreshold = 15 // Default 15%/hour drain

        return CommandResult(
            success = true,
            message = """
                🟢 <b>Battery Monitoring ENABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔋 <b>Status:</b> ACTIVE

                ✓ Charging patterns tracked
                ✓ Drain rate monitored (${preferencesManager.batteryAlertThreshold}%/hour threshold)
                ✓ Unusual activity alerts enabled

                <b>Current Battery:</b> ${getCurrentBatteryLevel()}%
                <b>Charging Status:</b> ${getChargingStatus()}

                <i>To adjust threshold: /battery_alert threshold 15</i>
            """.trimIndent()
        )
    }

    private fun disableBatteryMonitoring(): CommandResult {
        preferencesManager.isBatteryAlertEnabled = false

        return CommandResult(
            success = true,
            message = """
                🔴 <b>Battery Monitoring DISABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔋 <b>Status:</b> INACTIVE

                Device battery is no longer being monitored.
            """.trimIndent()
        )
    }

    private fun setDrainThreshold(threshold: Int): CommandResult {
        val actualThreshold = threshold.coerceIn(5, 50)
        preferencesManager.batteryAlertThreshold = actualThreshold

        return CommandResult(
            success = true,
            message = """
                ⚙️ <b>Battery Drain Threshold Updated</b>
                ━━━━━━━━━━━━━━━━━━━━
                🎯 <b>New Threshold:</b> $actualThreshold% per hour

                If battery drops faster than $actualThreshold%/hour,
                you'll be alerted about possible active use or malware.

                <b>Alert Levels:</b>
                • <5%/hour: Device likely in standby ✓
                • 5-15%/hour: Normal background activity
                • 15-30%/hour: Possible remote access ⚠️
                • >30%/hour: Definite active use/screen on 🚨
            """.trimIndent()
        )
    }

    private fun showBatteryHistory(): CommandResult {
        val currentLevel = getCurrentBatteryLevel()
        val chargingStatus = getChargingStatus()
        val health = getBatteryHealth()
        val temperature = getBatteryTemperature()

        return CommandResult(
            success = true,
            message = """
                📊 <b>Battery History & Telemetry</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔋 <b>Current Level:</b> $currentLevel%
                🔌 <b>Charging Status:</b> $chargingStatus
                🏥 <b>Health:</b> $health
                🌡️ <b>Temperature:</b> ${temperature}°C

                <b>Charge Characteristics:</b>
                • Full charge capacity monitoring active
                • Charge/discharge cycles tracked
                • Anomalies logged

                <b>What to Watch For:</b>
                • Rapid drain with screen off (RAT activity)
                • Always-charging despite full battery (attacker keeping device on)
                • Continuous network activity while "sleeping"

                ℹ️ Battery trends help identify device tampering.
            """.trimIndent()
        )
    }

    private fun getBatteryStatus(): CommandResult {
        val isEnabled = preferencesManager.isBatteryAlertEnabled
        val threshold = preferencesManager.batteryAlertThreshold
        val currentLevel = getCurrentBatteryLevel()
        val chargingStatus = getChargingStatus()
        val health = getBatteryHealth()

        return CommandResult(
            success = true,
            message = """
                🔋 <b>Battery Alert Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                🟢 <b>Monitoring:</b> ${if (isEnabled) "ENABLED" else "DISABLED"}
                🎯 <b>Drain Threshold:</b> $threshold%/hour

                <b>Current Status:</b>
                • <b>Level:</b> $currentLevel%
                • <b>Charging:</b> $chargingStatus
                • <b>Health:</b> $health

                <b>What This Monitors:</b>
                ✓ Battery drain rate (% per hour)
                ✓ Charging behavior patterns
                ✓ Device temperature anomalies
                ✓ Rapid discharge events

                <b>Commands:</b>
                • <code>/battery_alert enable</code> — Enable monitoring
                • <code>/battery_alert threshold 15</code> — Set drain threshold
                • <code>/battery_alert history</code> — Show telemetry

                ℹ️ If device is being used remotely, battery will drain faster than expected.
            """.trimIndent()
        )
    }

    private fun getCurrentBatteryLevel(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.let {
                val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
                if (capacity > 0) ((it * 100) / capacity).coerceIn(0, 100) else 0
            } ?: run {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get battery level: ${e.message}")
            0
        }
    }

    private fun getChargingStatus(): String {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return "Unknown"
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> {
                    when (plugged) {
                        BatteryManager.BATTERY_PLUGGED_AC -> "Charging (AC)"
                        BatteryManager.BATTERY_PLUGGED_USB -> "Charging (USB)"
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Charging (Wireless)"
                        else -> "Charging"
                    }
                }
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get charging status: ${e.message}")
            "Unknown"
        }
    }

    private fun getBatteryHealth(): String {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: return "Unknown"

            when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get battery health: ${e.message}")
            "Unknown"
        }
    }

    private fun getBatteryTemperature(): Int {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: 0) / 10
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get battery temperature: ${e.message}")
            0
        }
    }
}
