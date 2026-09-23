package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASA Thermal Anomaly Trap Command (/thermal)
 *
 * Counters physical lab disassembly and heat-gun attacks intended to short
 * Qualcomm 9008 EDL or MediaTek BROM test points.
 * Monitored via BatteryManager.EXTRA_TEMPERATURE while device is locked.
 */
@Singleton
class ThermalTrapCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/thermal"
    override val description = "Anti-EDL heat-gun & thermal anomaly trap"
    override val usage = "/thermal [on|off|threshold <40-65>|status]"

    companion object {
        private const val TAG = "PASA_ThermalTrap"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull()?.lowercase() ?: "status"

        return when (action) {
            "on", "enable", "arm" -> enableTrap()
            "off", "disable", "disarm" -> disableTrap()
            "threshold", "temp", "set" -> {
                val temp = args.getOrNull(1)?.toIntOrNull()
                if (temp == null || temp !in 40..65) {
                    CommandResult(
                        success = false,
                        message = "❌ Please specify a threshold between 40°C and 65°C, e.g. <code>/thermal threshold 48</code>"
                    )
                } else {
                    setThreshold(temp)
                }
            }
            "status", "info" -> getStatus()
            else -> CommandResult(
                success = false,
                message = """
                    🔥 <b>PASA Thermal Anomaly Trap (Anti-EDL / Heat-Gun Defense)</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Usage:</b>
                    • <code>/thermal on</code> — Enable thermal defense trap
                    • <code>/thermal off</code> — Disable thermal trap
                    • <code>/thermal threshold 48</code> — Set trigger temperature (40°C - 65°C)
                    • <code>/thermal status</code> — View current battery temp & configuration

                    🛡️ <b>Hardware Protection:</b>
                    If a thief heats the phone to unglue the back cover for Qualcomm EDL (9008) test-point shorts, PASA instantly engages Knox Kiosk lock and severs USB data pins.
                """.trimIndent()
            )
        }
    }

    private fun enableTrap(): CommandResult {
        preferencesManager.isThermalTrapEnabled = true
        Log.i(TAG, "Thermal Anomaly Trap enabled with threshold: ${preferencesManager.thermalTrapThresholdCelsius}°C")

        return CommandResult(
            success = true,
            message = """
                🟢 <b>Thermal Anomaly Trap ARMED</b>
                ━━━━━━━━━━━━━━━━━━━━
                🌡️ <b>Threshold:</b> ${preferencesManager.thermalTrapThresholdCelsius}°C
                🛡️ <b>Countermeasure:</b> Sever USB pins & Lock Knox Kiosk
                ⚠️ <i>Monitors battery temperature while phone is locked against heat-gun lab disassembly.</i>
            """.trimIndent()
        )
    }

    private fun disableTrap(): CommandResult {
        preferencesManager.isThermalTrapEnabled = false
        Log.i(TAG, "Thermal Anomaly Trap disabled")

        return CommandResult(
            success = true,
            message = """
                🔴 <b>Thermal Anomaly Trap DISARMED</b>
                ━━━━━━━━━━━━━━━━━━━━
                Battery thermal anomaly monitoring has been deactivated.
            """.trimIndent()
        )
    }

    private fun setThreshold(temp: Int): CommandResult {
        preferencesManager.thermalTrapThresholdCelsius = temp
        Log.i(TAG, "Thermal trap threshold set to ${temp}°C")

        return CommandResult(
            success = true,
            message = """
                🌡️ <b>Thermal Trap Threshold Updated</b>
                ━━━━━━━━━━━━━━━━━━━━
                Trigger temperature set to <b>${temp}°C</b>.
                If battery temperature reaches ${temp}°C while locked, emergency countermeasures will execute autonomously.
            """.trimIndent()
        )
    }

    private fun getStatus(): CommandResult {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val currentTemp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: 0) / 10
        val isCharging = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
        val isEnabled = preferencesManager.isThermalTrapEnabled
        val threshold = preferencesManager.thermalTrapThresholdCelsius
        val isDo = PasaDeviceAdmin.isDeviceOwner(context)

        return CommandResult(
            success = true,
            message = """
                🔥 <b>Thermal Anomaly Trap Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                • <b>State:</b> ${if (isEnabled) "🟢 <b>ARMED</b>" else "🔴 <b>DISARMED</b>"}
                • <b>Current Temperature:</b> ${currentTemp}°C
                • <b>Trigger Threshold:</b> ${threshold}°C
                • <b>Charging State:</b> ${if (isCharging) "⚡ Charging" else "🔋 Discharging"}
                • <b>Knox Hardware Lockout:</b> ${if (isDo) "✅ Available (Device Owner)" else "⚠️ Limited (Non-DO)"}
                
                <i>Target Attack: Qualcomm 9008 EDL / MediaTek BROM heat-gun disassembly.</i>
            """.trimIndent()
        )
    }
}
