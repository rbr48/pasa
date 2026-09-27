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

            // Schedule immediate watchdog heartbeat chain
            PasaWatchdogReceiver.scheduleHeartbeat(context, 1500L)

            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val isDirectBoot = userManager != null && !userManager.isUserUnlocked
            Log.i(TAG, "Boot mode: isDirectBoot=$isDirectBoot, isSetupComplete=${preferencesManager.isSetupComplete}")

            if (preferencesManager.isSetupComplete) {
                Log.i(TAG, "PASA configured — launching PasaService immediately")
                try {
                    PasaService.start(context)
                } catch (t: Throwable) {
                    Log.w(TAG, "Direct PasaService.start restricted by OS (${t.message}), falling back to AlarmManager")
                    scheduleWatchdogAlarm(context)
                }

                // If this is an APK update/replacement, also schedule an alarm safety-net
                if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    scheduleWatchdogAlarm(context)
                }
                
                // Re-apply Device Owner policies after boot (works even in Direct Boot)
                try {
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                    val component = android.content.ComponentName(context, com.izhaanintellect.pasa.admin.PasaDeviceAdmin::class.java)
                    if (dpm?.isDeviceOwnerApp(context.packageName) == true) {
                        dpm.setLockTaskPackages(component, arrayOf(context.packageName))
                        val shouldDisable = preferencesManager.isLostModeActive || preferencesManager.isFakeShutdownActive
                        dpm.setStatusBarDisabled(component, shouldDisable)
                        Log.i(TAG, "Device Owner policies re-applied after boot (statusBarDisabled=$shouldDisable)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not re-apply Device Owner policies: ${e.message}")
                }

                // Re-activate fake shutdown if it was active before reboot
                if (preferencesManager.isFakeShutdownActive) {
                    Log.i(TAG, "Fake shutdown was active before reboot — re-activating")
                    try {
                        val fakeIntent = com.izhaanintellect.pasa.ui.FakeShutdownActivity.createIntent(context)
                        com.izhaanintellect.pasa.util.SecurityActivityLauncher.launch(
                            context = context,
                            intent = fakeIntent,
                            notificationId = 2003,
                            notificationTitle = "System Power Management",
                            notificationText = "Display standby protocol active",
                            wakeScreen = true,
                            ongoing = true,
                            silentNotification = true
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not re-activate fake shutdown after boot: ${e.message}")
                    }
                }
                
                // Keep device protected storage in sync
                preferencesManager.syncToDeviceProtectedStorage()
            } else {
                Log.d(TAG, "PASA setup incomplete — skipping service launch")
            }
        }
    }

    private fun scheduleWatchdogAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            val watchdogIntent = Intent(context, PasaWatchdogReceiver::class.java).apply {
                action = PasaWatchdogReceiver.ACTION_WATCHDOG_RESTART
            }
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pending = android.app.PendingIntent.getBroadcast(context, 999, watchdogIntent, flags)
            alarmManager?.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1500L,
                pending
            )
            Log.i(TAG, "Scheduled watchdog resurrection alarm via AlarmManager in 1.5s")
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling watchdog alarm: ${e.message}")
        }
    }
}
