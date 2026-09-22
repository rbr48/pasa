package com.izhaanintellect.pasa.commands

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unlock Pattern Monitoring & Evidence Capture
 *
 * Detects failed unlock attempts and automatically captures evidence.
 * Thieves will try patterns/PINs - capture them in the act.
 */
@Singleton
class PatternGuardCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/pattern_guard"
    override val description = "Monitor failed unlock attempts and capture evidence"
    override val usage = "/pattern_guard [enable|disable|threshold|action|status]"

    companion object {
        private const val TAG = "PASA_PatternGuard"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull()?.lowercase() ?: "status"

        return when (action) {
            "enable", "on" -> enablePatternGuard()
            "disable", "off" -> disablePatternGuard()
            "threshold", "set_threshold" -> setThreshold(args.getOrNull(1)?.toIntOrNull() ?: 3)
            "action", "set_action" -> setAction(args.getOrNull(1) ?: "photo")
            "status" -> getPatternGuardStatus()
            else -> CommandResult(
                success = false,
                message = """
                    🔐 <b>Pattern Guard Command</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Usage:</b>
                    • <code>/pattern_guard enable</code> — Enable monitoring
                    • <code>/pattern_guard threshold 3</code> — Failed attempts before action
                    • <code>/pattern_guard action photo</code> — Auto-capture photos
                    • <code>/pattern_guard status</code> — Current state
                """.trimIndent()
            )
        }
    }

    private fun enablePatternGuard(): CommandResult {
        preferencesManager.isPatternGuardEnabled = true
        preferencesManager.patternGuardFailureCount = 0

        return CommandResult(
            success = true,
            message = """
                🟢 <b>Pattern Guard ENABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔍 <b>Monitoring:</b> ACTIVE

                ✓ Failed unlock attempts tracked
                ✓ After ${preferencesManager.patternGuardThreshold} failures: ${preferencesManager.patternGuardAction.uppercase()}
                ✓ Evidence will be captured automatically

                <i>Thieves will try patterns - you'll have proof.</i>
            """.trimIndent()
        )
    }

    private fun disablePatternGuard(): CommandResult {
        preferencesManager.isPatternGuardEnabled = false

        return CommandResult(
            success = true,
            message = """
                🔴 <b>Pattern Guard DISABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔍 <b>Monitoring:</b> INACTIVE

                Device is no longer capturing unlock attempt evidence.
            """.trimIndent()
        )
    }

    private fun setThreshold(threshold: Int): CommandResult {
        val actualThreshold = threshold.coerceIn(1, 10)
        preferencesManager.patternGuardThreshold = actualThreshold

        return CommandResult(
            success = true,
            message = """
                ⚙️ <b>Unlock Failure Threshold Updated</b>
                ━━━━━━━━━━━━━━━━━━━━
                🎯 <b>Threshold:</b> $actualThreshold failed attempts
                🎬 <b>Action:</b> ${preferencesManager.patternGuardAction.uppercase()}

                After $actualThreshold failed unlock attempts,
                the device will automatically ${preferencesManager.patternGuardAction}.
            """.trimIndent()
        )
    }

    private fun setAction(actionStr: String): CommandResult {
        val action = when (actionStr.lowercase()) {
            "photo", "snap", "capture" -> "photo"
            "video", "record" -> "video"
            "lock", "lockdown" -> "lock"
            else -> "photo"
        }

        preferencesManager.patternGuardAction = action

        return CommandResult(
            success = true,
            message = """
                🎬 <b>Unlock Attempt Action Updated</b>
                ━━━━━━━━━━━━━━━━━━━━
                🎯 <b>Action:</b> ${action.uppercase()}

                On ${preferencesManager.patternGuardThreshold} failed unlock attempts:
                ${when (action) {
                    "photo" -> "📸 Capture multiple photos (front & back camera)"
                    "video" -> "🎥 Record 30-second video from front camera"
                    "lock" -> "🔒 Lock device into Lost Mode"
                    else -> "Unknown action"
                }}
            """.trimIndent()
        )
    }

    private fun getPatternGuardStatus(): CommandResult {
        return CommandResult(
            success = true,
            message = """
                🔐 <b>Pattern Guard Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔍 <b>Monitoring:</b> ${if (preferencesManager.isPatternGuardEnabled) "🟢 ENABLED" else "🔴 DISABLED"}
                🎯 <b>Threshold:</b> ${preferencesManager.patternGuardThreshold} failed attempts
                🎬 <b>Action:</b> ${preferencesManager.patternGuardAction.uppercase()}
                📊 <b>Current Failures:</b> ${preferencesManager.patternGuardFailureCount}

                <b>How it works:</b>
                When someone tries to unlock the device:
                • Each failed attempt is counted
                • After ${preferencesManager.patternGuardThreshold} failures, ${preferencesManager.patternGuardAction} triggers
                • Owner receives alert with evidence

                <i>To enable: /pattern_guard enable</i>
            """.trimIndent()
        )
    }
}
