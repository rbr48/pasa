package com.izhaanintellect.pasa.crypto

import com.izhaanintellect.pasa.data.PreferencesManager

class DuplicateCommandException(message: String) : Exception(message)

interface ReplayStore {
    fun accept(sequence: Long, commandId: String): Boolean
    fun highWaterMark(): Long
}

/**
 * Persistent replay protection using encrypted SharedPreferences.
 * Maintains a monotonic high-water mark sequence and a sliding window of
 * recently seen command IDs to prevent both replay and duplicate delivery.
 */
class PersistentReplayStore(
    private val prefs: PreferencesManager,
    private val historySize: Int = 64
) : ReplayStore {

    @Synchronized
    override fun accept(sequence: Long, commandId: String): Boolean {
        if (commandId in seenCommandIds()) {
            throw DuplicateCommandException("Command $commandId was already accepted and processed")
        }
        val seenSeqs = seenSequences()
        if (sequence in seenSeqs) return false

        val hwm = highWaterMark()
        if (hwm > 0 && sequence < (hwm - historySize)) {
            // Sequence is older than sliding window threshold
            return false
        }

        if (sequence > hwm) {
            prefs.replayHighWaterMark = sequence
        }
        rememberCommandId(commandId)
        rememberSequence(sequence)
        return true
    }

    override fun highWaterMark(): Long = prefs.replayHighWaterMark

    private fun seenCommandIds(): Set<String> {
        val raw = prefs.replaySeenCommandIds
        return if (raw.isBlank()) emptySet() else raw.split('\n').filter { it.isNotBlank() }.toSet()
    }

    private fun rememberCommandId(commandId: String) {
        val updated = (seenCommandIds().toList() + commandId).takeLast(historySize)
        prefs.replaySeenCommandIds = updated.joinToString("\n")
    }

    private fun seenSequences(): Set<Long> {
        val raw = prefs.replaySeenSequences
        return if (raw.isBlank()) emptySet() else raw.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()
    }

    private fun rememberSequence(seq: Long) {
        val updated = (seenSequences().toList() + seq).takeLast(historySize)
        prefs.replaySeenSequences = updated.joinToString(",")
    }
}
