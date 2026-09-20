package com.izhaanintellect.pasa.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.service.PasaService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Automatically launches the PASA guardian service upon device boot.
 */
import android.os.UserManager

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager

    companion object {
        private const val TAG = "PASA_Boot"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.i(TAG, "Guardian wake event received ($action)")

            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            if (!userManager.isUserUnlocked) {
                Log.w(TAG, "Device is locked (Direct Boot). Using device-protected storage for minimal state check.")
                // Minimal check using Device Protected Storage
                val deviceContext = context.createDeviceProtectedStorageContext()
                val dpPrefs = deviceContext.getSharedPreferences("pasa_direct_boot", Context.MODE_PRIVATE)
                val isSetup = dpPrefs.getBoolean("setup_complete_dp", false)
                if (isSetup) {
                    PasaService.start(context)
                }
                return
            }

            if (preferencesManager.isSetupComplete) {
                Log.i(TAG, "PASA configured — launching PasaService")
                PasaService.start(context)
                
                // Keep device protected storage in sync for future reboots
                val deviceContext = context.createDeviceProtectedStorageContext()
                deviceContext.getSharedPreferences("pasa_direct_boot", Context.MODE_PRIVATE)
                    .edit().putBoolean("setup_complete_dp", true).apply()
            } else {
                Log.d(TAG, "PASA setup incomplete — skipping service launch")
            }
        }
    }
}
