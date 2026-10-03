package com.izhaanintellect.pasa.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import android.util.Base64
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Device Protected (DE) Storage for Direct Boot pre-first-unlock execution.
 *
 * In Android 7.0+ (API 24+), Credential Encrypted (CE) storage is locked by the Linux
 * kernel immediately after a cold boot until the user enters their lockscreen PIN.
 * This manager provides hardware-backed DE storage accessible before first unlock,
 * allowing out-of-band SMS C2 commands (TOTP and Master PIN verification) to execute
 * immediately upon boot even while the phone is locked at the boot credentials prompt.
 */
@Singleton
class DeviceProtectedStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PASA_DirectBoot"
        private const val PREFS_NAME = "pasa_de_secure_prefs"

        private const val KEY_DE_TOTP_SECRET = "de_tok_data"
        private const val KEY_DE_PASSWORD_HASH = "de_cred_hash"
        private const val KEY_DE_PASSWORD_SALT = "de_cred_salt"
        private const val KEY_DE_PASSWORD_ITERATIONS = "de_cred_iter"
        private const val KEY_DE_EMERGENCY_PHONE = "de_contact_data"
        private const val KEY_DE_ACTIVE_LOCK_PIN = "de_sec_val"
    }

    private fun obscure(value: String): String {
        if (value.isEmpty()) return ""
        val seed = (Build.FINGERPRINT + context.packageName).toByteArray(Charsets.UTF_8)
        val bytes = value.toByteArray(Charsets.UTF_8)
        val result = ByteArray(bytes.size)
        for (i in bytes.indices) {
            result[i] = (bytes[i].toInt() xor seed[i % seed.size].toInt()).toByte()
        }
        return Base64.encodeToString(result, Base64.NO_WRAP)
    }

    private fun reveal(value: String): String {
        if (value.isEmpty()) return ""
        val decoded = try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (_: Exception) { return "" }
        val seed = (Build.FINGERPRINT + context.packageName).toByteArray(Charsets.UTF_8)
        val result = ByteArray(decoded.size)
        for (i in decoded.indices) {
            result[i] = (decoded[i].toInt() xor seed[i % seed.size].toInt()).toByte()
        }
        return String(result, Charsets.UTF_8)
    }

    private val deContext: Context by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
    }

    private val dePrefs: SharedPreferences by lazy {
        deContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if the device is currently in Direct Boot mode (pre-first-unlock).
     */
    fun isDeviceLockedPreBoot(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            return userManager != null && !userManager.isUserUnlocked
        }
        return false
    }

    /**
     * Synchronizes authentication secrets from EncryptedSharedPreferences into
     * Device Protected Storage so they survive cold reboots and remain accessible pre-unlock.
     */
    fun syncSecrets(
        totpSecret: String,
        passwordHash: String,
        passwordSalt: String,
        passwordIterations: Int,
        emergencyPhone: String,
        activeLockPin: String?
    ) {
        try {
            dePrefs.edit()
                .putString(KEY_DE_TOTP_SECRET, obscure(totpSecret))
                .putString(KEY_DE_PASSWORD_HASH, obscure(passwordHash))
                .putString(KEY_DE_PASSWORD_SALT, obscure(passwordSalt))
                .putInt(KEY_DE_PASSWORD_ITERATIONS, passwordIterations)
                .putString(KEY_DE_EMERGENCY_PHONE, obscure(emergencyPhone))
                .putString(KEY_DE_ACTIVE_LOCK_PIN, obscure(activeLockPin ?: ""))
                .apply()
            Log.d(TAG, "Device Protected Storage secrets synchronized successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Failed synchronizing DE secrets: ${e.message}")
        }
    }

    fun syncFromPreferences(prefs: PreferencesManager) {
        syncSecrets(
            totpSecret = prefs.smsTotpSecret,
            passwordHash = prefs.masterPasswordHash,
            passwordSalt = prefs.passwordSalt,
            passwordIterations = prefs.passwordIterations,
            emergencyPhone = prefs.emergencyPhone,
            activeLockPin = prefs.activeLockPin
        )
    }

    fun getEmergencyPhone(): String {
        val raw = dePrefs.getString(KEY_DE_EMERGENCY_PHONE, "") ?: ""
        return reveal(raw).ifEmpty { raw }
    }

    fun getTotpSecret(): String {
        val raw = dePrefs.getString(KEY_DE_TOTP_SECRET, "") ?: ""
        return reveal(raw).ifEmpty { raw }
    }

    fun getActiveLockPin(): String {
        val raw = dePrefs.getString(KEY_DE_ACTIVE_LOCK_PIN, "") ?: ""
        return reveal(raw).ifEmpty { raw }
    }

    /**
     * Verifies TOTP code using secrets stored in Device Protected Storage.
     * Works pre-first-unlock immediately after boot.
     */
    fun isTotpValidDirectBoot(candidateCode: String): Boolean {
        val secret = getTotpSecret()
        if (secret.isBlank()) return false
        return Totp.verify(secret, candidateCode, window = 3)
    }

    /**
     * Verifies Master Password using salted PBKDF2 hash stored in Device Protected Storage.
     * Works pre-first-unlock immediately after boot.
     */
    fun verifyMasterPasswordDirectBoot(candidatePassword: String): Boolean {
        val rawSalt = dePrefs.getString(KEY_DE_PASSWORD_SALT, "") ?: ""
        val saltBase64 = reveal(rawSalt).ifEmpty { rawSalt }
        val rawHash = dePrefs.getString(KEY_DE_PASSWORD_HASH, "") ?: ""
        val storedHash = reveal(rawHash).ifEmpty { rawHash }
        val iterations = dePrefs.getInt(KEY_DE_PASSWORD_ITERATIONS, 100000)

        if (saltBase64.isBlank() || storedHash.isBlank()) return false

        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val spec: KeySpec = PBEKeySpec(candidatePassword.toCharArray(), salt, iterations, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val computedBytes = factory.generateSecret(spec).encoded
            val computedHash = Base64.encodeToString(computedBytes, Base64.NO_WRAP)

            slowEquals(storedHash, computedHash)
        } catch (e: Exception) {
            Log.e(TAG, "Direct Boot password verification failed: ${e.message}")
            false
        }
    }

    private fun slowEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        var diff = aBytes.size xor bBytes.size
        val minLen = minOf(aBytes.size, bBytes.size)
        for (i in 0 until minLen) {
            diff = diff or (aBytes[i].toInt() xor bBytes[i].toInt())
        }
        return diff == 0
    }
}
