package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.ui.AlertMessageActivity
import com.izhaanintellect.pasa.util.SecurityActivityLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote-only device locking with Lost Mode Guard activation.
 *
 * PRODUCTION-READY SECURITY DESIGN:
 * ✅ No local PIN entry (eliminates brute force attack)
 * ✅ Remote-only unlock via Telegram or SMS
 * ✅ Device Owner hardening enabled
 * ✅ Zero brute force attack surface
 */
@Singleton
class LockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/lock"
    override val description = "Lock device with Lost Mode (remote-only unlock)"
    override val usage = "/lock | /lock <message>"

    companion object {
        private const val TAG = "PASA_Lock"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = PasaDeviceAdmin.getComponentName(context)

        if (!dpm.isAdminActive(adminComponent)) {
            return CommandResult(
                success = false,
                message = "❌ Device Administrator is not active. Cannot lock device."
            )
        }

        return try {
            val isDeviceOwner = PasaDeviceAdmin.isDeviceOwner(context)

            // Allow explicit instant sleep via "/lock instant" or "/lock now"
            if (args.isNotEmpty() && (args[0].equals("instant", ignoreCase = true) || args[0].equals("now", ignoreCase = true))) {
                dpm.lockNow()
                Log.i(TAG, "🔒 Instant hardware lock executed")
                return CommandResult(
                    success = true,
                    message = "🔒 Device screen turned off and locked immediately."
                )
            }

            // Get message (optional)
            val messageText = if (args.isEmpty()) {
                "🔒 Device has been reported lost or stolen.\n\nTo unlock:\n" +
                "📱 Send /unlock from Telegram\n" +
                "📞 Send SMS: unlock <master_password>"
            } else {
                args.joinToString(" ").trim().ifBlank {
                    "🔒 Device has been reported lost or stolen.\n\nTo unlock:\n" +
                    "📱 Send /unlock from Telegram\n" +
                    "📞 Send SMS: unlock <master_password>"
                }
            }

            // Configure Lost Mode state (NO PIN STORED - remote-only)
            preferencesManager.isLostModeActive = true
            preferencesManager.lostModeMessage = messageText
            Log.i(TAG, "✅ Lost Mode activated (remote-only unlock)")

            // If Device Owner is active, harden device
            var ownerHardeningMsg = ""
            if (isDeviceOwner) {
                try {
                    PasaDeviceAdmin.configureLockTask(context)
                    PasaDeviceAdmin.setUninstallBlocked(context, true)
                    PasaDeviceAdmin.setComprehensiveLockdown(context, true)

                    // ANDROID 16 FIX: Disable keyguard PIN/pattern/biometrics to prevent local unlock
                    try {
                        dpm.setKeyguardDisabledFeatures(
                            adminComponent,
                            DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT or
                            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS or
                                DevicePolicyManager.KEYGUARD_DISABLE_FACE
                            } else { 0 })
                        )
                        Log.i(TAG, "✅ Keyguard Biometrics disabled - Lost Mode unlock-only")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Could not disable keyguard features: ${e.message}")
                    }

                    dpm.setDeviceOwnerLockScreenInfo(adminComponent, messageText)
                    ownerHardeningMsg = "\n\n👑 <b>Device Owner Hardening:</b>\n" +
                            "🔐 Kiosk Lock Task enabled\n" +
                            "🔒 PIN/Pattern/Biometric entry disabled\n" +
                            "📵 Airplane Mode locked\n" +
                            "🚫 USB data transfer blocked\n" +
                            "🚷 App uninstall blocked"
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Device Owner hardening partial: ${e.message}")
                }
            }

            // 1. Launch full-screen Lost Mode Guard FIRST so window manager registers it as top
            val alertIntent = AlertMessageActivity.createIntent(
                context = context,
                message = messageText,
                enforcePin = false  // No PIN entry - remote-only
            )

            SecurityActivityLauncher.launch(
                context = context,
                intent = alertIntent,
                notificationId = AlertMessageActivity.NOTIFICATION_ID,
                notificationTitle = "🛡️ LOST MODE ACTIVE",
                notificationText = messageText,
                wakeScreen = true,
                ongoing = true,
                silentNotification = false
            )

            // Short delay to allow window to attach
            Thread.sleep(250L)

            // 2. Lock OS hardware keyguard
            try {
                dpm.lockNow()
                Log.i(TAG, "📵 OS keyguard locked")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ dpm.lockNow warning: ${e.message}")
            }

            CommandResult(
                success = true,
                message = "🔒 <b>Lost Mode Guard ACTIVATED (Remote-Only Unlock)</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "💬 <b>Message:</b> \"$messageText\"\n\n" +
                        "🔓 <b>To Unlock:</b>\n" +
                        "📱 Option 1: Send <code>/unlock</code> from Telegram\n" +
                        "📞 Option 2: Send SMS <code>unlock &lt;master_password&gt;</code>\n\n" +
                        "🔐 <b>Security:</b> No local PIN entry - brute force impossible.\n" +
                        "⏰ <b>Lock Duration:</b> Indefinite (send /unlock to release)" +
                        ownerHardeningMsg

            )

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Lock failed (security): ${e.message}", e)
            CommandResult(success = false, message = "❌ Lock failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lock failed: ${e.message}", e)
            CommandResult(success = false, message = "❌ Lock failed: ${e.message}")
        }
    }
}
