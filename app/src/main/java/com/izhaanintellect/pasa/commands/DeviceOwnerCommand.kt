package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks and reports Android Enterprise Device Owner provisioning status
 * and telemetry dashboard for all enterprise hardware defense subsystems.
 */
@Singleton
class DeviceOwnerCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : Command {

    override val name = "/device_owner"
    override val description = "Enterprise Device Owner telemetry & protection status"
    override val usage = "/device_owner"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val isOwner = PasaDeviceAdmin.isDeviceOwner(context)
        val isAdmin = PasaDeviceAdmin.isAdminActive(context)

        return if (isOwner) {
            val antiTamper = prefs.antiTamperEnabled
            val usbDataEnabled = PasaDeviceAdmin.isUsbDataSignalingEnabled(context)
            val biometricsDisabled = PasaDeviceAdmin.isBiometricsDisabled(context)
            val escrowActive = PasaDeviceAdmin.isResetPasswordTokenActive(context)
            val secLogActive = PasaDeviceAdmin.isSecurityLoggingEnabled(context)
            val frozenCount = prefs.frozenPackages.size

            val sb = StringBuilder()
            sb.append("👑 <b>Android Enterprise Device Owner: ACTIVE</b>\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━\n")
            sb.append("🛡️ <b>Anti-Tamper Suite:</b> ").append(if (antiTamper) "✅ Armed (/antitamper)" else "⚠️ Disarmed").append("\n")
            sb.append("   • Safe Boot: ").append(if (antiTamper) "🚫 Blocked" else "Allowed").append("\n")
            sb.append("   • Airplane Mode: ").append(if (antiTamper) "🚫 Blocked" else "Allowed").append("\n")
            sb.append("   • Factory Reset: ").append(if (antiTamper) "🚫 Blocked" else "Allowed").append("\n\n")

            sb.append("🔌 <b>USB Data Killswitch:</b> ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sb.append(if (usbDataEnabled == false) "🔒 ARMED (Data Dead, /usb_lock)" else "✅ Enabled (Normal)").append("\n")
            } else {
                sb.append("ℹ️ Requires Android 12+\n")
            }

            sb.append("🧬 <b>Biometric Duress Lock:</b> ")
            sb.append(if (biometricsDisabled) "🚫 DISABLED (Duress Mode, /biometrics)" else "✅ Normal").append("\n")

            sb.append("🔑 <b>Hardware Escrow Token:</b> ")
            sb.append(if (escrowActive) "✅ Armed (/set_os_pin)" else "⏳ Pending First Unlock").append("\n")

            sb.append("📦 <b>Shadow App Vault:</b> ")
            sb.append(if (frozenCount > 0) "🧊 $frozenCount Apps Frozen (/frozen)" else "Empty (/freeze)").append("\n")

            sb.append("📑 <b>OS Security Auditing:</b> ")
            sb.append(if (secLogActive) "✅ Active (Kernel Event Log)" else "ℹ️ Standby").append("\n")

            sb.append("🔒 <b>Anti-Uninstall:</b> ✅ Permanently Protected\n")
            sb.append("📱 <b>Kiosk Mode:</b> ✅ Ready\n\n")
            sb.append("🛡️ <i>Your device has maximum hardware-level sovereign defense enabled!</i>")

            CommandResult(success = true, message = sb.toString())
        } else {
            CommandResult(
                success = true,
                message = "👑 <b>Android Device Owner: INACTIVE</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "Current Device Admin: ${if (isAdmin) "✅ Active (Standard Admin Only)" else "❌ Inactive"}\n\n" +
                        "<b>How to activate Enterprise Device Owner:</b>\n" +
                        "1. Enable Developer Options & USB Debugging on your phone.\n" +
                        "2. Connect phone to PC via USB.\n" +
                        "3. Run this command in terminal/PowerShell:\n" +
                        "<code>adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin</code>\n\n" +
                        "<i>Tip: If prompted with an account error, temporarily remove Google accounts in Settings > Accounts, run the command, then re-add your accounts.</i>"
            )
        }
    }
}
