package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tactile Device Location via Vibration
 *
 * Help locate device by vibrating pattern even when device is silenced.
 * Useful for finding phone in couch, bag, or nearby without attracting attention.
 *
 * Commands:
 *   /vibrate_pulse [count]       — Pulse N times (default 10)
 *   /vibrate_pulse sos           — SOS morse code pattern
 *   /vibrate_pulse location      — Continuous vibration pattern
 *   /vibrate_pulse stop          — Stop vibration immediately
 */
@Singleton
class VibratePulseCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/vibrate_pulse"
    override val description = "Locate device via vibration patterns (even when silenced)"
    override val usage = "/vibrate_pulse [count|sos|location|stop]"

    companion object {
        private const val TAG = "PASA_Vibrate"
    }

    private var vibrateJob: kotlinx.coroutines.Job? = null

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val pattern = args.firstOrNull()?.lowercase() ?: "pulse"

        return when (pattern) {
            "stop", "off", "cancel" -> stopVibration()
            "sos" -> sosPattern()
            "location", "continuous", "find" -> locationPattern()
            else -> {
                val count = pattern.toIntOrNull() ?: 10
                pulsePattern(count)
            }
        }
    }

    private suspend fun pulsePattern(count: Int): CommandResult {
        val actualCount = count.coerceIn(1, 100)

        vibrateJob?.cancel()
        vibrateJob = CoroutineScope(Dispatchers.IO).launch {
            for (i in 1..actualCount) {
                if (!isActive) break

                vibrate(200) // 200ms vibration
                delay(300) // 300ms pause

                Log.d(TAG, "Pulse $i/$actualCount")
            }
        }

        return CommandResult(
            success = true,
            message = """
                📳 <b>Vibrate Pulse Started</b>
                ━━━━━━━━━━━━━━━━━━━━
                ⏱️ <b>Pattern:</b> Pulse × $actualCount
                ⏰ <b>Duration:</b> ~${actualCount * 5} seconds
                🔇 <b>Works when silenced:</b> ✅ YES

                The device will vibrate $actualCount times with 300ms pauses.
                Useful for finding phone in couch, bag, or nearby location.

                <i>To stop: /vibrate_pulse stop</i>
            """.trimIndent()
        )
    }

    private suspend fun sosPattern(): CommandResult {
        vibrateJob?.cancel()
        vibrateJob = CoroutineScope(Dispatchers.IO).launch {
            // S: 3 short
            repeat(3) {
                vibrate(100)
                delay(200)
            }
            delay(500) // Gap between letters

            // O: 3 long
            repeat(3) {
                vibrate(300)
                delay(200)
            }
            delay(500)

            // S: 3 short
            repeat(3) {
                vibrate(100)
                delay(200)
            }

            Log.d(TAG, "SOS pattern completed")
        }

        return CommandResult(
            success = true,
            message = """
                🆘 <b>SOS Vibration Pattern</b>
                ━━━━━━━━━━━━━━━━━━━━
                📳 <b>Pattern:</b> SOS Morse Code
                ⏰ <b>Duration:</b> ~10 seconds

                ✓ Short-Short-Short (S)
                ✓ Long-Long-Long (O)
                ✓ Short-Short-Short (S)

                The distinctive SOS pattern helps identify the device
                even if it's hidden or in a noisy environment.

                <i>To stop: /vibrate_pulse stop</i>
            """.trimIndent()
        )
    }

    private suspend fun locationPattern(): CommandResult {
        vibrateJob?.cancel()
        vibrateJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                vibrate(500) // 500ms strong vibration
                delay(300) // 300ms pause
            }
        }

        return CommandResult(
            success = true,
            message = """
                📍 <b>Location Pattern - Continuous Vibration</b>
                ━━━━━━━━━━━━━━━━━━━━
                📳 <b>Pattern:</b> Continuous pulses
                🔁 <b>Mode:</b> Repeating

                ✓ Strong 500ms vibrations with 300ms pauses
                ✓ Continues until stopped
                ✓ Easy to locate by touch or sound

                Use this pattern to systematically search a location
                by following the vibration intensity.

                <i>To stop: /vibrate_pulse stop</i>
            """.trimIndent()
        )
    }

    private fun stopVibration(): CommandResult {
        vibrateJob?.cancel()
        vibrateJob = null

        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.cancel()
        } catch (_: Exception) {}

        return CommandResult(
            success = true,
            message = """
                ⏹️ <b>Vibration Stopped</b>
                ━━━━━━━━━━━━━━━━━━━━
                📳 <b>Status:</b> SILENT

                All vibration patterns have been cancelled.
            """.trimIndent()
        )
    }

    private fun vibrate(durationMs: Long) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate failed: ${e.message}", e)
        }
    }
}
