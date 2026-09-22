package com.izhaanintellect.pasa.admin

import android.Manifest
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.app.admin.SecurityLog
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
                UserManager.DISALLOW_CONFIG_LOCATION to "Location Toggle Tamper Lock"
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
                Manifest.permission.READ_CONTACTS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            val results = mutableMapOf<String, Boolean>()
            for (perm in perms) {
                try {
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
            return results
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
