package com.izhaanintellect.pasa.commands

import android.util.Log
import com.izhaanintellect.pasa.bot.SendLocationRequest
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles continuous periodic location tracking.
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

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val subCommand = args.firstOrNull()?.lowercase()

        return when {
            subCommand == "stop" || subCommand == "off" -> stopTracking()
            subCommand != null && subCommand.toIntOrNull() != null -> {
                val interval = subCommand.toInt().coerceIn(1, MAX_INTERVAL)
                startTracking(interval, chatId)
            }
            else -> {
                if (preferencesManager.isTrackingActive) {
                    CommandResult(
                        success = true,
                        message = "📡 Tracking is currently <b>ACTIVE</b> (interval: ${preferencesManager.trackingIntervalMinutes} min).\n" +
                                "Send <code>/track stop</code> to turn it off."
                    )
                } else {
                    startTracking(DEFAULT_INTERVAL, chatId)
                }
            }
        }
    }

    private fun startTracking(intervalMinutes: Int, chatId: Long): CommandResult {
        preferencesManager.isTrackingActive = true
        preferencesManager.trackingIntervalMinutes = intervalMinutes
        updateCount = 0
        startTimeMs = System.currentTimeMillis()

        locationTracker.startTracking(intervalMinutes) { location ->
            preferencesManager.lastKnownLatitude = location.latitude
            preferencesManager.lastKnownLongitude = location.longitude
            Log.d(TAG, "Location record saved: ${location.latitude}, ${location.longitude}")
            
            updateCount++
            
            val elapsedTimeMinutes = (System.currentTimeMillis() - startTimeMs) / (1000 * 60)
            if (elapsedTimeMinutes >= MAX_DURATION_MINUTES) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        telegramApi.sendMessage(
                            preferencesManager.botToken,
                            SendMessageRequest(chatId, "⏱️ <b>Tracking Auto-Stopped</b>\nMaximum tracking duration of $MAX_DURATION_MINUTES minutes reached.")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send auto-stop message", e)
                    }
                }
                stopTracking()
            } else {
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
        }

        Log.i(TAG, "Location tracking started ($intervalMinutes min interval)")
        return CommandResult(
            success = true,
            message = "📡 <b>Continuous Tracking Activated</b>\n" +
                    "📍 Interval: every <b>$intervalMinutes minutes</b>\n" +
                    "⏱️ Auto-stop after <b>$MAX_DURATION_MINUTES minutes</b>\n" +
                    "Send <code>/track stop</code> to deactivate early."
        )
    }

    private fun stopTracking(): CommandResult {
        preferencesManager.isTrackingActive = false
        locationTracker.stopTracking()
        Log.i(TAG, "Location tracking deactivated")
        return CommandResult(
            success = true,
            message = "📡 Continuous location tracking deactivated."
        )
    }
}
