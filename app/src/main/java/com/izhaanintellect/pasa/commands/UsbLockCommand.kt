package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.Totp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls hardware-level USB data pin signaling.
 * Neutralizes juice-jacking, BadUSB, and forensic data extraction boxes.
 * Disarming requires Master Password.
 */
@Singleton
class UsbLockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/usb_lock"
    override val description = "Hardware USB data pin killswitch [Device Owner / Android 12+]"
    override val usage = "/usb_lock [on|status] | /usb_lock <master_password> off"

    companion object {
        private val ACTION_WORDS = setOf("on", "lock", "disable_data", "off", "unlock", "enable_data", "status")
    }

    private fun verifyCredentials(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        val isPass = authManager.verifyMasterPassword(candidate)
        val totpSecret = prefs.smsTotpSecret
        val isTotp = totpSecret.isNotBlank() && Totp.verify(totpSecret, candidate, window = 3)
        return isPass || isTotp
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> Hardware USB pin control requires Android Device Owner.\n" +
                        "Run <code>/device_owner</code> for activation instructions."
            )
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return CommandResult(
                success = false,
                message = "❌ <b>Android 12+ Required:</b> Hardware USB Data Pin control (setUsbDataSignalingEnabled) " +
                        "requires Android 12+ (API 31+).\nYour device is running Android ${Build.VERSION.RELEASE}."
            )
        }

        val param = args.firstOrNull { it.lowercase().trim() in ACTION_WORDS }?.lowercase()?.trim() ?: "status"
        val candidate = args.firstOrNull { it.lowercase().trim() !in ACTION_WORDS }?.trim()

        if (param in setOf("off", "unlock", "enable_data") && authManager.hasMasterPassword()) {
            if (candidate.isNullOrBlank()) {
                return CommandResult(
                    success = false,
                    message = """
                        🔑 <b>Disarm USB Data Lock (Zero-Trust Guard)</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        Re-enabling USB data pins (allowing PC file transfer and ADB) requires your Master Password.

                        <b>Syntax:</b> <code>/usb_lock &lt;master_password&gt; off</code>
                        <b>Example:</b> <code>/usb_lock MySecretPass123 off</code>
                    """.trimIndent()
                )
            }
            if (!verifyCredentials(candidate)) {
                return CommandResult(
                    success = false,
                    message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password. Re-enabling USB data pins rejected."
                )
            }
        }

        return when (param) {
            "on", "lock", "disable_data" -> {
                // Lock ON means USB Data pins are DISABLED (Charging only)
                val (ok, text) = PasaDeviceAdmin.setUsbDataSignaling(context, false)
                if (ok) {
                    prefs.usbLockEnabled = true
                    CommandResult(
                        success = true,
                        message = "🔌 <b>Hardware USB Lock: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "🚫 <b>USB Data Pins:</b> DEAD / DISABLED\n" +
                                "⚡ <b>Charging:</b> ACTIVE (Safe AC Power Only)\n\n" +
                                "🛡️ <i>Juice-jacking attacks, forensic extraction boxes (Cellebrite/GrayKey), and BadUSB scripts are physically neutralized!</i>"
                    )
                } else {
                    CommandResult(success = false, message = text)
                }
            }

            "off", "unlock", "enable_data" -> {
                // Lock OFF means USB Data pins are ENABLED (Normal USB)
                val (ok, text) = PasaDeviceAdmin.setUsbDataSignaling(context, true)
                if (ok) {
                    prefs.usbLockEnabled = false
                    CommandResult(
                        success = true,
                        message = "🔌 <b>Hardware USB Lock: DISARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "✅ <b>USB Data Pins:</b> ACTIVE\n" +
                                "💻 Normal PC file transfer and ADB communication restored."
                    )
                } else {
                    CommandResult(success = false, message = text)
                }
            }

            "status" -> {
                val isDataEnabled = PasaDeviceAdmin.isUsbDataSignalingEnabled(context)
                val isLocked = isDataEnabled == false
                CommandResult(
                    success = true,
                    message = "🔌 <b>Hardware USB Data Port Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• USB Lock: ${if (isLocked) "🔒 <b>ARMED (Data Blocked)</b>" else "🔓 <b>DISARMED (Data Allowed)</b>"}\n" +
                            "• Data Signaling: ${if (isDataEnabled == true) "✅ Enabled (Normal)" else "🚫 Disabled (Charging Only)"}\n" +
                            "• OS Level: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n\n" +
                            "💡 <i>Use <code>/usb_lock on</code> to kill USB data lines, or <code>/usb_lock off</code> to re-enable.</i>"
                )
            }

            else -> CommandResult(
                success = false,
                message = "❓ <b>Invalid Parameter:</b> Use <code>/usb_lock on</code>, <code>/usb_lock off</code>, or <code>/usb_lock status</code>."
            )
        }
    }
}
