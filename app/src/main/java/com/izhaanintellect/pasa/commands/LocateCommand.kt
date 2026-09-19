package com.izhaanintellect.pasa.commands

import android.util.Log
import com.izhaanintellect.pasa.location.LocationTracker
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles single-shot GPS location requests.
 */
@Singleton
class LocateCommand @Inject constructor(
    private val locationTracker: LocationTracker
) : Command {

    override val name = "/locate"
    override val description = "Get current GPS coordinates and Google Maps pin"
    override val usage = "/locate"

    companion object {
        private const val TAG = "PASA_Locate"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        Log.i(TAG, "Resolving device location...")

        val location = locationTracker.getCurrentLocation()

        return if (location != null) {
            val formatted = locationTracker.formatLocation(location)
            Log.i(TAG, "Location resolved: ${location.latitude}, ${location.longitude}")
            CommandResult(
                success = true,
                message = formatted,
                location = Pair(location.latitude, location.longitude)
            )
        } else {
            Log.w(TAG, "Failed to resolve location")
            CommandResult(
                success = false,
                message = "❌ Unable to determine location. Please verify that GPS/Location is enabled and permissions are granted."
            )
        }
    }
}
