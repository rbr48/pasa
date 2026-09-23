package com.izhaanintellect.pasa.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages PASA configuration and state using EncryptedSharedPreferences.
 * Sensitive data (bot token, password hashes, encryption keys) is stored encrypted
 * using AES-256 via AndroidX Security.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PASA_Prefs"
        private const val PREFS_NAME = "pasa_secure_prefs"

        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_OWNER_CHAT_ID = "owner_chat_id"
        private const val KEY_MASTER_PASSWORD_HASH = "master_password_hash"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_PASSWORD_ITERATIONS = "password_iterations"
        private const val KEY_SMS_TOTP_SECRET = "sms_totp_secret"
        private const val KEY_BACKUP_EMAIL = "backup_email"
        private const val KEY_STEALTH_MODE = "stealth_mode"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_TRACKING_ACTIVE = "tracking_active"
        private const val KEY_TRACKING_INTERVAL = "tracking_interval"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val KEY_PENDING_OTP = "pending_otp"
        private const val KEY_OTP_TIMESTAMP = "otp_timestamp"
        private const val KEY_FAILED_UNLOCK_COUNT = "failed_unlock_count"
        private const val KEY_GEOFENCE_ENABLED = "geofence_enabled"
        private const val KEY_GEOFENCE_CONFIGURED = "geofence_configured"
        private const val KEY_GEOFENCE_LAT = "geofence_lat"
        private const val KEY_GEOFENCE_LNG = "geofence_lng"
        private const val KEY_GEOFENCE_RADIUS = "geofence_radius"
        private const val KEY_GEOFENCE_INSIDE = "geofence_inside"
        private const val KEY_LAST_LATITUDE = "last_latitude"
        private const val KEY_LAST_LONGITUDE = "last_longitude"
        private const val KEY_UPDATE_OFFSET = "update_offset"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USE_BACKEND = "use_backend"
        private const val KEY_ACTIVE_LOCK_PIN = "active_lock_pin"
        private const val KEY_LOST_MODE_ACTIVE = "lost_mode_active"
        private const val KEY_LOST_MODE_MESSAGE = "lost_mode_message"
        private const val KEY_FAKE_SHUTDOWN_ACTIVE = "fake_shutdown_active"
        private const val KEY_DURESS_PIN = "duress_pin"
        private const val KEY_DURESS_ACTIVE = "duress_active"
        private const val KEY_TRAP_ENABLED = "trap_enabled"
        private const val KEY_SNATCH_TRAP = "snatch_trap_enabled"
        private const val KEY_CHARGER_TRAP = "charger_trap_enabled"
        private const val KEY_POCKET_TRAP = "pocket_trap_enabled"
        private const val KEY_LICENSE_KEY = "license_key"
        private const val KEY_LICENSE_TIER = "license_tier"
        private const val KEY_LICENSE_CERT_PAYLOAD = "license_cert_payload"
        private const val KEY_LICENSE_CERT_SIGNATURE = "license_cert_signature"
        private const val KEY_RESET_PASSWORD_TOKEN = "reset_password_token"
        private const val KEY_ANTI_TAMPER_ENABLED = "anti_tamper_enabled"
        private const val KEY_USB_LOCK_ENABLED = "usb_lock_enabled"
        private const val KEY_BIOMETRICS_DISABLED = "biometrics_disabled"
        private const val KEY_FROZEN_PACKAGES = "frozen_packages"
        private const val KEY_NOTIFICATION_SUPPRESSED = "notification_suppressed"
        private const val KEY_APP_FIREWALL_ENABLED = "app_firewall_enabled"
        private const val KEY_APP_FIREWALL_WHITELIST_ONLY = "app_firewall_whitelist_only"
        private const val KEY_APP_FIREWALL_BLACKLIST = "app_firewall_blacklist"
        private const val KEY_BATTERY_ALERT_ENABLED = "battery_alert_enabled"
        private const val KEY_BATTERY_ALERT_THRESHOLD = "battery_alert_threshold"
        private const val KEY_PATTERN_GUARD_ENABLED = "pattern_guard_enabled"
        private const val KEY_PATTERN_GUARD_FAILURE_COUNT = "pattern_guard_failure_count"
        private const val KEY_PATTERN_GUARD_THRESHOLD = "pattern_guard_threshold"
        private const val KEY_PATTERN_GUARD_ACTION = "pattern_guard_action"
        private const val KEY_SIM_LOCK_ENABLED = "sim_lock_enabled"
        private const val KEY_SIM_LOCK_ALERT_ACTION = "sim_lock_alert_action"
        private const val KEY_SIM_LOCK_WHITELIST = "sim_lock_whitelist"
        private const val KEY_EMERGENCY_PHONE = "emergency_alert_phone"
        private const val KEY_TAMPER_DETECTION_ENABLED = "tamper_detection_enabled"
        private const val KEY_TAMPER_DETECTION_THREATS_FOUND = "tamper_detection_threats_found"
        private const val KEY_HARDENING_VERSION = "hardening_version"
        private const val KEY_DEAD_DROP_ENABLED = "dead_drop_enabled"
        private const val KEY_BOOT_HARDENED_LOCKED = "boot_hardened_locked"
        private const val KEY_LAST_SHUTDOWN_TIME = "last_shutdown_time"
        private const val KEY_LAST_BOOT_TIME = "last_boot_time"
        private const val KEY_LAST_RECOVERY_BOOT_DETECTION = "last_recovery_boot_detection"
        private const val KEY_RECOVERY_BOOT_ATTEMPTS = "recovery_boot_attempts"
        private const val KEY_DURESS_FAILURE_COUNT = "duress_failure_count"
        private const val KEY_DURESS_LAST_FAILURE_TIME_MS = "duress_last_failure_time_ms"
        private const val KEY_CAMERA_LOCKED = "camera_locked"
        private const val KEY_BLUETOOTH_LOCKED = "bluetooth_locked"
        private const val KEY_MIC_MUTED = "mic_muted"
        private const val KEY_LOCKSCREEN_INFO = "lockscreen_info"
        const val DEFAULT_SERVER_URL = "https://izhaanintellect.fun/pasa/"
    }

    var isEncryptionAvailable = true
        private set

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            isEncryptionAvailable = false
            Log.e(TAG, "CRITICAL: Failed to create EncryptedSharedPreferences! Falling back to unencrypted SharedPreferences.", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // --- Bot Configuration ---

    var botToken: String
        get() = prefs.getString(KEY_BOT_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BOT_TOKEN, value).apply()

    var ownerChatId: String
        get() = prefs.getString(KEY_OWNER_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OWNER_CHAT_ID, value).apply()

    val ownerChatIdLong: Long
        get() = ownerChatId.toLongOrNull() ?: 0L

    // --- Security & Crypto ---

    var masterPasswordHash: String
        get() = prefs.getString(KEY_MASTER_PASSWORD_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MASTER_PASSWORD_HASH, value).apply()

    var passwordSalt: String
        get() = prefs.getString(KEY_PASSWORD_SALT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASSWORD_SALT, value).apply()

    // PBKDF2 iteration count used for the currently stored password hash.
    // Defaults to the legacy value so pre-existing hashes remain verifiable,
    // then is upgraded transparently on the next successful login.
    var passwordIterations: Int
        get() = prefs.getInt(KEY_PASSWORD_ITERATIONS, 10000)
        set(value) = prefs.edit().putInt(KEY_PASSWORD_ITERATIONS, value).apply()

    // Base32 TOTP secret used to authenticate offline SMS commands without
    // ever sending the master password over SMS. Empty until enrolled.
    var smsTotpSecret: String
        get() = prefs.getString(KEY_SMS_TOTP_SECRET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SMS_TOTP_SECRET, value).apply()

    var dbPassphrase: String
        get() = prefs.getString(KEY_DB_PASSPHRASE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DB_PASSPHRASE, value).apply()

    var backupEmail: String
        get() = prefs.getString(KEY_BACKUP_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BACKUP_EMAIL, value).apply()

    // --- App State ---

    var isStealthMode: Boolean
        get() = prefs.getBoolean(KEY_STEALTH_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_STEALTH_MODE, value).apply()

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    // --- Tracking ---

    var isTrackingActive: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_TRACKING_ACTIVE, value).apply()

    var trackingIntervalMinutes: Int
        get() = prefs.getInt(KEY_TRACKING_INTERVAL, 5)
        set(value) = prefs.edit().putInt(KEY_TRACKING_INTERVAL, value).apply()

    var lastKnownLatitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LAST_LATITUDE, 0L))
        set(value) = prefs.edit().putLong(KEY_LAST_LATITUDE, java.lang.Double.doubleToRawLongBits(value)).apply()

    var lastKnownLongitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LAST_LONGITUDE, 0L))
        set(value) = prefs.edit().putLong(KEY_LAST_LONGITUDE, java.lang.Double.doubleToRawLongBits(value)).apply()

    // --- Geofencing (safe-zone breach alerts) ---

    var geofenceEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEOFENCE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GEOFENCE_ENABLED, value).apply()

    var geofenceConfigured: Boolean
        get() = prefs.getBoolean(KEY_GEOFENCE_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_GEOFENCE_CONFIGURED, value).apply()

    var geofenceCenterLat: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_GEOFENCE_LAT, 0L))
        set(value) = prefs.edit().putLong(KEY_GEOFENCE_LAT, java.lang.Double.doubleToRawLongBits(value)).apply()

    var geofenceCenterLng: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_GEOFENCE_LNG, 0L))
        set(value) = prefs.edit().putLong(KEY_GEOFENCE_LNG, java.lang.Double.doubleToRawLongBits(value)).apply()

    var geofenceRadiusMeters: Int
        get() = prefs.getInt(KEY_GEOFENCE_RADIUS, 200)
        set(value) = prefs.edit().putInt(KEY_GEOFENCE_RADIUS, value).apply()

    // -1 = unknown, 0 = outside, 1 = inside. Used to detect inside->outside transitions.
    var geofenceInsideState: Int
        get() = prefs.getInt(KEY_GEOFENCE_INSIDE, -1)
        set(value) = prefs.edit().putInt(KEY_GEOFENCE_INSIDE, value).apply()

    // --- OTP for destructive operations ---

    var pendingOTP: String?
        get() = prefs.getString(KEY_PENDING_OTP, null)
        set(value) = prefs.edit().putString(KEY_PENDING_OTP, value).apply()

    var otpTimestamp: Long
        get() = prefs.getLong(KEY_OTP_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_OTP_TIMESTAMP, value).apply()

    // --- Lost Mode & Deception ---

    var activeLockPin: String?
        get() = prefs.getString(KEY_ACTIVE_LOCK_PIN, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_LOCK_PIN, value).apply()

    var isLostModeActive: Boolean
        get() = prefs.getBoolean(KEY_LOST_MODE_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_LOST_MODE_ACTIVE, value).apply()

    var lostModeMessage: String
        get() = prefs.getString(KEY_LOST_MODE_MESSAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LOST_MODE_MESSAGE, value).apply()

    var isFakeShutdownActive: Boolean
        get() = prefs.getBoolean(KEY_FAKE_SHUTDOWN_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_FAKE_SHUTDOWN_ACTIVE, value).apply()

    var duressPin: String?
        get() = prefs.getString(KEY_DURESS_PIN, null)
        set(value) = prefs.edit().putString(KEY_DURESS_PIN, value).apply()

    var resetPasswordToken: String?
        get() = prefs.getString(KEY_RESET_PASSWORD_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_RESET_PASSWORD_TOKEN, value).apply()

    var isDuressActive: Boolean
        get() = prefs.getBoolean(KEY_DURESS_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_DURESS_ACTIVE, value).apply()

    var isTrapEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRAP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TRAP_ENABLED, value).apply()

    var isSnatchTrapEnabled: Boolean
        get() = prefs.getBoolean(KEY_SNATCH_TRAP, true)
        set(value) = prefs.edit().putBoolean(KEY_SNATCH_TRAP, value).apply()

    var isChargerTrapEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHARGER_TRAP, true)
        set(value) = prefs.edit().putBoolean(KEY_CHARGER_TRAP, value).apply()

    var isPocketTrapEnabled: Boolean
        get() = prefs.getBoolean(KEY_POCKET_TRAP, false)
        set(value) = prefs.edit().putBoolean(KEY_POCKET_TRAP, value).apply()

    // --- Detection Counters ---

    var knownSimId: String?
        get() = prefs.getString("known_sim_id", null)
        set(value) = prefs.edit().putString("known_sim_id", value).apply()

    var failedUnlockCount: Int
        get() = prefs.getInt(KEY_FAILED_UNLOCK_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_UNLOCK_COUNT, value).apply()

    var updateOffset: Long
        get() = prefs.getLong(KEY_UPDATE_OFFSET, 0L)
        set(value) = prefs.edit().putLong(KEY_UPDATE_OFFSET, value).apply()

    // --- VPS Backend Settings ---

    var serverUrl: String
        get() {
            val url = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
            return if (url.endsWith("/")) url else "$url/"
        }
        set(value) {
            val normalized = if (value.trim().endsWith("/")) value.trim() else "${value.trim()}/"
            prefs.edit().putString(KEY_SERVER_URL, normalized).apply()
        }

    var deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id.isNullOrBlank()) {
                id = "pasa_" + java.util.UUID.randomUUID().toString().substring(0, 8)
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var useBackendServer: Boolean
        get() = prefs.getBoolean(KEY_USE_BACKEND, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_BACKEND, value).apply()
        
    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    // --- Cryptographic Security Settings (ASTRA Hardened Layer) ---

    var replayHighWaterMark: Long
        get() = prefs.getLong("replay_high_water_mark", 0L)
        set(value) = prefs.edit().putLong("replay_high_water_mark", value).apply()

    var replaySeenCommandIds: String
        get() = prefs.getString("replay_seen_command_ids", "") ?: ""
        set(value) = prefs.edit().putString("replay_seen_command_ids", value).apply()

    var replaySeenSequences: String
        get() = prefs.getString("replay_seen_sequences", "") ?: ""
        set(value) = prefs.edit().putString("replay_seen_sequences", value).apply()

    var trustedCommandKeys: Map<String, String>
        get() {
            val raw = prefs.getString("trusted_command_keys", null) ?: return emptyMap()
            val json = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return emptyMap()
            return json.keys().asSequence().associateWith { json.getString(it) }
        }
        set(value) {
            val json = org.json.JSONObject()
            value.forEach { (k, v) -> json.put(k, v) }
            prefs.edit().putString("trusted_command_keys", json.toString()).apply()
        }

    fun addTrustedCommandKey(kid: String, jwk: String) {
        val current = trustedCommandKeys.toMutableMap()
        current[kid] = jwk
        trustedCommandKeys = current
    }

    var deviceKeySecurityLevel: String
        get() = prefs.getString("device_key_security_level", "UNKNOWN") ?: "UNKNOWN"
        set(value) = prefs.edit().putString("device_key_security_level", value).apply()

    var licenseKey: String
        get() = prefs.getString(KEY_LICENSE_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LICENSE_KEY, value).apply()

    var licenseTier: String
        get() = prefs.getString(KEY_LICENSE_TIER, "FREE_TRIAL") ?: "FREE_TRIAL"
        set(value) = prefs.edit().putString(KEY_LICENSE_TIER, value).apply()

    var licenseCertPayload: String
        get() = prefs.getString(KEY_LICENSE_CERT_PAYLOAD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LICENSE_CERT_PAYLOAD, value).apply()

    var licenseCertSignature: String
        get() = prefs.getString(KEY_LICENSE_CERT_SIGNATURE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LICENSE_CERT_SIGNATURE, value).apply()

    var antiTamperEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANTI_TAMPER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ANTI_TAMPER_ENABLED, value).apply()

    var usbLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_USB_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_USB_LOCK_ENABLED, value).apply()

    var biometricsDisabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS_DISABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS_DISABLED, value).apply()

    var frozenPackages: Set<String>
        get() = prefs.getStringSet(KEY_FROZEN_PACKAGES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FROZEN_PACKAGES, value).apply()

    fun addFrozenPackage(pkg: String) {
        val set = frozenPackages.toMutableSet()
        set.add(pkg)
        frozenPackages = set
    }

    fun removeFrozenPackage(pkg: String) {
        val set = frozenPackages.toMutableSet()
        set.remove(pkg)
        frozenPackages = set
    }

    var isNotificationSuppressed: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_SUPPRESSED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_SUPPRESSED, value).apply()

    var isAppFirewallEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_FIREWALL_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_FIREWALL_ENABLED, value).apply()

    var isAppFirewallWhitelistOnly: Boolean
        get() = prefs.getBoolean(KEY_APP_FIREWALL_WHITELIST_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_FIREWALL_WHITELIST_ONLY, value).apply()

    var appFirewallBlacklist: List<String>
        get() = prefs.getStringSet(KEY_APP_FIREWALL_BLACKLIST, emptySet())?.toList() ?: emptyList()
        set(value) = prefs.edit().putStringSet(KEY_APP_FIREWALL_BLACKLIST, value.toSet()).apply()

    var isBatteryAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_ALERT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_ALERT_ENABLED, value).apply()

    var batteryAlertThreshold: Int
        get() = prefs.getInt(KEY_BATTERY_ALERT_THRESHOLD, 15)
        set(value) = prefs.edit().putInt(KEY_BATTERY_ALERT_THRESHOLD, value).apply()

    var isPatternGuardEnabled: Boolean
        get() = prefs.getBoolean(KEY_PATTERN_GUARD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PATTERN_GUARD_ENABLED, value).apply()

    var patternGuardFailureCount: Int
        get() = prefs.getInt(KEY_PATTERN_GUARD_FAILURE_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_PATTERN_GUARD_FAILURE_COUNT, value).apply()

    var patternGuardThreshold: Int
        get() = prefs.getInt(KEY_PATTERN_GUARD_THRESHOLD, 3)
        set(value) = prefs.edit().putInt(KEY_PATTERN_GUARD_THRESHOLD, value).apply()

    var patternGuardAction: String
        get() = prefs.getString(KEY_PATTERN_GUARD_ACTION, "photo") ?: "photo"
        set(value) = prefs.edit().putString(KEY_PATTERN_GUARD_ACTION, value).apply()

    var isSimLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_SIM_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SIM_LOCK_ENABLED, value).apply()

    var simLockAlertAction: String
        get() = prefs.getString(KEY_SIM_LOCK_ALERT_ACTION, "alert") ?: "alert"
        set(value) = prefs.edit().putString(KEY_SIM_LOCK_ALERT_ACTION, value).apply()

    var simLockWhitelist: List<String>
        get() = prefs.getStringSet(KEY_SIM_LOCK_WHITELIST, emptySet())?.toList() ?: emptyList()
        set(value) = prefs.edit().putStringSet(KEY_SIM_LOCK_WHITELIST, value.toSet()).apply()

    var emergencyPhone: String
        get() = prefs.getString(KEY_EMERGENCY_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMERGENCY_PHONE, value).apply()

    var isTamperDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_TAMPER_DETECTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TAMPER_DETECTION_ENABLED, value).apply()

    var tamperDetectionThreatsFound: Int
        get() = prefs.getInt(KEY_TAMPER_DETECTION_THREATS_FOUND, 0)
        set(value) = prefs.edit().putInt(KEY_TAMPER_DETECTION_THREATS_FOUND, value).apply()

    var hardeningVersion: Int
        get() = prefs.getInt(KEY_HARDENING_VERSION, 0)
        set(value) = prefs.edit().putInt(KEY_HARDENING_VERSION, value).apply()

    var isDeadDropEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEAD_DROP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DEAD_DROP_ENABLED, value).apply()

    var isBootHardenedLocked: Boolean
        get() = prefs.getBoolean(KEY_BOOT_HARDENED_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_BOOT_HARDENED_LOCKED, value).apply()

    var lastShutdownTime: Long
        get() = prefs.getLong(KEY_LAST_SHUTDOWN_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SHUTDOWN_TIME, value).apply()

    var lastBootTime: Long
        get() = prefs.getLong(KEY_LAST_BOOT_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BOOT_TIME, value).apply()

    var lastRecoveryBootDetection: Long
        get() = prefs.getLong(KEY_LAST_RECOVERY_BOOT_DETECTION, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_RECOVERY_BOOT_DETECTION, value).apply()

    var recoveryBootAttempts: Int
        get() = prefs.getInt(KEY_RECOVERY_BOOT_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_RECOVERY_BOOT_ATTEMPTS, value).apply()

    var duressFailureCount: Int
        get() = prefs.getInt(KEY_DURESS_FAILURE_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_DURESS_FAILURE_COUNT, value).apply()

    var duressLastFailureTimeMs: Long
        get() = prefs.getLong(KEY_DURESS_LAST_FAILURE_TIME_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_DURESS_LAST_FAILURE_TIME_MS, value).apply()

    var isCameraLocked: Boolean
        get() = prefs.getBoolean(KEY_CAMERA_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_CAMERA_LOCKED, value).apply()

    var isBluetoothLocked: Boolean
        get() = prefs.getBoolean(KEY_BLUETOOTH_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_BLUETOOTH_LOCKED, value).apply()

    var isMicMuted: Boolean
        get() = prefs.getBoolean(KEY_MIC_MUTED, false)
        set(value) = prefs.edit().putBoolean(KEY_MIC_MUTED, value).apply()

    var lockScreenInfo: String
        get() = prefs.getString(KEY_LOCKSCREEN_INFO, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LOCKSCREEN_INFO, value).apply()

    /** Returns true if the minimum configuration required to run is present. */
    fun isConfigured(): Boolean {
        return botToken.isNotBlank() && ownerChatId.isNotBlank() && isSetupComplete
    }
}
