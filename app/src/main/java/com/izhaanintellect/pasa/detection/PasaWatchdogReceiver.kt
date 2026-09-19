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
 * Watchdog BroadcastReceiver triggered by AlarmManager to resurrect PasaService.
 * On Android 12+, broadcast receivers triggered by alarms are granted foreground service
 * start privileges, overcoming background execution limits.
 */
@AndroidEntryPoint
class PasaWatchdogReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager

    companion object {
        private const val TAG = "PASA_Watchdog"
        const val ACTION_WATCHDOG_RESTART = "com.izhaanintellect.pasa.ACTION_WATCHDOG_RESTART"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Watchdog alarm received, verifying guardian service state")
        if (preferencesManager.isSetupComplete) {
            try {
                PasaService.start(context)
                Log.i(TAG, "PasaService restarted successfully from watchdog receiver")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start PasaService from watchdog receiver", e)
            }
        }
    }
}
