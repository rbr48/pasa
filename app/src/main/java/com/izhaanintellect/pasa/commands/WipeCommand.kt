package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.OTPManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles remote emergency device wipe / factory reset.
 * Strictly requires master password or OTP verification before executing destructive wipe.
 */
@Singleton
class WipeCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val otpManager: OTPManager
) : Command {

    override val name = "/wipe"
    override val description = "Emergency factory reset (Requires authentication)"
    override val usage = "/wipe | /wipe_confirm <master_password_or_otp>"

    companion object {
        private const val TAG = "PASA_Wipe"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = PasaDeviceAdmin.getComponentName(context)

        if (!dpm.isAdminActive(adminComponent)) {
            return CommandResult(
                success = false,
                message = "❌ Device Admin is not active. Cannot execute wipe."
            )
        }

        val firstArg = args.firstOrNull()?.lowercase()

        // Confirmation branch
        if (firstArg == "confirm" || (args.isNotEmpty() && !firstArg.isNullOrEmpty() && firstArg != "external")) {
            val tokenOrPassword = if (firstArg == "confirm") args.getOrNull(1) else args[0]
            if (tokenOrPassword.isNullOrBlank()) {
                return CommandResult(
                    success = false,
                    message = "❌ Confirmation credential required: <code>/wipe_confirm &lt;master_password&gt;</code>"
                )
            }

            // Verify using Master Password ONLY
            val isPasswordValid = authManager.verifyMasterPassword(tokenOrPassword)

            if (isPasswordValid) {
                Log.w(TAG, "⚠️ FACTORY RESET CONFIRMED VIA AUTHENTICATION — WIPING DEVICE NOW")
                return try {
                    dpm.wipeData(0)
                    CommandResult(
                        success = true,
                        message = "🗑️ <b>Emergency wipe initiated.</b> All device data is being securely erased."
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Device wipe failed", e)
                    CommandResult(success = false, message = "❌ Wipe failed: ${e.message}")
                }
            } else {
                Log.w(TAG, "Unauthorized wipe confirmation attempt rejected")
                return CommandResult(
                    success = false,
                    message = "⛔ <b>Authentication Failed!</b> Invalid master password or expired OTP."
                )
            }
        }

        // Initiation branch: generate Wipe prompt
        Log.i(TAG, "Wipe challenge generated")

        return CommandResult(
            success = true,
            message = """
                ⚠️ <b>EMERGENCY FACTORY RESET REQUESTED</b>
                ━━━━━━━━━━━━━━━━━━━━
                This action will <b>PERMANENTLY ERASE ALL USER DATA</b>, accounts, and files from this device.
                
                🔐 <b>Confirmation Required:</b>
                To execute wipe, you must provide your Master Password:
                <code>/wipe_confirm &lt;your_master_password&gt;</code>
            """.trimIndent()
        )
    }
}
