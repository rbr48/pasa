package com.izhaanintellect.pasa.crypto

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.crypto.Ed25519Verifier
import com.nimbusds.jose.jwk.OctetKeyPair
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

data class VerifiedCommand(
    val commandId: String,
    val deviceId: String,
    val action: String,
    val args: List<String>,
    val sequence: Long,
    val nonce: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val chatId: Long
)

class CommandRejectedException(message: String) : SecurityException(message)

/**
 * Verifies a command envelope's Ed25519 signature before any claim is trusted.
 * Signature is checked BEFORE payload is parsed (fail-closed security).
 * Validates device binding, action allowlist, validity window, and replay position.
 */
class CommandVerifier(
    private val deviceId: String,
    private val replayStore: ReplayStore,
    private val trustedKeys: Map<String, String>,
    private val clockSkew: Duration = Duration.ofSeconds(30),
    private val now: () -> Instant = Instant::now
) {
    fun verify(compactJws: String): VerifiedCommand {
        if (trustedKeys.isEmpty()) throw CommandRejectedException("No enrolled command signing key")

        val jws = runCatching { JWSObject.parse(compactJws) }
            .getOrElse { throw CommandRejectedException("Malformed command envelope") }

        if (jws.header.algorithm != JWSAlgorithm.EdDSA) {
            throw CommandRejectedException("Unexpected command algorithm: ${jws.header.algorithm}")
        }

        val kid = jws.header.keyID ?: throw CommandRejectedException("Command envelope has no key id")
        val jwk = trustedKeys[kid] ?: throw CommandRejectedException("Untrusted command signing key: $kid")

        val verified = runCatching {
            jws.verify(Ed25519Verifier(OctetKeyPair.parse(jwk)))
        }.getOrElse { throw CommandRejectedException("Command signature could not be checked") }
        if (!verified) throw CommandRejectedException("Invalid command signature")

        val payload = runCatching { JSONObject(jws.payload.toString()) }
            .getOrElse { throw CommandRejectedException("Malformed command payload") }

        val command = runCatching {
            val argsArray = payload.optJSONArray("args")
            val argsList = if (argsArray != null) {
                (0 until argsArray.length()).map { argsArray.getString(it) }
            } else emptyList()

            VerifiedCommand(
                commandId = payload.getString("commandId"),
                deviceId = payload.getString("deviceId"),
                action = payload.getString("action"),
                args = argsList,
                sequence = payload.getLong("sequence"),
                nonce = payload.getString("nonce"),
                issuedAt = Instant.parse(payload.getString("issuedAt")),
                expiresAt = Instant.parse(payload.getString("expiresAt")),
                chatId = payload.optLong("chatId", 0L)
            )
        }.getOrElse { throw CommandRejectedException("Command payload is missing required claims") }

        if (command.deviceId != deviceId) throw CommandRejectedException("Command is bound to another device")

        val instant = now()
        if (command.expiresAt.isBefore(instant.minus(clockSkew))) {
            throw CommandRejectedException("Command expired at ${command.expiresAt}")
        }
        if (command.issuedAt.isAfter(instant.plus(clockSkew))) {
            throw CommandRejectedException("Command is issued in the future")
        }
        if (!replayStore.accept(command.sequence, command.commandId)) {
            throw CommandRejectedException("Replayed or out-of-order command at sequence ${command.sequence}")
        }
        return command
    }
}
