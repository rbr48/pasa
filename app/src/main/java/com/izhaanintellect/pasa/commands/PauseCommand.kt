package com.izhaanintellect.pasa.commands

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.bot.InlineKeyboardButton
import com.izhaanintellect.pasa.bot.InlineKeyboardMarkup
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.service.PasaService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * /pause — Put PASA into Dormant Mode (suspend all activities).
 * /resume — Wake PASA from Dormant Mode, restoring full operation.
 *
 * DORMANT MODE stops:
 * ✅ Telegram long-polling (no more commands processed, except /resume via direct Telegram poll restart)
 * ✅ GPS / location tracking
 * ✅ Sensor traps (snatch, charger, pocket)
 * ✅ Ransomware canary monitoring
 * ✅ OTP guard
 * ✅ USB autolock
 * ✅ Clipper guard
 *
 * DORMANT MODE PRESERVES:
 * ℹ️ All Knox restrictions (anti-tamper, USB lock, camera lock, etc.) remain ACTIVE
 * ℹ️ Device Owner status remains ACTIVE
 * ℹ️ All settings are saved — /resume restores full operation instantly
 *
 * Requires Master Password / active session for /pause.
 * /resume requires Master Password (since session is lost when polling stops).
 */
@Singleton
class PauseCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/pause"
    override val description = "Suspend all PASA activities (dormant mode)"
    override val usage = "/pause | /resume"

    companion object {
        private const val TAG = "PASA_Pause"
    }

    /** Called for /pause */
    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val sub = args.firstOrNull()?.lowercase()?.trim()
        return when {
            sub == "status" || (sub == null && preferencesManager.isPaused) && args.isEmpty() -> buildStatus()
            else -> executePause(args)
        }
    }

    suspend fun executePause(args: List<String>): CommandResult {
        // If already paused — show status
        if (preferencesManager.isPaused) {
            return buildStatus()
        }

        val password = args.firstOrNull()?.trim()

        // Require Master Password or active session
        val sessionOk = authManager.isSessionAuthenticated()
        val passwordOk = !password.isNullOrBlank() && authManager.verifyMasterPassword(password)

        if (!sessionOk && !passwordOk) {
            return CommandResult(
                success = false,
                message = buildString {
                    appendLine("⏸️ <b>PASA Dormant Mode</b>")
                    appendLine("━━━━━━━━━━━━━━━━━━━━")
                    appendLine("Suspends all PASA monitoring activities while preserving Device Owner restrictions.")
                    appendLine()
                    appendLine("To pause, authenticate first or include Master Password:")
                    appendLine()
                    appendLine("<code>/auth &lt;master_password&gt;</code>  — then  <code>/pause</code>")
                    appendLine("or")
                    appendLine("<code>/pause &lt;master_password&gt;</code>")
                },
                replyMarkup = InlineKeyboardMarkup(
                    inlineKeyboard = listOf(
                        listOf(
                            InlineKeyboardButton("📊 Current Status", callbackData = "cmd:pause:status"),
                            InlineKeyboardButton("🔓 Auth Session", callbackData = "cmd:auth")
                        )
                    )
                )
            )
        }

        // --- Enter Dormant Mode ---
        Log.w(TAG, "Entering PASA Dormant Mode")
        preferencesManager.isPaused = true
        preferencesManager.pausedAtMs = System.currentTimeMillis()

        // Stop the foreground service (stops polling, traps, tracking)
        PasaService.stop(context)

        return CommandResult(
            success = true,
            message = buildString {
                appendLine("⏸️ <b>PASA Dormant Mode ACTIVE</b>")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("All PASA monitoring activities have been suspended.")
                appendLine()
                appendLine("<b>Suspended:</b>")
                appendLine("• 🔕 Telegram command polling — paused")
                appendLine("• 📍 GPS / location tracking — stopped")
                appendLine("• 🛡️ Sensor traps (snatch, charger, pocket) — disarmed")
                appendLine("• 🪤 Ransomware canary guard — paused")
                appendLine("• 🛡️ OTP guard — paused")
                appendLine("• 🔌 USB autolock — paused")
                appendLine("• 🪙 Clipper guard — paused")
                appendLine()
                appendLine("<b>Still ACTIVE (Knox-enforced):</b>")
                appendLine("• ✅ All Anti-Tamper hardware restrictions")
                appendLine("• ✅ Device Owner status")
                appendLine("• ✅ USB data lock (if set)")
                appendLine("• ✅ Camera lock (if set)")
                appendLine("• ✅ DNS-over-TLS (if set)")
                appendLine()
                appendLine("To wake PASA and restore full operation:")
                appendLine("<code>/resume &lt;master_password&gt;</code>")
                appendLine()
                appendLine("⚠️ While dormant, Telegram commands will <b>not</b> be processed until /resume restarts polling.")
            }
        )
    }

    /** Called for /resume */
    suspend fun executeResume(args: List<String>): CommandResult {
        if (!preferencesManager.isPaused) {
            return CommandResult(
                success = true,
                message = buildString {
                    appendLine("✅ <b>PASA is Already Active</b>")
                    appendLine("━━━━━━━━━━━━━━━━━━━━")
                    appendLine("PASA is currently in full operation mode. No action needed.")
                    appendLine()
                    appendLine("Use <code>/pause</code> to enter Dormant Mode.")
                }
            )
        }

        val password = args.firstOrNull()?.trim()

        // /resume always requires Master Password (session was lost when polling stopped)
        val sessionOk = authManager.isSessionAuthenticated()
        val passwordOk = !password.isNullOrBlank() && authManager.verifyMasterPassword(password)

        if (!sessionOk && !passwordOk) {
            return CommandResult(
                success = false,
                message = buildString {
                    val pausedAt = preferencesManager.pausedAtMs
                    val elapsed = if (pausedAt > 0) {
                        val ms = System.currentTimeMillis() - pausedAt
                        formatElapsed(ms)
                    } else "unknown"
                    appendLine("⏸️ <b>PASA is in Dormant Mode</b>")
                    appendLine("━━━━━━━━━━━━━━━━━━━━")
                    appendLine("Dormant since: $elapsed ago")
                    appendLine()
                    appendLine("To wake PASA and restore full monitoring:")
                    appendLine("<code>/resume &lt;master_password&gt;</code>")
                    appendLine()
                    appendLine("All Knox restrictions remain active while dormant.")
                }
            )
        }

        // --- Exit Dormant Mode ---
        Log.w(TAG, "Exiting PASA Dormant Mode — restoring full operation")
        val pausedMs = if (preferencesManager.pausedAtMs > 0) {
            System.currentTimeMillis() - preferencesManager.pausedAtMs
        } else 0L

        preferencesManager.isPaused = false
        preferencesManager.pausedAtMs = 0L

        // Restart the foreground service — this re-starts polling, traps, and all subsystems
        PasaService.start(context)

        return CommandResult(
            success = true,
            message = buildString {
                appendLine("▶️ <b>PASA Dormant Mode DEACTIVATED</b>")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                if (pausedMs > 0) appendLine("Was dormant for: ${formatElapsed(pausedMs)}")
                appendLine()
                appendLine("<b>Restored:</b>")
                appendLine("• ✅ Telegram command polling — resumed")
                appendLine("• ✅ Sensor traps — re-armed (per saved settings)")
                appendLine("• ✅ GPS / tracking — resumed (if was active)")
                appendLine("• ✅ Ransomware canary — re-armed (if was active)")
                appendLine("• ✅ OTP guard — resumed (if was active)")
                appendLine("• ✅ USB autolock — resumed (if was active)")
                appendLine("• ✅ Clipper guard — resumed (if was active)")
                appendLine()
                appendLine("PASA is fully operational. 🛡️")
            }
        )
    }

    private fun buildStatus(): CommandResult {
        val isPaused = preferencesManager.isPaused
        val pausedAt = preferencesManager.pausedAtMs
        val elapsed = if (isPaused && pausedAt > 0) {
            "Dormant for: ${formatElapsed(System.currentTimeMillis() - pausedAt)}"
        } else ""
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        val msg = buildString {
            appendLine("⏸️ <b>PASA Dormant Mode Status</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            if (isPaused) {
                appendLine("Status: ⏸️ <b>DORMANT</b> — all monitoring suspended")
                if (pausedAt > 0) {
                    appendLine("Paused at: ${sdf.format(Date(pausedAt))}")
                    appendLine(elapsed)
                }
                appendLine()
                appendLine("To restore full operation:")
                appendLine("<code>/resume &lt;master_password&gt;</code>")
            } else {
                appendLine("Status: ✅ <b>ACTIVE</b> — full operation")
                appendLine()
                appendLine("To pause all monitoring:")
                appendLine("<code>/pause</code>")
            }
        }

        return CommandResult(
            success = true,
            message = msg,
            replyMarkup = if (isPaused) {
                InlineKeyboardMarkup(inlineKeyboard = listOf(
                    listOf(InlineKeyboardButton("▶️ Resume PASA", callbackData = "cmd:resume"))
                ))
            } else {
                InlineKeyboardMarkup(inlineKeyboard = listOf(
                    listOf(
                        InlineKeyboardButton("⏸️ Pause PASA", callbackData = "cmd:pause"),
                        InlineKeyboardButton("📊 Full Status", callbackData = "cmd:status")
                    )
                ))
            }
        )
    }

    private fun formatElapsed(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
