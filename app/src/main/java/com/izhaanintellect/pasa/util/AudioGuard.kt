package com.izhaanintellect.pasa.util

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Manages audio profile transitions for security activities.
 * Handles ringer mode save/restore and stream muting.
 */
object AudioGuard {

    private const val TAG = "PASA_AudioGuard"

    /**
     * Silences all audio streams. Returns the previous ringer mode for later restoration.
     */
    fun silenceAll(context: Context): Int {
        var previousMode = AudioManager.RINGER_MODE_NORMAL
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || nm?.isNotificationPolicyAccessGranted == true) {
                    previousMode = audioManager.ringerMode
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                } else {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Could not set silent mode: ${e.message}")
        }
        return previousMode
    }

    /**
     * Restores a previously saved ringer mode.
     */
    fun restore(context: Context, previousRingerMode: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.ringerMode = previousRingerMode
        } catch (_: Exception) {}
    }
}
