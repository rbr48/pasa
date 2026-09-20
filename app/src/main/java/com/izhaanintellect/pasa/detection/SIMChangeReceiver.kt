package com.izhaanintellect.pasa.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/**
 * Detects SIM card replacements, removals, and tampering events.
 *
 * Automatically triggers covert front-camera mugshot capture and dispatches
 * emergency location alerts when a SIM is ejected or replaced.
 */
@AndroidEntryPoint
class SIMChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var locationTracker: LocationTracker

    companion object {
        private const val TAG = "PASA_SIM"
        private var lastSimState: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.SIM_STATE_CHANGED") return

        val simState = intent.getStringExtra("ss") ?: return
        if (simState == lastSimState) return
        lastSimState = simState

        if (!preferencesManager.isConfigured()) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (simState == "ABSENT") {
                    Log.w(TAG, "🚨 CRITICAL: SIM card removed / ejected!")
                    handleSimRemoved(context)
                } else if (simState == "LOADED") {
                    Log.i(TAG, "SIM state changed to LOADED — inspecting provider")
                    handleSimLoaded(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SIM change event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleSimRemoved(context: Context) {
        // 1. Capture covert front-camera snapshot of person ejecting the tray
        val captureResult = runCatching {
            StealthCaptureBridge.capturePhoto(context, useFront = true)
        }.getOrNull()
        val mugshot = captureResult?.file

        // 2. Get current location
        val locationStr = try {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                "\n📍 Last GPS Fix: ${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}" +
                        "\n🗺️ https://maps.google.com/maps?q=${location.latitude},${location.longitude}"
            } else {
                "\n📍 Location: Satellite fix pending"
            }
        } catch (_: Exception) {
            "\n📍 Location: Unavailable"
        }

        // 3. Dispatch alert to owner via Telegram
        telegramApi.sendMessage(
            token = preferencesManager.botToken,
            request = SendMessageRequest(
                chatId = preferencesManager.ownerChatIdLong,
                text = """
                    🚨 <b>TAMPER ALERT: SIM CARD EJECTED!</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    The physical SIM card was just removed from your device.
                    
                    📸 Front-camera mugshot capture initiated.
                    $locationStr
                    
                    ⚠️ <i>If you did not eject your SIM, your phone has been stolen!</i>
                    Lock immediately using <code>/lock</code> or blackout with <code>/fakeshutdown</code>.
                """.trimIndent()
            )
        )

        // 4. Send photo if captured
        if (mugshot != null && mugshot.exists() && mugshot.length() > 0) {
            try {
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val requestBody = mugshot.asRequestBody(mediaType)
                val photoPart = okhttp3.MultipartBody.Part.createFormData("photo", mugshot.name, requestBody)
                val chatIdPart = preferencesManager.ownerChatIdLong.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val captionPart = "📸 Thief mugshot captured upon SIM ejection".toRequestBody("text/plain".toMediaTypeOrNull())
                telegramApi.sendPhoto(preferencesManager.botToken, chatIdPart, photoPart, captionPart)
            } catch (e: Exception) {
                Log.w(TAG, "Failed sending mugshot photo: ${e.message}")
            }
        }
    }

    private suspend fun handleSimLoaded(context: Context) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        var currentSimId: String? = null
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            currentSimId = telephonyManager.simSerialNumber ?: telephonyManager.subscriberId
        }

        val knownSimId = preferencesManager.knownSimId
        if (currentSimId != null && knownSimId != null && currentSimId == knownSimId) {
            Log.i(TAG, "SIM state is LOADED but SIM ID matches known SIM. No alert needed.")
            return
        }

        if (currentSimId != null) {
            preferencesManager.knownSimId = currentSimId
        }

        val operatorName = telephonyManager.networkOperatorName ?: "Unknown"
        val simOperator = telephonyManager.simOperatorName ?: "Unknown"
        val countryCode = telephonyManager.simCountryIso?.uppercase() ?: "Unknown"

        val locationStr = try {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                "\n📍 Location: ${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}" +
                        "\n🗺️ https://maps.google.com/maps?q=${location.latitude},${location.longitude}"
            } else {
                "\n📍 Location: Unavailable"
            }
        } catch (_: Exception) {
            "\n📍 Location: Error"
        }

        telegramApi.sendMessage(
            token = preferencesManager.botToken,
            request = SendMessageRequest(
                chatId = preferencesManager.ownerChatIdLong,
                text = """
                    🚨 <b>NEW SIM CARD INSERTED!</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    A new SIM card was detected in your device:
                    📞 Operator: <b>$operatorName</b>
                    📱 Provider: <b>$simOperator</b>
                    🌍 Country: <b>$countryCode</b>
                    $locationStr
                    
                    ⚠️ If you did not perform this change, your device may be stolen.
                    Lock immediately using <code>/lock</code> or wipe with <code>/wipe</code>.
                """.trimIndent()
            )
        )
    }
}
