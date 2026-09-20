package com.izhaanintellect.pasa.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceEvaluatorTest {

    @Test
    fun insideToOutside_fires() {
        val d = GeofenceEvaluator.evaluate(
            previousState = GeofenceEvaluator.STATE_INSIDE,
            distanceMeters = 350.0,
            radiusMeters = 200
        )
        assertFalse(d.nowInside)
        assertTrue("leaving the zone must alert", d.shouldAlert)
        assertEquals(GeofenceEvaluator.STATE_OUTSIDE, d.newState)
    }

    @Test
    fun stayingInside_doesNotFire() {
        val d = GeofenceEvaluator.evaluate(GeofenceEvaluator.STATE_INSIDE, 50.0, 200)
        assertTrue(d.nowInside)
        assertFalse(d.shouldAlert)
        assertEquals(GeofenceEvaluator.STATE_INSIDE, d.newState)
    }

    @Test
    fun stayingOutside_doesNotFire() {
        val d = GeofenceEvaluator.evaluate(GeofenceEvaluator.STATE_OUTSIDE, 500.0, 200)
        assertFalse(d.shouldAlert)
        assertEquals(GeofenceEvaluator.STATE_OUTSIDE, d.newState)
    }

    @Test
    fun unknownThenOutside_doesNotFire() {
        // First-ever reading outside the zone must not raise a false breach.
        val d = GeofenceEvaluator.evaluate(GeofenceEvaluator.STATE_UNKNOWN, 500.0, 200)
        assertFalse(d.shouldAlert)
        assertEquals(GeofenceEvaluator.STATE_OUTSIDE, d.newState)
    }

    @Test
    fun exactlyOnRadius_isInside() {
        val d = GeofenceEvaluator.evaluate(GeofenceEvaluator.STATE_INSIDE, 200.0, 200)
        assertTrue(d.nowInside)
        assertFalse(d.shouldAlert)
        assertFalse(d.shouldNotifyReturn)
    }

    @Test
    fun outsideToInside_notifiesReturn() {
        val d = GeofenceEvaluator.evaluate(
            previousState = GeofenceEvaluator.STATE_OUTSIDE,
            distanceMeters = 150.0,
            radiusMeters = 200
        )
        assertTrue(d.nowInside)
        assertFalse(d.shouldAlert)
        assertTrue("returning to safe zone must notify", d.shouldNotifyReturn)
        assertEquals(GeofenceEvaluator.STATE_INSIDE, d.newState)
    }
}
