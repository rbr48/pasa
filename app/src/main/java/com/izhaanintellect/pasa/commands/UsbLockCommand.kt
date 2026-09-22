package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls hardware-level USB data pin signaling.
 * Neutralizes juice-jacking, BadUSB, and forensic data extraction boxes.
 */
@Singleton
class UsbLockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : Command {

    override val name = "/usb_lock"
    override val description = "Hardware USB data pin killswitch [Device Owner / Android 12+]"
    override val usage = "/usb_lock [on|off|status]"

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

        val param = args.firstOrNull()?.lowercase() ?: "status"

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
