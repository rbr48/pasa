package com.izhaanintellect.pasa.security

import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces license-tier feature gating.
 *
 * Tier hierarchy (lowest → highest):
 *   FREE_TRIAL  → 7-day trial, basic commands only
 *   PRO_30      → Paid monthly, unlocks Pro + Pro-Paid features
 *   PRO_LIFETIME→ Paid lifetime, same as PRO_30
 *   ENTERPRISE  → Highest tier, all features
 *
 * Feature sets:
 *   FREE_COMMANDS     → Always allowed (lock, locate, ring, status, etc.)
 *   PRO_COMMANDS      → Requires active Pro OR FREE_TRIAL (trial gets access)
 *   PRO_PAID_COMMANDS → Requires paid Pro (PRO_30/PRO_LIFETIME/ENTERPRISE) — NOT FREE_TRIAL
 *   ENTERPRISE_COMMANDS → Reserved for future Enterprise-exclusive features
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

        /**
         * Commands that require an active Pro license OR are allowed during FREE_TRIAL.
         * Trial users get access to evaluate these features.
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
            "/screen_burst", "/burst"
        )

        /**
         * Commands that require a PAID Pro license (PRO_30, PRO_LIFETIME, or ENTERPRISE).
         * FREE_TRIAL does NOT get access to these — they require an actual purchase.
         * Screen recording is in this tier: it's a core Pro paid feature, not trial-gated.
         */
        private val PRO_PAID_COMMANDS = setOf(
            "/screenrecord", "/record_screen"
        )

        /**
         * Commands reserved exclusively for Enterprise tier.
         * Currently empty — extend when Enterprise-exclusive features are added.
         */
        private val ENTERPRISE_COMMANDS = emptySet<String>()

        /** All paid tiers (excludes FREE_TRIAL) */
        private val PAID_TIERS = setOf(
            "PRO_30", "PRO_LIFETIME", "ENTERPRISE", "ENTERPRISE_LIFETIME"
        )
    }

    fun isProCommand(command: String): Boolean = command.lowercase() in PRO_COMMANDS
    fun isProPaidCommand(command: String): Boolean = command.lowercase() in PRO_PAID_COMMANDS

    /**
     * Returns true if the device has an active Pro license (including FREE_TRIAL).
     */
    fun isProActive(): Boolean {
        return when (val tier = preferencesManager.licenseTier) {
            "PRO_LIFETIME", "PRO_30", "ENTERPRISE", "ENTERPRISE_LIFETIME" -> true
            "FREE_TRIAL" -> true  // Trial gets access to PRO_COMMANDS (not PRO_PAID_COMMANDS)
            else -> {
                Log.d(TAG, "No active pro license (tier=$tier)")
                false
            }
        }
    }

    /**
     * Returns true if the device has a PAID license (not just a free trial).
     * Used to gate PRO_PAID_COMMANDS.
     */
    fun isPaidLicense(): Boolean {
        return preferencesManager.licenseTier in PAID_TIERS
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
            if (tier == "ENTERPRISE" || tier == "ENTERPRISE_LIFETIME") return null
            return buildRejectionMessage(
                command = command,
                tier = tier,
                requiredTier = "Enterprise",
                features = listOf("Enterprise-exclusive capabilities")
            )
        }

        // ── Pro-Paid commands (paid license required, FREE_TRIAL blocked) ────
        if (cmd in PRO_PAID_COMMANDS) {
            if (isPaidLicense()) return null
            return if (tier == "FREE_TRIAL") {
                "🔒 <b>Paid License Required</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "The command <code>$command</code> requires a paid PASA Pro license.\n\n" +
                "Your current tier: <b>FREE_TRIAL</b> (7-day evaluation)\n\n" +
                "Screen recording is available on <b>Pro</b> and above.\n\n" +
                "💎 <b>Upgrade to Pro to unlock:</b>\n" +
                "• Screen recording\n" +
                "• All surveillance features\n" +
                "• Priority support\n\n" +
                "Visit <b>pasa.izhaanintellect.fun</b> to upgrade."
            } else {
                buildRejectionMessage(
                    command = command,
                    tier = tier,
                    requiredTier = "Pro",
                    features = listOf("Screen recording", "All Pro features")
                )
            }
        }

        // ── Pro commands (FREE_TRIAL allowed) ────────────────────────────────
        if (cmd in PRO_COMMANDS) {
            if (isProActive()) return null
            return buildRejectionMessage(
                command = command,
                tier = tier,
                requiredTier = "Pro",
                features = listOf(
                    "Video & audio recording",
                    "Screenshot & screen burst",
                    "Geofencing & trap system",
                    "Fake shutdown deception",
                    "Duress PIN & secure shred"
                )
            )
        }

        // ── Free commands — always allowed ────────────────────────────────────
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
     * was more than 24 hours ago.
     */
    suspend fun refreshIfStale() {
        try {
            val lastCheck = preferencesManager.run {
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
