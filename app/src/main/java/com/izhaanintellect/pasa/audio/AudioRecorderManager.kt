package com.izhaanintellect.pasa.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages ambient audio recording using Android MediaRecorder.
 * Records microphone audio for a specified duration and returns the saved file.
 */
@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PASA_Audio"
    }

    private var mediaRecorder: MediaRecorder? = null
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

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(context.cacheDir, "pasa_audio_${timestamp}.m4a")

        return try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                setMaxDuration(durationSeconds * 1000)
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaRecorder error: what=$what extra=$extra")
                }
                prepare()
                start()
            }

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
        }
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
