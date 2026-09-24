package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SIM Tray Lock — Cryptographic Hardware Defense Against Unauthorized SIM Insertion
 *
 * This is an escalation layer on top of /sim_lock. When armed and an unauthorized SIM
 * is inserted, PASA executes a full Device Owner deep-lockdown that makes the device
 * COMPLETELY UNUSABLE to anyone without the owner's remote authorization:
 *
 *   1. Lockscreen PIN is ROTATED to a cryptographically random 8-digit PIN via
 *      dpm.resetPasswordWithToken() — the PIN is sent ONLY to owner via Telegram.
 *      The thief cannot unlock the device without the owner's approval.
 *
 *   2. ALL installed apps (except PASA itself) are SUSPENDED via
 *      dpm.setPackagesSuspended() — the thief sees every app greyed out and unlaunchable.
 *
 *   3. Full Knox Kiosk Lost Mode engages (same as /sim_lock lock action).
 *
 *   4. Biometrics disabled — forces PIN entry only (which thief doesn't know).
 *
 *   5. Owner receives: emergency PIN + mugshot + GPS + new SIM carrier details.
 *
 * The device cannot be used or reset (DISALLOW_FACTORY_RESET is enforced). Only the
 * owner can un-brick it remotely via /sim_tray_lock release.
 *
 * Commands:
 *   /sim_tray_lock enable     — Arm tray lock and whitelist current SIM(s)
 *   /sim_tray_lock disable    — Disarm
 *   /sim_tray_lock release    — Un-suspend all apps + restore lockscreen (after breach)
 *   /sim_tray_lock whitelist  — Authorize currently inserted SIM(s) as trusted
 *   /sim_tray_lock status     — Show armed state and whitelisted SIMs
 */
@Singleton
class SimTrayLockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/sim_tray_lock"
    override val description = "Cryptographic SIM tray lock — device becomes completely unusable on unauthorized SIM insertion"
    override val usage = "/sim_tray_lock [enable|disable|release|whitelist|status]"

    companion object {
        private const val TAG = "PASA_SimTrayLock"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull()?.lowercase()?.trim() ?: "status"

        return when (action) {
            "enable", "on", "arm"           -> enableTrayLock()
            "disable", "off", "disarm"      -> disableTrayLock()
            "release", "unlock", "restore"  -> releaseLockdown()
            "whitelist", "trust", "allow"   -> whitelistCurrentSims()
            "status"                        -> getStatus()
            else                            -> showHelp()
        }
    }

    private fun enableTrayLock(): CommandResult {
        // Require Device Owner for full effectiveness
        val isDO = PasaDeviceAdmin.isDeviceOwner(context)

        // Capture current SIM ICCIDs as authorized baseline
        val whitelistResult = whitelistCurrentSims()

        preferencesManager.isSimTrayLockEnabled = true
        // Also ensure the standard sim_lock monitoring is active
        preferencesManager.isSimLockEnabled = true
        preferencesManager.simLockAlertAction = "lock"

        val doWarning = if (!isDO) {
            "\n\n⚠️ <b>Device Owner NOT active</b> — some capabilities (PIN rotation, app suspension) require Device Owner. Run <code>/admin status</code> to verify."
        } else ""

        return CommandResult(
            success = true,
            message = """
                🔐 <b>SIM TRAY LOCK — ARMED ✅</b>
                ━━━━━━━━━━━━━━━━━━━━
                The device will execute a <b>full deep-lockdown</b> the moment an unauthorized SIM is inserted.

                🛡️ <b>What happens on breach:</b>
                1️⃣ Lockscreen PIN instantly rotated to a secret random PIN
                2️⃣ ALL apps suspended — device becomes completely unusable
                3️⃣ Knox Kiosk Lost Mode engaged — no exit without remote command
                4️⃣ Biometrics disabled — PIN-only access (thief doesn't have it)
                5️⃣ Factory reset blocked via Device Owner
                6️⃣ Mugshot + GPS + SIM info → sent to your Telegram instantly

                📡 <b>Current SIM(s) whitelisted:</b> ${preferencesManager.simLockWhitelist.size}
                
                💡 To un-brick after a breach: <code>/sim_tray_lock release</code>
                💡 To authorize your other SIM: <code>/sim_tray_lock whitelist</code>$doWarning
            """.trimIndent()
        )
    }

    private fun disableTrayLock(): CommandResult {
        preferencesManager.isSimTrayLockEnabled = false
        return CommandResult(
            success = true,
            message = """
                🔓 <b>SIM Tray Lock — DISARMED</b>
                ━━━━━━━━━━━━━━━━━━━━
                Deep-lockdown policy deactivated.
                Standard SIM monitoring (<code>/sim_lock</code>) remains active.

                ℹ️ To re-arm: <code>/sim_tray_lock enable</code>
            """.trimIndent()
        )
    }

    /**
     * Called by the owner remotely after a breach to restore normal device operation.
     * Un-suspends all packages and clears the tray-lock breach state.
     */
    fun releaseLockdown(): CommandResult {
        return try {
            val dpm = context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE)
                as? android.app.admin.DevicePolicyManager
            val adminComponent = PasaDeviceAdmin.getComponentName(context)
            val unsuspended = mutableListOf<String>()

            if (dpm != null && PasaDeviceAdmin.isDeviceOwner(context)) {
                // Un-suspend ALL suspended packages
                val pm = context.packageManager
                val allPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .map { it.packageName }
                    .filter { it != context.packageName }
                    .toTypedArray()

                try {
                    val stillSuspended = dpm.setPackagesSuspended(adminComponent, allPackages, false)
                    unsuspended.addAll(allPackages.toList().minus(stillSuspended.toSet()))
                    Log.i(TAG, "Un-suspended ${unsuspended.size} packages after SIM tray lock release")
                } catch (e: Exception) {
                    Log.w(TAG, "Error un-suspending packages: ${e.message}")
                }

                // Re-enable biometrics
                try {
                    dpm.setKeyguardDisabledFeatures(adminComponent, DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE)
                } catch (_: Exception) {}

                // Re-enable status bar
                try { dpm.setStatusBarDisabled(adminComponent, false) } catch (_: Exception) {}
            }

            // Clear emergency PIN from prefs
            preferencesManager.simTrayLockEmergencyPin = ""

            CommandResult(
                success = true,
                message = """
                    🔓 <b>SIM Tray Lock — Lockdown Released</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    ✅ All suspended apps restored (${unsuspended.size} packages un-suspended)
                    ✅ Biometrics re-enabled
                    ✅ Status bar restored
                    ✅ Emergency PIN cleared from device

                    ℹ️ The device is now fully operational.
                    To re-arm: <code>/sim_tray_lock enable</code>
                """.trimIndent()
            )
        } catch (e: Exception) {
            CommandResult(false, "❌ Release failed: ${e.message}")
        }
    }

    private fun whitelistCurrentSims(): CommandResult {
        return try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val subs = subManager?.activeSubscriptionInfoList

            if (subs.isNullOrEmpty()) {
                return CommandResult(false, "❌ No active SIM found to whitelist.")
            }

            val whitelist = preferencesManager.simLockWhitelist.toMutableList()
            val added = mutableListOf<String>()

            subs.forEach { sub ->
                val iccid = sub.iccId ?: return@forEach
                if (!whitelist.contains(iccid)) {
                    whitelist.add(iccid)
                    added.add("SIM ${sub.simSlotIndex + 1}: ${sub.displayName} (${iccid.take(8)}...)")
                }
            }

            preferencesManager.simLockWhitelist = whitelist

            if (added.isEmpty()) {
                CommandResult(true, "ℹ️ All currently inserted SIM(s) are already whitelisted.\n\n📡 <b>Total whitelisted SIMs:</b> ${whitelist.size}")
            } else {
                CommandResult(
                    success = true,
                    message = """
                        ✅ <b>SIM(s) Whitelisted Successfully</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        ${added.joinToString("\n") { "• $it" }}

                        📡 <b>Total whitelisted:</b> ${whitelist.size} SIM(s)
                        These SIM(s) will never trigger the tray lock alarm.
                    """.trimIndent()
                )
            }
        } catch (e: Exception) {
            CommandResult(false, "❌ Whitelist failed: ${e.message}")
        }
    }

    private fun getStatus(): CommandResult {
        val trayArmed = preferencesManager.isSimTrayLockEnabled
        val simArmed  = preferencesManager.isSimLockEnabled
        val whitelist = preferencesManager.simLockWhitelist
        val isDO      = PasaDeviceAdmin.isDeviceOwner(context)
        val hasPin    = preferencesManager.simTrayLockEmergencyPin.isNotBlank()

        return CommandResult(
            success = true,
            message = """
                🔐 <b>SIM Tray Lock Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                🛡️ <b>Tray Lock (Deep Lockdown):</b> ${if (trayArmed) "🟢 ARMED" else "🔴 DISARMED"}
                📡 <b>SIM Monitoring (/sim_lock):</b> ${if (simArmed) "🟢 Active" else "🔴 Inactive"}
                👑 <b>Device Owner:</b> ${if (isDO) "✅ Active (full capability)" else "❌ Inactive (reduced capability)"}
                🔑 <b>Emergency PIN (breach active):</b> ${if (hasPin) "⚠️ BREACH STATE ACTIVE — run <code>/sim_tray_lock release</code> to restore" else "None"}
                ✅ <b>Whitelisted SIMs:</b> ${whitelist.size}

                🛡️ <b>On unauthorized SIM insertion, PASA will:</b>
                ${if (trayArmed && isDO) """
                • 🔑 Rotate lockscreen PIN to secret random PIN → sent to you via Telegram
                • 📵 Suspend ALL apps → device completely unusable to thief
                • 🔒 Engage Knox Kiosk Lost Mode
                • 👁️ Disable biometrics → PIN-only access
                • 📸 Capture mugshot + GPS + SIM carrier details
                """.trimIndent() else if (trayArmed) """
                • 🔒 Engage Knox Kiosk Lost Mode
                • 📸 Capture mugshot + GPS + SIM carrier details
                ⚠️ Device Owner required for PIN rotation and app suspension
                """.trimIndent() else "Not armed — run <code>/sim_tray_lock enable</code>"}

                💡 <b>Commands:</b>
                • <code>/sim_tray_lock enable</code>     — Arm deep-lockdown
                • <code>/sim_tray_lock whitelist</code>  — Trust current SIM(s)
                • <code>/sim_tray_lock release</code>    — Un-brick after breach
                • <code>/sim_lock phone &lt;number&gt;</code>  — Set emergency SMS number
            """.trimIndent()
        )
    }

    private fun showHelp(): CommandResult {
        return CommandResult(
            success = false,
            message = """
                🔐 <b>SIM Tray Lock</b>
                ━━━━━━━━━━━━━━━━━━━━
                Cryptographic SIM tray defense — makes the device completely unusable when an unauthorized SIM is inserted.

                <b>Commands:</b>
                • <code>/sim_tray_lock enable</code>    — Arm the tray lock
                • <code>/sim_tray_lock disable</code>   — Disarm
                • <code>/sim_tray_lock whitelist</code> — Trust current SIM(s)
                • <code>/sim_tray_lock release</code>   — Restore device after breach
                • <code>/sim_tray_lock status</code>    — Show current state

                ⚠️ <b>Requires:</b> Device Owner for full capability (PIN rotation + app suspension)
            """.trimIndent()
        )
    }
}

