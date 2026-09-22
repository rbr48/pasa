package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls lockscreen biometric authentication (fingerprint/face unlock).
 * Essential for duress defense against forced unlocking at checkpoints or during robberies.
 */
@Singleton
class BiometricsCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : Command {

    override val name = "/biometrics"
    override val description = "Toggle Biometric Coercion Killswitch [Device Owner]"
    override val usage = "/biometrics [on|off|status]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> Biometric keyguard controls require Device Owner privileges.\n" +
                        "Run <code>/device_owner</code> for activation instructions."
            )
        }

        val action = args.firstOrNull()?.lowercase() ?: "status"

        return when (action) {
            "off", "disable", "kill", "lock" -> {
                val (ok, text) = PasaDeviceAdmin.setBiometricsDisabled(context, true)
                if (ok) {
                    prefs.biometricsDisabled = true
                    CommandResult(
                        success = true,
                        message = "🧬 <b>Biometric Killswitch: ACTIVATED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "🚫 <b>Fingerprint Unlock:</b> DISABLED\n" +
                                "🚫 <b>Face Recognition:</b> DISABLED\n" +
                                "🔑 <b>Fallback:</b> Complex PIN / Password strictly enforced\n\n" +
                                "🛡️ <i>Forced biometric unlock cannot be used against you. Restore anytime using <code>/biometrics on</code>.</i>"
                    )
                } else {
                    CommandResult(false, text)
                }
            }

            "on", "enable", "restore" -> {
                val (ok, text) = PasaDeviceAdmin.setBiometricsDisabled(context, false)
                if (ok) {
                    prefs.biometricsDisabled = false
                    CommandResult(
                        success = true,
                        message = "🧬 <b>Biometric Unlock: RESTORED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "✅ Fingerprint and face authentication are now active on the lockscreen."
                    )
                } else {
                    CommandResult(false, text)
                }
            }

            "status" -> {
                val disabled = PasaDeviceAdmin.isBiometricsDisabled(context)
                CommandResult(
                    success = true,
                    message = "🧬 <b>Biometric Authentication Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• Fingerprint & Face Unlock: ${if (disabled) "🚫 <b>DISABLED (Duress Mode)</b>" else "✅ <b>ENABLED (Normal)</b>"}\n\n" +
                            "💡 <i>Use <code>/biometrics off</code> to immediately disable biometric sensors, or <code>/biometrics on</code> to restore.</i>"
                )
            }

            else -> CommandResult(
                success = false,
                message = "❓ <b>Invalid Parameter:</b> Use <code>/biometrics on</code>, <code>/biometrics off</code>, or <code>/biometrics status</code>."
            )
        }
    }
}
