package com.izhaanintellect.pasa.commands

import android.location.Location
import android.util.Log
import com.izhaanintellect.pasa.bot.SendLocationRequest
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles continuous periodic location tracking with immediate initial GPS fix.
 */
@Singleton
class TrackCommand @Inject constructor(
    private val locationTracker: LocationTracker,
    private val preferencesManager: PreferencesManager,
    private val telegramApi: TelegramApi
) : Command {

    override val name = "/track"
    override val description = "Start or stop continuous location tracking"
    override val usage = "/track <minutes> | /track stop"

    companion object {
        private const val TAG = "PASA_Track"
        private const val DEFAULT_INTERVAL = 5
        private const val MAX_INTERVAL = 60
        private const val MAX_DURATION_MINUTES = 30
    }
    
    private var updateCount = 0
    private var startTimeMs = 0L
    private var trackingJob: Job? = null

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val subCommand = args.firstOrNull()?.lowercase()

        return when {
            subCommand == "stop" || subCommand == "off" -> stopTracking()
            subCommand != null && subCommand.toIntOrNull() != null -> {
                val interval = subCommand.toInt().coerceIn(1, MAX_INTERVAL)
                startTracking(interval, chatId)
            }
            else -> {
                val interval = if (preferencesManager.isTrackingActive) preferencesManager.trackingIntervalMinutes else DEFAULT_INTERVAL
                startTracking(interval, chatId)
            }
        }
    }

    private suspend fun startTracking(intervalMinutes: Int, chatId: Long): CommandResult {
        preferencesManager.isTrackingActive = true
        preferencesManager.trackingIntervalMinutes = intervalMinutes
        updateCount = 0
        startTimeMs = System.currentTimeMillis()

        trackingJob?.cancel()

        // 1. Fetch immediate initial location fix so operator gets instant feedback
        val initialLoc = locationTracker.getCurrentLocation()
        if (initialLoc != null) {
            sendTrackingUpdate(initialLoc, chatId)
        }

        // 2. Register with LocationTracker for hardware fused updates
        locationTracker.startTracking(intervalMinutes) { location ->
            sendTrackingUpdate(location, chatId)
        }

        // 3. Active periodic coroutine polling loop (guarantees updates every intervalMinutes even when phone is stationary)
        trackingJob = CoroutineScope(Dispatchers.IO).launch {
            val intervalMs = intervalMinutes * 60 * 1000L
            while (isActive && preferencesManager.isTrackingActive) {
                delay(intervalMs)
                if (!isActive || !preferencesManager.isTrackingActive) break

                val elapsedTimeMinutes = (System.currentTimeMillis() - startTimeMs) / (1000 * 60)
                if (elapsedTimeMinutes >= MAX_DURATION_MINUTES) {
                    try {
                        telegramApi.sendMessage(
                            preferencesManager.botToken,
                            SendMessageRequest(chatId, "⏱️ <b>Tracking Auto-Stopped</b>\nMaximum tracking duration of $MAX_DURATION_MINUTES minutes reached.")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send auto-stop message", e)
                    }
                    stopTracking()
                    break
                }

                val loc = locationTracker.getCurrentLocation()
                if (loc != null) {
                    sendTrackingUpdate(loc, chatId)
                }
            }
        }

        val initialCoords = if (initialLoc != null) {
            "\n🌐 <b>Initial Fix:</b> <code>${String.format(Locale.US, "%.6f, %.6f", initialLoc.latitude, initialLoc.longitude)}</code> (±${String.format(Locale.US, "%.1f", initialLoc.accuracy)}m)"
        } else {
            "\n🛰️ <i>Acquiring high-accuracy satellite lock...</i>"
        }

        Log.i(TAG, "Location tracking started ($intervalMinutes min interval)")
        return CommandResult(
            success = true,
            message = "📡 <b>Continuous GPS Tracking Activated</b>\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "📍 Interval: every <b>$intervalMinutes minutes</b>\n" +
                    "⏱️ Auto-stop: <b>$MAX_DURATION_MINUTES minutes</b>" +
                    initialCoords + "\n\n" +
                    "Send <code>/track stop</code> to deactivate.",
            location = initialLoc?.let { Pair(it.latitude, it.longitude) }
        )
    }

    private fun sendTrackingUpdate(location: Location, chatId: Long) {
        preferencesManager.lastKnownLatitude = location.latitude
        preferencesManager.lastKnownLongitude = location.longitude
        updateCount++

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lat = String.format(Locale.US, "%.6f", location.latitude)
                val lng = String.format(Locale.US, "%.6f", location.longitude)
                val accuracy = String.format(Locale.US, "%.1f", location.accuracy)
                val mapsUrl = "https://maps.google.com/maps?q=$lat,$lng"
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val timeStr = dateFormat.format(Date(location.time))

                var speedStr = ""
                if (location.hasSpeed()) {
                    speedStr = "🚗 <b>Speed:</b> ${String.format(Locale.US, "%.1f", location.speed * 3.6)} km/h\n"
                }

                val msgText = "📍 <b>Tracking Update #$updateCount</b>\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "🌐 <b>Coordinates:</b> <code>$lat, $lng</code>\n" +
                        "🎯 <b>Accuracy:</b> ±${accuracy}m\n" +
                        "⏱️ <b>Time:</b> $timeStr\n" +
                        speedStr +
                        "🗺️ <a href=\"$mapsUrl\">View on Google Maps</a>"

                telegramApi.sendLocation(
                    preferencesManager.botToken,
                    SendLocationRequest(chatId, location.latitude, location.longitude)
                )

                telegramApi.sendMessage(
                    preferencesManager.botToken,
                    SendMessageRequest(chatId, msgText)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send location update to Telegram", e)
            }
        }
    }

    private fun stopTracking(): CommandResult {
        preferencesManager.isTrackingActive = false
        trackingJob?.cancel()
        trackingJob = null
        locationTracker.stopTracking()
        Log.i(TAG, "Location tracking deactivated")
        return CommandResult(
            success = true,
            message = "📡 <b>Continuous GPS tracking deactivated.</b>"
        )
    }
}
