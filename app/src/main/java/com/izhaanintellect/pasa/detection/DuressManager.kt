package com.izhaanintellect.pasa.detection

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.commands.TrackCommand
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.network.PasaBackendApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles anti-coercion Duress PIN detection, silent emergency SOS beacons,
 * forensic mugshot captures, and automatic high-frequency live GPS tracking.
 */
@Singleton
class DuressManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val telegramApi: TelegramApi,
    private val pasaBackendApi: PasaBackendApi,
    private val locationTracker: LocationTracker,
    private val trackCommand: TrackCommand
) {
    companion object {
        private const val TAG = "PASA_DuressManager"
        @Volatile
        private var lastTriggerTime = 0L
    }

    fun isDuressPin(pin: String): Boolean {
        val configured = preferencesManager.duressPin
        if (configured.isNullOrBlank()) return false
        return pin.trim() == configured.trim()
    }

    fun executeDuressUnlock(context: Context) {
        Log.i(TAG, "Executing duress unlock sequence...")
        preferencesManager.isDuressActive = true

        try {
            // 1. Dismiss Lost Mode overlay if active
            if (preferencesManager.isLostModeActive) {
                preferencesManager.isLostModeActive = false
                preferencesManager.activeLockPin = null
                context.sendBroadcast(android.content.Intent(com.izhaanintellect.pasa.ui.AlertMessageActivity.ACTION_DISMISS_LOST_MODE))
            }

            // 2. Clear physical lockscreen PIN via Device Owner Hardware Escrow Token
            val cleared = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.clearDevicePassword(context, preferencesManager)
            Log.i(TAG, "Duress lockscreen PIN cleared via escrow token: $cleared")

            // 3. Launch transparent DuressUnlockActivity to request Keyguard dismissal and bring phone to Home
            com.izhaanintellect.pasa.ui.DuressUnlockActivity.launch(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error in executeDuressUnlock: ${e.message}", e)
        }
    }

    fun triggerDuressSosAsync(context: Context, source: String = "Lockscreen Keypad") {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 8_000L) { // 8-second debounce
            Log.w(TAG, "Duress SOS trigger debounced")
            return
        }
        lastTriggerTime = now

        CoroutineScope(Dispatchers.IO).launch {
            triggerDuressSos(context, source)
        }
    }

    suspend fun triggerDuressSos(context: Context, source: String = "Lockscreen Keypad") {
        Log.w(TAG, "🚨 CRITICAL: Duress SOS beacon triggered from $source!")
        preferencesManager.isDuressActive = true

        try {
            // 1. High accuracy location retrieval
            val loc = locationTracker.getCurrentLocation()
            val locMsg = if (loc != null) {
                preferencesManager.lastKnownLatitude = loc.latitude
                preferencesManager.lastKnownLongitude = loc.longitude
                "\n📍 <b>Live Coercion GPS:</b> <a href=\"https://www.google.com/maps?q=${loc.latitude},${loc.longitude}\">${String.format(Locale.US, "%.6f", loc.latitude)}, ${String.format(Locale.US, "%.6f", loc.longitude)}</a> (±${String.format(Locale.US, "%.1f", loc.accuracy)}m)"
            } else {
                "\n📍 <i>Acquiring high-accuracy satellite lock...</i>"
            }

            val alertText = "🚨 <b>EMERGENCY COERCION DURESS BEACON!</b>\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "⚠️ <b>Decoy Duress PIN entered on device!</b>\n" +
                    "📱 <b>Detection Source:</b> $source\n" +
                    "👤 <b>Status:</b> The owner was forced to unlock under physical threat or coercion.\n" +
                    "🛡️ <b>Deception:</b> Device appeased attacker by clearing lockscreen and opening Home screen.$locMsg\n\n" +
                    "📸 <i>Stealth mugshot and live coordinates dispatched below.</i>\n" +
                    "📡 <i>High-frequency live GPS tracking automatically engaged.</i>\n\n" +
                    "🔒 <b>Remote control:</b> Send <code>/set_os_pin &lt;pin&gt;</code> to set a new PIN, or <code>/lock</code> to lockdown."

            // 2. Covert stealth front-camera mugshot
            val captureResult = StealthCaptureBridge.capturePhoto(context, useFront = true, timeoutMs = 8000L)
            val photoFile = captureResult.file

            var relayedViaBackend = false

            // 3. Relay via VPS Backend Control Plane
            if (preferencesManager.useBackendServer) {
                try {
                    val deviceIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                    val alertTypeBody = "COERCION_DURESS_PIN".toRequestBody("text/plain".toMediaTypeOrNull())
                    val msgBody = alertText.toRequestBody("text/plain".toMediaTypeOrNull())
                    val photoPart = photoFile?.let {
                        if (it.exists() && it.length() > 0) {
                            val reqFile = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("photo", it.name, reqFile)
                        } else null
                    }
                    val latBody = loc?.latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val lngBody = loc?.longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                    val res = pasaBackendApi.sendDeviceAlert(
                        deviceId = deviceIdBody,
                        alertType = alertTypeBody,
                        message = msgBody,
                        photo = photoPart,
                        latitude = latBody,
                        longitude = lngBody
                    )
                    relayedViaBackend = res.ok
                    Log.i(TAG, "Duress SOS relayed via VPS backend: ok=${res.ok}")
                } catch (e: Exception) {
                    Log.w(TAG, "VPS alert relay failed, falling back to direct Telegram: ${e.message}")
                }
            }

            // 4. Direct Telegram dispatch
            if (!relayedViaBackend && preferencesManager.botToken.isNotBlank()) {
                try {
                    telegramApi.sendMessage(
                        token = preferencesManager.botToken,
                        request = SendMessageRequest(
                            chatId = preferencesManager.ownerChatIdLong,
                            text = alertText
                        )
                    )

                    photoFile?.let { file ->
                        if (file.exists() && file.length() > 0) {
                            val chatIdBody = preferencesManager.ownerChatId.toRequestBody("text/plain".toMediaTypeOrNull())
                            val captionBody = "📸 Covert Duress Mugshot (Attacker Selfies)".toRequestBody("text/plain".toMediaTypeOrNull())
                            val fileBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData("photo", file.name, fileBody)

                            telegramApi.sendPhoto(
                                token = preferencesManager.botToken,
                                chatId = chatIdBody,
                                photo = part,
                                caption = captionBody
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed direct Telegram duress alert: ${e.message}")
                }
            }

            // 5. Automatically start continuous GPS tracking (every 2 minutes)
            try {
                trackCommand.execute(listOf("2"), preferencesManager.ownerChatIdLong)
                Log.i(TAG, "Continuous 2-minute live GPS tracking engaged on duress trigger")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-engage live tracking", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in triggerDuressSos", e)
        }
    }
}
