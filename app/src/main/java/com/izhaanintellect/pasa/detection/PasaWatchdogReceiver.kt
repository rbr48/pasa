package com.izhaanintellect.pasa.detection

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.service.PasaService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Watchdog BroadcastReceiver triggered by AlarmManager to resurrect PasaService
 * and defeat Android Doze mode and aggressive OEM task killers.
 * Operates in Direct Boot mode (pre-first-unlock) and standard mode.
 */
@AndroidEntryPoint
class PasaWatchdogReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager

    companion object {
        private const val TAG = "PASA_Watchdog"
        const val ACTION_WATCHDOG_RESTART = "com.izhaanintellect.pasa.ACTION_WATCHDOG_RESTART"
        private const val HEARTBEAT_INTERVAL_MS = 180_000L // 3 minutes periodic pulse for near-zero battery footprint

        fun scheduleHeartbeat(context: Context, delayMs: Long = HEARTBEAT_INTERVAL_MS) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, PasaWatchdogReceiver::class.java).apply {
                    action = ACTION_WATCHDOG_RESTART
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pending = PendingIntent.getBroadcast(context, 999, intent, flags)
                val triggerAtMillis = System.currentTimeMillis() + delayMs

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                }
                Log.d(TAG, "Heartbeat watchdog pulse scheduled in ${delayMs / 1000}s")
            } catch (e: Exception) {
                Log.e(TAG, "Failed scheduling watchdog heartbeat: ${e.message}")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Watchdog alarm pulse received, verifying guardian service state")

        // Acquire temporary wake lock to prevent OS from going to sleep during resurrection
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wl = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pasa:watchdog_pulse")
        wl?.acquire(15_000L)

        try {
            if (preferencesManager.isSetupComplete) {
                val service = PasaService.currentService
                if (service == null || !service.isServiceActive()) {
                    Log.w(TAG, "PasaService not running! Resurrecting daemon immediately.")
                    PasaService.start(context)
                } else {
                    service.verifyPollingHealth()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in watchdog pulse verification: ${e.message}", e)
        } finally {
            // Keep perpetual watchdog pulse active indefinitely
            scheduleHeartbeat(context)
            try {
                if (wl != null && wl.isHeld) {
                    wl.release()
                }
            } catch (_: Exception) {}
        }
    }
}
