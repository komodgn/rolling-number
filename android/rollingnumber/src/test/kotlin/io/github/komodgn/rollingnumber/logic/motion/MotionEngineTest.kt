package io.github.komodgn.rollingnumber.logic.motion

import io.github.komodgn.rollingnumber.logic.DecimalValue
import io.github.komodgn.rollingnumber.logic.FormattedNumber
import io.github.komodgn.rollingnumber.logic.NumberSymbols
import io.github.komodgn.rollingnumber.logic.Tokenizer
import io.github.komodgn.rollingnumber.logic.TransitionDiff
import io.github.komodgn.rollingnumber.logic.Trend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionEngineTest {
    private val symbols = NumberSymbols(".", ",")
    private fun n(text: String, value: String) = FormattedNumber(text, DecimalValue.parse(value)!!, symbols)

    private fun engineAt(from: String, fromValue: String, to: String, toValue: String): MotionEngine {
        val engine = MotionEngine()
        val a = n(from, fromValue)
        engine.showImmediately(Tokenizer.tokenize(a))
        engine.apply(TransitionDiff.compute(a, n(to, toValue), Trend.Auto))
        return engine
    }

    /** Steps every [frameMs] from t=0 until settled; returns settle time in seconds. */
    private fun settleSeconds(engine: MotionEngine, frameMs: Long = 16): Double {
        var t = 0L
        engine.step(0)
        while (engine.isAnimating) {
            t += frameMs
            engine.step(t * 1_000_000)
            check(t < 5_000) { "did not settle" }
        }
        return t / 1000.0
    }

    @Test
    fun defaultCurvesSettleWithinTargetRange() {
        val seconds = settleSeconds(engineAt("₩9,900", "9900", "₩12,900", "12900"))
        assertTrue("settled in $seconds s, expected 0.3..0.6", seconds in 0.3..0.6)
    }

    @Test
    fun resultsDependOnElapsedTimeOnlyNotFrameRate() {
        val e8 = engineAt("₩9,900", "9900", "₩12,900", "12900")
        val e16 = engineAt("₩9,900", "9900", "₩12,900", "12900")
        val eJitter = engineAt("₩9,900", "9900", "₩12,900", "12900")
        listOf(e8, e16, eJitter).forEach { it.step(0) }
        val jitter = longArrayOf(5, 13, 7, 19, 4, 12, 20, 16)
        var tj = 0L
        var i = 0
        for (t in 48L..480L step 48L) {
            var t8 = t - 48
            while (t8 < t) { t8 += 8; e8.step(t8 * 1_000_000) }
            var t16 = t - 48
            while (t16 < t) { t16 += 16; e16.step(t16 * 1_000_000) }
            while (tj < t) { tj = minOf(t, tj + jitter[i++ % jitter.size]); eJitter.step(tj * 1_000_000) }
            val a = e8.frame({ 10f }, 10f)
            val b = e16.frame({ 10f }, 10f)
            val c = eJitter.frame({ 10f }, 10f)
            assertEquals("t=$t 8 vs 16 ms", a, b)
            assertEquals("t=$t 8 ms vs jitter", a, c)
        }
    }

    @Test
    fun enteringDigitStartsBelowTransparentAndRests() {
        val engine = engineAt("₩9,900", "9900", "₩12,900", "12900")
        val entering = engine.elementStates.first { it.key == "I4" }
        val layer = entering.layers.single()
        assertEquals(1.0, layer.offset.value, 0.0)
        assertEquals(0.0, layer.alpha.value, 0.0)
        assertEquals(0.0, entering.presence.value, 0.0)
        settleSeconds(engine)
        assertEquals(0.0, layer.offset.value, 0.0)
        assertEquals(1.0, layer.alpha.value, 0.0)
        assertEquals(1.0, entering.presence.value, 0.0)
    }

    @Test
    fun exitingDigitLeavesDownwardAndIsRemoved() {
        val engine = engineAt("₩12,900", "12900", "₩9,900", "9900")
        val exiting = engine.elementStates.first { it.key == "I4" }
        assertTrue(exiting.exiting)
        assertEquals(1.0, exiting.layers.single().offset.target, 0.0)
        settleSeconds(engine)
        assertNull(engine.elementStates.firstOrNull { it.key == "I4" })
    }

    @Test
    fun rollingDigitKeepsConstantTabularWidth() {
        val engine = engineAt("₩15,000", "15000", "₩12,000", "12000")
        engine.step(0)
        var t = 0L
        while (engine.isAnimating) {
            t += 16
            engine.step(t * 1_000_000)
            val item = engine.frame({ 10f }, 12f).items.first { it.key == "I3" }
            assertEquals("t=$t", 12f, item.width, 1e-4f)
        }
    }

    @Test
    fun crossfadeWidthMovesContinuouslyBetweenTexts() {
        val engine = engineAt("1.2K", "1200", "1.2MM", "1200000")
        val measure: (String) -> Float = { it.length * 10f }
        var last = engine.frame(measure, null).items.first { it.key == "Ls0" }.width
        assertEquals(10f, last, 1e-4f)
        engine.step(0)
        var t = 0L
        while (engine.isAnimating) {
            t += 16
            engine.step(t * 1_000_000)
            val w = engine.frame(measure, null).items.first { it.key == "Ls0" }.width
            assertTrue("jump at t=$t: $last -> $w", kotlin.math.abs(w - last) <= 0.35f * 10f)
            last = w
        }
        assertEquals(20f, last, 1e-4f)
    }

    @Test
    fun settledValuesSnapExactlyToTargets() {
        val engine = engineAt("₩15,000", "15000", "₩12,000", "12000")
        settleSeconds(engine)
        assertFalse(engine.isAnimating)
        engine.elementStates.forEach { e ->
            assertEquals(1.0, e.presence.value, 0.0)
            e.layers.forEach { l ->
                assertEquals(0.0, l.offset.value, 0.0)
                assertEquals(1.0, l.alpha.value, 0.0)
            }
            assertEquals(1, e.layers.size)
        }
    }
}
