package com.izhaanintellect.pasa.commands

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles remote audible alarm / ring commands at maximum volume.
 */
@Singleton
class RingCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/ring"
    override val description = "Trigger loud emergency siren/alarm"
    override val usage = "/ring | /ring <seconds> | /ring stop"

    companion object {
        private const val TAG = "PASA_Ring"
        private var activePlayer: MediaPlayer? = null
        private var ringJob: Job? = null
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val sub = args.firstOrNull()?.lowercase()

        return when {
            sub == "stop" || sub == "off" -> stopAlarm()
            sub != null && sub.toIntOrNull() != null -> {
                val duration = sub.toInt().coerceIn(1, 300)
                startAlarm(duration)
            }
            else -> startAlarm(60) // Default 60 seconds
        }
    }

    private suspend fun startAlarm(durationSeconds: Int): CommandResult {
        return try {
            stopAlarmPlayer()

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val player = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            activePlayer = player
            Log.i(TAG, "Loud alarm playing for $durationSeconds seconds")

            ringJob = CoroutineScope(Dispatchers.IO).launch {
                delay(durationSeconds * 1000L)
                stopAlarmPlayer()
            }

            CommandResult(
                success = true,
                message = "🔊 Emergency alarm activated for $durationSeconds seconds at max volume."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alarm", e)
            CommandResult(success = false, message = "❌ Alarm playback failed: ${e.message}")
        }
    }

    private fun stopAlarm(): CommandResult {
        stopAlarmPlayer()
        return CommandResult(success = true, message = "🔇 Emergency alarm silenced.")
    }

    private fun stopAlarmPlayer() {
        try {
            ringJob?.cancel()
            ringJob = null
            activePlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            activePlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping player", e)
        }
    }
}
