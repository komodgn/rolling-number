package io.github.komodgn.rollingnumber.logic.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TweenTest {
    @Test
    fun cubicBezierEndpointsAndLinearMidpoint() {
        assertEquals(0.0, CubicBezier.ease(0.0, 0.4, 0.0, 0.2, 1.0), 0.0)
        assertEquals(1.0, CubicBezier.ease(1.0, 0.4, 0.0, 0.2, 1.0), 0.0)
        assertEquals(0.5, CubicBezier.ease(0.5, 0.0, 0.0, 1.0, 1.0), 1e-6)
        // Standard ease-out curve is ahead of linear at the midpoint.
        assertTrue(CubicBezier.ease(0.5, 0.4, 0.0, 0.2, 1.0) > 0.5)
    }

    @Test
    fun tweenCompletesExactlyAtItsDuration() {
        val v = AnimatedValue(0.0, MotionCurve.Tween(100.0, 0.0, 0.0, 1.0, 1.0), 0.001, 0.01)
        v.animateTo(1.0)
        repeat(24) { v.step(STEP_SECONDS) } // 96 ms
        assertFalse(v.isSettled)
        assertEquals(0.96, v.value, 1e-6)
        v.step(STEP_SECONDS) // 100 ms
        assertTrue(v.isSettled)
        assertEquals(1.0, v.value, 0.0)
    }

    @Test
    fun interruptedTweenRestartsFromCurrentValue() {
        val v = AnimatedValue(0.0, MotionCurve.Tween(100.0, 0.0, 0.0, 1.0, 1.0), 0.001, 0.01)
        v.animateTo(1.0)
        repeat(10) { v.step(STEP_SECONDS) } // 40 ms → 0.4
        val atInterrupt = v.value
        assertEquals(0.4, atInterrupt, 1e-6)
        v.animateTo(0.0)
        v.step(STEP_SECONDS)
        assertTrue("no jump after retarget", kotlin.math.abs(v.value - atInterrupt) < 0.05)
        repeat(30) { v.step(STEP_SECONDS) }
        assertEquals(0.0, v.value, 0.0)
    }
}
