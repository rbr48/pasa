package com.izhaanintellect.pasa.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ClipperGuardManager.
 * Detects and neutralizes Crypto Clipper malware in real-time.
 * Clipper malware monitors the Android clipboard and invisibly swaps copied
 * Bitcoin, Ethereum, Tron, or Solana addresses with the attacker's wallet address.
 */
@Singleton
class ClipperGuardManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val telegramApi: TelegramApi
) {
    companion object {
        private const val TAG = "PASA_ClipperGuard"

        // Cryptographic wallet address patterns
        private val BTC_PATTERN = Pattern.compile("^(bc1[ac-hj-np-z02-9]{11,71}|[13][a-km-zA-HJ-NP-Z1-9]{25,34})$")
        private val ETH_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$")
        private val TRON_PATTERN = Pattern.compile("^T[1-9A-HJ-NP-Za-km-z]{33}$")
        private val SOL_PATTERN = Pattern.compile("^[1-9A-HJ-NP-Za-km-z]{32,44}$")
    }

    private var clipboardManager: ClipboardManager? = null
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var isMonitoring = false

    private data class TrackedAddress(
        val address: String,
        val type: String,
        val timestamp: Long
    )

    private var lastLegitimateAddress: TrackedAddress? = null
    private var isSelfRestoring = false

    fun startMonitoring() {
        if (!prefs.isClipperGuardEnabled) {
            Log.d(TAG, "ClipperGuard disabled in preferences.")
            return
        }
        if (isMonitoring) return

        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboardManager == null) {
            Log.w(TAG, "ClipboardManager unavailable.")
            return
        }

        listener = ClipboardManager.OnPrimaryClipChangedListener {
            handleClipChange()
        }

        clipboardManager?.addPrimaryClipChangedListener(listener)
        isMonitoring = true
        Log.i(TAG, "ClipperGuard monitoring armed successfully.")
    }

    fun stopMonitoring() {
        if (isMonitoring && listener != null) {
            try {
                clipboardManager?.removePrimaryClipChangedListener(listener)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing clipboard listener: ${e.message}")
            }
            listener = null
            isMonitoring = false
            Log.i(TAG, "ClipperGuard monitoring stopped.")
        }
    }

    private fun detectCryptoType(text: String): String? {
        val trimmed = text.trim()
        return when {
            BTC_PATTERN.matcher(trimmed).matches() -> "Bitcoin (BTC)"
            ETH_PATTERN.matcher(trimmed).matches() -> "Ethereum / EVM (ETH/BSC/Polygon)"
            TRON_PATTERN.matcher(trimmed).matches() -> "Tron / TRC20 (TRX/USDT)"
            SOL_PATTERN.matcher(trimmed).matches() && !trimmed.contains(" ") -> "Solana (SOL)"
            else -> null
        }
    }

    private fun handleClipChange() {
        if (isSelfRestoring) {
            isSelfRestoring = false
            return
        }

        val cm = clipboardManager ?: return
        val clip = try {
            cm.primaryClip
        } catch (e: Exception) {
            Log.d(TAG, "Cannot read clip: ${e.message}")
            null
        } ?: return

        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0)?.text?.toString()?.trim() ?: return
        if (text.isBlank()) return

        val detectedType = detectCryptoType(text) ?: return
        val now = System.currentTimeMillis()

        val prev = lastLegitimateAddress
        // If a new address of the SAME crypto family replaces the previous address within 2500ms,
        // it indicates automated clipper malware tampering (humans cannot manually copy two different addresses in 2 seconds)
        if (prev != null && prev.type == detectedType && prev.address != text && (now - prev.timestamp) < 2500) {
            Log.w(TAG, "🚨 CLIPPER ATTACK DETECTED! Replaced ${prev.address} with $text in ${now - prev.timestamp}ms")

            // Neutralize and restore the original legitimate address
            isSelfRestoring = true
            try {
                val restoredClip = ClipData.newPlainText("text", prev.address)
                cm.setPrimaryClip(restoredClip)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore clipboard: ${e.message}")
            }

            // Dispatch emergency Telegram alert
            if (prefs.isConfigured()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val alertMsg = "🚨 <b>CRYPTO CLIPPER ATTACK INTERCEPTED!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                "⚠️ A background malware app attempted to hijack your cryptocurrency transfer!\n\n" +
                                "💰 <b>Asset:</b> $detectedType\n" +
                                "❌ <b>Attacker Address (Blocked):</b>\n<code>$text</code>\n\n" +
                                "✅ <b>Original Address (Restored):</b>\n<code>${prev.address}</code>\n\n" +
                                "🔒 <i>PASA Sentinel neutralized the hijacked clipboard instantly.</i>"

                        telegramApi.sendMessage(
                            token = prefs.botToken,
                            request = SendMessageRequest(
                                chatId = prefs.ownerChatIdLong,
                                text = alertMsg
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send clipper alert", e)
                    }
                }
            }
        } else {
            // Legitimate user copy
            lastLegitimateAddress = TrackedAddress(text, detectedType, now)
            Log.d(TAG, "Legitimate crypto address copied: $detectedType ($text)")
        }
    }
}
