package com.izhaanintellect.pasa.detection

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.ui.AlertMessageActivity
import com.izhaanintellect.pasa.util.SecurityActivityLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Autonomous Edge Defense Trap Manager.
 * Operates independently on device sensors without needing incoming commands:
 * 1. Snatch-and-Run Detection (Sudden acceleration spike > 26 m/s²)
 * 2. Charger Disconnect Trap
 * 3. Pickpocket Proximity Trap
 */
@Singleton
class TrapManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val telegramApi: TelegramApi,
    private val locationTracker: LocationTracker
) : SensorEventListener {

    companion object {
        private const val TAG = "PASA_TrapManager"
        private const val SNATCH_ACCEL_THRESHOLD = 26.0f // ~2.65G
        private const val SNATCH_COOLDOWN_MS = 60_000L
    }

    private var sensorManager: SensorManager? = null
    private var isMonitoring = false
    private var lastSnatchTriggerTime = 0L

    fun startMonitoring() {
        if (isMonitoring) return

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager?.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            isMonitoring = true
            Log.i(TAG, "Autonomous Traps armed (Snatch & Motion detection active)")
        } else {
            Log.w(TAG, "No accelerometer available for traps")
        }
    }

    fun stopMonitoring() {
        sensorManager?.unregisterListener(this)
        isMonitoring = false
        Log.i(TAG, "Autonomous Traps disarmed")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        if (!preferencesManager.isTrapEnabled && !preferencesManager.isSnatchTrapEnabled) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        if (magnitude > SNATCH_ACCEL_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastSnatchTriggerTime > SNATCH_COOLDOWN_MS) {
                lastSnatchTriggerTime = now
                Log.w(TAG, "SNATCH DETECTED! Acceleration spike: $magnitude m/s²")
                handleSnatchEvent(magnitude)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Triggered when sudden violent acceleration is detected.
     * Instantly locks device, initiates Kiosk lock, and snaps intruder photo.
     */
    private fun handleSnatchEvent(magnitude: Float) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = PasaDeviceAdmin.getComponentName(context)

        // 1. Immediately Lock Screen
        try {
            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                Log.i(TAG, "Device screen locked immediately via Snatch Trap")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lockNow on snatch", e)
        }

        // 2. Launch Lost Mode Guard Screen via SecurityActivityLauncher
        try {
            val alertIntent = AlertMessageActivity.createIntent(
                context = context,
                message = "🚨 SNATCH ALERT: Device locked automatically.",
                enforcePin = true
            )
            SecurityActivityLauncher.launch(
                context = context,
                intent = alertIntent,
                notificationId = AlertMessageActivity.NOTIFICATION_ID,
                notificationTitle = "🚨 SNATCH-AND-RUN DETECTED",
                notificationText = "Device automatically locked into security kiosk",
                wakeScreen = true,
                ongoing = true
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch AlertMessageActivity on snatch: ${e.message}")
        }

        // 3. Dispatch Forensics & Telegram Alert
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loc = locationTracker.getCurrentLocation()
                val locText = if (loc != null) {
                    "\n📍 <b>Snatch Location:</b> <a href=\"https://www.google.com/maps?q=${loc.latitude},${loc.longitude}\">${loc.latitude}, ${loc.longitude}</a>"
                } else ""

                val alertText = "🚨 <b>AUTONOMOUS SNATCH DEFENSE TRIGGERED!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "⚠️ The device experienced violent acceleration (<b>${String.format("%.1f", magnitude)} m/s²</b>).\n" +
                        "🔒 Device was locked automatically into Lost Mode.$locText"

                telegramApi.sendMessage(
                    token = preferencesManager.botToken,
                    request = SendMessageRequest(
                        chatId = preferencesManager.ownerChatIdLong,
                        text = alertText
                    )
                )

                // Capture perpetrator selfie
                val captureResult = StealthCaptureBridge.capturePhoto(context, useFront = true)
                captureResult.file?.let { photoFile ->
                    if (photoFile.exists() && photoFile.length() > 0) {
                        val chatIdBody = preferencesManager.ownerChatId.toRequestBody("text/plain".toMediaTypeOrNull())
                        val captionBody = "🚨 Snatch Perp Capture".toRequestBody("text/plain".toMediaTypeOrNull())
                        val fileBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("photo", photoFile.name, fileBody)

                        telegramApi.sendPhoto(
                            token = preferencesManager.botToken,
                            chatId = chatIdBody,
                            photo = part,
                            caption = captionBody
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in handleSnatchEvent", e)
            }
        }
    }

    /**
     * Triggered by PowerAlertReceiver when charger is unplugged while armed.
     */
    fun onChargerDisconnected() {
        if (!preferencesManager.isTrapEnabled && !preferencesManager.isChargerTrapEnabled) return

        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguard?.isDeviceLocked == true

        // Only trigger if device is currently locked (unauthorized unplug)
        if (isLocked) {
            Log.w(TAG, "Charger unplugged while device locked! Triggering Charger Trap.")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alertText = "🚨 <b>CHARGER DISCONNECT TRAP TRIGGERED!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "⚠️ Device was unplugged from charger while locked."

                    telegramApi.sendMessage(
                        token = preferencesManager.botToken,
                        request = SendMessageRequest(
                            chatId = preferencesManager.ownerChatIdLong,
                            text = alertText
                        )
                    )

                    val captureResult = StealthCaptureBridge.capturePhoto(context, useFront = true)
                    captureResult.file?.let { photoFile ->
                        if (photoFile.exists() && photoFile.length() > 0) {
                            val chatIdBody = preferencesManager.ownerChatId.toRequestBody("text/plain".toMediaTypeOrNull())
                            val captionBody = "🚨 Charger Disconnect Capture".toRequestBody("text/plain".toMediaTypeOrNull())
                            val fileBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData("photo", photoFile.name, fileBody)

                            telegramApi.sendPhoto(
                                token = preferencesManager.botToken,
                                chatId = chatIdBody,
                                photo = part,
                                caption = captionBody
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onChargerDisconnected", e)
                }
            }
        }
    }
}
