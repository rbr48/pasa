package com.izhaanintellect.pasa.crypto

import com.izhaanintellect.pasa.data.PreferencesManager

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
        if (commandId in seenCommandIds()) return false
        if (sequence <= highWaterMark()) return false
        prefs.replayHighWaterMark = sequence
        rememberCommandId(commandId)
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
}
