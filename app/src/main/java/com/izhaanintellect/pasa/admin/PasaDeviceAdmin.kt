package com.izhaanintellect.pasa.admin

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.detection.FailedUnlockDetector
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Device Administration receiver for PASA.
 * Handles device admin activation/deactivation and failed unlock attempts.
 */
class PasaDeviceAdmin : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "PASA_DeviceAdmin"

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, PasaDeviceAdmin::class.java)
        }

        fun isAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isAdminActive(getComponentName(context))
        }

        fun isDeviceOwner(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isDeviceOwnerApp(context.packageName)
        }

        fun setUninstallBlocked(context: Context, blocked: Boolean): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            return if (dpm.isDeviceOwnerApp(context.packageName)) {
                try {
                    dpm.setUninstallBlocked(component, context.packageName, blocked)
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to setUninstallBlocked: ${e.message}")
                    false
                }
            } else false
        }

        fun configureLockTask(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            return if (dpm.isDeviceOwnerApp(context.packageName)) {
                try {
                    dpm.setLockTaskPackages(component, arrayOf(context.packageName))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        dpm.setLockTaskFeatures(component, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            dpm.setStatusBarDisabled(component, true)
                        } catch (se: Exception) {
                            Log.w(TAG, "setStatusBarDisabled error: ${se.message}")
                        }
                    }
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to setLockTaskPackages: ${e.message}")
                    false
                }
            } else false
        }

        fun setComprehensiveLockdown(context: Context, enabled: Boolean): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            return if (dpm.isDeviceOwnerApp(context.packageName)) {
                try {
                    val restrictions = listOf(
                        UserManager.DISALLOW_AIRPLANE_MODE,
                        UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                        UserManager.DISALLOW_CONFIG_TETHERING,
                        UserManager.DISALLOW_DEBUGGING_FEATURES,
                        UserManager.DISALLOW_USB_FILE_TRANSFER
                    )
                    for (restriction in restrictions) {
                        if (enabled) {
                            dpm.addUserRestriction(component, restriction)
                        } else {
                            dpm.clearUserRestriction(component, restriction)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            dpm.setStatusBarDisabled(component, enabled)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to setStatusBarDisabled: ${e.message}")
                        }
                    }

                    Log.i(TAG, "Comprehensive Hardware Lockdown state set to: $enabled")
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to setComprehensiveLockdown: ${e.message}")
                    false
                }
            } else false
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AdminEntryPoint {
        fun preferencesManager(): PreferencesManager
        fun telegramApi(): TelegramApi
        fun failedUnlockDetector(): FailedUnlockDetector
    }

    private fun getEntryPoint(context: Context): AdminEntryPoint {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            AdminEntryPoint::class.java
        )
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "PASA Device Admin enabled successfully")

        val entryPoint = getEntryPoint(context)
        val prefs = entryPoint.preferencesManager()
        val api = entryPoint.telegramApi()

        if (prefs.isConfigured()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    api.sendMessage(
                        token = prefs.botToken,
                        request = SendMessageRequest(
                            chatId = prefs.ownerChatIdLong,
                            text = "🛡️ <b>PASA Device Admin Activated</b>\n\n" +
                                    "✅ Privileged device management enabled.\n" +
                                    "🔒 Remote lock and wipe features are now active."
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send admin enabled alert", e)
                }
            }
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "⚠️ PASA Device Admin DISABLED — potential tampering!")

        val entryPoint = getEntryPoint(context)
        val prefs = entryPoint.preferencesManager()
        val api = entryPoint.telegramApi()

        if (prefs.isConfigured()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    api.sendMessage(
                        token = prefs.botToken,
                        request = SendMessageRequest(
                            chatId = prefs.ownerChatIdLong,
                            text = "🚨 <b>SECURITY ALERT: Device Admin Revoked!</b>\n\n" +
                                    "⚠️ PASA admin privileges were revoked from device settings.\n" +
                                    "Remote lock and emergency wipe are no longer available until re-activated."
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send admin disabled alert", e)
                }
            }
        }
    }

    override fun onPasswordFailed(context: Context, intent: Intent, userHandle: UserHandle) {
        super.onPasswordFailed(context, intent, userHandle)
        Log.w(TAG, "Password failure event received")

        val entryPoint = getEntryPoint(context)
        val detector = entryPoint.failedUnlockDetector()

        CoroutineScope(Dispatchers.IO).launch {
            detector.onFailedAttempt()
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)
        Log.i(TAG, "Password succeeded — resetting failed attempt counter")

        val entryPoint = getEntryPoint(context)
        entryPoint.failedUnlockDetector().resetCounter()
    }
}
