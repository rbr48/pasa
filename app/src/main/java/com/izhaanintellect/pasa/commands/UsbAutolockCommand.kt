package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.UsbAutolockManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UsbAutolockCommand (/usb_autolock).
 * Controls the automatic USB data pin killswitch.
 * When enabled, the USB data lines are automatically severed whenever the phone is locked,
 * neutralizing forensic extraction tools (Cellebrite, GrayKey) and BadUSB attacks.
 */
@Singleton
class UsbAutolockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val usbAutolockManager: UsbAutolockManager
) : Command {

    override val name = "/usb_autolock"
    override val description = "Auto-drop USB data pins when locked [Device Owner]"
    override val usage = "/usb_autolock [status|enable|disable]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> USB Autolock requires Android Enterprise Device Owner privileges.\nRun <code>/device_owner</code> for activation instructions."
            )
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return CommandResult(
                success = false,
                message = "❌ <b>Android 12+ Required:</b> Physical USB data pin signaling control requires Android 12+ (API 31+).\nYour device is running Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})."
            )
        }

        val param = args.firstOrNull()?.lowercase()?.trim() ?: "status"

        return when (param) {
            "on", "enable", "arm" -> {
                prefs.isUsbAutolockEnabled = true
                usbAutolockManager.startMonitoring()
                val isCurrentlyLocked = (context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager)?.isKeyguardLocked == true
                if (isCurrentlyLocked) {
                    PasaDeviceAdmin.setUsbDataSignaling(context, false)
                }
                CommandResult(
                    success = true,
                    message = "🔌 <b>Locked-State USB Killswitch: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "✅ <b>Status:</b> ACTIVE (Auto-Guarded)\n" +
                            "🔒 <b>Behavior:</b> USB data signaling will automatically die whenever screen locks.\n" +
                            "🔓 <b>Auto-Restore:</b> Restores seamlessly when you unlock the device with your credentials.\n" +
                            "🛡️ <i>Cellebrite, GrayKey, and BadUSB attacks neutralized while locked!</i>"
                )
            }

            "off", "disable", "disarm" -> {
                prefs.isUsbAutolockEnabled = false
                usbAutolockManager.stopMonitoring()
                PasaDeviceAdmin.setUsbDataSignaling(context, true)
                CommandResult(
                    success = true,
                    message = "🔌 <b>Locked-State USB Killswitch: DISARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "⚠️ USB data pins will no longer automatically drop when screen locks.\n" +
                            "💡 <i>You can still permanently disable USB data pins anytime via <code>/usb_lock on</code>.</i>"
                )
            }

            else -> {
                val isArmed = prefs.isUsbAutolockEnabled
                val currentUsbState = PasaDeviceAdmin.isUsbDataSignalingEnabled(context)
                val isKeyguardLocked = (context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager)?.isKeyguardLocked == true

                CommandResult(
                    success = true,
                    message = "🔌 <b>Locked-State USB Killswitch Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• <b>Autolock Mode:</b> ${if (isArmed) "🔒 ARMED (Active)" else "🔓 DISARMED"}\n" +
                            "• <b>Current Physical USB Data:</b> ${if (currentUsbState == true) "🟢 CONNECTED" else "🔴 DEAD (Charging Only)"}\n" +
                            "• <b>Keyguard State:</b> ${if (isKeyguardLocked) "🔒 Locked" else "🔓 Unlocked"}\n\n" +
                            "💡 <i>Usage:</i>\n" +
                            "• <code>/usb_autolock enable</code> — Automatically kill USB data on lock\n" +
                            "• <code>/usb_autolock disable</code> — Turn off automatic killswitch"
                )
            }
        }
    }
}
