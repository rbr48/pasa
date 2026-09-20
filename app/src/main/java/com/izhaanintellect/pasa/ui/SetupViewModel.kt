package com.izhaanintellect.pasa.ui

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import com.izhaanintellect.pasa.network.RegisterDeviceRequest
import com.izhaanintellect.pasa.network.VerifyBotRequest
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
    fun getBackendMode(): String = if (preferencesManager.useBackendServer) "VPS Gateway" else "Direct Telegram"

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
     * Handshake verification: queries VPS backend /api/verify-bot first (robust against
     * ISP throttling and Retrofit encoding bugs), falling back to direct Telegram API if needed.
     */
    suspend fun testBotConnection(rawToken: String): Result<String> {
        return withContext(Dispatchers.IO) {
            val token = cleanBotToken(rawToken)
            if (token.isBlank()) {
                return@withContext Result.failure(Exception("Bot token cannot be blank"))
            }

            // 1. First Attempt: Verify through VPS Backend Gateway
            if (preferencesManager.useBackendServer) {
                try {
                    val vpsRes = pasaBackendApi.verifyBot(VerifyBotRequest(token))
                    if (vpsRes.ok && vpsRes.bot != null) {
                        return@withContext Result.success(vpsRes.bot.username ?: vpsRes.bot.firstName ?: "Bot")
                    } else if (!vpsRes.description.isNullOrBlank()) {
                        return@withContext Result.failure(Exception(vpsRes.description))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "VPS backend verify failed, falling back to direct Telegram: ${e.message}")
                }
            }

            // 2. Direct Telegram API Fallback (using clean direct URL)
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
            try {
                var publicKeyJwkStr: String? = null
                var attestationList: List<String>? = null

                context?.let { ctx ->
                    com.izhaanintellect.pasa.crypto.DeviceIdentity.ensureKey(ctx)
                    val level = com.izhaanintellect.pasa.crypto.DeviceIdentity.securityLevel()
                    preferencesManager.deviceKeySecurityLevel = level

                    if (com.izhaanintellect.pasa.crypto.DeviceIdentity.exists()) {
                        publicKeyJwkStr = com.izhaanintellect.pasa.crypto.DeviceIdentity.publicJwk().toString()
                        attestationList = com.izhaanintellect.pasa.crypto.DeviceIdentity.attestationChain()
                    }
                }

                val req = RegisterDeviceRequest(
                    deviceId = preferencesManager.deviceId,
                    deviceName = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                    botToken = preferencesManager.botToken,
                    ownerChatId = preferencesManager.ownerChatId,
                    email = preferencesManager.backupEmail,
                    publicKeyJwk = publicKeyJwkStr,
                    attestationChain = attestationList
                )
                val resp = pasaBackendApi.registerDevice(req)
                if (resp.ok) {
                    if (!resp.apiKey.isNullOrBlank()) {
                        preferencesManager.apiKey = resp.apiKey
                    }
                    if (!resp.signingKeyId.isNullOrBlank() && !resp.commandSigningPublicJwk.isNullOrBlank()) {
                        preferencesManager.addTrustedCommandKey(resp.signingKeyId, resp.commandSigningPublicJwk)
                        Log.i(TAG, "Enrolled trusted command signing key: ${resp.signingKeyId}")
                    }
                }
                resp.ok
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register with VPS backend: ${e.message}")
                false
            }
        }
    }

    suspend fun sendSetupConfirmation(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = telegramApi.sendMessage(
                    token = preferencesManager.botToken,
                    request = SendMessageRequest(
                        chatId = preferencesManager.ownerChatIdLong,
                        text = """
                            🛡️ <b>PASA (Private Android Security Agent) ONLINE</b>
                            ━━━━━━━━━━━━━━━━━━━━
                            ✅ Device linked: <code>${preferencesManager.deviceId}</code>
                            🌐 Backend Control Plane: Connected
                            🔒 Privileged commands are now active.
                            
                            Send <code>/help</code> to view all commands.
                            Send <code>/status</code> for an instant telemetry report.
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
}
