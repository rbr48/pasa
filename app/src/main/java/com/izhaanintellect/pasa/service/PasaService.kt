package com.izhaanintellect.pasa.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
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
import com.izhaanintellect.pasa.bot.TelegramMenuManager
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.detection.MotionDetector
import com.izhaanintellect.pasa.detection.PasaWatchdogReceiver
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
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
    @Inject lateinit var usbAutolockManager: com.izhaanintellect.pasa.security.UsbAutolockManager
    @Inject lateinit var clipperGuardManager: com.izhaanintellect.pasa.security.ClipperGuardManager
    @Inject lateinit var ransomwareCanaryManager: com.izhaanintellect.pasa.security.RansomwareCanaryManager
    @Inject lateinit var otpInterceptionGuardManager: com.izhaanintellect.pasa.security.OtpInterceptionGuardManager
    @Inject lateinit var telegramMenuManager: TelegramMenuManager

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

        fun elevateServiceToCamera() {
            serviceRef?.get()?.elevateToCamera()
        }

        fun demoteServiceFromCamera() {
            serviceRef?.get()?.demoteFromCamera()
        }

        fun elevateServiceToCameraAndMicrophone() {
            serviceRef?.get()?.elevateToCameraAndMicrophone()
        }

        fun demoteServiceFromCameraAndMicrophone() {
            serviceRef?.get()?.demoteFromCameraAndMicrophone()
        }

        const val ACTION_RESTART_POLLING = "com.izhaanintellect.pasa.action.RESTART_POLLING"

        fun restartPolling(context: Context) {
            val intent = Intent(context, PasaService::class.java).apply {
                action = ACTION_RESTART_POLLING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
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
    private var thermalAndHeartbeatReceiver: BroadcastReceiver? = null
    private var deadManJob: Job? = null
    private var lastThermalAlertTime = 0L

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

    fun elevateToCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                )
                Log.i(TAG, "PasaService elevated to FOREGROUND_SERVICE_TYPE_CAMERA")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to elevate to CAMERA FGS: ${e.message}")
            }
        }
    }

    fun demoteFromCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
                Log.i(TAG, "PasaService demoted from FOREGROUND_SERVICE_TYPE_CAMERA")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to demote from CAMERA FGS: ${e.message}")
            }
        }
    }

    fun elevateToCameraAndMicrophone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
                Log.i(TAG, "PasaService elevated to FOREGROUND_SERVICE_TYPE_CAMERA and MICROPHONE")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to elevate to CAMERA+MICROPHONE FGS: ${e.message}")
            }
        }
    }

    fun demoteFromCameraAndMicrophone() {
        demoteFromCamera()
    }

    private fun promoteToForeground() {
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
                PasaWatchdogReceiver.scheduleHeartbeat(this, 3000L)
            } else {
                Log.w(TAG, "startForeground initial start failed, falling back: ${e.message}")
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback attempt to start foreground service failed", e2)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceRef = java.lang.ref.WeakReference(this)
        Log.i(TAG, "PasaService created")
        acquireWakeLock(60_000L)
        promoteToForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userManager = getSystemService(Context.USER_SERVICE) as? android.os.UserManager
        val isDirectBoot = userManager != null && !userManager.isUserUnlocked

        if (isDirectBoot) {
            Log.w(TAG, "Device is in Direct Boot mode (pre-first-unlock). Operating in Sovereign Direct Boot mode.")
            val filter = android.content.IntentFilter(Intent.ACTION_USER_UNLOCKED)
            registerReceiver(object : android.content.BroadcastReceiver() {
                override fun onReceive(ctx: Context, broadcastIntent: Intent) {
                    Log.i(TAG, "User unlocked! Transitioning from Direct Boot to Full Sovereign mode.")
                    try { unregisterReceiver(this) } catch (_: Exception) {}
                    onUserUnlocked()
                }
            }, filter)
        }

        super.onStartCommand(intent, flags, startId)
        serviceRef = java.lang.ref.WeakReference(this)
        Log.i(TAG, "PasaService started (isDirectBoot=$isDirectBoot)")
        acquireWakeLock(60_000L)

        // Schedule recurring watchdog heartbeat pulse
        PasaWatchdogReceiver.scheduleHeartbeat(this)

        // If Device Owner is active and user unlocked, ensure password reset escrow token is enrolled
        if (!isDirectBoot && com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(this)) {
            com.izhaanintellect.pasa.admin.PasaDeviceAdmin.ensureResetPasswordToken(this, preferencesManager)

            // Ensure status bar is enabled if neither Lost Mode nor Fake Shutdown is active
            if (!preferencesManager.isLostModeActive && !preferencesManager.isFakeShutdownActive) {
                try {
                    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                    val component = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.getComponentName(this)
                    dpm?.setStatusBarDisabled(component, false)
                } catch (_: Exception) {}
            }
        }

        // Ensure foreground promotion is held
        promoteToForeground()

        if (intent?.action == ACTION_RESTART_POLLING) {
            Log.i(TAG, "PasaService: ACTION_RESTART_POLLING received. Re-arming polling loop with latest credentials.")
            if (!isRunning) {
                isRunning = true
                motionDetector.startMonitoring()
                trapManager.startMonitoring()
                if (!isDirectBoot) geofenceManager.startMonitoring()
                registerHardwareMonitors()
                startDeadManWatchdog()
                usbAutolockManager.startMonitoring()
                clipperGuardManager.startMonitoring()
                ransomwareCanaryManager.startMonitoring()
                otpInterceptionGuardManager.startMonitoring()
            }
            pollingJob?.cancel()
            pollingJob = null
            startPolling()
            return START_STICKY
        }

        if (!isRunning) {
            isRunning = true
            startPolling()
            motionDetector.startMonitoring()
            trapManager.startMonitoring()
            if (!isDirectBoot) geofenceManager.startMonitoring()
            registerHardwareMonitors()
            startDeadManWatchdog()
            usbAutolockManager.startMonitoring()
            clipperGuardManager.startMonitoring()
            ransomwareCanaryManager.startMonitoring()
            otpInterceptionGuardManager.startMonitoring()
            if (!isDirectBoot) {
                onUserUnlocked()
            }
        }

        return START_STICKY
    }

    private fun onUserUnlocked() {
        Log.i(TAG, "Executing post-unlock background sync and escrow check")
        preferencesManager.syncToDeviceProtectedStorage()
        if (com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(this)) {
            com.izhaanintellect.pasa.admin.PasaDeviceAdmin.ensureResetPasswordToken(this, preferencesManager)
            com.izhaanintellect.pasa.admin.PasaDeviceAdmin.selfHealPermissions(this)
        }
        try {
            geofenceManager.startMonitoring()
        } catch (e: Exception) {
            Log.w(TAG, "Post-unlock geofence start error: ${e.message}")
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                delay(15000)
                val result = otaUpdateManager.checkForUpdate()
                if (result.updateAvailable && result.versionName != null && preferencesManager.isConfigured()) {
                    val notice = "🔄 <b>OTA Update Available</b>\n━━━━━━━━━━━━━━━━━━━━\n🆕 Version <b>v${result.versionName}</b> is available.\nSend <code>/update_confirm</code> to download and install."
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
        deadManJob?.cancel()
        thermalAndHeartbeatReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        thermalAndHeartbeatReceiver = null
        motionDetector.stopMonitoring()
        trapManager.stopMonitoring()
        geofenceManager.stopMonitoring()
        usbAutolockManager.stopMonitoring()
        clipperGuardManager.stopMonitoring()
        ransomwareCanaryManager.disarmCanaryTrap()
        otpInterceptionGuardManager.stopMonitoring()
        locationTracker.stopTracking()
        releaseWakeLock()
        scheduleRestart()
        super.onDestroy()
    }

    fun isServiceActive(): Boolean = isRunning && (pollingJob?.isActive == true)

    private var lastSuccessfulPollTimestamp = System.currentTimeMillis()

    fun verifyPollingHealth() {
        val elapsed = System.currentTimeMillis() - lastSuccessfulPollTimestamp
        if (elapsed > 180_000L) { // 3 minutes without a successful poll
            Log.w(TAG, "Polling loop appear stalled ($elapsed ms since last poll). Re-arming polling loop.")
            acquireWakeLock(30_000L)
            pollingJob?.cancel()
            pollingJob = null
            startPolling()
        } else {
            Log.d(TAG, "PasaService health OK. Last poll was ${elapsed / 1000}s ago.")
        }
    }

    private var lastCommandReceivedAt = System.currentTimeMillis()

    private fun startPolling() {
        pollingJob = lifecycleScope.launch(Dispatchers.IO) {
            var currentBackoff = INITIAL_BACKOFF_MS
            var consecutiveErrors = 0

            Log.i(TAG, "Guardian polling loop active")

            // Automatically sync official bot commands to Telegram cloud menu on startup
            if (preferencesManager.isConfigured()) {
                try {
                    telegramMenuManager.syncBotCommands(telegramApi)
                } catch (e: Exception) {
                    Log.w(TAG, "Initial bot commands cloud sync: ${e.message}")
                }
            }

            while (isActive && isRunning) {
                try {
                    if (!preferencesManager.isConfigured()) {
                        delay(5000)
                        continue
                    }

                    var polledSuccessfully = false
                    var commandReceivedInCycle = false

                    // Dual-Channel C2: Poll VPS backend gateway when configured, fallback to direct Telegram
                    val shouldUseBackend = false
                    if (shouldUseBackend) {
                        try {
                            val pollResp = pasaBackendApi.pollCommands(preferencesManager.deviceId, timeout = 25)
                            if (pollResp.ok && !pollResp.commands.isNullOrEmpty()) {
                                // Dual-Lane Priority Dispatcher: Emergency defense commands execute before heavy media forensics
                                val sortedCommands = pollResp.commands.sortedByDescending { cmd ->
                                    val c = cmd.command.lowercase().trim()
                                    when {
                                        c.startsWith("/lock") || c.startsWith("/unlock") || c.startsWith("/wipe") ||
                                        c.startsWith("/ring_stop") || c.startsWith("/wake") || c.startsWith("/fakeshutdown") ||
                                        c.startsWith("/stopstream") || c.startsWith("/antitamper") || c.startsWith("/usb_lock") -> 100
                                        c.startsWith("/ring") || c.startsWith("/locate") || c.startsWith("/status") -> 50
                                        else -> 10
                                    }
                                }
                                for (remoteCmd in sortedCommands) {
                                    try {
                                        var commandToExecute = remoteCmd.command
                                        var argsToExecute = remoteCmd.args ?: emptyList()
                                        var chatIdToUse = remoteCmd.chatId

                                        // Ed25519 Cryptographic Envelope Verification (ASTRA Layer)
                                        // Fail-Closed Security Policy: Reject any remote command lacking a cryptographic signature
                                        val envelope = remoteCmd.envelope
                                        if (envelope.isNullOrBlank()) {
                                            Log.w(TAG, "🚨 Security rejection: Unsigned command ${remoteCmd.id} (${remoteCmd.command}) rejected. Only cryptographically signed envelopes are accepted.")
                                            commandExecutor.sendRejectionToBackend(remoteCmd.id, "⛔ Security Rejection: Missing cryptographic envelope signature")
                                            continue
                                        }

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

                                        val parsed = CommandParser.ParsedCommand(
                                            chatId = chatIdToUse,
                                            command = commandToExecute,
                                            args = argsToExecute,
                                            senderName = "VPS Signed (${remoteCmd.id.take(6)})",
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

                    // 2. Direct Telegram API fallback (if backend poll failed OR backend server is not configured)
                    if (!polledSuccessfully) {
                        val token = preferencesManager.botToken
                        val offset = preferencesManager.updateOffset

                        val response = telegramApi.getUpdates(
                            token = token,
                            offset = if (offset > 0) offset else null,
                            timeout = 20
                        )

                        if (response.ok) {
                            polledSuccessfully = true
                            lastSuccessfulPollTimestamp = System.currentTimeMillis()
                            consecutiveErrors = 0
                            currentBackoff = INITIAL_BACKOFF_MS

                            if (!response.result.isNullOrEmpty()) {
                                commandReceivedInCycle = true
                                acquireWakeLock(90_000L) // Keep CPU awake while processing commands and uploading media
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
                    }

                    if (commandReceivedInCycle) {
                        lastCommandReceivedAt = System.currentTimeMillis()
                        preferencesManager.lastOwnerHeartbeatTime = System.currentTimeMillis()
                        delay(200L) // Immediate follow-up for next queued command
                    } else if (polledSuccessfully) {
                        // Long-poll completed normally with no messages; immediately re-poll
                        delay(300L)
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

    private fun registerHardwareMonitors() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        thermalAndHeartbeatReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_USER_PRESENT -> {
                        preferencesManager.lastOwnerHeartbeatTime = System.currentTimeMillis()
                        Log.d(TAG, "Device physically unlocked (USER_PRESENT) — Dead Man's Switch heartbeat refreshed")
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        checkThermalAnomaly(intent)
                    }
                }
            }
        }

        try {
            registerReceiver(thermalAndHeartbeatReceiver, filter)
            Log.i(TAG, "Hardware thermal & heartbeat receiver registered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register thermal/heartbeat receiver: ${e.message}")
        }
    }

    private fun checkThermalAnomaly(intent: Intent) {
        if (!preferencesManager.isThermalTrapEnabled) return
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        val isLocked = km?.isKeyguardLocked == true || preferencesManager.isLostModeActive
        if (!isLocked) return

        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        if (tempRaw <= 0) return
        val tempC = tempRaw / 10
        val threshold = preferencesManager.thermalTrapThresholdCelsius

        if (tempC >= threshold) {
            val now = System.currentTimeMillis()
            if (now - lastThermalAlertTime > 300_000L) { // 5-min alert cooldown
                lastThermalAlertTime = now
                Log.w(TAG, "🔥 CRITICAL THERMAL ANOMALY: Battery temp = ${tempC}°C >= ${threshold}°C while locked!")
                lifecycleScope.launch(Dispatchers.IO) {
                    handleThermalAlert(tempC, threshold)
                }
            }
        }
    }

    private suspend fun handleThermalAlert(tempC: Int, threshold: Int) {
        // 1. Instantly sever USB Data Pins via Knox Device Owner (Android 12+) to block EDL 9008 / BROM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(this)
        ) {
            try {
                com.izhaanintellect.pasa.admin.PasaDeviceAdmin.setUsbDataSignaling(this, false)
                Log.i(TAG, "Hardware USB data pins severed via Thermal Anomaly Trap")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sever USB data pins: ${e.message}")
            }
        }

        // 2. Lock screen / Enforce Lost Mode
        try {
            if (com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(this)) {
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                dpm?.lockNow()
            }
        } catch (_: Exception) {}

        // 3. Stealth front photo
        var photoFile: File? = null
        try {
            photoFile = com.izhaanintellect.pasa.camera.StealthCaptureBridge.capturePhoto(this, useFront = true, timeoutMs = 8000L).file
        } catch (e: Exception) {
            Log.w(TAG, "Thermal trap mugshot capture failed: ${e.message}")
        }

        // 4. Send Telegram SOS
        val alertText = "🔥 <b>CRITICAL HARDWARE THERMAL ANOMALY DETECTED!</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "🌡️ Battery Temperature: <b>${tempC}°C</b> (Threshold: ${threshold}°C)\n" +
                "⚠️ <b>Suspected Threat:</b> Back-cover heat-gun attack (perpetrator attempting physical access to Qualcomm 9008 EDL / MediaTek BROM test points).\n\n" +
                "🛡️ <b>Autonomous Defenses Deployed:</b>\n" +
                "• Hardware USB Data Pins physically severed (anti-EDL/BROM)\n" +
                "• Knox screen lock enforced\n" +
                "• Intruder mugshot captured"

        try {
            val chatId = preferencesManager.ownerChatIdLong
            val botToken = preferencesManager.botToken
            if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
                val reqBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("photo", photoFile.name, reqBody)
                val chatIdBody = chatId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val captionBody = alertText.toRequestBody("text/plain".toMediaTypeOrNull())
                telegramApi.sendPhoto(botToken, chatIdBody, part, captionBody)
            } else {
                telegramApi.sendMessage(
                    botToken,
                    com.izhaanintellect.pasa.bot.SendMessageRequest(
                        chatId = chatId,
                        text = alertText
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send thermal anomaly Telegram alert: ${e.message}")
        } finally {
            try { photoFile?.delete() } catch (_: Exception) {}
        }
    }

    private fun startDeadManWatchdog() {
        deadManJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive && isRunning) {
                delay(300_000L) // Check every 5 minutes
                try {
                    if (!preferencesManager.isDeadManSwitchEnabled) continue
                    if (!com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(this@PasaService)) continue

                    val km = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                    val isLocked = km?.isKeyguardLocked == true || preferencesManager.isLostModeActive
                    if (!isLocked) {
                        preferencesManager.lastOwnerHeartbeatTime = System.currentTimeMillis()
                        continue
                    }

                    val lastHeartbeat = preferencesManager.lastOwnerHeartbeatTime
                    val timeoutMs = preferencesManager.deadManTimeoutHours * 3600 * 1000L
                    val elapsed = System.currentTimeMillis() - lastHeartbeat

                    if (elapsed >= timeoutMs) {
                        Log.e(TAG, "💀 DEAD MAN'S SWITCH EXPIRED! Elapsed: ${elapsed / 3600000}h >= ${preferencesManager.deadManTimeoutHours}h. PURGING DEVICE DATA NOW!")
                        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                        dpm?.wipeData(0)
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in Dead Man's Switch watchdog: ${e.message}")
                }
            }
        }
    }
}

