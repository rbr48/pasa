package com.izhaanintellect.pasa.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import com.izhaanintellect.pasa.security.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel managing setup verification, credential validation, bot handshake,
 * and registration with the VPS backend control plane.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager,
    private val telegramApi: TelegramApi,
    private val pasaBackendApi: PasaBackendApi,
    private val commandLogDao: com.izhaanintellect.pasa.data.CommandLogDao,
    private val pendingUploadDao: com.izhaanintellect.pasa.data.PendingUploadDao
) : ViewModel() {

    companion object {
        private const val TAG = "SetupViewModel"
    }

    val commandLogsFlow = commandLogDao.getAllLogsFlow()
    val pendingUploadsFlow = pendingUploadDao.getAllFlow()

    fun isSetupComplete(): Boolean = preferencesManager.isSetupComplete
    fun getSavedServerUrl(): String = preferencesManager.serverUrl
    fun getSavedDeviceId(): String = preferencesManager.deviceId
    fun isDeviceAdmin(): Boolean = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isAdminActive(context)
    fun isDeviceOwner(): Boolean = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(context)
    fun isBatteryWhitelisted(): Boolean = com.izhaanintellect.pasa.util.BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    fun getManufacturer(): String = com.izhaanintellect.pasa.util.BatteryOptimizationHelper.getManufacturer()
    fun getSecurityLevel(): String = preferencesManager.deviceKeySecurityLevel
    fun getBackendMode(): String = if (preferencesManager.useBackendServer) "VPS Gateway Relay" else "100% Sovereign (Direct Telegram)"

    fun flushUploadQueue() {
        com.izhaanintellect.pasa.worker.EvidenceUploadWorker.schedulePendingBatch(context)
    }

    fun restartGuardianService() {
        com.izhaanintellect.pasa.service.PasaService.stop(context)
        com.izhaanintellect.pasa.service.PasaService.start(context)
    }

    fun verifyMasterPassword(password: String): Boolean = authManager.verifyMasterPassword(password)

    fun unlockSetup() {
        preferencesManager.isSetupComplete = false
    }

    fun saveServerUrl(url: String) {
        preferencesManager.serverUrl = url
    }

    /**
     * Tests connectivity to the VPS Backend Control Plane.
     */
    suspend fun testServerConnection(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                preferencesManager.serverUrl = url
                val health = pasaBackendApi.getHealth()
                if (health.status == "ok") {
                    Result.success("VPS Server Online (v${health.version ?: "1.0"})")
                } else {
                    Result.failure(Exception("Server returned status: ${health.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Verifies the bot token by calling Telegram directly.
     * Zero-Data: botToken is NEVER sent to the VPS — direct Telegram only.
     */
    suspend fun testBotConnection(rawToken: String): Result<String> {
        return withContext(Dispatchers.IO) {
            val token = cleanBotToken(rawToken)
            if (token.isBlank()) {
                return@withContext Result.failure(Exception("Bot token cannot be blank"))
            }
            // Zero-Data: Always verify directly with Telegram — never send token to VPS
            try {
                val directUrl = "https://api.telegram.org/bot$token/getMe"
                val resp = telegramApi.getMeDirect(directUrl)
                if (resp.ok && resp.result != null) {
                    Result.success(resp.result.username ?: resp.result.firstName)
                } else {
                    Result.failure(Exception(resp.description ?: "Invalid bot response"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun validateAndSave(
        botToken: String,
        chatId: String,
        masterPassword: String,
        email: String,
        stealthMode: Boolean,
        serverUrl: String
    ): Boolean {
        val cleanToken = cleanBotToken(botToken)
        if (cleanToken.isBlank() || chatId.isBlank() || masterPassword.length < 8) {
            return false
        }

        preferencesManager.botToken = cleanToken
        preferencesManager.ownerChatId = chatId.trim()
        preferencesManager.backupEmail = email.trim()
        preferencesManager.isStealthMode = stealthMode
        preferencesManager.serverUrl = serverUrl.trim()
        preferencesManager.useBackendServer = false  // Zero-Data: Always Sovereign Mode (direct Telegram)
        preferencesManager.isSetupComplete = true

        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val telephonyManager = context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                val simId = telephonyManager.simSerialNumber ?: telephonyManager.subscriberId
                if (simId != null) {
                    preferencesManager.knownSimId = simId
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read initial SIM state", e)
        }

        authManager.setMasterPassword(masterPassword)
        return true
    }

    suspend fun registerDeviceWithBackend(context: android.content.Context? = null): Boolean {
        return withContext(Dispatchers.IO) {
            // Zero-Data Architecture: Device registration is disabled.
            // The server never receives botToken, ownerChatId, or device credentials.
            // All C2 (commands) are handled via direct Telegram polling (Sovereign Mode).
            // License activation is the only server contact, using anonymous deviceHash.
            Log.i(TAG, "🛡️ Zero-Data Mode: VPS registration disabled. All C2 is Sovereign (direct Telegram).")
            true
        }
    }

    suspend fun sendSetupConfirmation(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modeStr = if (preferencesManager.useBackendServer) "VPS Cloud Gateway" else "100% Sovereign (Direct Telegram)"
                val response = telegramApi.sendMessage(
                    token = preferencesManager.botToken,
                    request = SendMessageRequest(
                        chatId = preferencesManager.ownerChatIdLong,
                        text = """
                            🛡️ <b>PASA Sentinel (Private Android Security Agent) ONLINE</b>
                            ━━━━━━━━━━━━━━━━━━━━
                            ✅ Device linked: <code>${preferencesManager.deviceId}</code>
                            🔒 Architecture: <b>$modeStr</b>
                            ⚡ Privileged Knox & Device Owner defense active.
                            
                            Send <code>/help</code> to view all commands.
                            Send <code>/status</code> for an instant telemetry report.
                            Send <code>/license</code> to view Pro tier status.
                        """.trimIndent()
                    )
                )
                response.ok
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun cleanBotToken(token: String): String {
        var clean = token.trim()
        if (clean.startsWith("bot", ignoreCase = true) && clean.length > 3 && clean[3].isDigit()) {
            clean = clean.substring(3)
        }
        return clean
    }

    suspend fun initPairing(serverUrl: String, deviceId: String, deviceName: String): com.izhaanintellect.pasa.network.PairInitResponse {
        return withContext(Dispatchers.IO) {
            preferencesManager.serverUrl = serverUrl
            pasaBackendApi.initPairing(
                com.izhaanintellect.pasa.network.PairInitRequest(
                    deviceId = deviceId,
                    deviceName = deviceName
                )
            )
        }
    }

    suspend fun pollPairingStatus(code: String): com.izhaanintellect.pasa.network.PairStatusResponse {
        return withContext(Dispatchers.IO) {
            pasaBackendApi.pollPairingStatus(code)
        }
    }
}
