package io.github.komodgn.rollingnumber.logic.motion

import io.github.komodgn.rollingnumber.logic.DecimalValue
import io.github.komodgn.rollingnumber.logic.FormattedNumber
import io.github.komodgn.rollingnumber.logic.NumberSymbols
import io.github.komodgn.rollingnumber.logic.Tokenizer
import io.github.komodgn.rollingnumber.logic.TransitionDiff
import io.github.komodgn.rollingnumber.logic.Trend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** US3 / SC-006: interrupted transitions never jump or duplicate, sampled every 16 ms. */
class MotionContinuityTest {
    private val symbols = NumberSymbols(".", ",")
    private fun n(text: String) = FormattedNumber(text, DecimalValue.parse(text.filter { it.isDigit() })!!, symbols)

    private val digitWidth = 20f
    private val limit = 0.35

    private data class Sample(val offset: Double, val alpha: Double)

    /** Snapshot of every layer keyed by "elementKey/text", plus element widths. */
    private fun snapshot(engine: MotionEngine): Pair<Map<String, Sample>, Map<String, Float>> {
        val layers = HashMap<String, Sample>()
        engine.elementStates.forEach { e ->
            val texts = e.layers.map { it.text }
            if (texts.size != texts.toSet().size) fail("element ${e.key} has duplicate layers $texts")
            e.layers.forEach { layers["${e.key}/${it.text}"] = Sample(it.offset.value, it.alpha.value) }
        }
        val keys = engine.elementStates.map { it.key }
        if (keys.size != keys.toSet().size) fail("duplicate element keys $keys")
        val widths = engine.frame({ it.length * digitWidth }, digitWidth).items.associate { it.key to it.width }
        return layers to widths
    }

    private fun assertContinuous(before: Pair<Map<String, Sample>, Map<String, Float>>, after: Pair<Map<String, Sample>, Map<String, Float>>, t: Long) {
        after.first.forEach { (k, s) ->
            val p = before.first[k]
            if (p == null) {
                assertTrue("t=$t new layer $k appeared with alpha ${s.alpha}", s.alpha <= limit)
            } else {
                assertTrue("t=$t layer $k offset jumped ${p.offset} -> ${s.offset}", kotlin.math.abs(s.offset - p.offset) <= limit)
                assertTrue("t=$t layer $k alpha jumped ${p.alpha} -> ${s.alpha}", kotlin.math.abs(s.alpha - p.alpha) <= limit)
            }
        }
        after.second.forEach { (k, w) ->
            val p = before.second[k] ?: 0f
            assertTrue("t=$t element $k width jumped $p -> $w", kotlin.math.abs(w - p) <= limit * digitWidth)
        }
    }

    /** Runs from→mid, reverses to [back] at [reverseAtMs], samples every frame until settled. */
    private fun run(from: String, mid: String, back: String, reverseAtMs: Long, onReverse: (MotionEngine) -> Unit = {}): MotionEngine {
        val engine = MotionEngine()
        val a = n(from)
        val b = n(mid)
        engine.showImmediately(Tokenizer.tokenize(a))
        var prev = snapshot(engine)
        engine.apply(TransitionDiff.compute(a, b, Trend.Auto))
        engine.step(0)
        var t = 0L
        var reversed = false
        while (true) {
            t += 16
            if (!reversed && t >= reverseAtMs) {
                engine.apply(TransitionDiff.compute(b, n(back), Trend.Auto))
                reversed = true
                onReverse(engine)
            }
            engine.step(t * 1_000_000)
            val now = snapshot(engine)
            assertContinuous(prev, now, t)
            prev = now
            if (reversed && !engine.isAnimating) break
            check(t < 5_000) { "did not settle" }
        }
        return engine
    }

    @Test
    fun reversalAt40PercentRollsBackWithoutJumps() {
        val engine = run("₩15,000", "₩12,000", "₩15,000", reverseAtMs = 200)
        val i3 = engine.elementStates.first { it.key == "I3" }
        assertEquals(listOf("5"), i3.layers.map { it.text })
    }

    @Test
    fun reversedEnteringDigitFadesOutFromItsCurrentAlpha() {
        var alphaAtReverse = -1.0
        val engine = run("₩9,900", "₩12,900", "₩9,900", reverseAtMs = 200) { e ->
            val one = e.elementStates.first { it.key == "I4" }
            assertEquals("no new layer for the reversed digit", 1, one.layers.size)
            alphaAtReverse = one.layers.single().alpha.value
            assertTrue(one.exiting)
        }
        assertTrue("the '1' was partly visible when reversed: $alphaAtReverse", alphaAtReverse in 0.05..0.99)
        assertTrue(engine.elementStates.none { it.key == "I4" })
    }

    @Test
    fun manyRapidRetargetsStayContinuous() {
        val engine = MotionEngine()
        val values = listOf("₩10,000", "₩11,000", "₩9,900", "₩12,900", "₩900", "₩19,800")
        engine.showImmediately(Tokenizer.tokenize(n(values[0])))
        var prev = snapshot(engine)
        var current = n(values[0])
        engine.step(0)
        var t = 0L
        var next = 1
        while (next < values.size || engine.isAnimating) {
            t += 16
            if (next < values.size && t % 64 == 0L) {
                val target = n(values[next++])
                engine.apply(TransitionDiff.compute(current, target, Trend.Auto))
                current = target
            }
            engine.step(t * 1_000_000)
            val now = snapshot(engine)
            assertContinuous(prev, now, t)
            prev = now
            check(t < 5_000) { "did not settle" }
        }
        assertEquals("₩19,800".length, engine.elementStates.size)
    }
}
