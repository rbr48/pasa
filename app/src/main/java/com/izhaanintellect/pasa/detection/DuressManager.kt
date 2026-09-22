package com.izhaanintellect.pasa.detection

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.bot.SendLocationRequest
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

            // 2. Sterile Sandbox Decoy OS: If Device Owner, instantly vanish sensitive apps
            if (com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(context)) {
                deploySterileDecoySandbox(context)
            }

            // 3. Clear physical lockscreen PIN via Device Owner Hardware Escrow Token
            val cleared = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.clearDevicePassword(context, preferencesManager)
            Log.i(TAG, "Duress lockscreen PIN cleared via escrow token: $cleared")

            // 4. Launch transparent DuressUnlockActivity to request Keyguard dismissal and bring phone to Home
            com.izhaanintellect.pasa.ui.DuressUnlockActivity.launch(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error in executeDuressUnlock: ${e.message}", e)
        }
    }

    private fun deploySterileDecoySandbox(context: Context) {
        val targets = mutableSetOf<String>()
        // Include any user-frozen packages
        targets.addAll(preferencesManager.frozenPackages)
        // Include high-risk crypto, banking, and private messengers
        targets.addAll(listOf(
            "com.binance.dev",
            "com.coinbase.android",
            "com.kraken.invest.app",
            "com.bybit.app",
            "org.thoughtcrime.securesms",
            "org.telegram.messenger",
            "com.whatsapp",
            "com.bKash.customerapp",
            "com.nagad.customer",
            "com.google.android.apps.walletnfcrel"
        ))
        for (pkg in targets) {
            try {
                com.izhaanintellect.pasa.admin.PasaDeviceAdmin.setAppHidden(context, pkg, true)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to conceal $pkg in duress sandbox: ${e.message}")
            }
        }
        Log.i(TAG, "Decoy Coercion OS: sterile sandbox active, hidden ${targets.size} candidate vaults")
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
                    "🛡️ <b>Deception:</b> Device appeased attacker by clearing lockscreen and opening Home screen.$locMsg\n" +
                    "🎭 <b>Sterile Sandbox:</b> All private crypto, banking, and messenger vaults were vanished from the phone in real time.\n\n" +
                    "📸 <i>Stealth mugshot and live coordinates dispatched below.</i>\n" +
                    "📡 <i>High-frequency live GPS tracking automatically engaged.</i>\n\n" +
                    "🔒 <b>Remote control:</b> Send <code>/set_os_pin &lt;pin&gt;</code> to set a new PIN, <code>/unfreeze &lt;pkg&gt;</code> to restore apps, or <code>/lock</code> to lockdown."

            // 2. Covert stealth front-camera mugshot
            val captureResult = StealthCaptureBridge.capturePhoto(context, useFront = true, timeoutMs = 8000L)
            val photoFile = captureResult.file

            // 3. Direct Telegram dispatch (Strategy 1: Zero-Storage, zero server media persistence)
            if (preferencesManager.botToken.isNotBlank() && preferencesManager.ownerChatIdLong != 0L) {
                try {
                    // Send SOS text alert
                    telegramApi.sendMessage(
                        token = preferencesManager.botToken,
                        request = SendMessageRequest(
                            chatId = preferencesManager.ownerChatIdLong,
                            text = alertText
                        )
                    )

                    // Send covert front-camera mugshot directly to Telegram
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

                    // Send interactive Telegram GPS location pin directly to user
                    if (loc != null) {
                        try {
                            telegramApi.sendLocation(
                                token = preferencesManager.botToken,
                                request = SendLocationRequest(
                                    chatId = preferencesManager.ownerChatIdLong,
                                    latitude = loc.latitude,
                                    longitude = loc.longitude
                                )
                            )
                        } catch (locErr: Exception) {
                            Log.w(TAG, "Failed to send direct Telegram location pin: ${locErr.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed direct Telegram duress alert: ${e.message}")
                } finally {
                    // Immediately shred local photo file after dispatch
                    try {
                        photoFile?.let { if (it.exists()) it.delete() }
                    } catch (_: Exception) {}
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
