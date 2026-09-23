package com.izhaanintellect.pasa.bot

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.commands.*
import com.izhaanintellect.pasa.data.CommandLog
import com.izhaanintellect.pasa.data.CommandLogDao
import com.izhaanintellect.pasa.data.PendingUpload
import com.izhaanintellect.pasa.data.PendingUploadDao
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.EncryptionManager
import com.izhaanintellect.pasa.worker.EvidenceUploadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates, routes, and executes incoming Telegram commands.
 * Handles photo/audio/location dispatch and records audit logs.
 */
@Singleton
class CommandExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingUploadDao: PendingUploadDao,
    private val encryptionManager: EncryptionManager,
    private val authManager: AuthManager,
    private val preferencesManager: PreferencesManager,
    private val commandLogDao: CommandLogDao,
    private val telegramApi: TelegramApi,
    private val lockCommand: LockCommand,
    private val wipeCommand: WipeCommand,
    private val locateCommand: LocateCommand,
    private val trackCommand: TrackCommand,
    private val snapCommand: SnapCommand,
    private val screenshotCommand: ScreenshotCommand,
    private val screenBurstCommand: ScreenBurstCommand,
    private val screenRecordCommand: ScreenRecordCommand,
    private val recordCommand: RecordCommand,
    private val ringCommand: RingCommand,
    private val statusCommand: StatusCommand,
    private val infoCommand: InfoCommand,
    private val networkCommand: NetworkCommand,
    private val appManageCommand: AppManageCommand,
    private val stealthCommand: StealthCommand,
    private val helpCommand: HelpCommand,
    private val messageCommand: MessageCommand,
    private val clipboardCommand: ClipboardCommand,
    private val videoCommand: VideoCommand,
    private val unlockCommand: UnlockCommand,
    private val deviceOwnerCommand: DeviceOwnerCommand,
    private val fakeShutdownCommand: FakeShutdownCommand,
    private val checkUpdateCommand: CheckUpdateCommand,
    private val duressPinCommand: com.izhaanintellect.pasa.commands.DuressPinCommand,
    private val trapCommand: com.izhaanintellect.pasa.commands.TrapCommand,
    private val shredCommand: com.izhaanintellect.pasa.commands.ShredCommand,
    private val geofenceCommand: com.izhaanintellect.pasa.commands.GeofenceCommand,
    private val smsSetupCommand: com.izhaanintellect.pasa.commands.SmsSetupCommand,
    private val historyCommand: HistoryCommand,
    private val contactsCommand: ContactsCommand,
    private val callLogCommand: CallLogCommand,
    private val smsLogCommand: SmsLogCommand,
    private val sendSmsCommand: com.izhaanintellect.pasa.commands.SendSmsCommand,
    private val notificationToggleCommand: com.izhaanintellect.pasa.commands.NotificationToggleCommand,
    private val simCommand: com.izhaanintellect.pasa.commands.SimCommand,
    private val selfTestCommand: com.izhaanintellect.pasa.commands.SelfTestCommand,
    private val setOsPinCommand: com.izhaanintellect.pasa.commands.SetOsPinCommand,
    private val setMasterPinCommand: com.izhaanintellect.pasa.commands.SetMasterPinCommand,
    private val antiTamperCommand: com.izhaanintellect.pasa.commands.AntiTamperCommand,
    private val usbLockCommand: com.izhaanintellect.pasa.commands.UsbLockCommand,
    private val selfHealCommand: com.izhaanintellect.pasa.commands.SelfHealCommand,
    private val freezeCommand: com.izhaanintellect.pasa.commands.FreezeCommand,
    private val biometricsCommand: com.izhaanintellect.pasa.commands.BiometricsCommand,
    private val liveStreamCommand: LiveStreamCommand,
    private val stopStreamCommand: StopStreamCommand,
    private val liveStreamDiagnosticsCommand: com.izhaanintellect.pasa.commands.LiveStreamDiagnosticsCommand,
    private val dnsCommand: com.izhaanintellect.pasa.commands.DnsCommand,
    private val towerCommand: com.izhaanintellect.pasa.commands.TowerCommand,
    private val simLockCommand: com.izhaanintellect.pasa.commands.SimLockCommand,
    private val vibratePulseCommand: com.izhaanintellect.pasa.commands.VibratePulseCommand,
    private val patternGuardCommand: com.izhaanintellect.pasa.commands.PatternGuardCommand,
    private val appFirewallCommand: com.izhaanintellect.pasa.commands.AppFirewallCommand,
    private val batteryAlertCommand: com.izhaanintellect.pasa.commands.BatteryAlertCommand,
    private val tamperDetectionCommand: com.izhaanintellect.pasa.commands.TamperDetectionCommand,
    private val deadDropCommand: com.izhaanintellect.pasa.commands.DeadDropCommand,
    private val hardenBootCommand: com.izhaanintellect.pasa.commands.HardenBootCommand,
    private val factoryResetDefenseCommand: com.izhaanintellect.pasa.commands.FactoryResetDefenseCommand,
    private val licenseCommand: com.izhaanintellect.pasa.commands.LicenseCommand,
    private val licenseManager: com.izhaanintellect.pasa.security.LicenseManager,
    private val pasaBackendApi: com.izhaanintellect.pasa.network.PasaBackendApi,
    private val cameraLockCommand: com.izhaanintellect.pasa.commands.CameraLockCommand,
    private val peripheralLockCommand: com.izhaanintellect.pasa.commands.PeripheralLockCommand,
    private val lockscreenInfoCommand: com.izhaanintellect.pasa.commands.LockscreenInfoCommand,
    private val wifiProvisionCommand: com.izhaanintellect.pasa.commands.WifiProvisionCommand,
    private val securityAuditCommand: com.izhaanintellect.pasa.commands.SecurityAuditCommand
) {
    companion object {
        private const val TAG = "PASA_Executor"
    }

    suspend fun execute(parsed: CommandParser.ParsedCommand): String {
        Log.i(TAG, "Command received: '${parsed.command}' from chat ${parsed.chatId}")

        // 1. Authorization Verification
        if (!authManager.isAuthorizedChat(parsed.chatId)) {
            Log.w(TAG, "Rejected unauthorized command '${parsed.command}' from chat ${parsed.chatId}")
            logExecution(parsed, "REJECTED", "Unauthorized access denied")
            return "⛔ Access Denied: This PASA instance is configured for a different administrator."
        }

        // 2. License Tier Feature Gating
        var licenseRejection = licenseManager.checkAccess(parsed.command)
        if (licenseRejection != null) {
            // Attempt an immediate refresh from backend in case the device was recently licensed
            licenseManager.refreshIfStale(force = true)
            licenseRejection = licenseManager.checkAccess(parsed.command)
        }
        if (licenseRejection != null) {
            Log.w(TAG, "Command '${parsed.command}' blocked by license gating")
            sendText(parsed.chatId, licenseRejection)
            logExecution(parsed, "BLOCKED", licenseRejection)
            return licenseRejection
        }

        // 3. Command Lookup
        val handler = resolveHandler(parsed.command)
        if (handler == null) {
            val response = "❓ Unknown command: <code>${parsed.command}</code>\nSend <code>/help</code> for available commands."
            sendText(parsed.chatId, response)
            logExecution(parsed, "FAILED", response)
            return response
        }

        // 4. Execution
        return try {
            val result = handler.execute(parsed.args, parsed.chatId)

            // Deliver text response
            sendText(parsed.chatId, result.message)

            // Deliver photo if generated (encrypted into vault & queued in WorkManager)
            result.photoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    val (encFile, _) = enqueueAndEncryptEvidence(null, file, "PHOTO")
                    sendPhoto(parsed.chatId, encFile, "📸 Captured photo")
                }
            }

            // Deliver audio if recorded (encrypted into vault & queued in WorkManager)
            result.audioFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    val (encFile, _) = enqueueAndEncryptEvidence(null, file, "AUDIO")
                    sendAudio(parsed.chatId, encFile, "🎙️ Audio recording")
                }
            }

            // Deliver video if recorded (encrypted into vault & queued in WorkManager)
            result.videoFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    val (encFile, _) = enqueueAndEncryptEvidence(null, file, "VIDEO")
                    sendVideo(parsed.chatId, encFile, "🎥 Captured video")
                }
            }

            // Deliver pin location if acquired
            result.location?.let { (lat, lng) ->
                sendLocation(parsed.chatId, lat, lng)
            }

            val status = if (result.success) "SUCCESS" else "FAILED"
            logExecution(parsed, status, result.message)
            result.message

        } catch (e: Exception) {
            Log.e(TAG, "Command execution failure", e)
            val errorMsg = "❌ Execution Error: ${e.localizedMessage ?: "Unexpected failure"}"
            sendText(parsed.chatId, errorMsg)
            logExecution(parsed, "FAILED", errorMsg)
            errorMsg
        }
    }

    private suspend fun enqueueAndEncryptEvidence(
        commandId: String?,
        file: File,
        fileType: String
    ): Pair<File, String> {
        return try {
            val uploadId = "up_${System.currentTimeMillis()}_${file.nameWithoutExtension}"
            val encFile = File(file.parentFile, "${file.nameWithoutExtension}.enc")
            encryptionManager.encryptEvidenceVaultFile(file, encFile)
            val pending = PendingUpload(
                id = uploadId,
                commandId = commandId ?: "",
                fileType = fileType,
                filePath = encFile.absolutePath,
                isEncrypted = true,
                status = "PENDING"
            )
            pendingUploadDao.insert(pending)
            EvidenceUploadWorker.schedule(context, uploadId)
            // Safely delete unencrypted original file now that vault copy is persisted
            try { file.delete() } catch (_: Exception) {}
            Pair(encFile, uploadId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to encrypt evidence vault file: ${e.message}")
            Pair(file, "")
        }
    }

    suspend fun executeDirect(command: String, args: List<String>, chatId: Long): com.izhaanintellect.pasa.commands.CommandResult {
        val licenseRejection = licenseManager.checkAccess(command)
        if (licenseRejection != null) {
            return com.izhaanintellect.pasa.commands.CommandResult(false, licenseRejection)
        }
        val handler = resolveHandler(command) ?: return com.izhaanintellect.pasa.commands.CommandResult(false, "Unknown command: $command")
        return handler.execute(args, chatId)
    }

    suspend fun executeRemoteCommand(parsed: CommandParser.ParsedCommand, commandId: String): String {
        Log.i(TAG, "Executing remote command '$commandId': ${parsed.command}")
        
        if (!authManager.isAuthorizedChat(parsed.chatId)) {
            Log.w(TAG, "Rejected unauthorized remote command '${parsed.command}' from chat ${parsed.chatId}")
            val errorMsg = "⛔ Access Denied: This PASA instance is configured for a different administrator."
            sendResponseToBackend(commandId, errorMsg, null, null, null, null)
            logExecution(parsed, "REJECTED", "Unauthorized access denied")
            return errorMsg
        }

        var licenseRejection = licenseManager.checkAccess(parsed.command)
        if (licenseRejection != null) {
            licenseManager.refreshIfStale(force = true)
            licenseRejection = licenseManager.checkAccess(parsed.command)
        }
        if (licenseRejection != null) {
            Log.w(TAG, "Remote command '${parsed.command}' blocked by license gating")
            sendResponseToBackend(commandId, licenseRejection, null, null, null, null)
            logExecution(parsed, "BLOCKED", licenseRejection)
            return licenseRejection
        }

        val handler = resolveHandler(parsed.command)
        if (handler == null) {
            val response = "❓ Unknown command: <code>${parsed.command}</code>\nSend <code>/help</code> for available commands."
            sendResponseToBackend(commandId, response, null, null, null, null)
            return response
        }

        return try {
            val result = handler.execute(parsed.args, parsed.chatId)

            var photo = result.photoFile
            var photoUploadId = ""
            if (photo != null && photo.exists() && photo.length() > 0) {
                val p = enqueueAndEncryptEvidence(commandId, photo, "PHOTO")
                photo = p.first
                photoUploadId = p.second
            }

            var audio = result.audioFile
            var audioUploadId = ""
            if (audio != null && audio.exists() && audio.length() > 0) {
                val a = enqueueAndEncryptEvidence(commandId, audio, "AUDIO")
                audio = a.first
                audioUploadId = a.second
            }

            var video = result.videoFile
            var videoUploadId = ""
            if (video != null && video.exists() && video.length() > 0) {
                val v = enqueueAndEncryptEvidence(commandId, video, "VIDEO")
                video = v.first
                videoUploadId = v.second
            }

            val delivered = sendResponseToBackend(
                commandId = commandId,
                message = result.message,
                photoFile = photo,
                audioFile = audio,
                videoFile = video,
                location = result.location
            )

            if (delivered) {
                listOf(photoUploadId, audioUploadId, videoUploadId).filter { it.isNotBlank() }.forEach { upId ->
                    pendingUploadDao.getById(upId)?.let { u ->
                        pendingUploadDao.update(u.copy(status = "COMPLETED", completedAt = System.currentTimeMillis()))
                    }
                }
                try { photo?.delete() } catch (_: Exception) {}
                try { audio?.delete() } catch (_: Exception) {}
                try { video?.delete() } catch (_: Exception) {}
            } else {
                // Direct fallback to Telegram
                sendText(parsed.chatId, result.message)
                photo?.let { sendPhoto(parsed.chatId, it, "📸 Captured photo") }
                audio?.let { sendAudio(parsed.chatId, it, "🎙️ Audio recording") }
                video?.let { sendVideo(parsed.chatId, it, "🎥 Captured video") }
                result.location?.let { (lat, lng) -> sendLocation(parsed.chatId, lat, lng) }
            }

            val status = if (result.success) "SUCCESS" else "FAILED"
            logExecution(parsed, status, result.message)
            result.message
        } catch (e: Exception) {
            Log.e(TAG, "Remote command execution failure", e)
            val errorMsg = "❌ Execution Error: ${e.localizedMessage ?: "Unexpected failure"}"
            sendResponseToBackend(commandId, errorMsg, null, null, null, null)
            logExecution(parsed, "FAILED", errorMsg)
            errorMsg
        }
    }

    suspend fun sendRejectionToBackend(commandId: String, reason: String): Boolean {
        return sendResponseToBackend(commandId, reason, null, null, null, null)
    }

    suspend fun sendResponseToBackend(
        commandId: String?,
        message: String,
        photoFile: File? = null,
        audioFile: File? = null,
        videoFile: File? = null,
        location: Pair<Double, Double>? = null
    ): Boolean {
        if (!preferencesManager.useBackendServer) return false
        return try {
            val deviceIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
            val cmdIdBody = commandId?.toRequestBody("text/plain".toMediaTypeOrNull())
            val msgBody = message.toRequestBody("text/plain".toMediaTypeOrNull())

            val photoPart = photoFile?.let {
                if (it.exists() && it.length() > 0) {
                    val reqFile = if (encryptionManager.isEncryptedVaultFile(it)) {
                        object : okhttp3.RequestBody() {
                            override fun contentType() = "image/jpeg".toMediaTypeOrNull()
                            override fun writeTo(sink: okio.BufferedSink) {
                                encryptionManager.decryptEvidenceVaultToStream(it).use { input ->
                                    val buffer = ByteArray(8192)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        sink.write(buffer, 0, read)
                                    }
                                }
                            }
                        }
                    } else {
                        it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    }
                    MultipartBody.Part.createFormData("photo", "photo.jpg", reqFile)
                } else null
            }

            val audioPart = audioFile?.let {
                if (it.exists() && it.length() > 0) {
                    val reqFile = if (encryptionManager.isEncryptedVaultFile(it)) {
                        object : okhttp3.RequestBody() {
                            override fun contentType() = "audio/m4a".toMediaTypeOrNull()
                            override fun writeTo(sink: okio.BufferedSink) {
                                encryptionManager.decryptEvidenceVaultToStream(it).use { input ->
                                    val buffer = ByteArray(8192)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        sink.write(buffer, 0, read)
                                    }
                                }
                            }
                        }
                    } else {
                        it.asRequestBody("audio/m4a".toMediaTypeOrNull())
                    }
                    MultipartBody.Part.createFormData("audio", "audio.m4a", reqFile)
                } else null
            }

            val videoPart = videoFile?.let {
                if (it.exists() && it.length() > 0) {
                    val reqFile = if (encryptionManager.isEncryptedVaultFile(it)) {
                        object : okhttp3.RequestBody() {
                            override fun contentType() = "video/mp4".toMediaTypeOrNull()
                            override fun writeTo(sink: okio.BufferedSink) {
                                encryptionManager.decryptEvidenceVaultToStream(it).use { input ->
                                    val buffer = ByteArray(8192)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        sink.write(buffer, 0, read)
                                    }
                                }
                            }
                        }
                    } else {
                        it.asRequestBody("video/mp4".toMediaTypeOrNull())
                    }
                    MultipartBody.Part.createFormData("video", "video.mp4", reqFile)
                } else null
            }

            val latBody = location?.first?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lngBody = location?.second?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            val resp = pasaBackendApi.sendDeviceResponse(
                deviceId = deviceIdBody,
                commandId = cmdIdBody,
                message = msgBody,
                photo = photoPart,
                audio = audioPart,
                video = videoPart,
                evidence = null,
                latitude = latBody,
                longitude = lngBody
            )
            resp.ok
        } catch (e: Exception) {
            Log.w(TAG, "Failed to relay response via VPS backend: ${e.message}")
            false
        }
    }

    private fun resolveHandler(cmd: String): Command? {
        return when (cmd) {
            "/lock", "/lock_message", "/lock_pin" -> lockCommand
            "/set_os_pin", "/set_pin", "/reset_pin" -> setOsPinCommand
            "/set_master_pin", "/set_password", "/master_pin", "/master_password" -> setMasterPinCommand
            "/unlock" -> unlockCommand
            "/device_owner", "/owner", "/kiosk" -> deviceOwnerCommand
            "/antitamper", "/tamper", "/harden" -> antiTamperCommand
            "/usb_lock", "/usb_data", "/usblock" -> usbLockCommand
            "/self_heal", "/permissions_lock", "/heal" -> selfHealCommand
            "/freeze", "/hide_app" -> freezeCommand
            "/unfreeze", "/unhide_app" -> object : Command {
                override val name = "/unfreeze"
                override val description = "Unfreeze/restore hidden application"
                override val usage = "/unfreeze <target>"
                override suspend fun execute(args: List<String>, chatId: Long) = freezeCommand.executeUnfreeze(args)
            }
            "/frozen", "/quarantine" -> object : Command {
                override val name = "/frozen"
                override val description = "List frozen applications"
                override val usage = "/frozen"
                override suspend fun execute(args: List<String>, chatId: Long) = freezeCommand.listFrozenApps()
            }
            "/biometrics", "/biometric", "/duress_biometrics" -> biometricsCommand
            "/reboot", "/restart" -> object : Command {
                override val name = "/reboot"
                override val description = "Remotely restart device (Device Owner)"
                override val usage = "/reboot"
                override suspend fun execute(args: List<String>, chatId: Long): com.izhaanintellect.pasa.commands.CommandResult {
                    if (!com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(context)) {
                        return com.izhaanintellect.pasa.commands.CommandResult(
                            success = false,
                            message = "❌ <b>Reboot Failed:</b> Android Enterprise Device Owner is required to trigger remote hardware reboot.\nCheck status with <code>/device_owner</code>."
                        )
                    }
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        kotlinx.coroutines.delay(1500L)
                        com.izhaanintellect.pasa.admin.PasaDeviceAdmin.rebootDevice(context)
                    }
                    return com.izhaanintellect.pasa.commands.CommandResult(
                        success = true,
                        message = "🔄 <b>Hardware Reboot Initiated</b>\n━━━━━━━━━━━━━━━━━━━━\nDevice Owner is restarting your phone now.\nPASA will automatically resume monitoring upon boot."
                    )
                }
            }
            "/fakeshutdown", "/blackout", "/fake_off" -> fakeShutdownCommand
            "/wake", "/wake_up" -> object : Command {
                override val name = "/wake"
                override val description = "Wake from fake shutdown"
                override val usage = "/wake"
                override suspend fun execute(args: List<String>, chatId: Long) = fakeShutdownCommand.wakeDevice()
            }
            "/check_update", "/update" -> checkUpdateCommand
            "/update_confirm" -> object : Command {
                override val name = "/update_confirm"
                override val description = "Confirm and install pending OTA update"
                override val usage = "/update_confirm"
                override suspend fun execute(args: List<String>, chatId: Long) = checkUpdateCommand.confirmInstall()
            }
            "/duress_pin", "/duress", "/coercion" -> duressPinCommand
            "/trap", "/traps", "/alarm_trap" -> trapCommand
            "/geofence", "/fence", "/safezone" -> geofenceCommand
            "/smssetup", "/sms_setup", "/smscode" -> smsSetupCommand
            "/history", "/logs", "/audit" -> historyCommand
            "/contacts", "/addressbook" -> contactsCommand
            "/call_log", "/calls" -> callLogCommand
            "/sms_log", "/inbox" -> smsLogCommand
            "/sendsms", "/send_sms" -> sendSmsCommand
            "/notification", "/notify", "/notif" -> notificationToggleCommand
            "/sim", "/sim_info", "/carrier" -> simCommand
            "/shred", "/wipe_folder" -> shredCommand
            "/wipe", "/wipe_confirm", "/wipe_external", "/format" -> wipeCommand
            "/locate", "/gps", "/where", "/location" -> locateCommand
            "/track", "/track_stop" -> trackCommand
            "/snap", "/photo", "/camera" -> snapCommand
            "/screenshot", "/screen" -> screenshotCommand
            "/screen_burst", "/burst" -> screenBurstCommand
            "/screenrecord", "/record_screen" -> screenRecordCommand
            "/video", "/videocap", "/vr" -> videoCommand
            "/livestream", "/live_stream", "/live", "/stream" -> liveStreamCommand
            "/stopstream", "/stop_stream", "/stoplive" -> stopStreamCommand
            "/livestream_diag", "/stream_diag", "/livestream_diagnostics" -> liveStreamDiagnosticsCommand
            "/record", "/audio", "/mic" -> recordCommand
            "/ring", "/alarm", "/siren", "/ring_stop" -> ringCommand
            "/status" -> statusCommand
            "/info", "/device" -> infoCommand
            "/network", "/net", "/ip" -> networkCommand
            "/apps", "/app_uninstall" -> appManageCommand
            "/message", "/msg", "/broadcast", "/alert_screen" -> messageCommand
            "/clipboard", "/clip", "/paste" -> clipboardCommand
            "/license", "/pro" -> licenseCommand
            "/hide" -> object : Command {
                override val name = "/hide"
                override val description = "Hide launcher icon"
                override val usage = "/hide"
                override suspend fun execute(args: List<String>, chatId: Long) = stealthCommand.execute(listOf("hide"), chatId)
            }
            "/show" -> object : Command {
                override val name = "/show"
                override val description = "Show launcher icon"
                override val usage = "/show"
                override suspend fun execute(args: List<String>, chatId: Long) = stealthCommand.execute(listOf("show"), chatId)
            }
            "/stealth" -> stealthCommand
            "/dns", "/privatedns", "/doh" -> dnsCommand
            "/tower", "/cell", "/celltower", "/bts" -> towerCommand
            "/selftest", "/health", "/diagnostics" -> selfTestCommand
            "/sim_lock", "/sim_swap", "/simlockdown" -> simLockCommand
            "/vibrate_pulse", "/vibrate", "/pulse", "/sos" -> vibratePulseCommand
            "/pattern_guard", "/unlock_guard", "/pattern_monitor" -> patternGuardCommand
            "/app_firewall", "/firewall", "/rat_block" -> appFirewallCommand
            "/battery_alert", "/battery", "/charge_monitor" -> batteryAlertCommand
            "/tamper_detect", "/detect_tamper", "/integrity_check" -> tamperDetectionCommand
            "/dead_drop", "/deadrop", "/vault_backup" -> deadDropCommand
            "/harden_boot", "/lock_recovery", "/bootlock" -> hardenBootCommand
            "/factory_reset_defense", "/frdefense", "/reset_protection" -> factoryResetDefenseCommand
            "/camera_lock", "/camlock", "/cam_lock" -> cameraLockCommand
            "/bluetooth_lock", "/bt_lock", "/btlock" -> peripheralLockCommand
            "/mic_mute", "/master_mute", "/mute_mic" -> object : Command {
                override val name = "/mic_mute"
                override val description = "Hardware audio master mute [Device Owner]"
                override val usage = "/mic_mute [on|off|status]"
                override suspend fun execute(args: List<String>, chatId: Long) = peripheralLockCommand.executeMicMute(args)
            }
            "/lockscreen_info", "/lockscreen_banner", "/owner_info" -> lockscreenInfoCommand
            "/autolock", "/screen_timeout", "/auto_lock" -> object : Command {
                override val name = "/autolock"
                override val description = "Screen inactivity autolock policy [Device Admin]"
                override val usage = "/autolock [<sec>|default|status]"
                override suspend fun execute(args: List<String>, chatId: Long) = lockscreenInfoCommand.executeAutolock(args)
            }
            "/wifi_connect", "/wifi_provision", "/connect_wifi" -> wifiProvisionCommand
            "/security_audit", "/audit_logs", "/sec_audit" -> securityAuditCommand
            "/sms_help", "/smscommands", "/smshelp", "/sms_guide" -> object : Command {
                override val name = "/sms_help"
                override val description = "Air-gapped cellular SMS command manual & cheat sheet"
                override val usage = "/sms_help"
                override suspend fun execute(args: List<String>, chatId: Long) = helpCommand.executeSmsHelp()
            }
            "/help", "/start" -> helpCommand
            else -> null
        }
    }

    private suspend fun sendVideo(chatId: Long, file: File, caption: String) {
        try {
            val chatIdBody = chatId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
            val part = if (encryptionManager.isEncryptedVaultFile(file)) {
                val body = object : okhttp3.RequestBody() {
                    override fun contentType() = "video/mp4".toMediaTypeOrNull()
                    override fun writeTo(sink: okio.BufferedSink) {
                        encryptionManager.decryptEvidenceVaultToStream(file).use { input ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                sink.write(buffer, 0, read)
                            }
                        }
                    }
                }
                MultipartBody.Part.createFormData("video", "video.mp4", body)
            } else {
                val fileBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("video", file.name, fileBody)
            }

            telegramApi.sendVideo(
                token = preferencesManager.botToken,
                chatId = chatIdBody,
                video = part,
                caption = captionBody
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload video", e)
        }
    }

    private suspend fun sendText(chatId: Long, message: String) {
        try {
            telegramApi.sendMessage(
                token = preferencesManager.botToken,
                request = SendMessageRequest(chatId = chatId, text = message)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send text message", e)
        }
    }

    private suspend fun sendPhoto(chatId: Long, file: File, caption: String) {
        try {
            val chatIdBody = chatId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
            val part = if (encryptionManager.isEncryptedVaultFile(file)) {
                val body = object : okhttp3.RequestBody() {
                    override fun contentType() = "image/jpeg".toMediaTypeOrNull()
                    override fun writeTo(sink: okio.BufferedSink) {
                        encryptionManager.decryptEvidenceVaultToStream(file).use { input ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                sink.write(buffer, 0, read)
                            }
                        }
                    }
                }
                MultipartBody.Part.createFormData("photo", "photo.jpg", body)
            } else {
                val fileBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photo", file.name, fileBody)
            }

            telegramApi.sendPhoto(
                token = preferencesManager.botToken,
                chatId = chatIdBody,
                photo = part,
                caption = captionBody
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload photo", e)
        }
    }

    private suspend fun sendAudio(chatId: Long, file: File, caption: String) {
        try {
            val chatIdBody = chatId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
            val part = if (encryptionManager.isEncryptedVaultFile(file)) {
                val body = object : okhttp3.RequestBody() {
                    override fun contentType() = "audio/mp4".toMediaTypeOrNull()
                    override fun writeTo(sink: okio.BufferedSink) {
                        encryptionManager.decryptEvidenceVaultToStream(file).use { input ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                sink.write(buffer, 0, read)
                            }
                        }
                    }
                }
                MultipartBody.Part.createFormData("audio", "audio.m4a", body)
            } else {
                val fileBody = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("audio", file.name, fileBody)
            }

            telegramApi.sendAudio(
                token = preferencesManager.botToken,
                chatId = chatIdBody,
                audio = part,
                caption = captionBody
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload audio", e)
        }
    }

    private suspend fun sendLocation(chatId: Long, lat: Double, lng: Double) {
        try {
            telegramApi.sendLocation(
                token = preferencesManager.botToken,
                request = SendLocationRequest(chatId = chatId, latitude = lat, longitude = lng)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch location pin", e)
        }
    }

    private suspend fun logExecution(parsed: CommandParser.ParsedCommand, status: String, response: String) {
        try {
            commandLogDao.insert(
                CommandLog(
                    command = parsed.command,
                    args = parsed.args.joinToString(" "),
                    chatId = parsed.chatId,
                    senderName = parsed.senderName,
                    status = status,
                    response = response.take(500)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting command log", e)
        }
    }
}
