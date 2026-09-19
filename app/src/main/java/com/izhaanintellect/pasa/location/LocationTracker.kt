package com.izhaanintellect.pasa.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages all device location operations using Google Play Services FusedLocationProvider.
 * Provides single-shot location retrieval and continuous tracking.
 */
@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PASA_Location"
    }

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    /**
     * Gets the current device location with high accuracy.
     * Falls back to last known location if fresh location is unavailable.
     */
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                try {
                    val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()

                    @Suppress("MissingPermission")
                    fusedClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(location)
                        } else {
                            @Suppress("MissingPermission")
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                continuation.resume(lastLoc)
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "getCurrentLocation failed, trying lastLocation", e)
                        @Suppress("MissingPermission")
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                            continuation.resume(lastLoc)
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }

                    continuation.invokeOnCancellation {
                        cancellationTokenSource.cancel()
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException getting location", e)
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }

    /**
     * Starts continuous location tracking at the given interval in minutes.
     */
    fun startTracking(intervalMinutes: Int, onLocationUpdate: (Location) -> Unit) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot start tracking: no permission")
            return
        }

        stopTracking()

        val intervalMs = intervalMinutes * 60 * 1000L
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d(TAG, "Periodic location update: ${location.latitude}, ${location.longitude}")
                    onLocationUpdate(location)
                }
            }
        }

        try {
            @Suppress("MissingPermission")
            fusedClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.i(TAG, "Continuous tracking started (interval: ${intervalMinutes}m)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to request location updates", e)
        }
    }

    fun stopTracking() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
            locationCallback = null
            Log.i(TAG, "Continuous tracking stopped")
        }
    }

    fun formatLocation(location: Location): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeStr = dateFormat.format(Date(location.time))
        val lat = String.format(Locale.US, "%.6f", location.latitude)
        val lng = String.format(Locale.US, "%.6f", location.longitude)
        val accuracy = String.format(Locale.US, "%.1f", location.accuracy)
        val mapsUrl = "https://maps.google.com/maps?q=$lat,$lng"

        return """
            📍 <b>Current Device Location</b>
            ━━━━━━━━━━━━━━━━━━━━
            🌐 <b>Coordinates:</b> <code>$lat, $lng</code>
            🎯 <b>Accuracy:</b> ±${accuracy}m
            ⏱️ <b>Time:</b> $timeStr
            ${if (location.hasAltitude()) "⛰️ <b>Altitude:</b> ${String.format(Locale.US, "%.1f", location.altitude)}m\n" else ""}${if (location.hasSpeed()) "🚗 <b>Speed:</b> ${String.format(Locale.US, "%.1f", location.speed * 3.6)} km/h\n" else ""}
            🗺️ <a href="$mapsUrl">View on Google Maps</a>
        """.trimIndent()
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
