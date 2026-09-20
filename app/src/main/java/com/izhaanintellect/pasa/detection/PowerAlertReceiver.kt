package com.izhaanintellect.pasa.detection

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.SendLocationRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.network.PasaBackendApi
import dagger.hilt.android.AndroidEntryPoint
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

/**
 * Monitors critical hardware power events:
 * 1. Critical battery depletion (<15%): Dispatches emergency warning with last known GPS fix.
 * 2. Unauthorized charger disconnection: Dispatches intrusion alert if power cable is pulled while device is locked.
 */
@AndroidEntryPoint
class PowerAlertReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var pasaBackendApi: PasaBackendApi

    companion object {
        private const val TAG = "PASA_PowerAlert"
        private var lastBatteryAlertTime = 0L
        private var lastPowerDisconnectAlertTime = 0L
        private const val MIN_BATTERY_ALERT_INTERVAL_MS = 15 * 60 * 1000L // 15 mins
        private const val MIN_POWER_ALERT_INTERVAL_MS = 2 * 60 * 1000L    // 2 mins
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (!preferencesManager.isConfigured()) return

        val now = System.currentTimeMillis()

        when (action) {
            Intent.ACTION_BATTERY_LOW -> {
                if (now - lastBatteryAlertTime < MIN_BATTERY_ALERT_INTERVAL_MS) return
                lastBatteryAlertTime = now
                handleBatteryLowAlert(context)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                // Only alert if phone is locked (unplugged by stranger/thief)
                if (!km.isKeyguardLocked) {
                    Log.d(TAG, "Charger unplugged while device unlocked — no alert needed.")
                    return
                }
                if (now - lastPowerDisconnectAlertTime < MIN_POWER_ALERT_INTERVAL_MS) return
                lastPowerDisconnectAlertTime = now
                handlePowerDisconnectedAlert(context)
            }
        }
    }

    private fun handleBatteryLowAlert(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.w(TAG, "Battery dropped to critical low! Capturing final GPS + photo beacon...")
                val batteryPct = getBatteryPercentage(context)
                val location = locationTracker.getCurrentLocation()

                // Best-effort silent photo so the last frame before the phone dies is captured.
                // Bounded timeout to stay within the broadcast's async window.
                val photoFile = try {
                    StealthCaptureBridge.capturePhoto(context, useFront = true, timeoutMs = 8000L).file
                } catch (e: Exception) {
                    Log.w(TAG, "Battery beacon photo capture failed: ${e.message}")
                    null
                }

                val locText = if (location != null) {
                    val lat = String.format(Locale.US, "%.5f", location.latitude)
                    val lng = String.format(Locale.US, "%.5f", location.longitude)
                    "📍 <b>Last Known GPS:</b> $lat, $lng\n🗺️ <a href=\"https://maps.google.com/maps?q=$lat,$lng\">Open in Google Maps</a>"
                } else {
                    "📍 <b>Location:</b> Acquiring fix failed (GPS offline)"
                }

                val alertMsg = """
                    ⚠️ <b>CRITICAL BATTERY WARNING</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    🔋 <b>Remaining Charge:</b> ${batteryPct}%
                    ⚠️ Device may power off soon due to low battery.

                    $locText
                    ${if (photoFile != null) "\n📸 Final camera snapshot attached." else ""}
                """.trimIndent()

                dispatchAlert(
                    alertType = "BATTERY_CRITICAL",
                    message = alertMsg,
                    lat = location?.latitude,
                    lng = location?.longitude,
                    photoFile = photoFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to deliver battery alert", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handlePowerDisconnectedAlert(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.w(TAG, "Charger unplugged while screen locked! Potential theft in progress.")
                val batteryPct = getBatteryPercentage(context)
                val location = locationTracker.getCurrentLocation()

                // Capture stealth photo of whoever unplugged the charger
                val photoFile = try {
                    StealthCaptureBridge.capturePhoto(context, useFront = true, timeoutMs = 8000L).file
                } catch (e: Exception) {
                    Log.w(TAG, "Charger disconnect photo capture failed: ${e.message}")
                    null
                }

                val locText = if (location != null) {
                    val lat = String.format(Locale.US, "%.5f", location.latitude)
                    val lng = String.format(Locale.US, "%.5f", location.longitude)
                    "📍 <b>Current GPS:</b> $lat, $lng\n🗺️ <a href=\"https://maps.google.com/maps?q=$lat,$lng\">View on Google Maps</a>"
                } else {
                    "📍 <b>Location:</b> Signal unavailable"
                }

                val alertMsg = """
                    🔌 <b>CHARGER DISCONNECTED ALERT</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    🚨 Power cable unplugged while device screen was locked!
                    🔋 <b>Battery Level:</b> ${batteryPct}%
                    
                    $locText
                    ${if (photoFile != null) "\n📸 Intruder snapshot attached." else ""}
                    
                    <i>Send <code>/lock</code> or <code>/ring</code> to trigger emergency defenses.</i>
                """.trimIndent()

                dispatchAlert(
                    alertType = "CHARGER_UNPLUGGED",
                    message = alertMsg,
                    lat = location?.latitude,
                    lng = location?.longitude,
                    photoFile = photoFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to deliver power disconnect alert", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun dispatchAlert(
        alertType: String,
        message: String,
        lat: Double?,
        lng: Double?,
        photoFile: File?
    ) {
        val photo: File? = photoFile?.takeIf { it.exists() && it.length() > 0 }

        // 1. Send to VPS backend if enabled
        if (preferencesManager.useBackendServer) {
            try {
                val devIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                val typeBody = alertType.toRequestBody("text/plain".toMediaTypeOrNull())
                val msgBody = message.toRequestBody("text/plain".toMediaTypeOrNull())
                val latBody = lat?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val lngBody = lng?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val photoPart = photo?.let {
                    MultipartBody.Part.createFormData(
                        "photo", it.name, it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                }

                pasaBackendApi.sendDeviceAlert(
                    deviceId = devIdBody,
                    alertType = typeBody,
                    message = msgBody,
                    photo = photoPart,
                    latitude = latBody,
                    longitude = lngBody
                )
                return
            } catch (e: Exception) {
                Log.w(TAG, "VPS backend alert delivery failed, falling back to direct Telegram: ${e.message}")
            }
        }

        // 2. Direct Telegram fallback
        try {
            telegramApi.sendMessage(
                token = preferencesManager.botToken,
                request = SendMessageRequest(
                    chatId = preferencesManager.ownerChatIdLong,
                    text = message
                )
            )
            if (lat != null && lng != null) {
                telegramApi.sendLocation(
                    token = preferencesManager.botToken,
                    request = SendLocationRequest(
                        chatId = preferencesManager.ownerChatIdLong,
                        latitude = lat,
                        longitude = lng
                    )
                )
            }
            photo?.let {
                val chatIdBody = preferencesManager.ownerChatIdLong.toString()
                    .toRequestBody("text/plain".toMediaTypeOrNull())
                val captionBody = "📸 PASA final battery-beacon snapshot"
                    .toRequestBody("text/plain".toMediaTypeOrNull())
                val photoPart = MultipartBody.Part.createFormData(
                    "photo", it.name, it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                telegramApi.sendPhoto(
                    token = preferencesManager.botToken,
                    chatId = chatIdBody,
                    photo = photoPart,
                    caption = captionBody
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct Telegram alert delivery failed", e)
        }
    }

    private fun getBatteryPercentage(context: Context): Int {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(context, null, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(null, filter)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }
}
