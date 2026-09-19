package com.izhaanintellect.pasa.security

import android.util.Base64
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication and authorization for incoming Telegram commands.
 * Enforces owner chat ID checks and secure password verification using PBKDF2WithHmacSHA256.
 */
@Singleton
class AuthManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "PASA_Auth"
        // OWASP-recommended work factor for PBKDF2-HMAC-SHA256 (>= 600,000).
        // Hashes created before this used 10,000 iterations; the stored count is
        // read from PreferencesManager so old hashes still verify and are then
        // transparently re-hashed at the current work factor on next login.
        private const val ITERATIONS = 600000
        private const val KEY_LENGTH = 256
        private val DESTRUCTIVE_COMMANDS = setOf(
            "/wipe", "/wipe_external", "/format", "/wipe_confirm"
        )
    }

    /**
     * Checks if the given Telegram chat ID is the authorized owner.
     */
    fun isAuthorizedChat(chatId: Long): Boolean {
        val authorized = chatId != 0L && chatId == preferencesManager.ownerChatIdLong
        if (!authorized) {
            Log.w(TAG, "Unauthorized command attempt from chat ID: $chatId")
        }
        return authorized
    }

    /**
     * Hashes and stores the master password with a cryptographically secure random salt.
     */
    fun setMasterPassword(password: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPassword(password, salt, ITERATIONS)
        preferencesManager.passwordSalt = saltBase64
        preferencesManager.masterPasswordHash = hash
        preferencesManager.passwordIterations = ITERATIONS
    }

    /**
     * Verifies the master password against the stored salted PBKDF2 hash.
     * Legacy hashes (stored with a lower iteration count) still verify and are
     * transparently re-hashed at the current work factor on success.
     */
    fun verifyMasterPassword(password: String): Boolean {
        val saltBase64 = preferencesManager.passwordSalt
        val storedHash = preferencesManager.masterPasswordHash
        if (saltBase64.isBlank() || storedHash.isBlank()) return false

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val storedIterations = preferencesManager.passwordIterations
        val computedHash = hashPassword(password, salt, storedIterations)
        val matches = slowEquals(storedHash, computedHash)

        // Upgrade older/weaker hashes to the current work factor once verified.
        if (matches && storedIterations < ITERATIONS) {
            Log.i(TAG, "Upgrading master password hash from $storedIterations to $ITERATIONS iterations")
            setMasterPassword(password)
        }
        return matches
    }

    private fun hashPassword(password: String, salt: ByteArray, iterations: Int): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private fun slowEquals(a: String, b: String): Boolean {
        var diff = a.length xor b.length
        var i = 0
        while (i < a.length && i < b.length) {
            diff = diff or (a[i].code xor b[i].code)
            i++
        }
        return diff == 0
    }

    fun isDestructiveCommand(command: String): Boolean {
        return command.lowercase() in DESTRUCTIVE_COMMANDS
    }
}
