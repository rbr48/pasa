package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages PASA Sentinel persistent foreground notification suppression.
 * Uses Knox-Grade Device Owner permission controls to completely hide the
 * ongoing notification from the shade and lock screen without terminating the daemon.
 *
 * Commands:
 *   /notification hide      — Permanently hide PASA notification from drawer
 *   /notification show      — Restore PASA notification to drawer
 *   /notification status    — Check current visibility state
 *   /notification toggle    — Switch between hide/show
 */
@Singleton
class NotificationToggleCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/notification"
    override val description = "Permanently hide or restore PASA foreground notification (Device Owner)"
    override val usage = "/notification [hide|show|toggle|status]"

    companion object {
        private const val TAG = "PASA_Notification"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        // First, ensure system status bar is always enabled
        restoreStatusBar()

        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = """
                    🔒 <b>Device Owner Required</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    ⚠️ Changing notification policy requires Device Owner privileges.

                    <b>Status:</b>
                    🔴 Device Owner: <b>NOT ACTIVE</b>
                """.trimIndent()
            )
        }

        val action = args.firstOrNull()?.lowercase() ?: "status"

        return when (action) {
            "hide", "off", "disable" -> hideNotification()
            "show", "on", "enable", "restore" -> showNotification()
            "toggle" -> toggleNotification()
            "status" -> getNotificationStatus()
            else -> CommandResult(
                success = false,
                message = """
                    📳 <b>PASA Notification Control</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Usage:</b>
                    • <code>/notification hide</code> — Permanently hide ongoing notification
                    • <code>/notification show</code> — Restore ongoing notification
                    • <code>/notification toggle</code> — Switch hide/show
                    • <code>/notification status</code> — Check current status

                    <i>Uses Device Owner policy to eliminate notification tray clutter while keeping the security daemon 100% active.</i>
                """.trimIndent()
            )
        }
    }

    private fun restoreStatusBar() {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val component = PasaDeviceAdmin.getComponentName(context)
            if (dpm != null && PasaDeviceAdmin.isDeviceOwner(context) && !preferencesManager.isLostModeActive && !preferencesManager.isFakeShutdownActive) {
                dpm.setStatusBarDisabled(component, false)
            }
        } catch (_: Exception) {}
    }

    private fun hideNotification(): CommandResult {
        val success = PasaDeviceAdmin.setNotificationSuppressed(context, true)
        return if (success) {
            CommandResult(
                success = true,
                message = """
                    ✅ <b>PASA Notification Hidden Forever</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    📳 <b>Notification Shade:</b> CLEAN / HIDDEN
                    🛡️ <b>Foreground Daemon:</b> ACTIVE & PERSISTENT

                    ✓ Ongoing "System Security Core" notification suppressed
                    ✓ Notification drawer & lockscreen are 100% clean
                    ✓ Background surveillance & commands remain fully operational
                    ✓ Persists across reboots

                    <i>To restore: <code>/notification show</code></i>
                """.trimIndent()
            )
        } else {
            CommandResult(
                success = false,
                message = "❌ <b>Failed to update notification suppression policy.</b>"
            )
        }
    }

    private fun showNotification(): CommandResult {
        val success = PasaDeviceAdmin.setNotificationSuppressed(context, false)
        return if (success) {
            CommandResult(
                success = true,
                message = """
                    🔔 <b>PASA Notification Restored</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    📳 <b>Notification Shade:</b> VISIBLE
                    🛡️ <b>Foreground Daemon:</b> ACTIVE

                    ✓ "System Security Core" ongoing notification restored
                    ✓ Managed by enterprise policy

                    <i>To hide again: <code>/notification hide</code></i>
                """.trimIndent()
            )
        } else {
            CommandResult(
                success = false,
                message = "❌ <b>Failed to restore notification.</b>"
            )
        }
    }

    private fun toggleNotification(): CommandResult {
        val currentlySuppressed = preferencesManager.isNotificationSuppressed
        return if (currentlySuppressed) {
            showNotification()
        } else {
            hideNotification()
        }
    }

    private fun getNotificationStatus(): CommandResult {
        val suppressed = preferencesManager.isNotificationSuppressed
        val statusText = if (suppressed) "🔴 HIDDEN (Suppressed)" else "🟢 VISIBLE"
        return CommandResult(
            success = true,
            message = """
                📳 <b>PASA Notification Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                • <b>Visibility:</b> $statusText
                • <b>Device Owner:</b> 🟢 ACTIVE
                • <b>Android Version:</b> ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})

                <b>Commands:</b>
                • <code>/notification hide</code> — Hide from drawer
                • <code>/notification show</code> — Show in drawer
            """.trimIndent()
        )
    }
}
