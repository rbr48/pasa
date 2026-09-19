package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles remote device locking.
 * Uses DevicePolicyManager to immediately lock the screen and optionally update lock messages.
 */
@Singleton
class LockCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/lock"
    override val description = "Lock device immediately"
    override val usage = "/lock | /lock_message <text>"

    companion object {
        private const val TAG = "PASA_Lock"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = PasaDeviceAdmin.getComponentName(context)

        if (!dpm.isAdminActive(adminComponent)) {
            return CommandResult(
                success = false,
                message = "❌ Device Administrator is not active. Cannot lock device."
            )
        }

        return try {
            if (args.isNotEmpty()) {
                val fullArg = args.joinToString(" ")
                val message = if (args[0].equals("message", ignoreCase = true)) {
                    args.drop(1).joinToString(" ")
                } else {
                    fullArg.removePrefix("message").trim()
                }

                if (message.isNotBlank()) {
                    try {
                        dpm.setDeviceOwnerLockScreenInfo(adminComponent, message)
                    } catch (e: SecurityException) {
                        Log.w(TAG, "setDeviceOwnerLockScreenInfo requires Device Owner", e)
                    }

                    // Display full-screen message overlay over lockscreen
                    try {
                        val alertIntent = com.izhaanintellect.pasa.ui.AlertMessageActivity.createIntent(context, message)
                        context.startActivity(alertIntent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not start AlertMessageActivity: ${e.message}")
                    }

                    dpm.lockNow()
                    CommandResult(
                        success = true,
                        message = "🔒 Device locked with lock-screen message: \"$message\""
                    )
                } else {
                    dpm.lockNow()
                    CommandResult(success = true, message = "🔒 Device locked successfully.")
                }
            } else {
                dpm.lockNow()
                Log.i(TAG, "Device screen locked via remote command")
                CommandResult(
                    success = true,
                    message = "🔒 Device locked immediately."
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Lock failed due to permissions", e)
            CommandResult(success = false, message = "❌ Lock failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Lock failed", e)
            CommandResult(success = false, message = "❌ Lock failed: ${e.message}")
        }
    }
}
