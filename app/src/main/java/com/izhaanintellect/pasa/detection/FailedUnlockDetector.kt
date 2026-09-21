package com.izhaanintellect.pasa.detection

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.bot.SendLocationRequest
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCameraManager
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects repeated failed unlock attempts.
 * After threshold (default 3), captures front photo and location,
 * dispatching an emergency alert via Telegram.
 */
@Singleton
class FailedUnlockDetector @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val telegramApi: TelegramApi,
    private val pasaBackendApi: com.izhaanintellect.pasa.network.PasaBackendApi,
    private val cameraManager: StealthCameraManager,
    private val locationTracker: LocationTracker
) {
    companion object {
        private const val TAG = "PASA_Intruder"
        private const val ALERT_THRESHOLD = 3
    }

    suspend fun onFailedAttempt() {
        val count = preferencesManager.failedUnlockCount + 1
        preferencesManager.failedUnlockCount = count

        Log.w(TAG, "Failed unlock attempt #$count")

        if (count >= ALERT_THRESHOLD && preferencesManager.isConfigured()) {
            Log.w(TAG, "Intruder threshold reached ($ALERT_THRESHOLD) — sending security alert")
            triggerAlert(count)
        }
    }

    private suspend fun triggerAlert(attemptCount: Int) {
        try {
            val photoFile = try {
                cameraManager.capturePhoto(useFrontCamera = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture intruder photo", e)
                null
            }

            var latVal: Double? = null
            var lngVal: Double? = null
            val locationStr = try {
                val location = locationTracker.getCurrentLocation()
                if (location != null) {
                    latVal = location.latitude
                    lngVal = location.longitude
                    preferencesManager.lastKnownLatitude = location.latitude
                    preferencesManager.lastKnownLongitude = location.longitude
                    "📍 ${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}\n" +
                            "🗺️ https://maps.google.com/maps?q=${location.latitude},${location.longitude}"
                } else {
                    "📍 Location unavailable"
                }
            } catch (e: Exception) {
                "📍 Location retrieval error"
            }

            val alertMessage = """
                ⚠️ <b>$attemptCount failed unlock attempts</b> recorded on your device!
                
                $locationStr
                
                ${if (photoFile != null) "📸 Intruder photo attached below." else "📸 Front camera capture unavailable."}
                
                🔒 Send <code>/lock</code> to secure immediately
                🗑️ Send <code>/wipe</code> if device is stolen
            """.trimIndent()

            // Direct Telegram dispatch (Strategy 1: Zero-Storage, zero server media persistence)
            if (preferencesManager.botToken.isNotBlank() && preferencesManager.ownerChatIdLong != 0L) {
                try {
                    // 1. Text alert
                    telegramApi.sendMessage(
                        token = preferencesManager.botToken,
                        request = SendMessageRequest(
                            chatId = preferencesManager.ownerChatIdLong,
                            text = "🚨 <b>INTRUSION ALERT DETECTED!</b>\n\n$alertMessage"
                        )
                    )

                    // 2. Intruder photo directly to Telegram
                    photoFile?.let { file ->
                        if (file.exists() && file.length() > 0) {
                            val chatIdBody = preferencesManager.ownerChatIdLong.toString()
                                .toRequestBody("text/plain".toMediaTypeOrNull())
                            val captionBody = "🚨 PASA Intruder Photo (Attempt #$attemptCount)"
                                .toRequestBody("text/plain".toMediaTypeOrNull())
                            val photoBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val photoPart = MultipartBody.Part.createFormData("photo", file.name, photoBody)

                            telegramApi.sendPhoto(
                                token = preferencesManager.botToken,
                                chatId = chatIdBody,
                                photo = photoPart,
                                caption = captionBody
                            )
                        }
                    }

                    // 3. Location pin directly to Telegram
                    if (latVal != null && lngVal != null) {
                        try {
                            telegramApi.sendLocation(
                                token = preferencesManager.botToken,
                                request = SendLocationRequest(
                                    chatId = preferencesManager.ownerChatIdLong,
                                    latitude = latVal,
                                    longitude = lngVal
                                )
                            )
                        } catch (locErr: Exception) {
                            Log.w(TAG, "Failed to send intruder location pin: ${locErr.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed direct Telegram intruder alert: ${e.message}")
                } finally {
                    // Immediately shred local intruder photo after dispatch
                    try {
                        photoFile?.let { if (it.exists()) it.delete() }
                    } catch (_: Exception) {}
                }
            }

            Log.i(TAG, "Intrusion alert and photo dispatched")
            preferencesManager.failedUnlockCount = 0

        } catch (e: Exception) {
            Log.e(TAG, "Failed to deliver intrusion alert", e)
        }
    }

    fun resetCounter() {
        preferencesManager.failedUnlockCount = 0
    }
}
