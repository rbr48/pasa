package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASA Dead Man's Switch — Autonomous Anti-EDL & Anti-Theft Auto-Destruct
 *
 * Designed to counter offline lab attacks (Qualcomm 9008 EDL, MediaTek BROM, and chip-off dumps).
 * If the device is separated from the owner or held in theft lockdown without a valid heartbeat
 * for the configured timeout, it initiates an autonomous cryptographic factory purge (dpm.wipeData(0)).
 */
@Singleton
class DeadManSwitchCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/deadman"
    override val description = "Anti-EDL offline auto-destruct timer"
    override val usage = "/deadman [enable|disable|hours <1-72>|status|heartbeat]"

    companion object {
        private const val TAG = "PASA_DeadMan"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull()?.lowercase() ?: "status"

        return when (action) {
            "enable", "on", "start", "arm" -> enableDeadMan()
            "disable", "off", "stop", "disarm" -> disableDeadMan()
            "hours", "time", "set" -> {
                val h = args.getOrNull(1)?.toIntOrNull()
                if (h == null || h !in 1..72) {
                    CommandResult(
                        success = false,
                        message = "❌ Please specify hours between 1 and 72, e.g. <code>/deadman hours 6</code>"
                    )
                } else {
                    setHours(h)
                }
            }
            "heartbeat", "ping", "reset" -> resetHeartbeat()
            "status", "info" -> getStatus()
            else -> CommandResult(
                success = false,
                message = """
                    💀 <b>PASA Dead Man's Switch (Anti-EDL Auto-Destruct)</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Usage:</b>
                    • <code>/deadman enable</code> — Arm the countdown timer
                    • <code>/deadman disable</code> — Disarm the timer
                    • <code>/deadman hours 6</code> — Set timeout duration (1 to 72 hrs)
                    • <code>/deadman heartbeat</code> — Reset heartbeat manually
                    • <code>/deadman status</code> — Check countdown & state

                    🛡️ <b>Anti-Forensic Purpose:</b>
                    If stolen and kept offline in a lab, the phone wipes itself BEFORE the thief can attempt EDL/BROM test-point attacks.
                """.trimIndent()
            )
        }
    }

    private fun enableDeadMan(): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ Device Owner permission required to arm Dead Man's Switch."
            )
        }

        preferencesManager.isDeadManSwitchEnabled = true
        preferencesManager.lastOwnerHeartbeatTime = System.currentTimeMillis()
        Log.i(TAG, "💀 Dead Man's Switch ARMED with timeout: ${preferencesManager.deadManTimeoutHours} hours")

        return CommandResult(
            success = true,
            message = """
                🟢 <b>Dead Man's Switch ARMED</b>
                ━━━━━━━━━━━━━━━━━━━━
                ⏳ <b>Timeout:</b> ${preferencesManager.deadManTimeoutHours} hours
                💓 <b>Heartbeat:</b> Reset to NOW
                🛡️ <b>Target:</b> Cryptographic Purge (<code>dpm.wipeData(0)</code>)

                ✓ Whenever you unlock the phone or send any command, the timer resets automatically.
                ✓ If stolen and isolated for > ${preferencesManager.deadManTimeoutHours}h, the phone purges all data before EDL flashing can be attempted.

                <i>To check state: /deadman status</i>
            """.trimIndent()
        )
    }

    private fun disableDeadMan(): CommandResult {
        preferencesManager.isDeadManSwitchEnabled = false
        Log.i(TAG, "Dead Man's Switch DISARMED")

        return CommandResult(
            success = true,
            message = """
                🔴 <b>Dead Man's Switch DISARMED</b>
                ━━━━━━━━━━━━━━━━━━━━
                The autonomous auto-destruct countdown timer has been deactivated.
                <i>To re-arm: /deadman enable</i>
            """.trimIndent()
        )
    }

    private fun setHours(hours: Int): CommandResult {
        preferencesManager.deadManTimeoutHours = hours
        preferencesManager.lastOwnerHeartbeatTime = System.currentTimeMillis()

        return CommandResult(
            success = true,
            message = """
                ⏱️ <b>Dead Man's Switch Timeout Updated</b>
                ━━━━━━━━━━━━━━━━━━━━
                ⏳ <b>New Duration:</b> $hours hours
                💓 <b>Heartbeat:</b> Reset to NOW

                The phone will auto-purge if isolated offline in theft mode for over $hours hours.
            """.trimIndent()
        )
    }

    private fun resetHeartbeat(): CommandResult {
        preferencesManager.lastOwnerHeartbeatTime = System.currentTimeMillis()

        return CommandResult(
            success = true,
            message = """
                💓 <b>Heartbeat Reset</b>
                ━━━━━━━━━━━━━━━━━━━━
                Countdown timer has been refreshed.
                <b>Next Expiration:</b> in ${preferencesManager.deadManTimeoutHours} hours.
            """.trimIndent()
        )
    }

    private fun getStatus(): CommandResult {
        val isArmed = preferencesManager.isDeadManSwitchEnabled
        val hours = preferencesManager.deadManTimeoutHours
        val lastHeartbeat = preferencesManager.lastOwnerHeartbeatTime
        val now = System.currentTimeMillis()
        val elapsedMs = now - lastHeartbeat
        val totalMs = TimeUnit.HOURS.toMillis(hours.toLong())
        val remainingMs = (totalMs - elapsedMs).coerceAtLeast(0L)

        val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingMs)
        val remainingMins = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val lastDateStr = dateFormat.format(Date(lastHeartbeat))

        return CommandResult(
            success = true,
            message = """
                💀 <b>Dead Man's Switch Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                🛡️ <b>State:</b> ${if (isArmed) "🟢 ARMED (ACTIVE)" else "🔴 DISARMED"}
                ⏳ <b>Configured Timeout:</b> $hours hours
                💓 <b>Last Heartbeat:</b> <code>$lastDateStr</code>
                ⏱️ <b>Time Remaining:</b> ${remainingHours}h ${remainingMins}m

                <b>Auto-Heartbeat Triggers:</b>
                • Physical device unlock by owner
                • Any incoming Telegram command
                • Any valid air-gapped SMS command
                • Manual <code>/deadman heartbeat</code>

                ⚠️ <b>When Expired in Theft/Lost Mode:</b>
                ${if (isArmed) "✓ Irreversible factory data purge executes" else "❌ Inactive (no auto-wipe)"}
            """.trimIndent()
        )
    }
}
