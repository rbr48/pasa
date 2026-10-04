package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SIM Swap Attack Prevention & Detection
 *
 * Monitors for unauthorized SIM changes and triggers emergency lockdown.
 * SIM swap is the #1 attack vector for account takeover - this closes that gap.
 *
 * Commands:
 *   /sim_lock enable              — Start monitoring for SIM changes
 *   /sim_lock disable             — Stop monitoring
 *   /sim_lock whitelist [IMSI]    — Add SIM to trusted list
 *   /sim_lock alert_action lock   — Auto-lock on SIM swap detected
 *   /sim_lock status              — Show current monitoring state
 */
@Singleton
class SimLockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/sim_lock"
    override val description = "Monitor for SIM swap attacks and trigger lockdown"
    override val usage = "/sim_lock [enable|disable|whitelist|alert_action|phone|status]"

    companion object {
        private const val TAG = "PASA_SimLock"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull()?.lowercase() ?: "status"

        return when (action) {
            "enable", "on", "start" -> enableSimLock()
            "disable", "off", "stop" -> disableSimLock()
            "whitelist", "add", "trust" -> whitelistSim(args.getOrNull(1))
            "alert_action", "action" -> setAlertAction(args.getOrNull(1))
            "phone", "alert_phone", "number", "emergency_phone" -> setEmergencyPhone(args.getOrNull(1))
            "status" -> getSimLockStatus()
            else -> CommandResult(
                success = false,
                message = """
                    🔐 <b>SIM Lock Command</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Usage:</b>
                    • <code>/sim_lock enable</code> — Start SIM monitoring
                    • <code>/sim_lock disable</code> — Stop monitoring
                    • <code>/sim_lock whitelist</code> — Trust current SIM
                    • <code>/sim_lock phone &lt;number&gt;</code> — Set emergency SMS recipient
                    • <code>/sim_lock alert_action lock</code> — Auto-lock on swap
                    • <code>/sim_lock status</code> — Show state

                    ⚠️ <b>SIM swap is the #1 account takeover vector.</b>
                    This feature detects unauthorized SIM changes, auto-locks Kiosk Lost Mode, and silently texts your emergency phone with the thief's new number & GPS.
                """.trimIndent()
            )
        }
    }

    private fun enableSimLock(): CommandResult {
        preferencesManager.isSimLockEnabled = true
        captureCurrentSimIdentity()

        return CommandResult(
            success = true,
            message = """
                🟢 <b>SIM Lock ENABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                📡 <b>Monitoring:</b> ACTIVE

                ✓ Current SIM captured as baseline
                ✓ Any SIM change will trigger alert
                ✓ Auto-action: ${preferencesManager.simLockAlertAction.uppercase()}

                <b>Current SIM:</b>
                ${getCurrentSimInfo()}

                <i>To see status: /sim_lock status</i>
            """.trimIndent()
        )
    }

    private fun disableSimLock(): CommandResult {
        preferencesManager.isSimLockEnabled = false

        return CommandResult(
            success = true,
            message = """
                🔴 <b>SIM Lock DISABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                📡 <b>Monitoring:</b> INACTIVE

                ⚠️ Device is no longer protected against SIM swap attacks.
                <i>To re-enable: /sim_lock enable</i>
            """.trimIndent()
        )
    }

    private fun whitelistSim(imsiOrArg: String?): CommandResult {
        val currentWhitelist = preferencesManager.simLockWhitelist.toMutableList()

        if (!imsiOrArg.isNullOrBlank()) {
            val token = imsiOrArg.trim()
            if (!currentWhitelist.contains(token)) {
                currentWhitelist.add(token)
                preferencesManager.simLockWhitelist = currentWhitelist
            }
            return CommandResult(
                success = true,
                message = """
                    ✅ <b>SIM Identifier Whitelisted</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    📡 <b>Identifier:</b> <code>$token</code>

                    ✓ This identifier is now trusted
                    ✓ SIM swaps to unknown SIMs will still trigger alert

                    <b>Whitelisted SIMs:</b> ${currentWhitelist.size}
                """.trimIndent()
            )
        }

        val identifiers = com.izhaanintellect.pasa.detection.SIMChangeReceiver.collectCurrentSimIdentifiers(context)
        if (identifiers.isEmpty()) {
            return CommandResult(
                success = false,
                message = "❌ Unable to resolve active SIM identifiers. Check SIM insertion and permissions."
            )
        }

        val enriched = (currentWhitelist + identifiers).distinct()
        preferencesManager.simLockWhitelist = enriched
        if (preferencesManager.knownSimId.isNullOrBlank() || preferencesManager.knownSimId == "_") {
            preferencesManager.knownSimId = identifiers.firstOrNull()
        }

        return CommandResult(
            success = true,
            message = """
                ✅ <b>Active SIM Whitelisted</b>
                ━━━━━━━━━━━━━━━━━━━━
                📡 <b>Enrolled Keys:</b> <code>${identifiers.take(5).joinToString(", ")}</code>

                ✓ All identifiers for this SIM are now trusted
                ✓ SIM swaps to unknown SIMs will still trigger alert

                <b>Whitelisted SIMs:</b> ${enriched.size}
            """.trimIndent()
        )
    }

    private fun setAlertAction(action: String?): CommandResult {
        val validActions = listOf("lock", "alert", "wipe")
        val selectedAction = when (action?.lowercase()) {
            "lock", "lockdown" -> "lock"
            "alert", "notify" -> "alert"
            "wipe", "destroy" -> "wipe"
            else -> "alert"
        }

        preferencesManager.simLockAlertAction = selectedAction

        return CommandResult(
            success = true,
            message = """
                ⚙️ <b>SIM Lock Alert Action Updated</b>
                ━━━━━━━━━━━━━━━━━━━━
                🎯 <b>Action:</b> ${selectedAction.uppercase()}

                When SIM swap detected:
                ${when (selectedAction) {
                    "lock" -> "• Device will lock immediately\n• Lost mode will activate\n• Photos will be captured"
                    "alert" -> "• Owner notified via Telegram\n• Device continues operating\n• Evidence collected"
                    "wipe" -> "• Factory reset triggered\n• Data permanently erased\n• Owner alerted"
                    else -> "Unknown"
                }}
            """.trimIndent()
        )
    }

    private fun setEmergencyPhone(phone: String?): CommandResult {
        if (phone.isNullOrBlank()) {
            val current = preferencesManager.emergencyPhone
            return if (current.isNotBlank()) {
                CommandResult(
                    success = true,
                    message = "📱 <b>Emergency Alert Phone:</b> <code>$current</code>\n\nTo update: <code>/sim_lock phone &lt;number&gt;</code>\nTo clear: <code>/sim_lock phone clear</code>"
                )
            } else {
                CommandResult(
                    success = false,
                    message = "❌ No emergency alert phone configured.\n\n<b>Usage:</b> <code>/sim_lock phone +1234567890</code>"
                )
            }
        }

        if (phone.equals("clear", ignoreCase = true) || phone.equals("remove", ignoreCase = true) || phone.equals("none", ignoreCase = true)) {
            preferencesManager.emergencyPhone = ""
            return CommandResult(
                success = true,
                message = "🗑️ <b>Emergency Alert Phone Cleared.</b> Auto-SMS alerts on foreign SIM insertion are now disabled."
            )
        }

        preferencesManager.emergencyPhone = phone.trim()
        return CommandResult(
            success = true,
            message = """
                ✅ <b>Emergency Alert Phone Configured!</b>
                ━━━━━━━━━━━━━━━━━━━━
                📱 <b>Alert Number:</b> <code>${phone.trim()}</code>

                ✓ When a foreign or unknown SIM is inserted into this device:
                • A silent outbound emergency SMS will be sent to <code>${phone.trim()}</code>
                • The SMS includes the device IMEI, Carrier name, and Google Maps GPS fix
                • <b>Caller ID Discovery:</b> You will immediately see the thief's new phone number!
                • You can then reply via SMS with <code>PASA &lt;PIN&gt; /locate</code>, <code>/lock</code>, or <code>/call</code> to take control.
            """.trimIndent()
        )
    }

    private fun getSimLockStatus(): CommandResult {
        val isEnabled = preferencesManager.isSimLockEnabled
        val action = preferencesManager.simLockAlertAction
        val whitelist = preferencesManager.simLockWhitelist
        val alertPhone = preferencesManager.emergencyPhone

        return CommandResult(
            success = true,
            message = """
                🔐 <b>SIM Lock Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                📡 <b>Monitoring:</b> ${if (isEnabled) "🟢 ENABLED" else "🔴 DISABLED"}
                🎯 <b>Alert Action:</b> ${action.uppercase()}
                📱 <b>Emergency SMS Phone:</b> ${if (alertPhone.isNotBlank()) "<code>$alertPhone</code>" else "<i>Not configured (use /sim_lock phone &lt;number&gt;)</i>"}
                ✅ <b>Whitelisted SIMs:</b> ${whitelist.size}

                <b>Current SIM:</b>
                ${getCurrentSimInfo()}

                <b>Commands:</b>
                • <code>/sim_lock enable</code> — Enable protection
                • <code>/sim_lock whitelist</code> — Trust this SIM
                • <code>/sim_lock phone &lt;number&gt;</code> — Set emergency SMS recipient
                • <code>/sim_lock alert_action lock</code> — Change action

                ⚠️ <b>When SIM swap detected:</b>
                ${if (isEnabled) "✓ Device is PROTECTED" else "❌ Device is UNPROTECTED"}
            """.trimIndent()
        )
    }

    private fun captureCurrentSimIdentity() {
        try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            val activeSubscriptions = subscriptionManager?.activeSubscriptionInfoList ?: emptyList()

            if (activeSubscriptions.isNotEmpty()) {
                val subInfo = activeSubscriptions.first()
                val iccid = subInfo.iccId ?: ""

                if (iccid.isNotBlank()) {
                    val whitelist = preferencesManager.simLockWhitelist.toMutableList()
                    if (!whitelist.contains(iccid)) {
                        whitelist.add(iccid)
                        preferencesManager.simLockWhitelist = whitelist
                    }
                    Log.i(TAG, "Current SIM captured: ${iccid.take(10)}...")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to capture current SIM: ${e.message}")
        }
    }

    private fun getCurrentSimInfo(): String {
        return try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            val activeSubscriptions = subscriptionManager?.activeSubscriptionInfoList ?: emptyList()

            if (activeSubscriptions.isEmpty()) {
                "⚠️ No active SIM detected"
            } else {
                val subInfo = activeSubscriptions.first()
                val carrier = subInfo.displayName?.toString() ?: "Unknown"
                val iccid = subInfo.iccId?.take(10)?.let { "$it...***" } ?: "Unknown"
                "🏢 Carrier: $carrier\n🔐 ICCID: $iccid"
            }
        } catch (e: Exception) {
            "❌ Error reading SIM: ${e.message}"
        }
    }
}
