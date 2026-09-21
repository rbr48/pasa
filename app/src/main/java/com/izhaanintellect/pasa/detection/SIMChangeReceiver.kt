package com.izhaanintellect.pasa.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.network.PasaBackendApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
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
    @Inject lateinit var pasaBackendApi: PasaBackendApi

    companion object {
        private const val TAG = "PASA_SIM"
        private var lastSimState: String? = null
    }

    /**
     * Retrieves the device IMEI. Requires Device Owner or READ_PRIVILEGED_PHONE_STATE on Android 10+.
     * Falls back to ANDROID_ID if IMEI is unavailable.
     */
    private fun getDeviceImei(context: Context): String? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tm?.imei ?: tm?.getImei(0)
            } else {
                @Suppress("DEPRECATION")
                tm?.deviceId
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot read IMEI (requires Device Owner or privileged permission): ${e.message}")
            try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (_: Exception) { null }
        }
    }

    /**
     * Retrieves the phone number associated with the active SIM card.
     * Uses SubscriptionManager on Android 13+, SubscriptionInfo on Android 5.1-12, or TelephonyManager.
     */
    private fun getPhoneNumber(context: Context): String? {
        return try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            var num: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                num = subManager?.getPhoneNumber(SubscriptionManager.getDefaultSubscriptionId())
            }
            if (num.isNullOrBlank()) {
                val activeList = subManager?.activeSubscriptionInfoList
                num = activeList?.firstOrNull()?.number
            }
            if (num.isNullOrBlank()) {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                @Suppress("DEPRECATION")
                num = tm?.line1Number
            }
            if (!num.isNullOrBlank()) num else null
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot read phone number: ${e.message}")
            null
        }
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
        // 1. Capture IMEI / device identifier
        val imei = getDeviceImei(context)
        val imeiStr = if (imei != null) "\n📱 <b>Device IMEI:</b> <code>$imei</code>" else ""

        // 2. Capture covert front-camera snapshot with retry mechanism
        var mugshot: java.io.File? = null
        for (attempt in 1..3) {
            mugshot = try {
                StealthCaptureBridge.capturePhoto(context, useFront = true)?.file
            } catch (e: Exception) {
                Log.w(TAG, "Mugshot capture attempt $attempt failed: ${e.message}")
                null
            }
            if (mugshot != null && mugshot.exists() && mugshot.length() > 0) break
            if (attempt < 3) {
                delay(1500) // Wait before retry
            }
        }

        // 3. Get current location
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

        val locPair = try {
            locationTracker.getCurrentLocation()?.let { Pair(it.latitude, it.longitude) }
        } catch (_: Exception) { null }

        val alertText = """
            🚨 <b>TAMPER ALERT: SIM CARD EJECTED!</b>
            ━━━━━━━━━━━━━━━━━━━━
            The physical SIM card was just removed from your device.$imeiStr
            
            📸 Front-camera mugshot capture initiated.
            $locationStr
            
            ⚠️ <i>If you did not eject your SIM, your phone has been stolen!</i>
            Lock immediately using <code>/lock</code> or blackout with <code>/fakeshutdown</code>.
        """.trimIndent()

        dispatchSimAlert(alertText, mugshot, locPair)
    }

    private suspend fun handleSimLoaded(context: Context) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Fix: Wrap in try-catch to prevent SecurityException crash on Android 10+
        // simSerialNumber and subscriberId require READ_PRIVILEGED_PHONE_STATE (signature-only)
        var currentSimId: String? = null
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            currentSimId = try {
                telephonyManager.simSerialNumber ?: telephonyManager.subscriberId
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot read SIM serial (Android 10+ restriction): ${e.message}")
                // Fallback: use carrier + country as a rough identifier
                "${telephonyManager.simOperatorName ?: "unknown"}_${telephonyManager.simCountryIso ?: "unknown"}"
            }
        }

        val knownSimId = preferencesManager.knownSimId
        if (currentSimId != null && knownSimId != null && currentSimId == knownSimId) {
            Log.i(TAG, "SIM state is LOADED but SIM ID matches known SIM. No alert needed.")
            return
        }

        if (currentSimId != null) {
            preferencesManager.knownSimId = currentSimId
        }

        // Capture IMEI and phone number
        val imei = getDeviceImei(context)
        val phoneNumber = getPhoneNumber(context)

        val operatorName = telephonyManager.networkOperatorName ?: "Unknown"
        val simOperator = telephonyManager.simOperatorName ?: "Unknown"
        val countryCode = telephonyManager.simCountryIso?.uppercase() ?: "Unknown"

        val imeiStr = if (imei != null) "\n📱 <b>Device IMEI:</b> <code>$imei</code>" else ""
        val phoneStr = if (!phoneNumber.isNullOrBlank()) "\n📞 <b>New Number:</b> <code>$phoneNumber</code>" else "\n📞 <b>New Number:</b> <i>Unavailable (carrier restricted)</i>"

        // Capture mugshot of person inserting new SIM with retry
        var mugshot: java.io.File? = null
        for (attempt in 1..3) {
            mugshot = try {
                StealthCaptureBridge.capturePhoto(context, useFront = true)?.file
            } catch (e: Exception) {
                Log.w(TAG, "SIM swap mugshot attempt $attempt failed: ${e.message}")
                null
            }
            if (mugshot != null && mugshot.exists() && mugshot.length() > 0) break
            if (attempt < 3) {
                delay(1500)
            }
        }

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

        val locPair = try {
            locationTracker.getCurrentLocation()?.let { Pair(it.latitude, it.longitude) }
        } catch (_: Exception) { null }

        val alertText = """
            🚨 <b>TAMPER ALERT: NEW SIM CARD DETECTED!</b>
            ━━━━━━━━━━━━━━━━━━━━$imeiStr$phoneStr
            🏢 <b>New Carrier:</b> $operatorName ($countryCode)
            📱 <b>Provider:</b> $simOperator
            📸 Front-camera mugshot capture initiated.
            $locationStr
            
            ⚠️ <i>A foreign SIM card has been inserted into your device. If you did not do this, your phone has been compromised!</i>
            Lock immediately: <code>/lock</code> | Blackout: <code>/fakeshutdown</code> | Wipe: <code>/wipe</code>
        """.trimIndent()

        dispatchSimAlert(alertText, mugshot, locPair)
    }

    private suspend fun dispatchSimAlert(
        message: String,
        photo: java.io.File? = null,
        location: Pair<Double, Double>? = null
    ) {
        if (preferencesManager.useBackendServer) {
            try {
                val deviceIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                val msgBody = message.toRequestBody("text/plain".toMediaTypeOrNull())
                val photoPart = photo?.takeIf { it.exists() && it.length() > 0 }?.let {
                    val req = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("photo", it.name, req)
                }
                val latBody = location?.first?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val lngBody = location?.second?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val resp = pasaBackendApi.sendDeviceResponse(
                    deviceId = deviceIdBody,
                    commandId = null,
                    message = msgBody,
                    photo = photoPart,
                    audio = null,
                    video = null,
                    evidence = null,
                    latitude = latBody,
                    longitude = lngBody
                )
                Log.i(TAG, "SIM alert dispatched via backend API: ${resp.ok}")
                return
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch SIM alert via backend: ${e.message}")
            }
        }

        // Direct Telegram fallback
        if (preferencesManager.botToken.isNotBlank() && preferencesManager.ownerChatIdLong != 0L) {
            try {
                telegramApi.sendMessage(
                    token = preferencesManager.botToken,
                    request = SendMessageRequest(
                        chatId = preferencesManager.ownerChatIdLong,
                        text = message
                    )
                )
                if (photo != null && photo.exists() && photo.length() > 0) {
                    val mediaType = "image/jpeg".toMediaTypeOrNull()
                    val requestBody = photo.asRequestBody(mediaType)
                    val photoPart = MultipartBody.Part.createFormData("photo", photo.name, requestBody)
                    val chatIdPart = preferencesManager.ownerChatIdLong.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val captionPart = "📸 Thief mugshot captured upon SIM event".toRequestBody("text/plain".toMediaTypeOrNull())
                    telegramApi.sendPhoto(preferencesManager.botToken, chatIdPart, photoPart, captionPart)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch SIM alert via Telegram: ${e.message}")
            }
        }
    }
}
