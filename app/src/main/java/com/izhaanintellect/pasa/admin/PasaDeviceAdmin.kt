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

        fun rebootDevice(context: Context): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ Remote reboot requires Android Device Owner permissions.\nCheck status with /device_owner.")
            }
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    dpm.reboot(component)
                    Pair(true, "🔄 Device reboot initiated.")
                } else {
                    Pair(false, "❌ Hardware reboot requires Android 7.0+ (API 24+).")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reboot device", e)
                Pair(false, "❌ Hardware reboot failed: ${e.message}")
            }
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

        fun isResetPasswordTokenActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            return try {
                dpm.isResetPasswordTokenActive(component)
            } catch (_: Exception) { false }
        }

        fun ensureResetPasswordToken(context: Context, prefs: PreferencesManager): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

            return try {
                if (dpm.isResetPasswordTokenActive(component)) {
                    Log.d(TAG, "Reset password token is already active")
                    return true
                }

                // Retrieve persistent token or generate new 32-byte token
                val existingBytes = if (!prefs.resetPasswordToken.isNullOrBlank()) {
                    try {
                        android.util.Base64.decode(prefs.resetPasswordToken, android.util.Base64.NO_WRAP)
                    } catch (_: Exception) { null }
                } else null

                val tokenBytes = if (existingBytes != null && existingBytes.size >= 32) {
                    existingBytes
                } else {
                    ByteArray(32).apply { java.security.SecureRandom().nextBytes(this) }.also {
                        prefs.resetPasswordToken = android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
                    }
                }

                val setSuccess = dpm.setResetPasswordToken(component, tokenBytes)
                Log.i(TAG, "setResetPasswordToken result: $setSuccess, active: ${dpm.isResetPasswordTokenActive(component)}")
                setSuccess
            } catch (e: Exception) {
                Log.w(TAG, "Error ensuring reset password token: ${e.message}")
                false
            }
        }

        fun resetDevicePassword(context: Context, newPin: String, prefs: PreferencesManager): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)

            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "Device Owner permission is not granted on this phone.")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return Pair(false, "Remote OS password reset requires Android 8.0+.")
            }

            return try {
                ensureResetPasswordToken(context, prefs)
                val tokenStr = prefs.resetPasswordToken
                    ?: return Pair(false, "No escrow reset token available on device.")
                val tokenBytes = android.util.Base64.decode(tokenStr, android.util.Base64.NO_WRAP)

                if (!dpm.isResetPasswordTokenActive(component)) {
                    return Pair(
                        false,
                        "Hardware escrow token enrolled, but waiting for one-time lockscreen activation.\n\n" +
                        "📱 <b>Action needed:</b> Press the power button to lock your phone screen, then unlock it once using your current lockscreen PIN/password. Android Keyguard will instantly arm the escrow token, enabling remote password resets anytime."
                    )
                }

                val success = dpm.resetPasswordWithToken(component, newPin, tokenBytes, 0)
                if (success) {
                    Pair(true, "Android OS lockscreen PIN successfully changed.")
                } else {
                    Pair(false, "dpm.resetPasswordWithToken returned false. Verify PIN meets device password quality requirements.")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException resetting password", e)
                Pair(false, "SecurityException: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Exception resetting password", e)
                Pair(false, "Error: ${e.message}")
            }
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
            val pendingResult = goAsync()
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
                } finally {
                    pendingResult.finish()
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
            val pendingResult = goAsync()
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
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onPasswordFailed(context: Context, intent: Intent, userHandle: UserHandle) {
        super.onPasswordFailed(context, intent, userHandle)
        Log.w(TAG, "Password failure event received")

        val entryPoint = getEntryPoint(context)
        val detector = entryPoint.failedUnlockDetector()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                detector.onFailedAttempt()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)
        Log.i(TAG, "Password succeeded — resetting failed attempt counter")

        val entryPoint = getEntryPoint(context)
        entryPoint.failedUnlockDetector().resetCounter()
    }
}
