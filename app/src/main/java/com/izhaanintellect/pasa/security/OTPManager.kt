package com.izhaanintellect.pasa.security

import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages one-time password (OTP) generation and verification
 * for destructive commands like factory reset.
 */
@Singleton
class OTPManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "PASA_OTP"
        private const val OTP_LENGTH = 6
        private const val OTP_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
    }

    private val secureRandom = SecureRandom()

    fun generateOTP(): String {
        val otp = (0 until OTP_LENGTH)
            .map { secureRandom.nextInt(10) }
            .joinToString("")
        return otp
    }

    fun storeOTP(otp: String) {
        preferencesManager.pendingOTP = hashOTP(otp)
        preferencesManager.otpTimestamp = System.currentTimeMillis()
        Log.d(TAG, "OTP stored with timestamp ${preferencesManager.otpTimestamp}")
    }

    fun verifyOTP(otp: String): Boolean {
        val storedHash = preferencesManager.pendingOTP ?: return false

        if (isOTPExpired()) {
            Log.w(TAG, "OTP has expired")
            clearOTP()
            return false
        }

        val inputHash = hashOTP(otp)
        val isValid = inputHash == storedHash

        if (isValid) {
            Log.i(TAG, "OTP verified successfully")
            clearOTP()
        } else {
            Log.w(TAG, "OTP verification failed")
        }

        return isValid
    }

    fun isOTPExpired(): Boolean {
        val timestamp = preferencesManager.otpTimestamp
        if (timestamp == 0L) return true
        return System.currentTimeMillis() - timestamp > OTP_EXPIRY_MS
    }

    fun clearOTP() {
        preferencesManager.pendingOTP = null
        preferencesManager.otpTimestamp = 0L
    }

    private fun hashOTP(otp: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(otp.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
