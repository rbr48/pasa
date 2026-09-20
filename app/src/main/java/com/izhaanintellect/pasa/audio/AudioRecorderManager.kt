package com.izhaanintellect.pasa.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.izhaanintellect.pasa.service.PasaService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages ambient audio recording using Android MediaRecorder.
 * Features:
 * - Active WakeLock to prevent CPU suspend & zero-sample AudioFlinger muting
 * - FGS microphone elevation for Android 14-16 compliance
 * - High-gain ambient VOICE_RECOGNITION source with MIC fallback
 * - Explicit Mono AAC configuration to prevent hardware channel starvation
 */
@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PASA_Audio"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var activeWakeLock: PowerManager.WakeLock? = null
    private var isRecording = false

    suspend fun record(durationSeconds: Int): File? {
        if (!hasAudioPermission()) {
            Log.w(TAG, "Audio recording permission not granted")
            return null
        }

        if (isRecording) {
            Log.w(TAG, "Already recording — resetting active session")
            stopRecording()
        }

        // 1. Acquire WakeLock so CPU doesn't sleep during background recording
        acquireWakeLock(durationSeconds)

        // 2. Request Foreground Service microphone elevation (Android 14+)
        try {
            PasaService.currentService?.elevateToMicrophone()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to elevate PasaService to microphone: ${e.message}")
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(context.cacheDir, "pasa_audio_${timestamp}.m4a")

        return try {
            val initialized = initRecorder(outputFile, MediaRecorder.AudioSource.VOICE_RECOGNITION, durationSeconds) ||
                              initRecorder(outputFile, MediaRecorder.AudioSource.MIC, durationSeconds)

            if (!initialized || mediaRecorder == null) {
                throw IllegalStateException("Failed to initialize MediaRecorder with any audio source")
            }

            mediaRecorder?.start()
            isRecording = true
            Log.i(TAG, "Audio recording started: ${durationSeconds}s -> ${outputFile.name}")

            delay(durationSeconds * 1000L)
            stopRecording()

            if (outputFile.exists() && outputFile.length() > 0) {
                Log.i(TAG, "Audio recording complete (${outputFile.length()} bytes)")
                outputFile
            } else {
                Log.e(TAG, "Recorded audio file is empty or missing")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio recording failed", e)
            stopRecording()
            null
        } finally {
            releaseWakeLock()
        }
    }

    private fun initRecorder(outputFile: File, audioSource: Int, durationSeconds: Int): Boolean {
        return try {
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.apply {
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1) // Mono for universal hardware mic capture
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                setMaxDuration((durationSeconds + 2) * 1000)
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaRecorder error: what=$what extra=$extra")
                }
                prepare()
            }
            mediaRecorder = mr
            Log.d(TAG, "MediaRecorder successfully initialized with source=$audioSource")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to init MediaRecorder with source $audioSource: ${e.message}")
            try { mediaRecorder?.release() } catch (_: Exception) {}
            mediaRecorder = null
            false
        }
    }

    fun stopRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: RuntimeException) {
                        Log.w(TAG, "MediaRecorder stop failed (possibly too short)", e)
                    }
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock(durationSeconds: Int) {
        try {
            if (activeWakeLock == null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                activeWakeLock = pm?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "pasa:audio_recorder_wakelock"
                )
            }
            if (activeWakeLock?.isHeld == false) {
                activeWakeLock?.acquire((durationSeconds + 10) * 1000L)
                Log.d(TAG, "AudioRecorder WakeLock acquired for ${durationSeconds + 10}s")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire audio WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (activeWakeLock?.isHeld == true) {
                activeWakeLock?.release()
                Log.d(TAG, "AudioRecorder WakeLock released")
            }
        } catch (_: Exception) {}
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
