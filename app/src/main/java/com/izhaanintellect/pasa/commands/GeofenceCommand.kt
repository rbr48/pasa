package com.izhaanintellect.pasa.commands

import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.detection.GeofenceManager
import com.izhaanintellect.pasa.location.LocationTracker
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configures a geofence "safe zone". When armed, PASA raises an alert the
 * moment the device leaves the zone.
 *
 *   /geofence here [radius_m]  — set the zone center to the current location and arm
 *   /geofence on | off         — arm / disarm an already-configured zone
 *   /geofence radius <m>       — change the zone radius
 *   /geofence status           — show current configuration
 */
@Singleton
class GeofenceCommand @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val locationTracker: LocationTracker,
    private val geofenceManager: GeofenceManager
) : Command {

    override val name = "/geofence"
    override val description = "Alert when the device leaves a safe zone"
    override val usage = "/geofence [status|here <radius_m>|on|off|radius <m>]"

    companion object {
        private const val TAG = "PASA_GeofenceCmd"
        private const val MIN_RADIUS_M = 50
        private const val MAX_RADIUS_M = 50000
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        return when (args.getOrNull(0)?.lowercase()) {
            null, "status" -> statusResult()
            "here", "set" -> setHere(args.getOrNull(1))
            "on", "arm", "enable" -> arm()
            "off", "disarm", "disable" -> disarm()
            "radius" -> setRadius(args.getOrNull(1))
            else -> CommandResult(
                success = false,
                message = "⚠️ Unknown action. Usage:\n<code>$usage</code>"
            )
        }
    }

    private suspend fun setHere(radiusArg: String?): CommandResult {
        val loc = locationTracker.getCurrentLocation()
            ?: return CommandResult(
                success = false,
                message = "❌ Could not acquire a GPS fix to set the zone. Ensure location is on and retry."
            )

        val radius = radiusArg?.toIntOrNull()?.coerceIn(MIN_RADIUS_M, MAX_RADIUS_M)
            ?: preferencesManager.geofenceRadiusMeters.takeIf { it > 0 }
            ?: GeofenceManager.DEFAULT_RADIUS_M

        preferencesManager.geofenceCenterLat = loc.latitude
        preferencesManager.geofenceCenterLng = loc.longitude
        preferencesManager.geofenceRadiusMeters = radius
        preferencesManager.geofenceConfigured = true
        preferencesManager.geofenceEnabled = true
        // We are standing at the center, so we are inside; first exit is what fires.
        preferencesManager.geofenceInsideState = 1
        geofenceManager.startMonitoring()

        Log.i(TAG, "Geofence set at ${loc.latitude},${loc.longitude} r=${radius}m")
        val lat = String.format(Locale.US, "%.5f", loc.latitude)
        val lng = String.format(Locale.US, "%.5f", loc.longitude)
        return CommandResult(
            success = true,
            message = "🟢 <b>Safe zone armed</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                    "📍 <b>Center:</b> $lat, $lng\n" +
                    "📏 <b>Radius:</b> $radius m\n\n" +
                    "You'll get an alert if the device leaves this zone. Disarm with <code>/geofence off</code>."
        )
    }

    private fun arm(): CommandResult {
        if (!preferencesManager.geofenceConfigured) {
            return CommandResult(
                success = false,
                message = "⚠️ No zone set yet. Use <code>/geofence here</code> at the location you want to protect."
            )
        }
        preferencesManager.geofenceEnabled = true
        geofenceManager.startMonitoring()
        geofenceManager.seedState()
        return CommandResult(success = true, message = "🟢 <b>Geofence armed.</b>")
    }

    private fun disarm(): CommandResult {
        preferencesManager.geofenceEnabled = false
        return CommandResult(success = true, message = "🔴 <b>Geofence disarmed.</b>")
    }

    private fun setRadius(radiusArg: String?): CommandResult {
        val radius = radiusArg?.toIntOrNull()
            ?: return CommandResult(
                success = false,
                message = "⚠️ Provide a radius in meters, e.g. <code>/geofence radius 300</code>."
            )
        val clamped = radius.coerceIn(MIN_RADIUS_M, MAX_RADIUS_M)
        preferencesManager.geofenceRadiusMeters = clamped
        geofenceManager.seedState()
        return CommandResult(
            success = true,
            message = "📏 <b>Zone radius set to $clamped m.</b>" +
                    if (clamped != radius) " (clamped to allowed range)" else ""
        )
    }

    private fun statusResult(): CommandResult {
        val configured = preferencesManager.geofenceConfigured
        val enabled = preferencesManager.geofenceEnabled
        val body = if (!configured) {
            "• Status: 🔴 <b>Not configured</b>\n\nSet one with <code>/geofence here [radius_m]</code>."
        } else {
            val lat = String.format(Locale.US, "%.5f", preferencesManager.geofenceCenterLat)
            val lng = String.format(Locale.US, "%.5f", preferencesManager.geofenceCenterLng)
            val insideLabel = when (preferencesManager.geofenceInsideState) {
                1 -> "🏠 Inside"
                0 -> "🚶 Outside"
                else -> "❔ Unknown"
            }
            "• Status: ${if (enabled) "🟢 <b>ARMED</b>" else "🔴 <b>DISARMED</b>"}\n" +
                    "• Center: <code>$lat, $lng</code>\n" +
                    "• Radius: ${preferencesManager.geofenceRadiusMeters} m\n" +
                    "• Current position: $insideLabel"
        }
        return CommandResult(
            success = true,
            message = "🛰️ <b>Geofence Safe Zone</b>\n━━━━━━━━━━━━━━━━━━━━\n$body\n\n" +
                    "<b>Commands:</b>\n" +
                    "• <code>/geofence here 200</code> — set zone here (200 m)\n" +
                    "• <code>/geofence on</code> / <code>/geofence off</code>\n" +
                    "• <code>/geofence radius 300</code>"
        )
    }
}
