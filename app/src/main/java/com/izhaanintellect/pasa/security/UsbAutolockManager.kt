package com.izhaanintellect.pasa.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UsbAutolockManager.
 * Automatically deactivates physical USB data signaling (Cellebrite/GrayKey/Juice-Jacking killswitch)
 * whenever the screen is turned off or locked, and automatically restores USB data when the owner
 * unlocks the phone with their authentic credentials.
 */
@Singleton
class UsbAutolockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) {
    companion object {
        private const val TAG = "PASA_UsbAutolock"
    }

    private var receiver: BroadcastReceiver? = null
    private var isRegistered = false

    fun startMonitoring() {
        if (!prefs.isUsbAutolockEnabled) {
            Log.d(TAG, "UsbAutolock is disabled in preferences; skipping registration.")
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Log.w(TAG, "USB data signaling control requires Android 12+ (API 31+).")
            return
        }
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            Log.w(TAG, "UsbAutolock requires Android Enterprise Device Owner.")
            return
        }
        if (isRegistered) return

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        if (prefs.isUsbAutolockEnabled && PasaDeviceAdmin.isDeviceOwner(context)) {
                            Log.i(TAG, "Screen OFF / Locked: Automatically disabling USB data signaling...")
                            PasaDeviceAdmin.setUsbDataSignaling(context, false)
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        if (prefs.isUsbAutolockEnabled && PasaDeviceAdmin.isDeviceOwner(context)) {
                            // Only restore if user hasn't explicitly locked USB permanently via /usb_lock
                            if (!prefs.usbLockEnabled) {
                                Log.i(TAG, "User Authenticated / Unlocked: Restoring USB data signaling...")
                                PasaDeviceAdmin.setUsbDataSignaling(context, true)
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(receiver, filter)
        isRegistered = true
        Log.i(TAG, "UsbAutolockManager monitoring armed successfully.")
    }

    fun stopMonitoring() {
        if (isRegistered && receiver != null) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering UsbAutolock receiver: ${e.message}")
            }
            receiver = null
            isRegistered = false
            Log.i(TAG, "UsbAutolockManager monitoring stopped.")
        }
    }
}
