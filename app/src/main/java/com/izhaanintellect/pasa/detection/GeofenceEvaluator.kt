package com.izhaanintellect.pasa.detection

/**
 * Pure geofence transition logic, separated from Android location APIs so it can
 * be unit-tested in isolation. Decides the new inside/outside state and whether
 * an alert should fire (only on a genuine inside -> outside transition).
 */
object GeofenceEvaluator {

    const val STATE_UNKNOWN = -1
    const val STATE_OUTSIDE = 0
    const val STATE_INSIDE = 1

    data class Decision(
        val nowInside: Boolean,
        val shouldAlert: Boolean,
        val shouldNotifyReturn: Boolean,
        val newState: Int
    )

    fun evaluate(previousState: Int, distanceMeters: Double, radiusMeters: Int): Decision {
        val nowInside = distanceMeters <= radiusMeters
        val shouldAlert = previousState == STATE_INSIDE && !nowInside
        val shouldNotifyReturn = previousState == STATE_OUTSIDE && nowInside
        val newState = if (nowInside) STATE_INSIDE else STATE_OUTSIDE
        return Decision(
            nowInside = nowInside,
            shouldAlert = shouldAlert,
            shouldNotifyReturn = shouldNotifyReturn,
            newState = newState
        )
    }
}
