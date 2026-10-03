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
        private const val KEY_STATUS_BAR_DISABLED = "status_bar_disabled"
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
        private const val KEY_DEAD_MAN_ENABLED = "dead_man_switch_enabled"
        private const val KEY_DEAD_MAN_TIMEOUT_HOURS = "dead_man_timeout_hours"
        private const val KEY_LAST_OWNER_HEARTBEAT = "last_owner_heartbeat_time"
        private const val KEY_THERMAL_TRAP_ENABLED = "thermal_trap_enabled"
        private const val KEY_THERMAL_TRAP_THRESHOLD = "thermal_trap_threshold"
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
        private const val KEY_TRIAL_START_TIME = "trial_start_time"
        // SIM Tray Lock — deep lockdown on unauthorized SIM insertion
        private const val KEY_SIM_TRAY_LOCK_ENABLED = "sim_tray_lock_enabled"
        private const val KEY_SIM_TRAY_LOCK_EMERGENCY_PIN = "sim_tray_lock_emergency_pin"

        // Cyber Defense Suite
        private const val KEY_A11Y_SHIELD_ENABLED = "a11y_shield_enabled"
        private const val KEY_A11Y_SHIELD_WHITELIST = "a11y_shield_whitelist"
        private const val KEY_USB_AUTOLOCK_ENABLED = "usb_autolock_enabled"
        private const val KEY_ANTI_2G_ENABLED = "anti_2g_enabled"
        private const val KEY_CLIPPER_GUARD_ENABLED = "clipper_guard_enabled"
        private const val KEY_APP_INSTALL_LOCK_MODE = "app_install_lock_mode"
        private const val KEY_CANARY_GUARD_ARMED = "canary_guard_armed"
        private const val KEY_OTP_GUARD_ENABLED = "otp_guard_enabled"
        private const val KEY_OTP_GUARD_AUTO_NEUTRALIZE = "otp_guard_auto_neutralize"
        private const val KEY_OTP_GUARD_WHITELIST = "otp_guard_whitelist"
        private const val KEY_FAKE_SHUTDOWN_AUTO_POWER_MENU = "fake_shutdown_auto_power_menu"
        private const val KEY_FAKE_SHUTDOWN_AUTO_LOCKED_ONLY = "fake_shutdown_auto_locked_only"
        private const val KEY_FAKE_AIRPLANE_ACTIVE = "fake_airplane_active"
        private const val KEY_FARADAY_TRAP_ENABLED = "faraday_trap_enabled"
        private const val KEY_LAST_BOT_COMMANDS_SYNC = "last_bot_commands_sync"

        // Dormant / pause state
        private const val KEY_PASA_PAUSED = "pasa_paused"
        private const val KEY_PASA_PAUSED_AT = "pasa_paused_at_ms"

        const val DEFAULT_SERVER_URL = ""
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

    private val dePrefs: SharedPreferences by lazy {
        val deContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
        deContext.getSharedPreferences("pasa_de_secure_prefs", Context.MODE_PRIVATE)
    }

    val isUserUnlocked: Boolean
        get() {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val um = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager
                um?.isUserUnlocked ?: true
            } else {
                true
            }
        }

    // --- Bot Configuration ---

    var botToken: String
        get() = try {
            val v = if (isUserUnlocked) prefs.getString(KEY_BOT_TOKEN, "") ?: "" else ""
            if (v.isNotBlank()) v else dePrefs.getString(KEY_BOT_TOKEN, "") ?: ""
        } catch (_: Exception) {
            dePrefs.getString(KEY_BOT_TOKEN, "") ?: ""
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putString(KEY_BOT_TOKEN, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putString(KEY_BOT_TOKEN, value).apply()
        }

    var ownerChatId: String
        get() = try {
            val v = if (isUserUnlocked) prefs.getString(KEY_OWNER_CHAT_ID, "") ?: "" else ""
            if (v.isNotBlank()) v else dePrefs.getString(KEY_OWNER_CHAT_ID, "") ?: ""
        } catch (_: Exception) {
            dePrefs.getString(KEY_OWNER_CHAT_ID, "") ?: ""
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putString(KEY_OWNER_CHAT_ID, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putString(KEY_OWNER_CHAT_ID, value).apply()
        }

    val ownerChatIdLong: Long
        get() = ownerChatId.toLongOrNull() ?: 0L

    // --- Security & Crypto ---

    var masterPasswordHash: String
        get() = try {
            val v = prefs.getString(KEY_MASTER_PASSWORD_HASH, "") ?: ""
            if (v.isNotBlank()) v else dePrefs.getString(KEY_MASTER_PASSWORD_HASH, "") ?: ""
        } catch (_: Exception) {
            dePrefs.getString(KEY_MASTER_PASSWORD_HASH, "") ?: ""
        }
        set(value) {
            prefs.edit().putString(KEY_MASTER_PASSWORD_HASH, value).apply()
            dePrefs.edit().putString(KEY_MASTER_PASSWORD_HASH, value).apply()
        }

    var passwordSalt: String
        get() = try {
            val v = prefs.getString(KEY_PASSWORD_SALT, "") ?: ""
            if (v.isNotBlank()) v else dePrefs.getString(KEY_PASSWORD_SALT, "") ?: ""
        } catch (_: Exception) {
            dePrefs.getString(KEY_PASSWORD_SALT, "") ?: ""
        }
        set(value) {
            prefs.edit().putString(KEY_PASSWORD_SALT, value).apply()
            dePrefs.edit().putString(KEY_PASSWORD_SALT, value).apply()
        }

    // PBKDF2 iteration count used for the currently stored password hash.
    var passwordIterations: Int
        get() = try {
            val v = prefs.getInt(KEY_PASSWORD_ITERATIONS, 0)
            if (v > 0) v else dePrefs.getInt(KEY_PASSWORD_ITERATIONS, 100000)
        } catch (_: Exception) {
            dePrefs.getInt(KEY_PASSWORD_ITERATIONS, 100000)
        }
        set(value) {
            prefs.edit().putInt(KEY_PASSWORD_ITERATIONS, value).apply()
            dePrefs.edit().putInt(KEY_PASSWORD_ITERATIONS, value).apply()
        }

    // Base32 TOTP secret used to authenticate offline SMS commands without
    // ever sending the master password over SMS. Mirrors to Device Protected Storage for Direct Boot.
    var smsTotpSecret: String
        get() = try {
            val v = prefs.getString(KEY_SMS_TOTP_SECRET, "") ?: ""
            if (v.isNotBlank()) v else dePrefs.getString(KEY_SMS_TOTP_SECRET, "") ?: ""
        } catch (_: Exception) {
            dePrefs.getString(KEY_SMS_TOTP_SECRET, "") ?: ""
        }
        set(value) {
            prefs.edit().putString(KEY_SMS_TOTP_SECRET, value).apply()
            dePrefs.edit().putString(KEY_SMS_TOTP_SECRET, value).apply()
        }

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
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_SETUP_COMPLETE, false) else false
            if (v) true else (dePrefs.getBoolean(KEY_SETUP_COMPLETE, false) || dePrefs.getBoolean("setup_complete_dp", false))
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_SETUP_COMPLETE, false) || dePrefs.getBoolean("setup_complete_dp", false)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply() } catch (_: Exception) {}
            dePrefs.edit()
                .putBoolean(KEY_SETUP_COMPLETE, value)
                .putBoolean("setup_complete_dp", value)
                .apply()
        }

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
        get() = try {
            val v = prefs.getString(KEY_ACTIVE_LOCK_PIN, null)
            v ?: dePrefs.getString(KEY_ACTIVE_LOCK_PIN, null)
        } catch (_: Exception) {
            dePrefs.getString(KEY_ACTIVE_LOCK_PIN, null)
        }
        set(value) {
            prefs.edit().putString(KEY_ACTIVE_LOCK_PIN, value).apply()
            dePrefs.edit().putString(KEY_ACTIVE_LOCK_PIN, value).apply()
        }

    var isLostModeActive: Boolean
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_LOST_MODE_ACTIVE, false) else false
            if (v) true else dePrefs.getBoolean(KEY_LOST_MODE_ACTIVE, false)
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_LOST_MODE_ACTIVE, false)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_LOST_MODE_ACTIVE, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putBoolean(KEY_LOST_MODE_ACTIVE, value).apply()
        }

    var lostModeMessage: String
        get() = prefs.getString(KEY_LOST_MODE_MESSAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LOST_MODE_MESSAGE, value).apply()

    var isFakeShutdownActive: Boolean
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_FAKE_SHUTDOWN_ACTIVE, false) else false
            if (v) true else dePrefs.getBoolean(KEY_FAKE_SHUTDOWN_ACTIVE, false)
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_FAKE_SHUTDOWN_ACTIVE, false)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_FAKE_SHUTDOWN_ACTIVE, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putBoolean(KEY_FAKE_SHUTDOWN_ACTIVE, value).apply()
        }

    var isFakeShutdownAutoPowerMenu: Boolean
        get() = prefs.getBoolean(KEY_FAKE_SHUTDOWN_AUTO_POWER_MENU, true)
        set(value) = prefs.edit().putBoolean(KEY_FAKE_SHUTDOWN_AUTO_POWER_MENU, value).apply()

    var isFakeShutdownAutoLockedOnly: Boolean
        get() = prefs.getBoolean(KEY_FAKE_SHUTDOWN_AUTO_LOCKED_ONLY, true)
        set(value) = prefs.edit().putBoolean(KEY_FAKE_SHUTDOWN_AUTO_LOCKED_ONLY, value).apply()

    var isFakeAirplaneActive: Boolean
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_FAKE_AIRPLANE_ACTIVE, false) else false
            if (v) true else dePrefs.getBoolean(KEY_FAKE_AIRPLANE_ACTIVE, false)
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_FAKE_AIRPLANE_ACTIVE, false)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_FAKE_AIRPLANE_ACTIVE, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putBoolean(KEY_FAKE_AIRPLANE_ACTIVE, value).apply()
        }

    var isFaradayTrapEnabled: Boolean
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_FARADAY_TRAP_ENABLED, true) else true
            if (v) true else dePrefs.getBoolean(KEY_FARADAY_TRAP_ENABLED, true)
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_FARADAY_TRAP_ENABLED, true)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_FARADAY_TRAP_ENABLED, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putBoolean(KEY_FARADAY_TRAP_ENABLED, value).apply()
        }

    var duressPin: String?
        get() = prefs.getString(KEY_DURESS_PIN, null)
        set(value) = prefs.edit().putString(KEY_DURESS_PIN, value).apply()

    var resetPasswordToken: String?
        get() = prefs.getString(KEY_RESET_PASSWORD_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_RESET_PASSWORD_TOKEN, value).apply()

    var isEscrowTokenArmed: Boolean
        get() = prefs.getBoolean("escrow_token_armed", false)
        set(value) = prefs.edit().putBoolean("escrow_token_armed", value).apply()

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
        get() = try {
            val v = if (isUserUnlocked) prefs.getLong(KEY_UPDATE_OFFSET, 0L) else 0L
            if (v > 0) v else dePrefs.getLong(KEY_UPDATE_OFFSET, 0L)
        } catch (_: Exception) {
            dePrefs.getLong(KEY_UPDATE_OFFSET, 0L)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putLong(KEY_UPDATE_OFFSET, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putLong(KEY_UPDATE_OFFSET, value).apply()
        }

    // --- VPS Backend Settings ---

    var serverUrl: String
        get() = ""
        set(_) {
            prefs.edit().putString(KEY_SERVER_URL, "").apply()
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
        get() = false
        set(_) {
            prefs.edit().putBoolean(KEY_USE_BACKEND, false).apply()
        }
        
    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var hasAcceptedTerms: Boolean
        get() = prefs.getBoolean("has_accepted_terms", false)
        set(value) = prefs.edit().putBoolean("has_accepted_terms", value).apply()

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

    var trialStartTime: Long
        get() {
            var time = prefs.getLong(KEY_TRIAL_START_TIME, 0L)
            if (time <= 0L) {
                time = System.currentTimeMillis()
                prefs.edit().putLong(KEY_TRIAL_START_TIME, time).apply()
            }
            return time
        }
        set(value) = prefs.edit().putLong(KEY_TRIAL_START_TIME, value).apply()

    val trialExpiresAt: Long
        get() = trialStartTime + 7 * 24 * 60 * 60 * 1000L

    var antiTamperEnabled: Boolean
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_ANTI_TAMPER_ENABLED, true) else true
            if (v) true else dePrefs.getBoolean(KEY_ANTI_TAMPER_ENABLED, true)
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_ANTI_TAMPER_ENABLED, true)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_ANTI_TAMPER_ENABLED, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putBoolean(KEY_ANTI_TAMPER_ENABLED, value).apply()
        }

    var isStatusBarDisabled: Boolean
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_STATUS_BAR_DISABLED, false) else false
            if (v) true else dePrefs.getBoolean(KEY_STATUS_BAR_DISABLED, false)
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_STATUS_BAR_DISABLED, false)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_STATUS_BAR_DISABLED, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putBoolean(KEY_STATUS_BAR_DISABLED, value).apply()
        }

    var usbLockEnabled: Boolean
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_USB_LOCK_ENABLED, false) else false
            if (v) true else dePrefs.getBoolean(KEY_USB_LOCK_ENABLED, false)
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_USB_LOCK_ENABLED, false)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_USB_LOCK_ENABLED, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putBoolean(KEY_USB_LOCK_ENABLED, value).apply()
        }

    var biometricsDisabled: Boolean
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_BIOMETRICS_DISABLED, false) else false
            if (v) true else dePrefs.getBoolean(KEY_BIOMETRICS_DISABLED, false)
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_BIOMETRICS_DISABLED, false)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_BIOMETRICS_DISABLED, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putBoolean(KEY_BIOMETRICS_DISABLED, value).apply()
        }

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
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_SIM_LOCK_ENABLED, false) else false
            if (v) true else dePrefs.getBoolean(KEY_SIM_LOCK_ENABLED, false)
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_SIM_LOCK_ENABLED, false)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_SIM_LOCK_ENABLED, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putBoolean(KEY_SIM_LOCK_ENABLED, value).apply()
        }

    var simLockAlertAction: String
        get() = prefs.getString(KEY_SIM_LOCK_ALERT_ACTION, "alert") ?: "alert"
        set(value) = prefs.edit().putString(KEY_SIM_LOCK_ALERT_ACTION, value).apply()

    var simLockWhitelist: List<String>
        get() = prefs.getStringSet(KEY_SIM_LOCK_WHITELIST, emptySet())?.toList() ?: emptyList()
        set(value) = prefs.edit().putStringSet(KEY_SIM_LOCK_WHITELIST, value.toSet()).apply()

    var emergencyPhone: String
        get() = try {
            val v = if (isUserUnlocked) prefs.getString(KEY_EMERGENCY_PHONE, "") ?: "" else ""
            if (v.isNotBlank()) v else (dePrefs.getString(KEY_EMERGENCY_PHONE, "") ?: dePrefs.getString("de_emergency_phone", "") ?: "")
        } catch (_: Exception) {
            dePrefs.getString(KEY_EMERGENCY_PHONE, "") ?: dePrefs.getString("de_emergency_phone", "") ?: ""
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putString(KEY_EMERGENCY_PHONE, value).apply() } catch (_: Exception) {}
            dePrefs.edit()
                .putString(KEY_EMERGENCY_PHONE, value)
                .putString("de_emergency_phone", value)
                .apply()
        }

    /** SIM Tray Lock — deep-lockdown policy on unauthorized SIM insertion */
    var isSimTrayLockEnabled: Boolean
        get() = try {
            val v = if (isUserUnlocked) prefs.getBoolean(KEY_SIM_TRAY_LOCK_ENABLED, false) else false
            if (v) true else dePrefs.getBoolean(KEY_SIM_TRAY_LOCK_ENABLED, false)
        } catch (_: Exception) {
            dePrefs.getBoolean(KEY_SIM_TRAY_LOCK_ENABLED, false)
        }
        set(value) {
            try { if (isUserUnlocked) prefs.edit().putBoolean(KEY_SIM_TRAY_LOCK_ENABLED, value).apply() } catch (_: Exception) {}
            dePrefs.edit().putBoolean(KEY_SIM_TRAY_LOCK_ENABLED, value).apply()
        }

    /** Temporary emergency PIN generated on SIM tray breach, sent to owner via Telegram */
    var simTrayLockEmergencyPin: String
        get() = prefs.getString(KEY_SIM_TRAY_LOCK_EMERGENCY_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SIM_TRAY_LOCK_EMERGENCY_PIN, value).apply()

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

    var isDeadManSwitchEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEAD_MAN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DEAD_MAN_ENABLED, value).apply()

    var deadManTimeoutHours: Int
        get() = prefs.getInt(KEY_DEAD_MAN_TIMEOUT_HOURS, 6)
        set(value) = prefs.edit().putInt(KEY_DEAD_MAN_TIMEOUT_HOURS, value.coerceIn(1, 72)).apply()

    var lastOwnerHeartbeatTime: Long
        get() = prefs.getLong(KEY_LAST_OWNER_HEARTBEAT, System.currentTimeMillis())
        set(value) = prefs.edit().putLong(KEY_LAST_OWNER_HEARTBEAT, value).apply()

    var isThermalTrapEnabled: Boolean
        get() = prefs.getBoolean(KEY_THERMAL_TRAP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_THERMAL_TRAP_ENABLED, value).apply()

    var thermalTrapThresholdCelsius: Int
        get() = prefs.getInt(KEY_THERMAL_TRAP_THRESHOLD, 48)
        set(value) = prefs.edit().putInt(KEY_THERMAL_TRAP_THRESHOLD, value.coerceIn(40, 65)).apply()

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

    // ── Cyber Defense Suite Properties ──────────────────────────────────────────

    var isA11yShieldEnabled: Boolean
        get() = prefs.getBoolean(KEY_A11Y_SHIELD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_A11Y_SHIELD_ENABLED, value).apply()

    var a11yShieldWhitelist: Set<String>
        get() = prefs.getStringSet(KEY_A11Y_SHIELD_WHITELIST, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_A11Y_SHIELD_WHITELIST, value).apply()

    var isUsbAutolockEnabled: Boolean
        get() = prefs.getBoolean(KEY_USB_AUTOLOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_USB_AUTOLOCK_ENABLED, value).apply()

    var isAnti2gEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANTI_2G_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ANTI_2G_ENABLED, value).apply()

    var isClipperGuardEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLIPPER_GUARD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CLIPPER_GUARD_ENABLED, value).apply()

    var appInstallLockMode: String
        get() = prefs.getString(KEY_APP_INSTALL_LOCK_MODE, "none") ?: "none"
        set(value) = prefs.edit().putString(KEY_APP_INSTALL_LOCK_MODE, value).apply()

    var isCanaryGuardArmed: Boolean
        get() = prefs.getBoolean(KEY_CANARY_GUARD_ARMED, false)
        set(value) = prefs.edit().putBoolean(KEY_CANARY_GUARD_ARMED, value).apply()

    var isOtpGuardEnabled: Boolean
        get() = prefs.getBoolean(KEY_OTP_GUARD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_OTP_GUARD_ENABLED, value).apply()

    var isOtpGuardAutoNeutralize: Boolean
        get() = prefs.getBoolean(KEY_OTP_GUARD_AUTO_NEUTRALIZE, false)
        set(value) = prefs.edit().putBoolean(KEY_OTP_GUARD_AUTO_NEUTRALIZE, value).apply()

    var otpGuardWhitelist: Set<String>
        get() = prefs.getStringSet(KEY_OTP_GUARD_WHITELIST, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_OTP_GUARD_WHITELIST, value).apply()

    var lastBotCommandsSyncTimestamp: Long
        get() = try { prefs.getLong(KEY_LAST_BOT_COMMANDS_SYNC, 0L) } catch (_: Exception) { 0L }
        set(value) = prefs.edit().putLong(KEY_LAST_BOT_COMMANDS_SYNC, value).apply()

    /**
     * Synchronizes all critical configuration and credentials into Device Protected (DE) storage
     * so PASA can operate seamlessly in Direct Boot mode before first unlock and survive reboots.
     */
    fun syncToDeviceProtectedStorage() {
        if (!isUserUnlocked) return
        try {
            val token = try { prefs.getString(KEY_BOT_TOKEN, "") ?: "" } catch (_: Exception) { "" }
            val chatId = try { prefs.getString(KEY_OWNER_CHAT_ID, "") ?: "" } catch (_: Exception) { "" }
            val isSetup = try { prefs.getBoolean(KEY_SETUP_COMPLETE, false) } catch (_: Exception) { false }
            val offset = try { prefs.getLong(KEY_UPDATE_OFFSET, 0L) } catch (_: Exception) { 0L }
            val lost = try { prefs.getBoolean(KEY_LOST_MODE_ACTIVE, false) } catch (_: Exception) { false }
            val fakeShut = try { prefs.getBoolean(KEY_FAKE_SHUTDOWN_ACTIVE, false) } catch (_: Exception) { false }
            val antiTamper = try { prefs.getBoolean(KEY_ANTI_TAMPER_ENABLED, false) } catch (_: Exception) { false }
            val usbLock = try { prefs.getBoolean(KEY_USB_LOCK_ENABLED, false) } catch (_: Exception) { false }
            val biometrics = try { prefs.getBoolean(KEY_BIOMETRICS_DISABLED, false) } catch (_: Exception) { false }
            val simLock = try { prefs.getBoolean(KEY_SIM_LOCK_ENABLED, false) } catch (_: Exception) { false }
            val simTrayLock = try { prefs.getBoolean(KEY_SIM_TRAY_LOCK_ENABLED, false) } catch (_: Exception) { false }
            val phone = try { prefs.getString(KEY_EMERGENCY_PHONE, "") ?: "" } catch (_: Exception) { "" }
            val passHash = try { prefs.getString(KEY_MASTER_PASSWORD_HASH, "") ?: "" } catch (_: Exception) { "" }
            val salt = try { prefs.getString(KEY_PASSWORD_SALT, "") ?: "" } catch (_: Exception) { "" }
            val iterations = try { prefs.getInt(KEY_PASSWORD_ITERATIONS, 100000) } catch (_: Exception) { 100000 }
            val totp = try { prefs.getString(KEY_SMS_TOTP_SECRET, "") ?: "" } catch (_: Exception) { "" }

            val edit = dePrefs.edit()
            if (token.isNotBlank()) edit.putString(KEY_BOT_TOKEN, token)
            if (chatId.isNotBlank()) edit.putString(KEY_OWNER_CHAT_ID, chatId)
            if (isSetup) {
                edit.putBoolean(KEY_SETUP_COMPLETE, true)
                edit.putBoolean("setup_complete_dp", true)
            }
            if (offset > 0) edit.putLong(KEY_UPDATE_OFFSET, offset)
            if (lost) edit.putBoolean(KEY_LOST_MODE_ACTIVE, true)
            if (fakeShut) edit.putBoolean(KEY_FAKE_SHUTDOWN_ACTIVE, true)
            if (antiTamper) edit.putBoolean(KEY_ANTI_TAMPER_ENABLED, true)
            if (usbLock) edit.putBoolean(KEY_USB_LOCK_ENABLED, true)
            if (biometrics) edit.putBoolean(KEY_BIOMETRICS_DISABLED, true)
            if (simLock) edit.putBoolean(KEY_SIM_LOCK_ENABLED, true)
            if (simTrayLock) edit.putBoolean(KEY_SIM_TRAY_LOCK_ENABLED, true)
            if (phone.isNotBlank()) {
                edit.putString(KEY_EMERGENCY_PHONE, phone)
                edit.putString("de_emergency_phone", phone)
            }
            if (passHash.isNotBlank()) {
                edit.putString(KEY_MASTER_PASSWORD_HASH, passHash)
                edit.putString("de_master_password_hash", passHash)
            }
            if (salt.isNotBlank()) {
                edit.putString(KEY_PASSWORD_SALT, salt)
                edit.putString("de_password_salt", salt)
            }
            if (iterations > 0) {
                edit.putInt(KEY_PASSWORD_ITERATIONS, iterations)
                edit.putInt("de_password_iterations", iterations)
            }
            if (totp.isNotBlank()) {
                edit.putString(KEY_SMS_TOTP_SECRET, totp)
                edit.putString("de_totp_secret", totp)
            }
            edit.apply()

            // Keep legacy pasa_direct_boot file in sync
            val legacyDirectBootPrefs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.createDeviceProtectedStorageContext().getSharedPreferences("pasa_direct_boot", Context.MODE_PRIVATE)
            } else {
                context.getSharedPreferences("pasa_direct_boot", Context.MODE_PRIVATE)
            }
            legacyDirectBootPrefs.edit().putBoolean("setup_complete_dp", isSetup).apply()

            Log.d(TAG, "Preferences mirrored to Device Protected Storage successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Error mirroring preferences to DE storage: ${e.message}")
        }
    }

    // --- Dormant / Pause State ---

    /** When true, PasaService will skip all polling and sensor activities. */
    var isPaused: Boolean
        get() = prefs.getBoolean(KEY_PASA_PAUSED, false)
        set(value) = prefs.edit().putBoolean(KEY_PASA_PAUSED, value).apply()

    /** Timestamp when PASA was paused (ms epoch). */
    var pausedAtMs: Long
        get() = prefs.getLong(KEY_PASA_PAUSED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_PASA_PAUSED_AT, value).apply()

    /**
     * Cryptographically clears all sensitive credentials and secrets.
     * Called by /retire before self-uninstall.
     * Does NOT clear bot token / chat ID — those are needed to send the final farewell message.
     */
    fun shredAllCredentials() {
        try {
            prefs.edit()
                .remove(KEY_MASTER_PASSWORD_HASH)
                .remove(KEY_PASSWORD_SALT)
                .remove(KEY_PASSWORD_ITERATIONS)
                .remove(KEY_SMS_TOTP_SECRET)
                .remove(KEY_RESET_PASSWORD_TOKEN)
                .remove(KEY_DURESS_PIN)
                .remove(KEY_DB_PASSPHRASE)
                .remove(KEY_DEVICE_ID)
                .apply()
            dePrefs.edit()
                .remove(KEY_MASTER_PASSWORD_HASH)
                .remove(KEY_PASSWORD_SALT)
                .remove(KEY_SMS_TOTP_SECRET)
                .remove(KEY_RESET_PASSWORD_TOKEN)
                .remove(KEY_DURESS_PIN)
                .apply()
            Log.w(TAG, "shredAllCredentials: All cryptographic secrets cleared from EncryptedSharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "shredAllCredentials: Error during shredding: ${e.message}", e)
            throw e
        }
    }

    /** Returns true if the minimum configuration required to run is present. */
    fun isConfigured(): Boolean {
        return botToken.isNotBlank() && ownerChatId.isNotBlank() && isSetupComplete
    }
}
