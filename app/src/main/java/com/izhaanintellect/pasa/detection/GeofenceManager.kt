package com.izhaanintellect.pasa.detection

import android.content.Context
import android.location.Location
import android.util.Log
import com.izhaanintellect.pasa.bot.SendLocationRequest
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.network.PasaBackendApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autonomous geofence monitor.
 * Periodically samples the device location and raises an alert the moment the
 * device transitions from inside a configured safe zone to outside it.
 * Runs a self-contained polling loop so it never competes with the /track
 * command for the single FusedLocation callback.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val telegramApi: TelegramApi,
    private val locationTracker: LocationTracker,
    private val pasaBackendApi: PasaBackendApi
) {
    companion object {
        private const val TAG = "PASA_Geofence"
        private const val CHECK_INTERVAL_MS = 90_000L        // sample every 90s
        private const val ALERT_COOLDOWN_MS = 5 * 60_000L    // at most one breach alert / 5 min
        const val DEFAULT_RADIUS_M = 200
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    private var lastBreachAlertTime = 0L

    fun startMonitoring() {
        if (job?.isActive == true) return
        job = scope.launch {
            Log.i(TAG, "Geofence monitor loop started")
            while (isActive) {
                try {
                    if (preferencesManager.geofenceEnabled &&
                        preferencesManager.geofenceConfigured &&
                        preferencesManager.isConfigured()
                    ) {
                        locationTracker.getCurrentLocation()?.let { evaluate(it) }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Geofence check error: ${e.message}")
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    fun stopMonitoring() {
        job?.cancel()
        job = null
        Log.i(TAG, "Geofence monitor loop stopped")
    }

    /**
     * Seeds the inside/outside state from the current location without alerting.
     * Call after (re)configuring the zone so the first genuine exit is what fires.
     */
    fun seedState() {
        scope.launch {
            try {
                val loc = locationTracker.getCurrentLocation() ?: return@launch
                val inside = distanceToCenter(loc) <= preferencesManager.geofenceRadiusMeters
                preferencesManager.geofenceInsideState = if (inside) 1 else 0
                Log.i(TAG, "Geofence state seeded: ${if (inside) "INSIDE" else "OUTSIDE"}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to seed geofence state: ${e.message}")
            }
        }
    }

    private fun distanceToCenter(loc: Location): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            loc.latitude, loc.longitude,
            preferencesManager.geofenceCenterLat, preferencesManager.geofenceCenterLng,
            results
        )
        return results[0]
    }

    private suspend fun evaluate(loc: Location) {
        val radius = preferencesManager.geofenceRadiusMeters
        val distance = distanceToCenter(loc)
        val decision = GeofenceEvaluator.evaluate(
            previousState = preferencesManager.geofenceInsideState,
            distanceMeters = distance.toDouble(),
            radiusMeters = radius
        )
        preferencesManager.geofenceInsideState = decision.newState

        if (decision.shouldAlert) {
            val now = System.currentTimeMillis()
            if (now - lastBreachAlertTime < ALERT_COOLDOWN_MS) return
            lastBreachAlertTime = now
            Log.w(TAG, "Geofence breach: ${distance.toInt()}m from center (radius ${radius}m)")
            dispatchBreach(loc, distance)
        }
    }

    private suspend fun dispatchBreach(loc: Location, distance: Float) {
        val lat = String.format(Locale.US, "%.5f", loc.latitude)
        val lng = String.format(Locale.US, "%.5f", loc.longitude)
        val message = """
            🚧 <b>GEOFENCE BREACH ALERT</b>
            ━━━━━━━━━━━━━━━━━━━━
            ⚠️ Your device has left its safe zone.
            📏 <b>Distance from center:</b> ~${distance.toInt()} m (zone radius ${preferencesManager.geofenceRadiusMeters} m)

            📍 <b>Current GPS:</b> $lat, $lng
            🗺️ <a href="https://maps.google.com/maps?q=$lat,$lng">Open in Google Maps</a>

            <i>Send <code>/locate</code> to keep tracking or <code>/lock</code> to secure it.</i>
        """.trimIndent()

        // 1. VPS backend first, if enabled.
        if (preferencesManager.useBackendServer) {
            try {
                pasaBackendApi.sendDeviceAlert(
                    deviceId = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    alertType = "GEOFENCE_BREACH".toRequestBody("text/plain".toMediaTypeOrNull()),
                    message = message.toRequestBody("text/plain".toMediaTypeOrNull()),
                    photo = null,
                    latitude = loc.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    longitude = loc.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                )
                return
            } catch (e: Exception) {
                Log.w(TAG, "VPS geofence alert failed, falling back to Telegram: ${e.message}")
            }
        }

        // 2. Direct Telegram fallback.
        try {
            telegramApi.sendMessage(
                token = preferencesManager.botToken,
                request = SendMessageRequest(
                    chatId = preferencesManager.ownerChatIdLong,
                    text = message
                )
            )
            telegramApi.sendLocation(
                token = preferencesManager.botToken,
                request = SendLocationRequest(
                    chatId = preferencesManager.ownerChatIdLong,
                    latitude = loc.latitude,
                    longitude = loc.longitude
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Direct Telegram geofence alert failed", e)
        }
    }
}
