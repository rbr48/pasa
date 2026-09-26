package com.izhaanintellect.pasa.commands

import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.ClipperGuardManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ClipperGuardCommand (/clipper_guard).
 * Controls the real-time Crypto Clipper Trap.
 * Shields Bitcoin, Ethereum, Tron, and Solana wallet transfers from clipboard hijacking malware.
 */
@Singleton
class ClipperGuardCommand @Inject constructor(
    private val prefs: PreferencesManager,
    private val clipperGuardManager: ClipperGuardManager
) : Command {

    override val name = "/clipper_guard"
    override val description = "Crypto Clipper Clipboard Hijack Trap"
    override val usage = "/clipper_guard [status|enable|disable]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull()?.lowercase()?.trim() ?: "status"

        return when (action) {
            "on", "enable", "arm" -> {
                prefs.isClipperGuardEnabled = true
                clipperGuardManager.startMonitoring()
                CommandResult(
                    success = true,
                    message = "📋 <b>Crypto Clipper Trap: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "✅ <b>Status:</b> ACTIVE (Monitoring Clipboard)\n" +
                            "🛡️ <b>Protected Assets:</b>\n" +
                            "• Bitcoin (BTC Legacy, SegWit, Bech32)\n" +
                            "• Ethereum & EVM (ETH, BSC, Polygon, Arbitrum)\n" +
                            "• Tron & TRC-20 (TRX, USDT)\n" +
                            "• Solana (SOL)\n\n" +
                            "🔒 <i>Any stealth attempt by malware to alter your copied wallet addresses will be instantly intercepted, neutralized, and reported to Telegram.</i>"
                )
            }

            "off", "disable", "disarm" -> {
                prefs.isClipperGuardEnabled = false
                clipperGuardManager.stopMonitoring()
                CommandResult(
                    success = true,
                    message = "📋 <b>Crypto Clipper Trap: DISARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "⚠️ Clipboard monitoring for crypto wallet address tampering is paused."
                )
            }

            else -> {
                val isArmed = prefs.isClipperGuardEnabled
                CommandResult(
                    success = true,
                    message = "📋 <b>Crypto Clipper Trap Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• <b>Status:</b> ${if (isArmed) "🔒 ARMED & MONITORING" else "🔓 DISARMED"}\n" +
                            "• <b>Real-Time Defense:</b> Active clipboard memory inspection\n" +
                            "• <b>Automated Countermeasure:</b> Instant address restoration + Telegram alarm\n\n" +
                            "💡 <i>Usage:</i>\n" +
                            "• <code>/clipper_guard enable</code> — Arm real-time crypto protection\n" +
                            "• <code>/clipper_guard disable</code> — Disarm clipper trap"
                )
            }
        }
    }
}
