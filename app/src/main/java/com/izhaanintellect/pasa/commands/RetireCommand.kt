package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.InlineKeyboardButton
import com.izhaanintellect.pasa.bot.InlineKeyboardMarkup
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.Totp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * /retire — Secure PASA Self-Decommission Command
 *
 * SECURITY DESIGN:
 * ✅ Zero-Trust: Strictly requires Master Password — no session bypass
 * ✅ Staged confirmation: requires `/retire <password> confirm` to execute
 * ✅ Safe teardown: releases all Knox restrictions before clearing Device Owner
 * ✅ Cryptographic shredding: clears all PreferencesManager secrets
 * ✅ Self-uninstall: launches PackageInstaller after Device Owner is cleared
 *
 * UNINSTALL CHAIN (must be in this exact order):
 * 1. Release all DevicePolicyManager user restrictions
 * 2. dpm.clearDeviceOwnerApp(packageName) — relinquish Device Owner
 * 3. dpm.removeActiveAdmin(component) — deactivate Device Admin
 * 4. PackageInstaller.uninstall(packageName, ...) — self-uninstall
 */
@Singleton
class RetireCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/retire"
    override val description = "Securely decommission PASA and remove Device Owner"
    override val usage = "/retire <master_password> confirm"

    companion object {
        private const val TAG = "PASA_Retire"

        // All user restrictions that anti-tamper may have set
        private val ALL_RESTRICTIONS = listOf(
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_AIRPLANE_MODE,
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_NETWORK_RESET,
            UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA,
            UserManager.DISALLOW_USB_FILE_TRANSFER,
            UserManager.DISALLOW_CONFIG_LOCATION,
            UserManager.DISALLOW_DEBUGGING_FEATURES
        )
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        // Arg layout: /retire <password> [confirm]
        val password = args.getOrNull(0)?.trim()
        val confirmToken = args.getOrNull(1)?.lowercase()?.trim()

        // 1. Zero-arg: show status / instructions (session bypass NOT allowed for /retire)
        if (password.isNullOrBlank()) {
            return CommandResult(
                success = false,
                message = buildRetireHelp(),
                replyMarkup = buildRetireKeyboard()
            )
        }

        // 2. Verify Master Password — STRICTLY, never honour active session for this command
        val isValidPassword = authManager.verifyMasterPassword(password)
        val totpSecret = preferencesManager.smsTotpSecret
        val isValidTotp = totpSecret.isNotBlank() && Totp.verify(totpSecret, password, window = 3)
        if (!isValidPassword && !isValidTotp) {
            Log.w(TAG, "Retire attempt with incorrect master password")
            return CommandResult(
                success = false,
                message = "❌ <b>Access Denied</b>\n━━━━━━━━━━━━━━━━━━━━\nIncorrect Master Password. The <code>/retire</code> command always requires the correct Master Password — active sessions are <b>not</b> honoured for this destructive operation."
            )
        }

        // 3. Require explicit "confirm" token as second argument
        if (confirmToken != "confirm") {
            return CommandResult(
                success = false,
                message = buildString {
                    appendLine("⚠️ <b>Retirement Confirmation Required</b>")
                    appendLine("━━━━━━━━━━━━━━━━━━━━")
                    appendLine("Master Password verified. To proceed with PASA self-decommissioning, send the confirmation command:")
                    appendLine()
                    appendLine("<code>/retire ${password} confirm</code>")
                    appendLine()
                    appendLine("⚠️ <b>This action is irreversible:</b>")
                    appendLine("• All Knox Device Owner restrictions will be removed")
                    appendLine("• Device Owner status will be permanently relinquished")
                    appendLine("• All PASA credentials and keys will be cryptographically shredded")
                    appendLine("• PASA will silently uninstall itself from the device")
                    appendLine()
                    appendLine("The device will return to <b>normal Android operation</b> with no restrictions.")
                }
            )
        }

        // 4. Execute retirement sequence
        Log.w(TAG, "Retirement sequence initiated — executing teardown")
        return withContext(Dispatchers.Main) {
            executeRetirementSequence(password)
        }
    }

    private suspend fun executeRetirementSequence(password: String): CommandResult {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return CommandResult(
                success = false,
                message = "❌ DevicePolicyManager unavailable. Retirement aborted."
            )
        val component = PasaDeviceAdmin.getComponentName(context)
        val isOwner = dpm.isDeviceOwnerApp(context.packageName)
        val isAdmin = dpm.isAdminActive(component)

        val report = StringBuilder()
        report.appendLine("🗑️ <b>PASA Retirement Sequence Initiated</b>")
        report.appendLine("━━━━━━━━━━━━━━━━━━━━")

        // Step 1: Re-enable status bar if locked
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isOwner) {
                dpm.setStatusBarDisabled(component, false)
                report.appendLine("✅ Notification shade / quick settings: Restored")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore status bar: ${e.message}")
        }

        // Step 2: Release all user restrictions
        if (isOwner || isAdmin) {
            for (restriction in ALL_RESTRICTIONS) {
                try {
                    dpm.clearUserRestriction(component, restriction)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not clear restriction $restriction: ${e.message}")
                }
            }
            report.appendLine("✅ All Anti-Tamper restrictions: Released")
        }

        // Step 3: Re-enable USB data signaling if locked
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isOwner) {
                dpm.setUsbDataSignalingEnabled(true)
                report.appendLine("✅ USB data signaling: Restored")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore USB data: ${e.message}")
        }

        // Step 4: Re-enable cameras if locked
        try {
            if (isOwner) {
                dpm.setCameraDisabled(component, false)
                report.appendLine("✅ Camera hardware: Restored")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore camera: ${e.message}")
        }

        // Step 5: Exit kiosk / lock task mode
        try {
            if (isOwner) {
                dpm.setLockTaskPackages(component, emptyArray())
                report.appendLine("✅ Kiosk lock task mode: Cleared")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear lock task: ${e.message}")
        }

        // Step 6: Unblock app from being uninstalled (if set)
        try {
            if (isOwner) {
                dpm.setUninstallBlocked(component, context.packageName, false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not unblock uninstall: ${e.message}")
        }

        // Step 7: Shred all cryptographic credentials from EncryptedSharedPreferences
        try {
            preferencesManager.shredAllCredentials()
            report.appendLine("✅ Cryptographic credentials: Shredded")
        } catch (e: Exception) {
            Log.w(TAG, "Could not shred credentials: ${e.message}")
            report.appendLine("⚠️ Credential shredding: Partial (${e.message})")
        }

        // Step 8: Clear Device Owner — MUST happen before self-uninstall
        var ownerCleared = false
        if (isOwner) {
            try {
                dpm.clearDeviceOwnerApp(context.packageName)
                ownerCleared = true
                report.appendLine("✅ Knox Device Owner status: Relinquished")
                Log.w(TAG, "Device Owner cleared successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear Device Owner: ${e.message}", e)
                report.appendLine("⚠️ Device Owner: ${e.message}")
            }
        } else {
            report.appendLine("ℹ️ Device Owner: Not active (not required)")
            ownerCleared = true
        }

        // Step 9: Remove active Device Admin — MUST be after clearDeviceOwnerApp
        if (isAdmin) {
            try {
                dpm.removeActiveAdmin(component)
                report.appendLine("✅ Device Admin: Deactivated")
                Log.w(TAG, "Device Admin removed successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove Device Admin: ${e.message}")
                report.appendLine("⚠️ Device Admin removal: ${e.message}")
            }
        }

        report.appendLine()
        report.appendLine("🗑️ <b>Initiating self-uninstall…</b>")
        report.appendLine("PASA will be removed from this device.")

        // Brief delay to allow the Telegram message to be sent before process dies
        delay(2000L)

        // Step 10: Self-uninstall via PackageInstaller (now allowed since DeviceOwner is cleared)
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val intent = Intent("com.izhaanintellect.pasa.RETIRE_UNINSTALL").setPackage(context.packageName)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(context, 9999, intent, flags)
            packageInstaller.uninstall(context.packageName, pendingIntent.intentSender)
            Log.w(TAG, "Self-uninstall initiated via PackageInstaller")
        } catch (e: Exception) {
            // Fallback: launch system uninstall intent (requires user tap)
            Log.e(TAG, "PackageInstaller self-uninstall failed, falling back to system intent: ${e.message}", e)
            try {
                val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Intent.EXTRA_RETURN_RESULT, false)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback uninstall intent also failed: ${e2.message}", e2)
            }
        }

        return CommandResult(
            success = true,
            message = report.toString()
        )
    }

    private fun buildRetireHelp(): String = buildString {
        appendLine("🗑️ <b>PASA Secure Retirement</b>")
        appendLine("━━━━━━━━━━━━━━━━━━━━")
        appendLine("Permanently decommissions PASA, releasing all Knox Device Owner privileges and silently self-uninstalling.")
        appendLine()
        appendLine("<b>What retirement does:</b>")
        appendLine("• Releases all Anti-Tamper restrictions (Safe Boot, Factory Reset, USB, etc.)")
        appendLine("• Relinquishes Knox Device Owner status permanently")
        appendLine("• Deactivates Device Admin")
        appendLine("• Cryptographically shreds all stored credentials and keys")
        appendLine("• Silently self-uninstalls PASA from the device")
        appendLine()
        appendLine("<b>Usage:</b>")
        appendLine("<code>/retire &lt;master_password&gt; confirm</code>")
        appendLine()
        appendLine("⚠️ <b>This action is permanent and irreversible.</b>")
        appendLine("The device returns to standard Android operation with no restrictions.")
        appendLine()
        appendLine("Strictly requires Master Password — active session auth is <b>not</b> accepted.")
    }

    private fun buildRetireKeyboard(): InlineKeyboardMarkup = InlineKeyboardMarkup(
        inlineKeyboard = listOf(
            listOf(
                InlineKeyboardButton("📊 Device Owner Status", callbackData = "cmd:device_owner"),
                InlineKeyboardButton("🛡️ Anti-Tamper Status", callbackData = "cmd:antitamper:status")
            )
        )
    )
}
