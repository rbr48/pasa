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
    @Inject lateinit var geofenceManager: com.izhaanintellect.pasa.detection.GeofenceManager

    companion object {
        private const val TAG = "PASA_Service"
        private const val NOTIFICATION_ID = 2001
        private const val MAX_BACKOFF_MS = 30000L
        private const val INITIAL_BACKOFF_MS = 3000L

        private var serviceRef: java.lang.ref.WeakReference<PasaService>? = null

        val currentService: PasaService?
            get() = serviceRef?.get()

        fun elevateServiceToMicrophone() {
            serviceRef?.get()?.elevateToMicrophone()
        }

        fun demoteServiceFromMicrophone() {
            serviceRef?.get()?.demoteFromMicrophone()
        }

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

    fun acquireWakeLock(timeoutMs: Long = 60_000L) {
        try {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pasa:guardian_wakelock").apply {
                    setReferenceCounted(false)
                }
            }
            wakeLock?.acquire(timeoutMs)
            Log.d(TAG, "Guardian Partial WakeLock acquired with timeout: ${timeoutMs}ms")
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

    fun elevateToMicrophone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
                Log.i(TAG, "PasaService elevated to FOREGROUND_SERVICE_TYPE_MICROPHONE")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to elevate to MICROPHONE FGS: ${e.message}")
            }
        }
    }

    fun demoteFromMicrophone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
                Log.i(TAG, "PasaService demoted from FOREGROUND_SERVICE_TYPE_MICROPHONE")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to demote from MICROPHONE FGS: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceRef = java.lang.ref.WeakReference(this)
        Log.i(TAG, "PasaService created")
        acquireWakeLock(30_000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userManager = getSystemService(Context.USER_SERVICE) as? android.os.UserManager
        if (userManager != null && !userManager.isUserUnlocked) {
            Log.w(TAG, "Device is in Direct Boot mode (pre-first-unlock). Deferring full init.")
            val filter = android.content.IntentFilter(Intent.ACTION_USER_UNLOCKED)
            registerReceiver(object : android.content.BroadcastReceiver() {
                override fun onReceive(ctx: Context, broadcastIntent: Intent) {
                    Log.i(TAG, "User unlocked! Initializing full PASA services.")
                    unregisterReceiver(this)
                    start(ctx)
                }
            }, filter)
            return START_STICKY
        }

        super.onStartCommand(intent, flags, startId)
        serviceRef = java.lang.ref.WeakReference(this)
        Log.i(TAG, "PasaService started")
        acquireWakeLock(30_000L)

        // If Device Owner is active, ensure password reset escrow token is enrolled
        if (com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(this)) {
            com.izhaanintellect.pasa.admin.PasaDeviceAdmin.ensureResetPasswordToken(this, preferencesManager)
        }

        // Start as foreground with Android 14+ safe background foreground service types
        val notification = createNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
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
            if (Build.VERSION.SDK_INT >= 34 && e.javaClass.simpleName == "ForegroundServiceStartNotAllowedException") {
                Log.w(TAG, "FGS start blocked, scheduling retry via AlarmManager")
                val retryIntent = Intent(this, PasaService::class.java)
                val pi = PendingIntent.getService(this, 0, retryIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val am = getSystemService(AlarmManager::class.java)
                am?.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    android.os.SystemClock.elapsedRealtime() + 5000, pi)
            } else {
                Log.w(TAG, "startForeground initial start failed, falling back: ${e.message}")
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback attempt to start foreground service failed", e2)
                }
            }
        }

        if (!isRunning) {
            isRunning = true
            startPolling()
            motionDetector.startMonitoring()
            trapManager.startMonitoring()
            geofenceManager.startMonitoring()

            // Check for OTA updates on service start (notify owner if update is ready)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    kotlinx.coroutines.delay(15000) // Wait 15s after startup
                    val result = otaUpdateManager.checkForUpdate()
                    if (result.updateAvailable && result.versionName != null && preferencesManager.isConfigured()) {
                        Log.i(TAG, "OTA update found on startup: v${result.versionName}")
                        val notice = "🔄 <b>OTA Update Available</b>\n" +
                                "━━━━━━━━━━━━━━━━━━━━\n" +
                                "🆕 Version <b>v${result.versionName}</b> is available.\n" +
                                "Send <code>/update_confirm</code> to download and install."
                        telegramApi.sendMessage(
                            token = preferencesManager.botToken,
                            request = com.izhaanintellect.pasa.bot.SendMessageRequest(
                                chatId = preferencesManager.ownerChatIdLong,
                                text = notice
                            )
                        )
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
        serviceRef?.clear()
        serviceRef = null
        isRunning = false
        pollingJob?.cancel()
        motionDetector.stopMonitoring()
        trapManager.stopMonitoring()
        geofenceManager.stopMonitoring()
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
                                            } catch (e: com.izhaanintellect.pasa.crypto.DuplicateCommandException) {
                                                Log.i(TAG, "Command ${remoteCmd.id} was already executed previously. Acknowledging duplicate delivery to VPS.")
                                                commandExecutor.sendResponseToBackend(remoteCmd.id, "ALREADY_COMPLETED", null, null, null, null)
                                                continue
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
                                        val cmdLower = commandToExecute.lowercase()
                                        val commandTimeoutMs = when (cmdLower) {
                                            "/video", "/videocap", "/vr" -> 120_000L
                                            "/screenrecord", "/record_screen" -> 120_000L
                                            "/record", "/audio", "/mic" -> 150_000L
                                            "/livestream", "/stream" -> 360_000L
                                            "/shred" -> 120_000L
                                            "/screen_burst", "/burst" -> 90_000L
                                            else -> 60_000L
                                        }
                                        withTimeoutOrNull(commandTimeoutMs) {
                                            commandExecutor.executeRemoteCommand(parsed, remoteCmd.id)
                                        } ?: run {
                                            val deadlineSec = commandTimeoutMs / 1000
                                            Log.e(TAG, "Command ${remoteCmd.id} exceeded ${deadlineSec}s execution deadline!")
                                            commandExecutor.sendResponseToBackend(remoteCmd.id, "❌ Execution Timeout (${deadlineSec}s exceeded)", null, null, null, null)
                                        }
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

        return NotificationCompat.Builder(this, PasaApp.STEALTH_CHANNEL_ID)
            .setContentTitle("System Security Core")
            .setContentText("Active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
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

