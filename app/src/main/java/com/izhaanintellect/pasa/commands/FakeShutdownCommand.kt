package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.Totp
import com.izhaanintellect.pasa.ui.FakeShutdownActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import com.izhaanintellect.pasa.util.SecurityActivityLauncher

/**
 * Handles simulated power-off deception (/fakeshutdown) and restoration (/wake).
 * Strictly requires Master Password for zero-trust protection.
 */
@Singleton
class FakeShutdownCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/fakeshutdown"
    override val description = "Simulate power off with black screen and touch forensics (Requires Master Password)"
    override val usage = "/fakeshutdown <master_password> | /wake <master_password>"

    companion object {
        private const val TAG = "PASA_FakeShutdownCmd"
        const val NOTIFICATION_ID = 2003
    }

    private fun verifyCredentials(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        val isPass = authManager.verifyMasterPassword(candidate)
        val totpSecret = preferencesManager.smsTotpSecret
        val isTotp = totpSecret.isNotBlank() && Totp.verify(totpSecret, candidate, window = 3)
        return isPass || isTotp
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val isWake = args.any { it.lowercase() in setOf("wake", "stop", "off") }
        val candidate = args.firstOrNull { it.lowercase() !in setOf("wake", "stop", "off") }?.trim()

        if (isWake) {
            return wakeDevice(candidate)
        }

        // Fake Shutdown activation
        if (authManager.hasMasterPassword()) {
            if (candidate.isNullOrBlank()) {
                return CommandResult(
                    success = false,
                    message = """
                        🔑 <b>Fake Shutdown Deception (Zero-Trust Guard)</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        Because Fake Shutdown places the device into an authentic OEM power-down blackout and suppresses hardware buttons, activating it strictly requires your Master Password.

                        This guarantees that a compromised server or unauthorized entity can never black out your device.

                        <b>Syntax:</b> <code>/fakeshutdown &lt;master_password&gt;</code>
                        <b>Wake Device:</b> <code>/wake &lt;master_password&gt;</code>
                        <b>Example:</b> <code>/fakeshutdown MySecretPass123</code>
                    """.trimIndent()
                )
            }
            if (!verifyCredentials(candidate)) {
                return CommandResult(
                    success = false,
                    message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password or TOTP. Fake shutdown rejected."
                )
            }
        }

        return startFakeShutdown()
    }

    fun wakeDevice(candidate: String? = null): CommandResult {
        if (authManager.hasMasterPassword()) {
            if (candidate.isNullOrBlank()) {
                return CommandResult(
                    success = false,
                    message = """
                        🔑 <b>Wake Device (Zero-Trust Guard)</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        To wake the device from Fake Shutdown blackout canvas, Master Password verification is required.

                        <b>Syntax:</b> <code>/wake &lt;master_password&gt;</code>
                        <b>Example:</b> <code>/wake MySecretPass123</code>
                    """.trimIndent()
                )
            }
            if (!verifyCredentials(candidate)) {
                return CommandResult(
                    success = false,
                    message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password or TOTP. Wake rejected."
                )
            }
        }

        Log.i(TAG, "Waking device from Fake Shutdown")
        preferencesManager.isFakeShutdownActive = false

        try {
            val dismissIntent = Intent(FakeShutdownActivity.ACTION_DISMISS_FAKE_SHUTDOWN).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(dismissIntent)
            SecurityActivityLauncher.dismissNotification(context, NOTIFICATION_ID)

            // Re-enable status bar if not in Lost Mode
            if (!preferencesManager.isLostModeActive) {
                try {
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                    val component = android.content.ComponentName(context, com.izhaanintellect.pasa.admin.PasaDeviceAdmin::class.java)
                    if (dpm?.isDeviceOwnerApp(context.packageName) == true) {
                        dpm.setStatusBarDisabled(component, false)
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast wake intent: ${e.message}")
        }

        return CommandResult(
            success = true,
            message = "☀️ <b>Device Woken Up</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                    "Fake Shutdown mode deactivated.\n" +
                    "Screen brightness and audio ringer have been restored."
        )
    }

    private fun startFakeShutdown(): CommandResult {
        Log.i(TAG, "Starting Fake Shutdown deception")
        preferencesManager.isFakeShutdownActive = true

        try {
            // Ensure Lock Task mode is configured before blackout (critical for power button suppression)
            try {
                val dpm = context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                val component = android.content.ComponentName(context, com.izhaanintellect.pasa.admin.PasaDeviceAdmin::class.java)
                if (dpm?.isDeviceOwnerApp(context.packageName) == true) {
                    dpm.setLockTaskPackages(component, arrayOf(context.packageName))
                    dpm.setLockTaskFeatures(component, android.app.admin.DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
                    dpm.setStatusBarDisabled(component, true)
                    Log.i(TAG, "Lock Task mode re-applied for fake shutdown")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not configure Lock Task mode: ${e.message}")
            }

            val intent = FakeShutdownActivity.createIntent(context)
            SecurityActivityLauncher.launch(
                context = context,
                intent = intent,
                notificationId = NOTIFICATION_ID,
                notificationTitle = "System Power Management",
                notificationText = "Display standby protocol active",
                wakeScreen = true,
                ongoing = true,
                silentNotification = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch FakeShutdownActivity", e)
            return CommandResult(
                success = false,
                message = "❌ Could not activate Fake Shutdown: ${e.message}"
            )
        }

        return CommandResult(
            success = true,
            message = "📴 <b>Fake Shutdown Activated!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                    "The phone is displaying an authentic power-off spinner and will black out completely.\n" +
                    "To a thief, the device appears completely powered off.\n\n" +
                    "📸 <i>If the thief touches the screen, silent front-camera snapshots and GPS telemetry will be captured and sent to Telegram!</i>\n\n" +
                    "Send <code>/wake</code> at any time to restore normal screen operation."
        )
    }
}
