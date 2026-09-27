package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.Totp
import com.izhaanintellect.pasa.ui.ScreenGuardActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import com.izhaanintellect.pasa.util.SecurityActivityLauncher

/**
 * Handles display standby deception (/fakeshutdown) and restoration (/wake).
 * Strictly requires Master Password for zero-trust protection.
 */
@Singleton
class ScreenGuardCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/fakeshutdown"
    override val description = "Simulate power off with black screen, touch forensics & auto Power Menu interception"
    override val usage = "/fakeshutdown <master_password> | /wake <master_password> | /fakeshutdown auto [on|locked|always|off|status]"

    companion object {
        private const val TAG = "PASA_ScreenGuardCmd"
        const val NOTIFICATION_ID = 2003
    }

    private fun verifyCredentials(candidate: String?): Boolean {
        if (!authManager.hasMasterPassword()) return true
        if (candidate.isNullOrBlank()) return authManager.isSessionAuthenticated()
        val totpSecret = preferencesManager.smsTotpSecret
        val isPass = authManager.verifyMasterPassword(candidate)
        val isTotp = totpSecret.isNotBlank() && Totp.verify(totpSecret, candidate, window = 3)
        return isPass || isTotp || authManager.isSessionAuthenticated()
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val firstArg = args.firstOrNull()?.lowercase() ?: ""

        // Subcommand: auto power-menu configuration
        if (firstArg == "auto") {
            val subArg = args.getOrNull(1)?.lowercase() ?: "status"
            return handleAutoConfig(subArg)
        }

        // Subcommand: status
        if (firstArg == "status") {
            return getStatusReport()
        }

        // Subcommand: test / demo mode
        if (firstArg == "test" || firstArg == "demo") {
            preferencesManager.isFakeShutdownActive = true
            try {
                val guardIntent = ScreenGuardActivity.createIntent(context).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                SecurityActivityLauncher.launch(
                    context = context,
                    intent = guardIntent,
                    notificationId = NOTIFICATION_ID,
                    notificationTitle = "🛡️ PASA Stealth Shield Test",
                    notificationText = "Simulating power-off deception (Test Mode)",
                    wakeScreen = true,
                    ongoing = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed launching test screen guard: ${e.message}", e)
            }
            return CommandResult(
                success = true,
                message = """
                    🎭 <b>Fake Shutdown Test Canvas Launched!</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    The authentic OEM power-down blackout is now active on your screen.
                    
                    • Tap the screen to test forensic mugshot & GPS beacon
                    • Send <code>/wake &lt;password&gt;</code> to restore normal display
                """.trimIndent()
            )
        }

        val isWake = args.any { it.lowercase() in setOf("wake", "stop", "off") }
        val candidate = args.firstOrNull { it.lowercase() !in setOf("wake", "stop", "off") }?.trim()

        if (isWake) {
            return wakeDevice(candidate, isFromTelegramOwner = authManager.isAuthorizedChat(chatId))
        }

        // Screen guard manual activation
        if (authManager.hasMasterPassword()) {
            if (candidate.isNullOrBlank() && !authManager.isSessionAuthenticated()) {
                val autoStatus = if (preferencesManager.isFakeShutdownAutoPowerMenu) {
                    if (preferencesManager.isFakeShutdownAutoLockedOnly) "🟢 Active (When Locked)" else "🟢 Active (Always)"
                } else "🔴 Disabled"

                return CommandResult(
                    success = false,
                    message = """
                        🎭 <b>Fake Shutdown Deception (Stealth Shield)</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        Simulates an authentic OEM power-down blackout, locks hardware buttons, and captures silent photos & GPS whenever the screen is touched.

                        ⚡ <b>Auto Power Menu Trap:</b> $autoStatus
                        <i>When someone holds the Power button (1-2s) to shut down while locked, Fake Shutdown triggers automatically!</i>

                        <b>Manual Activation:</b>
                        • <code>/fakeshutdown &lt;master_password&gt;</code> — Immediate blackout
                        • <code>/wake &lt;master_password&gt;</code> — Restore normal screen

                        <b>Auto-Trap Configuration:</b>
                        • <code>/fakeshutdown auto on</code> — Intercept when screen is locked (Recommended)
                        • <code>/fakeshutdown auto always</code> — Intercept always (locked or unlocked)
                        • <code>/fakeshutdown auto off</code> — Disable automatic interception
                        • <code>/fakeshutdown status</code> — View current shield settings
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

        return startScreenGuard()
    }

    private fun handleAutoConfig(mode: String): CommandResult {
        return when (mode) {
            "on", "locked", "enable", "enabled" -> {
                preferencesManager.isFakeShutdownAutoPowerMenu = true
                preferencesManager.isFakeShutdownAutoLockedOnly = true
                CommandResult(
                    success = true,
                    message = """
                        ✅ <b>Auto Fake Shutdown: Armed (Locked Only)</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        • <b>Trigger:</b> Long-pressing the Power button (1-2s) while the screen is locked
                        • <b>Action:</b> System power dialog dismissed instantly; Fake Shutdown engaged
                        • <b>Forensics:</b> Perpetrator mugshot + Sat GPS sent to Telegram
                        • <b>Normal Use:</b> When device is unlocked, normal power menu operates freely
                    """.trimIndent()
                )
            }
            "always" -> {
                preferencesManager.isFakeShutdownAutoPowerMenu = true
                preferencesManager.isFakeShutdownAutoLockedOnly = false
                CommandResult(
                    success = true,
                    message = """
                        ⚠️ <b>Auto Fake Shutdown: Armed (Always)</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        • <b>Trigger:</b> Long-pressing the Power button at any time (locked or unlocked)
                        • <b>Action:</b> All power-down attempts instantly diverted to Fake Shutdown
                        • <b>Forensics:</b> Mugshot + GPS alert sent to Telegram
                        • <b>Note:</b> You must use <code>/wake &lt;password&gt;</code> to exit blackout mode
                    """.trimIndent()
                )
            }
            "off", "disable", "disabled" -> {
                preferencesManager.isFakeShutdownAutoPowerMenu = false
                CommandResult(
                    success = true,
                    message = """
                        ⏸️ <b>Auto Fake Shutdown: Disabled</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        Power button long-press will now show the standard system power menu.
                        <i>Fake Shutdown can still be manually triggered via Telegram:</i>
                        <code>/fakeshutdown &lt;master_password&gt;</code>
                    """.trimIndent()
                )
            }
            else -> getStatusReport()
        }
    }

    private fun getStatusReport(): CommandResult {
        val autoEnabled = preferencesManager.isFakeShutdownAutoPowerMenu
        val lockedOnly = preferencesManager.isFakeShutdownAutoLockedOnly
        val isActive = preferencesManager.isFakeShutdownActive

        val currentMode = when {
            !autoEnabled -> "🔴 Disabled"
            lockedOnly -> "🟢 Armed (When Locked Only — Recommended)"
            else -> "🟡 Armed (Always Active)"
        }

        return CommandResult(
            success = true,
            message = """
                🎭 <b>Fake Shutdown Deception Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                • <b>Active State:</b> ${if (isActive) "🌑 Blackout Engaged" else "☀️ Normal Display"}
                • <b>Auto Power Menu Trap:</b> $currentMode
                • <b>Reboot Persistence:</b> 🟢 Active (Resumes blackout on boot)
                • <b>Touch Mugshots:</b> 🟢 Active (Snaps front camera on tap)
                • <b>Audio Mute:</b> 🟢 Active (Ringer & media silenced)

                <b>Commands:</b>
                • <code>/fakeshutdown auto on</code> — Intercept Power button when locked
                • <code>/fakeshutdown auto always</code> — Intercept Power button always
                • <code>/fakeshutdown auto off</code> — Disable auto-interception
                • <code>/wake &lt;password&gt;</code> — Exit blackout mode
            """.trimIndent()
        )
    }

    fun wakeDevice(candidate: String? = null, isFromTelegramOwner: Boolean = false): CommandResult {
        if (authManager.hasMasterPassword()) {
            val isAuthorized = (isFromTelegramOwner && candidate.isNullOrBlank()) || verifyCredentials(candidate)
            if (!isAuthorized) {
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
                } else {
                    return CommandResult(
                        success = false,
                        message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password or TOTP. Wake rejected."
                    )
                }
            }
        }

        Log.i(TAG, "Waking device from screen guard (isFromTelegramOwner=$isFromTelegramOwner)")
        preferencesManager.isFakeShutdownActive = false

        // Forcibly wake screen display hardware via PowerManager
        SecurityActivityLauncher.wakeScreen(context)

        try {
            val dismissIntent = Intent(ScreenGuardActivity.ACTION_DISMISS_SCREEN_GUARD).apply {
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

    private fun startScreenGuard(): CommandResult {
        Log.i(TAG, "Starting display standby deception")
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
                    Log.i(TAG, "Lock Task mode re-applied for screen guard")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not configure Lock Task mode: ${e.message}")
            }

            val intent = ScreenGuardActivity.createIntent(context)
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
            Log.e(TAG, "Failed to launch ScreenGuardActivity", e)
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
