package com.izhaanintellect.pasa.security

import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces license-tier feature gating.
 * Pro-only commands are blocked when the device is on an expired free trial.
 * Periodically re-validates the license against the VPS backend.
 */
@Singleton
class LicenseManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val pasaBackendApi: PasaBackendApi
) {
    companion object {
        private const val TAG = "PASA_License"
        private const val RECHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val KEY_LAST_LICENSE_CHECK = "last_license_check_ms"

        /** Commands that require an active Pro license. */
        private val PRO_COMMANDS = setOf(
            "/video", "/videocap", "/vr",
            "/record", "/audio", "/mic",
            "/shred", "/wipe_folder",
            "/fakeshutdown", "/blackout", "/fake_off",
            "/duress_pin", "/duress", "/coercion",
            "/trap", "/traps", "/alarm_trap",
            "/geofence", "/fence", "/safezone"
        )
    }

    /**
     * Returns true if the given command requires a Pro license.
     */
    fun isProCommand(command: String): Boolean = command.lowercase() in PRO_COMMANDS

    /**
     * Returns true if the device has an active Pro license or an active trial.
     */
    fun isProActive(): Boolean {
        val tier = preferencesManager.licenseTier
        return when {
            tier == "PRO_LIFETIME" -> true
            tier == "PRO_30" -> true // Server controls expiry via checkLicense
            tier == "FREE_TRIAL" -> {
                // Trial is considered active until the server says otherwise
                // Default permissive: if we haven't checked yet, allow
                true
            }
            else -> false
        }
    }

    /**
     * Checks if the command should be blocked due to licensing.
     * Returns null if allowed, or a user-facing rejection message if blocked.
     */
    fun checkAccess(command: String): String? {
        if (!isProCommand(command)) return null
        if (isProActive()) return null

        return "🔒 <b>Pro Feature Locked</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "The command <code>$command</code> requires a PASA Pro license.\n\n" +
                "Your current tier: <b>${preferencesManager.licenseTier}</b>\n\n" +
                "💎 Upgrade to Pro to unlock:\n" +
                "• Video & audio recording\n" +
                "• Geofencing & trap system\n" +
                "• Fake shutdown deception\n" +
                "• Duress PIN & secure shred\n\n" +
                "Visit <b>pasa.izhaanintellect.fun</b> to upgrade."
    }

    /**
     * Refreshes the license status from the VPS backend if the last check
     * was more than 24 hours ago.
     */
    suspend fun refreshIfStale() {
        try {
            val lastCheck = preferencesManager.run {
                // Use a simple SharedPreferences timestamp
                val prefs = javaClass.getDeclaredField("prefs").apply { isAccessible = true }.get(this) as android.content.SharedPreferences
                prefs.getLong(KEY_LAST_LICENSE_CHECK, 0L)
            }
            val now = System.currentTimeMillis()
            if (now - lastCheck < RECHECK_INTERVAL_MS) return

            val response = pasaBackendApi.checkLicense(preferencesManager.deviceId)
            if (response.ok) {
                preferencesManager.licenseTier = response.tier
                if (!response.licenseKey.isNullOrBlank()) {
                    preferencesManager.licenseKey = response.licenseKey
                }
                Log.i(TAG, "License refreshed: tier=${response.tier}, daysLeft=${response.daysLeft}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "License refresh failed: ${e.message}")
        }
    }
}
