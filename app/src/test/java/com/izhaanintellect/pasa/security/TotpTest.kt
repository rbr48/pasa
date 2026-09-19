package com.izhaanintellect.pasa.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the TOTP/Base32 implementation against the official RFC 6238
 * SHA-1 test vectors (seed = ASCII "12345678901234567890").
 */
class TotpTest {

    // RFC 6238 Appendix B test seed, encoded to Base32 via our own encoder.
    private val secret = Totp.base32Encode("12345678901234567890".toByteArray(Charsets.US_ASCII))

    private fun codeAtSeconds(seconds: Long, digits: Int) =
        Totp.codeAt(secret, timeMillis = seconds * 1000L, digits = digits)

    @Test
    fun rfc6238_vectors_8digit() {
        assertEquals("94287082", codeAtSeconds(59L, 8))
        assertEquals("07081804", codeAtSeconds(1111111109L, 8))
        assertEquals("14050471", codeAtSeconds(1111111111L, 8))
        assertEquals("89005924", codeAtSeconds(1234567890L, 8))
        assertEquals("69279037", codeAtSeconds(2000000000L, 8))
        assertEquals("65353130", codeAtSeconds(20000000000L, 8))
    }

    @Test
    fun base32_roundTrip() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 127, -128, -1)
        val encoded = Totp.base32Encode(data)
        assertTrue(Totp.base32Decode(encoded).contentEquals(data))
    }

    @Test
    fun verify_acceptsCurrentCode() {
        val now = 1234567890_000L
        val code = Totp.codeAt(secret, timeMillis = now, digits = 6)
        assertTrue(Totp.verify(secret, code, timeMillis = now, digits = 6))
    }

    @Test
    fun verify_toleratesOneStepDrift() {
        val now = 1234567890_000L
        val prevStepCode = Totp.codeAt(secret, timeMillis = now - 30_000L, digits = 6)
        assertTrue("code from previous 30s step should verify within window",
            Totp.verify(secret, prevStepCode, timeMillis = now, digits = 6, window = 1))
    }

    @Test
    fun verify_rejectsWrongAndMalformedCodes() {
        val now = 1234567890_000L
        val correct = Totp.codeAt(secret, timeMillis = now, digits = 6)
        // Flip the first digit to guarantee a wrong-but-well-formed code.
        val wrong = (((correct[0] - '0' + 1) % 10).toString()) + correct.substring(1)
        assertFalse(Totp.verify(secret, wrong, timeMillis = now, digits = 6, window = 0))
        assertFalse(Totp.verify(secret, "12", timeMillis = now, digits = 6))       // too short
        assertFalse(Totp.verify(secret, "abcdef", timeMillis = now, digits = 6))   // non-numeric
    }
}
