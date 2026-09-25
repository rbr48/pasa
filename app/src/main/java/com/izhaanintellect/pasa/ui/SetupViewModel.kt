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
        com.izhaanintellect.pasa.service.PasaService.restartPolling(context)
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
     * Verifies the bot token by calling Telegram directly via unblocked gateway,
     * falling back to VPS backend verification if needed.
     */
    suspend fun testBotConnection(rawToken: String): Result<String> {
        return withContext(Dispatchers.IO) {
            val token = cleanBotToken(rawToken)
            if (token.isBlank()) {
                return@withContext Result.failure(Exception("Bot token cannot be blank"))
            }
            // 1. Try unblocked gateway first
            try {
                val directUrl = "https://pasa.izhaanintellect.fun/tg/bot$token/getMe"
                val resp = telegramApi.getMeDirect(directUrl)
                if (resp.ok && resp.result != null) {
                    return@withContext Result.success(resp.result.username ?: resp.result.firstName)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct bot test failed: ${e.message}, falling back to VPS verify-bot")
            }

            // 2. Fallback to VPS backend verify-bot endpoint
            try {
                val vpsResp = pasaBackendApi.verifyBot(com.izhaanintellect.pasa.network.VerifyBotRequest(token))
                if (vpsResp.ok && vpsResp.bot != null) {
                    Result.success(vpsResp.bot.username ?: vpsResp.bot.firstName ?: "PASA Bot")
                } else {
                    Result.failure(Exception(vpsResp.description ?: "Invalid bot response"))
                }
            } catch (e2: Exception) {
                Result.failure(e2)
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

        if (preferencesManager.botToken != cleanToken) {
            preferencesManager.updateOffset = 0L
        }
        preferencesManager.botToken = cleanToken
        preferencesManager.ownerChatId = chatId.trim()
        preferencesManager.backupEmail = email.trim()
        preferencesManager.isStealthMode = stealthMode
        preferencesManager.serverUrl = serverUrl.trim()
        preferencesManager.useBackendServer = serverUrl.isNotBlank()
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
            if (!preferencesManager.useBackendServer || preferencesManager.serverUrl.isBlank()) {
                Log.i(TAG, "🛡️ Pure Sovereign Mode: VPS registration skipped.")
                return@withContext true
            }
            try {
                val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                val req = com.izhaanintellect.pasa.network.RegisterDeviceRequest(
                    deviceId = preferencesManager.deviceId,
                    deviceName = deviceName,
                    botToken = preferencesManager.botToken,
                    ownerChatId = preferencesManager.ownerChatId
                )
                val resp = pasaBackendApi.registerDevice(req)
                if (resp.ok) {
                    if (!resp.apiKey.isNullOrBlank()) {
                        preferencesManager.apiKey = resp.apiKey
                    }
                    if (!resp.signingKeyId.isNullOrBlank() && !resp.commandSigningPublicJwk.isNullOrBlank()) {
                        preferencesManager.addTrustedCommandKey(resp.signingKeyId, resp.commandSigningPublicJwk)
                    }
                }
                resp.ok
            } catch (e: Exception) {
                Log.w(TAG, "VPS device registration deferred: ${e.message}")
                true
            }
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
