package com.izhaanintellect.pasa.security

import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces license-tier feature gating.
 *
 * All Pro surveillance and containment commands (including /screenrecord)
 * are accessible during the 7-day FREE_TRIAL and all paid tiers
 * (PRO_30, PRO_LIFETIME, PRO_ENTERPRISE, ENTERPRISE).
 */
@Singleton
class LicenseManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val pasaBackendApi: PasaBackendApi
) {
    companion object {
        private const val TAG = "PASA_License"
        private const val RECHECK_INTERVAL_MS = 60 * 60 * 1000L // 1 hour
        private const val KEY_LAST_LICENSE_CHECK = "last_license_check_ms"

        /**
         * Commands that require an active Pro license or active FREE_TRIAL.
         * /screenrecord is included here so trial and pro users have full evaluation access.
         */
        private val PRO_COMMANDS = setOf(
            "/video", "/videocap", "/vr",
            "/record", "/audio", "/mic",
            "/shred", "/wipe_folder",
            "/fakeshutdown", "/blackout", "/fake_off",
            "/duress_pin", "/duress", "/coercion",
            "/trap", "/traps", "/alarm_trap",
            "/geofence", "/fence", "/safezone",
            "/screenshot", "/screen",
            "/screen_burst", "/burst",
            "/screenrecord", "/record_screen"
        )

        /**
         * Reserved for future Enterprise-only MDM/fleet management capabilities.
         */
        private val ENTERPRISE_COMMANDS = emptySet<String>()

        /** All recognized paid tiers */
        private val PAID_TIERS = setOf(
            "PRO_30", "PRO_LIFETIME", "PRO_ENTERPRISE", "PRO_ANNUAL", "ENTERPRISE", "ENTERPRISE_LIFETIME"
        )
    }

    fun isProCommand(command: String): Boolean = command.lowercase() in PRO_COMMANDS

    /**
     * Returns true if the device has an active Pro license or an active FREE_TRIAL.
     */
    fun isProActive(): Boolean {
        val tier = preferencesManager.licenseTier.uppercase()
        return when {
            tier in PAID_TIERS -> true
            tier == "FREE_TRIAL" -> true // Trial gets access to all pro features including screen recording
            tier.startsWith("PRO") || tier.startsWith("ENTERPRISE") -> true
            else -> {
                Log.d(TAG, "No active pro license (tier=$tier)")
                false
            }
        }
    }

    /**
     * Returns true if the device has a paid license.
     */
    fun isPaidLicense(): Boolean {
        val tier = preferencesManager.licenseTier.uppercase()
        return tier in PAID_TIERS || tier.startsWith("PRO_") || tier == "ENTERPRISE"
    }

    /**
     * Checks if the command should be blocked due to licensing.
     * Returns null if allowed, or a user-facing rejection message if blocked.
     */
    fun checkAccess(command: String): String? {
        val cmd = command.lowercase()
        val tier = preferencesManager.licenseTier

        // ── Enterprise-exclusive commands ────────────────────────────────────
        if (cmd in ENTERPRISE_COMMANDS) {
            val upperTier = tier.uppercase()
            if (upperTier in setOf("ENTERPRISE", "ENTERPRISE_LIFETIME", "PRO_ENTERPRISE")) return null
            return buildRejectionMessage(
                command = command,
                tier = tier,
                requiredTier = "Enterprise",
                features = listOf("Enterprise fleet management")
            )
        }

        // ── Pro & Trial commands (screenrecord, video, screenshot, etc.) ─────
        if (isProCommand(command)) {
            if (isProActive()) return null

            return buildRejectionMessage(
                command = command,
                tier = tier,
                requiredTier = "Pro",
                features = listOf(
                    "Screen recording & screenshot capture",
                    "Covert video & ambient audio recording",
                    "Geofencing & automated trap system",
                    "Fake shutdown deception & blackout mode",
                    "Duress PIN distress & cryptographic shredding"
                )
            )
        }

        // ── Standard commands — always allowed ─────────────────────────────────
        return null
    }

    private fun buildRejectionMessage(
        command: String,
        tier: String,
        requiredTier: String,
        features: List<String>
    ): String {
        val featureList = features.joinToString("\n") { "• $it" }
        return "🔒 <b>$requiredTier Feature Locked</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "The command <code>$command</code> requires PASA $requiredTier.\n\n" +
                "Your current tier: <b>$tier</b>\n\n" +
                "💎 <b>Upgrade to $requiredTier to unlock:</b>\n" +
                featureList + "\n\n" +
                "Visit <b>pasa.izhaanintellect.fun</b> to upgrade."
    }

    /**
     * Refreshes the license status from the VPS backend if the last check
     * was more than 1 hour ago or if explicitly forced.
     */
    suspend fun refreshIfStale(force: Boolean = false) {
        try {
            val lastCheck = preferencesManager.run {
                val prefs = javaClass.getDeclaredField("prefs").apply { isAccessible = true }.get(this) as android.content.SharedPreferences
                prefs.getLong(KEY_LAST_LICENSE_CHECK, 0L)
            }
            val now = System.currentTimeMillis()
            if (!force && (now - lastCheck < RECHECK_INTERVAL_MS) && preferencesManager.licenseTier != "FREE_TRIAL") {
                return
            }

            val response = pasaBackendApi.checkLicense(preferencesManager.deviceId)
            if (response.ok) {
                if (!response.tier.isNullOrBlank()) {
                    preferencesManager.licenseTier = response.tier
                }
                if (!response.licenseKey.isNullOrBlank()) {
                    preferencesManager.licenseKey = response.licenseKey
                }
                preferencesManager.run {
                    val prefs = javaClass.getDeclaredField("prefs").apply { isAccessible = true }.get(this) as android.content.SharedPreferences
                    prefs.edit().putLong(KEY_LAST_LICENSE_CHECK, now).apply()
                }
                Log.i(TAG, "License refreshed: tier=${response.tier}, daysLeft=${response.daysLeft}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "License refresh failed: ${e.message}")
        }
    }
}
