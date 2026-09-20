package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.ui.AlertMessageActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remotely dismisses Lost Mode Guard, clears custom PIN, and releases lock task kiosk.
 */
@Singleton
class UnlockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/unlock"
    override val description = "Remotely unlock device and dismiss Lost Mode Guard"
    override val usage = "/unlock"

    companion object {
        private const val TAG = "PASA_Unlock"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        Log.i(TAG, "Executing remote /unlock command")

        // 1. Clear Lost Mode preferences
        preferencesManager.isLostModeActive = false
        preferencesManager.activeLockPin = null
        preferencesManager.lostModeMessage = ""

        // 2. Clear Device Owner lockdown restrictions
        try {
            com.izhaanintellect.pasa.admin.PasaDeviceAdmin.setComprehensiveLockdown(context, false)
            com.izhaanintellect.pasa.admin.PasaDeviceAdmin.setUninstallBlocked(context, false)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear lockdown restrictions: ${e.message}")
        }

        // 3. Broadcast dismissal to AlertMessageActivity
        try {
            val dismissIntent = Intent(AlertMessageActivity.ACTION_DISMISS_LOST_MODE).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(dismissIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send unlock broadcast: ${e.message}")
        }

        // 4. Dismiss full-screen alert notification
        com.izhaanintellect.pasa.util.SecurityActivityLauncher.dismissNotification(context, AlertMessageActivity.NOTIFICATION_ID)

        return CommandResult(
            success = true,
            message = "🔓 <b>Device Unlocked Remotely</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                    "✅ Lost Mode Guard has been deactivated.\n" +
                    "Emergency PIN cleared and screen kiosk released."
        )
    }
}
