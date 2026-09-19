package com.izhaanintellect.pasa.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * Self-contained RFC 6238 (TOTP) / RFC 4648 (Base32) implementation.
 *
 * Pure JVM — no Android APIs — so it is unit-testable and portable. Used to
 * authenticate offline SMS commands without ever transmitting the master
 * password: the owner enrolls the shared secret in an authenticator app and
 * texts the current 6-digit code instead.
 */
object Totp {

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val DEFAULT_STEP_SECONDS = 30L
    private const val DEFAULT_DIGITS = 6

    private val secureRandom = SecureRandom()

    /** Generates a new random Base32 secret (default 20 bytes = 160 bits, SHA-1 block size). */
    fun generateSecretBase32(numBytes: Int = 20): String {
        val bytes = ByteArray(numBytes).also(secureRandom::nextBytes)
        return base32Encode(bytes)
    }

    /** Computes the TOTP code for a given epoch-millis timestamp. */
    fun codeAt(
        secretBase32: String,
        timeMillis: Long,
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        digits: Int = DEFAULT_DIGITS
    ): String {
        val counter = Math.floorDiv(timeMillis / 1000L, stepSeconds)
        return hotp(base32Decode(secretBase32), counter, digits)
    }

    /**
     * Verifies a submitted code against the current time, allowing a +/- window
     * of steps to tolerate clock drift and SMS delivery latency.
     */
    fun verify(
        secretBase32: String,
        code: String,
        timeMillis: Long = System.currentTimeMillis(),
        stepSeconds: Long = DEFAULT_STEP_SECONDS,
        digits: Int = DEFAULT_DIGITS,
        window: Int = 1
    ): Boolean {
        val cleaned = code.trim().filter { it.isDigit() }
        if (cleaned.length != digits) return false
        val key = try { base32Decode(secretBase32) } catch (e: Exception) { return false }
        val currentCounter = Math.floorDiv(timeMillis / 1000L, stepSeconds)
        for (offset in -window..window) {
            val candidate = hotp(key, currentCounter + offset, digits)
            if (constantTimeEquals(candidate, cleaned)) return true
        }
        return false
    }

    /** Builds an otpauth:// URI that authenticator apps can import via QR or manual entry. */
    fun otpauthUri(secretBase32: String, account: String, issuer: String = "PASA"): String {
        val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20") }
        return "otpauth://totp/${enc(issuer)}:${enc(account)}" +
                "?secret=$secretBase32&issuer=${enc(issuer)}&algorithm=SHA1&digits=$DEFAULT_DIGITS&period=$DEFAULT_STEP_SECONDS"
    }

    // --- HOTP (RFC 4226) core ---

    private fun hotp(key: ByteArray, counter: Long, digits: Int): String {
        val msg = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            msg[i] = (c and 0xff).toByte()
            c = c shr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(msg)
        val offset = (hash[hash.size - 1].toInt() and 0x0f)
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)
        val otp = binary % Math.pow(10.0, digits.toDouble()).toInt()
        return otp.toString().padStart(digits, '0')
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    // --- Base32 (RFC 4648, no padding) ---

    fun base32Encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xff)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1f
                sb.append(BASE32_ALPHABET[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1f
            sb.append(BASE32_ALPHABET[index])
        }
        return sb.toString()
    }

    fun base32Decode(encoded: String): ByteArray {
        val clean = encoded.trim().replace(" ", "").replace("-", "").uppercase().trimEnd('=')
        if (clean.isEmpty()) return ByteArray(0)
        val out = ArrayList<Byte>(clean.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        for (ch in clean) {
            val index = BASE32_ALPHABET.indexOf(ch)
            require(index >= 0) { "Invalid Base32 character: $ch" }
            buffer = (buffer shl 5) or index
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out.add(((buffer shr (bitsLeft - 8)) and 0xff).toByte())
                bitsLeft -= 8
            }
        }
        return out.toByteArray()
    }
}
