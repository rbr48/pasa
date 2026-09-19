package com.izhaanintellect.pasa.detection

import android.app.KeyguardManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Detects device movement while the screen is locked.
 * Sends a motion alert to the owner with a cooldown to avoid repeated spam.
 */
@Singleton
class MotionDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val telegramApi: TelegramApi,
    private val locationTracker: LocationTracker
) : SensorEventListener {

    companion object {
        private const val TAG = "PASA_Motion"
        private const val MOTION_THRESHOLD = 15.0f
        private const val COOLDOWN_MS = 5 * 60 * 1000L // 5 min
    }

    private var sensorManager: SensorManager? = null
    private var lastAlertTime = 0L
    private var isMonitoring = false

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
            Log.i(TAG, "Motion detection armed")
        } else {
            Log.w(TAG, "No accelerometer available on device")
        }
    }

    fun stopMonitoring() {
        sensorManager?.unregisterListener(this)
        isMonitoring = false
        Log.i(TAG, "Motion detection disarmed")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(x * x + y * y + z * z)

        if (magnitude > MOTION_THRESHOLD && isDeviceLocked()) {
            val now = System.currentTimeMillis()
            if (now - lastAlertTime > COOLDOWN_MS) {
                lastAlertTime = now
                Log.w(TAG, "Movement detected while locked! Magnitude: $magnitude")
                sendMotionAlert()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isDeviceLocked
    }

    private fun sendMotionAlert() {
        if (!preferencesManager.isConfigured()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locationStr = try {
                    val location = locationTracker.getCurrentLocation()
                    if (location != null) {
                        "📍 ${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}\n" +
                                "🗺️ https://maps.google.com/maps?q=${location.latitude},${location.longitude}"
                    } else {
                        "📍 Location unavailable"
                    }
                } catch (e: Exception) {
                    "📍 Location error"
                }

                telegramApi.sendMessage(
                    token = preferencesManager.botToken,
                    request = SendMessageRequest(
                        chatId = preferencesManager.ownerChatIdLong,
                        text = """
                            📳 <b>DEVICE MOVEMENT DETECTED!</b>
                            
                            Your device was moved while the screen was locked.
                            
                            $locationStr
                            
                            Send <code>/snap front</code> to capture a photo of whoever picked it up.
                        """.trimIndent()
                    )
                )

                Log.i(TAG, "Motion alert sent to owner")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send motion alert", e)
            }
        }
    }
}
