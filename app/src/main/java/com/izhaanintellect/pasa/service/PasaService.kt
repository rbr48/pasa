package com.izhaanintellect.pasa.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.izhaanintellect.pasa.PasaApp
import com.izhaanintellect.pasa.R
import com.izhaanintellect.pasa.bot.CommandExecutor
import com.izhaanintellect.pasa.bot.CommandParser
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.detection.MotionDetector
import com.izhaanintellect.pasa.detection.PasaWatchdogReceiver
import com.izhaanintellect.pasa.update.OtaUpdateManager
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.ui.SetupActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Core persistent foreground guardian service.
 * Manages Telegram bot long-polling, motion monitoring, and self-healing lifecycle.
 */
@AndroidEntryPoint
class PasaService : LifecycleService() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var pasaBackendApi: com.izhaanintellect.pasa.network.PasaBackendApi
    @Inject lateinit var commandParser: CommandParser
    @Inject lateinit var commandExecutor: CommandExecutor
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var motionDetector: MotionDetector
    @Inject lateinit var commandVerifier: com.izhaanintellect.pasa.crypto.CommandVerifier
    @Inject lateinit var otaUpdateManager: OtaUpdateManager
    @Inject lateinit var trapManager: com.izhaanintellect.pasa.detection.TrapManager

    companion object {
        private const val TAG = "PASA_Service"
        private const val NOTIFICATION_ID = 2001
        private const val MAX_BACKOFF_MS = 30000L
        private const val INITIAL_BACKOFF_MS = 3000L

        fun start(context: Context) {
            val intent = Intent(context, PasaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PasaService::class.java))
        }
    }

    private var pollingJob: Job? = null
    private var isRunning = false
    private var wakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pasa:guardian_wakelock").apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire()
                Log.d(TAG, "Guardian Partial WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Guardian Partial WakeLock released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing wake lock: ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "PasaService created")
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(TAG, "PasaService started")
        acquireWakeLock()

        // Start as foreground with Android 14+ foreground service types
        val notification = createNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback attempt to start foreground service failed", e2)
            }
        }

        if (!isRunning) {
            isRunning = true
            startPolling()
            motionDetector.startMonitoring()
            trapManager.startMonitoring()

            // Check for OTA updates on service start (with delay to avoid startup congestion)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    kotlinx.coroutines.delay(15000) // Wait 15s after startup
                    val result = otaUpdateManager.checkForUpdate()
                    if (result.updateAvailable && result.downloadUrl != null && result.sha256 != null) {
                        Log.i(TAG, "OTA update found on startup: v${result.versionName}")
                        otaUpdateManager.downloadAndInstall(result.downloadUrl, result.sha256)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Startup OTA check failed: ${e.message}")
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        Log.w(TAG, "PasaService destroying — scheduling watchdog restart")
        isRunning = false
        pollingJob?.cancel()
        motionDetector.stopMonitoring()
        trapManager.stopMonitoring()
        locationTracker.stopTracking()
        releaseWakeLock()
        scheduleRestart()
        super.onDestroy()
    }


    private var lastCommandReceivedAt = System.currentTimeMillis()

    private fun startPolling() {
        pollingJob = lifecycleScope.launch(Dispatchers.IO) {
            var currentBackoff = INITIAL_BACKOFF_MS
            var consecutiveErrors = 0

            Log.i(TAG, "Guardian polling loop active")

            while (isActive && isRunning) {
                try {
                    if (!preferencesManager.isConfigured()) {
                        delay(5000)
                        continue
                    }

                    var polledSuccessfully = false
                    var commandReceivedInCycle = false

                    // 1. Try VPS Backend polling first if enabled (HTTP long-polling)
                    if (preferencesManager.useBackendServer) {
                        try {
                            val pollResp = pasaBackendApi.pollCommands(preferencesManager.deviceId, timeout = 25)
                            if (pollResp.ok && !pollResp.commands.isNullOrEmpty()) {
                                commandReceivedInCycle = true
                                for (remoteCmd in pollResp.commands) {
                                    try {
                                        var commandToExecute = remoteCmd.command
                                        var argsToExecute = remoteCmd.args ?: emptyList()
                                        var chatIdToUse = remoteCmd.chatId

                                        // Ed25519 Cryptographic Envelope Verification (ASTRA Layer)
                                        val envelope = remoteCmd.envelope
                                        if (!envelope.isNullOrBlank()) {
                                            try {
                                                val verified = commandVerifier.verify(envelope)
                                                commandToExecute = "/" + verified.action.lowercase()
                                                argsToExecute = verified.args
                                                if (verified.chatId > 0) chatIdToUse = verified.chatId
                                                Log.i(TAG, "✅ Cryptographically verified command envelope: ${verified.action} (seq=${verified.sequence})")
                                            } catch (e: Exception) {
                                                Log.w(TAG, "🚨 Security rejection for command envelope ${remoteCmd.id}: ${e.message}")
                                                val rejectMsg = "⛔ Security Rejection: Command ${remoteCmd.command} rejected (${e.message})"
                                                commandExecutor.sendRejectionToBackend(remoteCmd.id, rejectMsg)
                                                continue
                                            }
                                        }

                                        val parsed = CommandParser.ParsedCommand(
                                            chatId = chatIdToUse,
                                            command = commandToExecute,
                                            args = argsToExecute,
                                            senderName = if (envelope.isNullOrBlank()) "VPS Gateway" else "VPS Signed (${remoteCmd.id.take(6)})",
                                            rawText = "$commandToExecute ${argsToExecute.joinToString(" ")}"
                                        )
                                        commandExecutor.executeRemoteCommand(parsed, remoteCmd.id)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error executing remote command ${remoteCmd.id}", e)
                                    }
                                }
                            }
                            polledSuccessfully = true
                        } catch (e: Exception) {
                            Log.w(TAG, "VPS backend poll warning: ${e.message}")
                        }
                    }

                    // 2. Direct Telegram API fallback (ONLY if backend server is NOT used, preventing 409 Conflict)
                    if (!polledSuccessfully && !preferencesManager.useBackendServer) {
                        val token = preferencesManager.botToken
                        val offset = preferencesManager.updateOffset

                        val response = telegramApi.getUpdates(
                            token = token,
                            offset = if (offset > 0) offset else null,
                            timeout = 25
                        )

                        if (response.ok && !response.result.isNullOrEmpty()) {
                            commandReceivedInCycle = true
                            for (update in response.result) {
                                try {
                                    val parsed = commandParser.parse(update)
                                    if (parsed != null) {
                                        commandExecutor.execute(parsed)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error executing update #${update.updateId}", e)
                                }
                                preferencesManager.updateOffset = update.updateId + 1
                            }
                        }
                    }

                    consecutiveErrors = 0
                    currentBackoff = INITIAL_BACKOFF_MS

                    if (commandReceivedInCycle) {
                        lastCommandReceivedAt = System.currentTimeMillis()
                        delay(500L) // Immediate follow-up for next queued command
                    } else if (polledSuccessfully) {
                        // Long-poll already waited on server; re-poll quickly to maintain active connection
                        delay(1500L)
                    } else {
                        delay(currentBackoff)
                    }

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    consecutiveErrors++
                    Log.e(TAG, "Polling error (#$consecutiveErrors): ${e.message}")
                    delay(currentBackoff)
                    currentBackoff = (currentBackoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SetupActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PasaApp.GUARDIAN_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_guardian_title))
            .setContentText(getString(R.string.notification_guardian_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun scheduleRestart() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, PasaWatchdogReceiver::class.java).apply {
                action = PasaWatchdogReceiver.ACTION_WATCHDOG_RESTART
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + 5000

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }

            Log.i(TAG, "Watchdog restart broadcast scheduled in 5s via PasaWatchdogReceiver")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule restart", e)
        }
    }
}

