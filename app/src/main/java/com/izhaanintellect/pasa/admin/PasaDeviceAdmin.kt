package com.izhaanintellect.pasa.admin

import android.Manifest
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.app.admin.SecurityLog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
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

        fun getComponent(context: Context): ComponentName = getComponentName(context)

        fun wipeDevice(context: Context, reason: String = "") {
            try {
                Log.w(TAG, "Wipe device requested: $reason")
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                dpm?.wipeData(0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to wipe device: ${e.message}", e)
            }
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
                        dpm.setLockTaskFeatures(
                            component,
                            DevicePolicyManager.LOCK_TASK_FEATURE_NONE or DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD
                        )
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

        fun applyAntiTamperSuite(context: Context, enabled: Boolean): Map<String, Boolean> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return emptyMap()

            val results = mutableMapOf<String, Boolean>()
            val restrictions = listOf(
                UserManager.DISALLOW_SAFE_BOOT to "Safe Boot Prohibition",
                UserManager.DISALLOW_AIRPLANE_MODE to "Airplane Mode Lock",
                UserManager.DISALLOW_FACTORY_RESET to "Factory Reset Block",
                UserManager.DISALLOW_NETWORK_RESET to "Network Reset Block",
                UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA to "OTG / Media Mount Block",
                UserManager.DISALLOW_USB_FILE_TRANSFER to "USB MTP File Transfer Block",
                UserManager.DISALLOW_CONFIG_LOCATION to "Location Toggle Tamper Lock",
                UserManager.DISALLOW_DEBUGGING_FEATURES to "USB ADB Debugging Block"
            )

            for ((restriction, name) in restrictions) {
                try {
                    if (enabled) {
                        dpm.addUserRestriction(component, restriction)
                    } else {
                        dpm.clearUserRestriction(component, restriction)
                    }
                    results[name] = true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to toggle restriction $restriction: ${e.message}")
                    results[name] = false
                }
            }

            // Lock OEM Bootloader Unlocking if supported by firmware
            try {
                val method = dpm.javaClass.getMethod("setOemUnlockAllowed", ComponentName::class.java, Boolean::class.javaPrimitiveType)
                method.invoke(dpm, component, !enabled)
                results["OEM Bootloader Unlock Disabled"] = true
            } catch (e: Exception) {
                Log.d(TAG, "setOemUnlockAllowed not supported: ${e.message}")
            }

            // Disable Developer Options in Global Settings
            try {
                if (enabled) {
                    android.provider.Settings.Global.putInt(
                        context.contentResolver,
                        android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                        0
                    )
                    results["Developer Options Disabled"] = true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Disable developer options error: ${e.message}")
            }

            try {
                dpm.setStatusBarDisabled(component, enabled)
                results["Notification Shade / Quick Settings Lockout"] = true
            } catch (e: Exception) {
                results["Notification Shade / Quick Settings Lockout"] = false
            }

            return results
        }

        fun setComprehensiveLockdown(context: Context, enabled: Boolean): Boolean {
            val map = applyAntiTamperSuite(context, enabled)
            return map.isNotEmpty() && map.values.any { it }
        }

        fun setUsbDataSignaling(context: Context, enabled: Boolean): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ USB Data Control requires Android Device Owner.")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return Pair(false, "❌ Physical USB data pin control requires Android 12+ (API 31+).\nYour device is running Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}).")
            }
            return try {
                dpm.setUsbDataSignalingEnabled(enabled)
                val status = if (enabled) "ENABLED (Normal USB Connection)" else "DISABLED (Data pins dead, AC charging only)"
                Pair(true, "🔌 USB Data Signaling state updated: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set USB data signaling: ${e.message}", e)
                Pair(false, "❌ Error setting USB data state: ${e.message}")
            }
        }

        fun isUsbDataSignalingEnabled(context: Context): Boolean? {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (!dpm.isDeviceOwnerApp(context.packageName)) return null
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
            return try {
                dpm.isUsbDataSignalingEnabled
            } catch (_: Exception) { null }
        }

        fun selfHealPermissions(context: Context): Map<String, Boolean> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return emptyMap()

            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.READ_MEDIA_IMAGES)
                perms.add(Manifest.permission.READ_MEDIA_VIDEO)
                perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            }

            val results = mutableMapOf<String, Boolean>()
            for (perm in perms) {
                try {
                    val currentState = dpm.getPermissionGrantState(component, context.packageName, perm)
                    if (currentState == DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED) {
                        results[perm.substringAfterLast('.')] = true
                        continue
                    }
                    val success = dpm.setPermissionGrantState(
                        component,
                        context.packageName,
                        perm,
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                    )
                    results[perm.substringAfterLast('.')] = success
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to self-heal permission $perm: ${e.message}")
                    results[perm.substringAfterLast('.')] = false
                }
            }

            // Notification permission policy: if suppressed by user, DENY to hide from shade; otherwise GRANT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val prefs = com.izhaanintellect.pasa.data.PreferencesManager(context)
                    val targetState = if (prefs.isNotificationSuppressed) {
                        DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED
                    } else {
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                    }
                    val currentState = dpm.getPermissionGrantState(
                        component,
                        context.packageName,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    if (currentState == targetState) {
                        results["POST_NOTIFICATIONS"] = true
                    } else {
                        val success = dpm.setPermissionGrantState(
                            component,
                            context.packageName,
                            Manifest.permission.POST_NOTIFICATIONS,
                            targetState
                        )
                        results["POST_NOTIFICATIONS"] = success
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set POST_NOTIFICATIONS grant state: ${e.message}")
                    results["POST_NOTIFICATIONS"] = false
                }
            }

            return results
        }

        fun setNotificationSuppressed(context: Context, suppressed: Boolean): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return false
            val prefs = com.izhaanintellect.pasa.data.PreferencesManager(context)
            prefs.isNotificationSuppressed = suppressed
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val targetState = if (suppressed) {
                    DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED
                } else {
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                }
                try {
                    val currentState = dpm.getPermissionGrantState(
                        component,
                        context.packageName,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    if (currentState == targetState) {
                        true
                    } else {
                        dpm.setPermissionGrantState(
                            component,
                            context.packageName,
                            Manifest.permission.POST_NOTIFICATIONS,
                            targetState
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set POST_NOTIFICATIONS grant state: ${e.message}", e)
                    false
                }
            } else {
                true
            }
        }

        fun forceLocationHardware(context: Context, enabled: Boolean): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return try {
                dpm.setLocationEnabled(component, enabled)
                true
            } catch (e: Exception) {
                Log.w(TAG, "forceLocationHardware error: ${e.message}")
                false
            }
        }

        fun isLocationHardwareEnabled(context: Context): Boolean? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
            return try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                lm.isLocationEnabled
            } catch (_: Exception) { null }
        }

        fun setAppHidden(context: Context, packageName: String, hidden: Boolean): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ Device Owner required to freeze/hide applications.")
            }
            if (packageName == context.packageName) {
                return Pair(false, "⚠️ Cannot hide PASA Sentinel itself.")
            }
            return try {
                val success = dpm.setApplicationHidden(component, packageName, hidden)
                if (success) {
                    val state = if (hidden) "FROZEN & HIDDEN (Stealth Vault)" else "RESTORED & VISIBLE"
                    Pair(true, "📦 Package <code>$packageName</code> is now $state.")
                } else {
                    Pair(false, "❌ OS returned false for package <code>$packageName</code>. Verify package is installed.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling app hidden state", e)
                Pair(false, "❌ Error: ${e.message}")
            }
        }

        fun isAppHidden(context: Context, packageName: String): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return false
            return try {
                dpm.isApplicationHidden(component, packageName)
            } catch (_: Exception) { false }
        }

        fun setBiometricsDisabled(context: Context, disabled: Boolean): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ Device Owner required to manage biometric authentication.")
            }
            return try {
                val flags = if (disabled) {
                    var f = DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        f = f or DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS or DevicePolicyManager.KEYGUARD_DISABLE_FACE
                    }
                    f
                } else {
                    DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE
                }
                dpm.setKeyguardDisabledFeatures(component, flags)
                val state = if (disabled) "DISABLED (Duress Mode: Complex PIN/Password required)" else "ENABLED (Biometrics restored)"
                Pair(true, "🧬 Biometric authentication is now $state.")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting keyguard disabled features", e)
                Pair(false, "❌ Error: ${e.message}")
            }
        }

        fun isBiometricsDisabled(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            return try {
                val features = dpm.getKeyguardDisabledFeatures(component)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    (features and DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS) != 0 ||
                    (features and DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0
                } else {
                    (features and DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0
                }
            } catch (_: Exception) { false }
        }

        fun enableSecurityLogging(context: Context, enabled: Boolean): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
            return try {
                dpm.setSecurityLoggingEnabled(component, enabled)
                true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to setSecurityLoggingEnabled: ${e.message}")
                false
            }
        }

        fun isSecurityLoggingEnabled(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
            return try {
                dpm.isSecurityLoggingEnabled(component)
            } catch (_: Exception) { false }
        }

        fun setGlobalPrivateDns(context: Context, host: String?): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ Device Owner required to manage system Private DNS.")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return Pair(false, "❌ Global Private DNS enforcement requires Android 10+ (API 29+).\nYour device is running Android ${Build.VERSION.RELEASE}.")
            }

            return try {
                if (host.isNullOrBlank() || host.equals("off", ignoreCase = true) || host.equals("auto", ignoreCase = true)) {
                    val res = dpm.setGlobalPrivateDnsModeOpportunistic(component)
                    if (res == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) {
                        Pair(true, "🌐 System Private DNS set to: <b>OPPORTUNISTIC (Automatic)</b>")
                    } else {
                        Pair(false, "❌ Failed to set Private DNS (code: $res)")
                    }
                } else {
                    val cleanHost = host.trim().lowercase()
                    val res = dpm.setGlobalPrivateDnsModeSpecifiedHost(component, cleanHost)
                    when (res) {
                        DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR -> {
                            Pair(true, "🌐 <b>System-Wide Encrypted DNS Locked:</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                    "• Provider Host: <code>$cleanHost</code>\n" +
                                    "• Protocol: <b>DNS-over-TLS (DoT, Port 853)</b>\n" +
                                    "🔒 <i>All ISP and cellular rogue DNS tracking/tampering blocked!</i>")
                        }
                        DevicePolicyManager.PRIVATE_DNS_SET_ERROR_HOST_NOT_SERVING -> {
                            Pair(false, "⚠️ Host <code>$cleanHost</code> is not currently serving DNS-over-TLS queries on port 853.")
                        }
                        DevicePolicyManager.PRIVATE_DNS_SET_ERROR_FAILURE_SETTING -> {
                            Pair(false, "❌ OS failed to apply Private DNS setting.")
                        }
                        else -> Pair(false, "❌ Private DNS error (code: $res)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting global private DNS", e)
                Pair(false, "❌ Error: ${e.message}")
            }
        }

        fun getGlobalPrivateDns(context: Context): Pair<Int, String?> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return Pair(-1, null)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return Pair(-1, null)
            return try {
                val mode = dpm.getGlobalPrivateDnsMode(component)
                val host = dpm.getGlobalPrivateDnsHost(component)
                Pair(mode, host)
            } catch (_: Exception) { Pair(-1, null) }
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

        fun clearDevicePassword(context: Context, prefs: PreferencesManager): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)

            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                Log.w(TAG, "clearDevicePassword failed: Device Owner not granted")
                return false
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Log.w(TAG, "clearDevicePassword requires Android 8.0+")
                return false
            }

            return try {
                ensureResetPasswordToken(context, prefs)
                if (!dpm.isResetPasswordTokenActive(component)) {
                    Log.w(TAG, "clearDevicePassword: Reset password token is not active")
                    return false
                }
                val tokenStr = prefs.resetPasswordToken ?: return false
                val tokenBytes = android.util.Base64.decode(tokenStr, android.util.Base64.NO_WRAP)

                var success = dpm.resetPasswordWithToken(component, null, tokenBytes, 0)
                if (!success) {
                    success = dpm.resetPasswordWithToken(component, "", tokenBytes, 0)
                }
                Log.i(TAG, "clearDevicePassword result: $success")
                success
            } catch (e: Exception) {
                Log.e(TAG, "Exception clearing password via escrow token", e)
                false
            }
        }

        fun setCameraDisabled(context: Context, disabled: Boolean): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isAdminActive(component)) {
                return Pair(false, "❌ Device Admin privileges required to manage camera state.")
            }
            return try {
                dpm.setCameraDisabled(component, disabled)
                val status = if (disabled) "LOCKED (All cameras disabled system-wide)" else "UNLOCKED (Normal camera access restored)"
                Pair(true, "📷 Hardware Camera state: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setCameraDisabled: ${e.message}", e)
                Pair(false, "❌ Error setting camera state: ${e.message}")
            }
        }

        fun isCameraDisabled(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            return try {
                dpm.getCameraDisabled(component)
            } catch (_: Exception) { false }
        }

        fun setMasterMute(context: Context, muted: Boolean): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ Master audio mute requires Android Device Owner permissions.")
            }
            return try {
                dpm.setMasterVolumeMuted(component, muted)
                val status = if (muted) "MUTED (All audio output silenced)" else "UNMUTED (Audio output restored)"
                Pair(true, "🔇 Hardware Audio Master Mute: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setMasterVolumeMuted: ${e.message}", e)
                Pair(false, "❌ Error setting audio mute: ${e.message}")
            }
        }

        fun isMasterMute(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return false
            return try {
                dpm.isMasterVolumeMuted(component)
            } catch (_: Exception) { false }
        }

        fun setBluetoothDisabled(context: Context, disabled: Boolean): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ Bluetooth lock requires Android Device Owner permissions.")
            }
            return try {
                if (disabled) {
                    dpm.addUserRestriction(component, UserManager.DISALLOW_BLUETOOTH)
                    dpm.addUserRestriction(component, UserManager.DISALLOW_BLUETOOTH_SHARING)
                } else {
                    dpm.clearUserRestriction(component, UserManager.DISALLOW_BLUETOOTH)
                    dpm.clearUserRestriction(component, UserManager.DISALLOW_BLUETOOTH_SHARING)
                }
                val status = if (disabled) "LOCKED (Bluetooth & file sharing disallowed)" else "UNLOCKED (Bluetooth allowed)"
                Pair(true, "📡 Hardware Bluetooth state: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setBluetoothDisabled: ${e.message}", e)
                Pair(false, "❌ Error setting Bluetooth restriction: ${e.message}")
            }
        }

        fun isBluetoothDisabled(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return false
            return try {
                val bundle = dpm.getUserRestrictions(component)
                bundle.getBoolean(UserManager.DISALLOW_BLUETOOTH, false)
            } catch (_: Exception) { false }
        }

        fun setLockScreenInfo(context: Context, info: CharSequence?): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ Setting lockscreen info banner requires Android Device Owner permissions.")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return Pair(false, "❌ Lockscreen banner requires Android 7.0+ (API 24+).")
            }
            return try {
                dpm.setDeviceOwnerLockScreenInfo(component, info)
                if (info.isNullOrBlank()) {
                    Pair(true, "📱 Lockscreen info banner cleared.")
                } else {
                    Pair(true, "📱 Lockscreen info banner updated:\n\"$info\"")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setDeviceOwnerLockScreenInfo: ${e.message}", e)
                Pair(false, "❌ Error setting lockscreen info: ${e.message}")
            }
        }

        fun getLockScreenInfo(context: Context): CharSequence? {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return null
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
            return try {
                dpm.getDeviceOwnerLockScreenInfo()
            } catch (_: Exception) { null }
        }

        fun setMaximumTimeToLock(context: Context, timeoutMs: Long): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isAdminActive(component)) {
                return Pair(false, "❌ Device Admin privileges required to manage screen timeout policy.")
            }
            return try {
                dpm.setMaximumTimeToLock(component, timeoutMs)
                if (timeoutMs == 0L) {
                    Pair(true, "⏱️ Inactivity lock policy reset to system default.")
                } else {
                    val sec = timeoutMs / 1000L
                    Pair(true, "⏱️ Maximum screen inactivity timeout set to: ${sec}s (${sec / 60}m).")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setMaximumTimeToLock: ${e.message}", e)
                Pair(false, "❌ Error setting autolock policy: ${e.message}")
            }
        }

        fun getMaximumTimeToLock(context: Context): Long {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            return try {
                dpm.getMaximumTimeToLock(component)
            } catch (_: Exception) { 0L }
        }

        fun retrieveSecurityLogsList(context: Context, full: Boolean = false): Pair<Boolean, List<String>> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, listOf("❌ Security Log inspection requires Android Device Owner permissions."))
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return Pair(false, listOf("❌ Security Log retrieval requires Android 7.0+ (API 24+)."))
            }
            return try {
                val events = dpm.retrieveSecurityLogs(component)
                if (events.isNullOrEmpty()) {
                    Pair(true, emptyList())
                } else {
                    val targetEvents = if (full) events.takeLast(5000) else events.takeLast(30)
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)
                    val formatted = targetEvents.map { event ->
                        val tagStr = when (event.tag) {
                            SecurityLog.TAG_ADB_SHELL_INTERACTIVE -> "ADB_SHELL_INTERACTIVE"
                            SecurityLog.TAG_ADB_SHELL_CMD -> "ADB_SHELL_CMD"
                            SecurityLog.TAG_MEDIA_MOUNT -> "MEDIA_MOUNT"
                            SecurityLog.TAG_MEDIA_UNMOUNT -> "MEDIA_UNMOUNT"
                            SecurityLog.TAG_KEY_DESTRUCTION -> "KEYSTORE_KEY_DESTRUCTION"
                            SecurityLog.TAG_KEY_GENERATED -> "KEYSTORE_KEY_GENERATED"
                            SecurityLog.TAG_KEY_IMPORT -> "KEYSTORE_KEY_IMPORT"
                            SecurityLog.TAG_LOGGING_STARTED -> "SECURITY_LOGGING_STARTED"
                            SecurityLog.TAG_LOGGING_STOPPED -> "SECURITY_LOGGING_STOPPED"
                            SecurityLog.TAG_APP_PROCESS_START -> "APP_PROCESS_START"
                            SecurityLog.TAG_KEYGUARD_DISMISS_AUTH_ATTEMPT -> "KEYGUARD_AUTH_ATTEMPT"
                            SecurityLog.TAG_KEYGUARD_DISMISSED -> "KEYGUARD_DISMISSED"
                            SecurityLog.TAG_KEYGUARD_SECURED -> "KEYGUARD_SECURED"
                            else -> "TAG_${event.tag}"
                        }
                        val time = sdf.format(java.util.Date(event.timeNanos / 1_000_000L))
                        val dataStr = when (val d = event.data) {
                            is Array<*> -> d.joinToString(", ")
                            is ByteArray -> d.joinToString("") { "%02x".format(it) }
                            null -> "N/A"
                            else -> d.toString()
                        }
                        "[$time] $tagStr: $dataStr"
                    }
                    Pair(true, formatted)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieveSecurityLogs: ${e.message}", e)
                Pair(false, listOf("❌ Error retrieving security logs: ${e.message}"))
            }
        }

        fun silentUninstall(context: Context, packageName: String): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ Silent uninstallation requires Android Device Owner permissions.")
            }
            return try {
                val packageInstaller = context.packageManager.packageInstaller
                val intent = Intent("com.izhaanintellect.pasa.UNINSTALL_COMPLETE").setPackage(context.packageName)
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                } else {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(context, 0, intent, flags)
                packageInstaller.uninstall(packageName, pendingIntent.intentSender)
                Pair(true, "🗑️ Silent uninstallation initiated for package: <code>$packageName</code>")
            } catch (e: Exception) {
                Log.e(TAG, "Failed silent uninstall of $packageName: ${e.message}", e)
                Pair(false, "❌ Silent uninstall failed: ${e.message}")
            }
        }

        fun connectWifi(context: Context, ssid: String, pass: String): Pair<Boolean, String> {
            return try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    ?: return Pair(false, "❌ Wi-Fi Service unavailable on device.")

                if (!wifiManager.isWifiEnabled) {
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = true
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val specifier = if (pass.isNotBlank()) {
                        WifiNetworkSpecifier.Builder()
                            .setSsid(ssid)
                            .setWpa2Passphrase(pass)
                            .build()
                    } else {
                        WifiNetworkSpecifier.Builder()
                            .setSsid(ssid)
                            .build()
                    }
                    val request = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(specifier)
                        .build()

                    connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            super.onAvailable(network)
                            Log.i(TAG, "Emergency Wi-Fi network available: $ssid")
                            connectivityManager.bindProcessToNetwork(network)
                        }
                    })
                    Pair(true, "📶 Emergency Wi-Fi connection requested for SSID: <b>$ssid</b> (Android 10+ Specifier).")
                } else {
                    @Suppress("DEPRECATION")
                    val wifiConfig = WifiConfiguration().apply {
                        this.SSID = "\"$ssid\""
                        if (pass.isNotBlank()) {
                            this.preSharedKey = "\"$pass\""
                        } else {
                            this.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        }
                    }
                    @Suppress("DEPRECATION")
                    val netId = wifiManager.addNetwork(wifiConfig)
                    if (netId != -1) {
                        @Suppress("DEPRECATION")
                        wifiManager.disconnect()
                        @Suppress("DEPRECATION")
                        wifiManager.enableNetwork(netId, true)
                        @Suppress("DEPRECATION")
                        wifiManager.reconnect()
                        Pair(true, "📶 Emergency Wi-Fi network provisioned and connected to: <b>$ssid</b>")
                    } else {
                        Pair(false, "❌ Failed to configure Wi-Fi network <b>$ssid</b>.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Wi-Fi $ssid: ${e.message}", e)
                Pair(false, "❌ Wi-Fi connection error: ${e.message}")
            }
        }

        // ── Cyber Defense Suite Device Owner Helpers ──────────────────────────────

        fun setPermittedAccessibilityServices(context: Context, packageList: List<String>?): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ Accessibility Trojan Shield requires Android Device Owner permissions.")
            }
            return try {
                val success = dpm.setPermittedAccessibilityServices(component, packageList)
                if (success) {
                    if (packageList == null) {
                        Pair(true, "🛡️ <b>Accessibility Trojan Shield: UNLOCKED</b>\nAll accessibility services are now permitted.")
                    } else if (packageList.isEmpty()) {
                        Pair(true, "🛡️ <b>Accessibility Trojan Shield: MAXIMUM LOCKDOWN</b>\nAll 3rd-party accessibility services blocked system-wide.")
                    } else {
                        Pair(true, "🛡️ <b>Accessibility Trojan Shield: ARMED</b>\nOnly ${packageList.size} whitelisted packages permitted:\n" +
                                packageList.joinToString("\n") { "• <code>$it</code>" })
                    }
                } else {
                    Pair(false, "❌ OS returned false when setting permitted accessibility services.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setPermittedAccessibilityServices: ${e.message}", e)
                Pair(false, "❌ Error setting permitted accessibility services: ${e.message}")
            }
        }

        fun getPermittedAccessibilityServices(context: Context): List<String>? {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return null
            return try {
                dpm.getPermittedAccessibilityServices(component)
            } catch (_: Exception) { null }
        }

        fun setAppInstallRestrictions(context: Context, mode: String): Pair<Boolean, String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                return Pair(false, "❌ App Installation Lockdown requires Android Device Owner permissions.")
            }
            return try {
                when (mode.lowercase().trim()) {
                    "unknown_only", "sideload" -> {
                        dpm.addUserRestriction(component, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        dpm.clearUserRestriction(component, UserManager.DISALLOW_INSTALL_APPS)
                        Pair(true, "📦 <b>App Installation Lockdown: UNKNOWN SOURCES BLOCKED</b>\n" +
                                "🚫 Sideloading APKs via Chrome, WhatsApp, Files, or downloaders is DISABLED.\n" +
                                "✅ Official store (Google Play) installs remain permitted.")
                    }
                    "block_all", "all", "lock" -> {
                        dpm.addUserRestriction(component, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        dpm.addUserRestriction(component, UserManager.DISALLOW_INSTALL_APPS)
                        Pair(true, "📦 <b>App Installation Lockdown: COMPLETE FREEZE</b>\n" +
                                "🚫 ALL app installations, sideloading, ADB package installs, and store updates are completely BLOCKED.")
                    }
                    "allow", "off", "unlock" -> {
                        dpm.clearUserRestriction(component, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        dpm.clearUserRestriction(component, UserManager.DISALLOW_INSTALL_APPS)
                        Pair(true, "📦 <b>App Installation Lockdown: UNRESTRICTED</b>\n" +
                                "✅ App installations and sideloading restored to standard user policy.")
                    }
                    else -> Pair(false, "❌ Invalid mode: <code>$mode</code>. Use: <code>unknown_only</code>, <code>block_all</code>, or <code>allow</code>.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setAppInstallRestrictions: ${e.message}", e)
                Pair(false, "❌ Error setting app install restrictions: ${e.message}")
            }
        }

        fun suspendAllThirdPartyApps(context: Context, suspend: Boolean): List<String> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) return emptyList()

            return try {
                val pm = context.packageManager
                val allPackages = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                    .map { it.packageName }
                    .filter { it != context.packageName }
                    .toTypedArray()

                val failedToSuspend = dpm.setPackagesSuspended(component, allPackages, suspend)
                allPackages.toList().minus(failedToSuspend.toSet())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to suspend/unsuspend packages: ${e.message}", e)
                emptyList()
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
        val isOwner = isDeviceOwner(context)

        if (isOwner) {
            try {
                selfHealPermissions(context)
                if (prefs.antiTamperEnabled) {
                    applyAntiTamperSuite(context, true)
                }
                enableSecurityLogging(context, true)
            } catch (e: Exception) {
                Log.w(TAG, "Error applying initial Device Owner configuration: ${e.message}")
            }
        }

        if (prefs.isConfigured()) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val msg = if (isOwner) {
                        "👑 <b>PASA Enterprise Device Owner Activated</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "✅ <b>Anti-Tamper Suite:</b> Armed (Safe boot, airplane mode, factory reset locked)\n" +
                                "✅ <b>Permissions:</b> Self-healed & locked as unrevokable\n" +
                                "✅ <b>Security Auditing:</b> Active\n" +
                                "🛡️ <i>Sovereign mobile defense operational!</i>"
                    } else {
                        "🛡️ <b>PASA Device Admin Activated</b>\n\n" +
                                "✅ Privileged device management enabled.\n" +
                                "🔒 Remote lock and wipe features are now active.\n\n" +
                                "💡 <i>To unlock maximum protection (anti-tamper, USB killswitch, self-healing permissions), provision Device Owner via ADB using <code>/device_owner</code>.</i>"
                    }
                    api.sendMessage(
                        token = prefs.botToken,
                        request = SendMessageRequest(
                            chatId = prefs.ownerChatIdLong,
                            text = msg
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

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i(TAG, "👑 PASA Zero-Touch Android Enterprise Provisioning Complete via QR Code!")

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = getComponentName(context)

        try {
            dpm.setProfileEnabled(admin)
        } catch (e: Exception) {
            Log.w(TAG, "setProfileEnabled notice during provisioning: ${e.message}")
        }

        // Apply enterprise self-healing & anti-tamper immediately
        try {
            selfHealPermissions(context)
            applyAntiTamperSuite(context, true)
            enableSecurityLogging(context, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed applying enterprise policies during QR provisioning: ${e.message}")
        }

        val entryPoint = getEntryPoint(context)
        val prefs = entryPoint.preferencesManager()

        // Extract extras bundle from the provisioning QR payload
        try {
            val bundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                    PersistableBundle::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
            }

            bundle?.let { b ->
                val licenseKey = b.getString("license_key")
                val botToken = b.getString("bot_token")
                val ownerChatId = b.getString("owner_chat_id")
                val serverUrl = b.getString("server_url")

                if (!licenseKey.isNullOrBlank()) {
                    prefs.licenseKey = licenseKey.trim().uppercase()
                    Log.i(TAG, "Auto-configured licenseKey from QR provisioning: ${prefs.licenseKey}")
                }
                if (!botToken.isNullOrBlank()) {
                    prefs.botToken = botToken.trim()
                }
                if (!ownerChatId.isNullOrBlank()) {
                    prefs.ownerChatId = ownerChatId.trim()
                }
                if (!serverUrl.isNullOrBlank()) {
                    prefs.serverUrl = serverUrl.trim()
                }

                if (prefs.botToken.isNotBlank() && prefs.ownerChatId.isNotBlank()) {
                    prefs.isSetupComplete = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading provisioning extras bundle: ${e.message}", e)
        }

        // Start PasaService persistent daemon
        try {
            com.izhaanintellect.pasa.service.PasaService.start(context)
        } catch (e: Exception) {
            Log.w(TAG, "Failed starting PasaService during provisioning: ${e.message}")
        }

        // Send Telegram alert if credentials were configured via QR code
        if (prefs.isConfigured()) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val api = entryPoint.telegramApi()
                    val alertText = "👑 <b>PASA ZERO-TOUCH ENROLLMENT COMPLETE</b>\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n" +
                            "✅ <b>Device Owner:</b> Provisioned via Android Enterprise QR Code\n" +
                            "✅ <b>Anti-Tamper Suite:</b> Armed & Enforced\n" +
                            "✅ <b>Self-Healing Permissions:</b> Permanently Locked\n" +
                            "🔑 <b>License Key:</b> <code>${prefs.licenseKey.ifBlank { "Unlicensed" }}</code>\n" +
                            "🛡️ <i>Your sovereign device is armed and operational!</i>"

                    api.sendMessage(
                        token = prefs.botToken,
                        request = SendMessageRequest(
                            chatId = prefs.ownerChatIdLong,
                            text = alertText
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed sending QR provisioning confirmation alert: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onSecurityLogsAvailable(context: Context, intent: Intent) {
        super.onSecurityLogsAvailable(context, intent)
        Log.i(TAG, "Android OS Security Logs available")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = getComponentName(context)
        val logs = try {
            dpm.retrieveSecurityLogs(component)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve security logs: ${e.message}")
            null
        } ?: return

        val entryPoint = getEntryPoint(context)
        val prefs = entryPoint.preferencesManager()
        val api = entryPoint.telegramApi()

        if (!prefs.isConfigured() || logs.isEmpty()) return

        val suspiciousEvents = mutableListOf<String>()
        for (event in logs) {
            when (event.tag) {
                SecurityLog.TAG_ADB_SHELL_INTERACTIVE -> suspiciousEvents.add("⚠️ <b>Interactive ADB Shell Opened</b>")
                SecurityLog.TAG_MEDIA_MOUNT -> suspiciousEvents.add("⚠️ <b>External Storage / Media Mounted</b>")
                SecurityLog.TAG_KEY_DESTRUCTION -> suspiciousEvents.add("⚠️ <b>Android KeyStore Key Destroyed</b>")
            }
        }

        if (suspiciousEvents.isNotEmpty()) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val report = "🚨 <b>Hardware Security Event Detected</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            suspiciousEvents.distinct().joinToString("\n") + "\n\n" +
                            "🕒 <i>Audit Event logged by Android Kernel</i>"
                    api.sendMessage(
                        token = prefs.botToken,
                        request = SendMessageRequest(
                            chatId = prefs.ownerChatIdLong,
                            text = report
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send security log alert", e)
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
