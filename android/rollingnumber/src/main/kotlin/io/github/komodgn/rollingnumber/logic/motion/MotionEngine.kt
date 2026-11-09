package io.github.komodgn.rollingnumber.logic.motion

import io.github.komodgn.rollingnumber.logic.DiffAction
import io.github.komodgn.rollingnumber.logic.DiffResult
import io.github.komodgn.rollingnumber.logic.Token
import io.github.komodgn.rollingnumber.logic.TokenKind

internal data class RenderLayer(val text: String, val offsetY: Float, val alpha: Float)

internal data class RenderItem(
    val key: String,
    val kind: TokenKind,
    val x: Float,
    val width: Float,
    val layers: List<RenderLayer>,
)

internal data class RenderFrame(val totalWidth: Float, val items: List<RenderItem>, val isAnimating: Boolean)

/**
 * Animates display elements toward the latest diff (research R5, data-model §8).
 * Time advances in fixed 4 ms steps so results depend only on elapsed time.
 */
internal class MotionEngine(spec: MotionSpec = MotionSpec()) {
    var spec: MotionSpec = spec

    private val elements = ArrayList<ElementState>()
    private var lastNanos: Long? = null
    private var accumulatedNanos = 0L

    val isAnimating: Boolean get() = elements.any { !it.isSettled }

    /** Read-only view for tests. */
    internal val elementStates: List<ElementState> get() = elements

    /** Replaces everything with resting [tokens]; no animation. */
    fun showImmediately(tokens: List<Token>) {
        elements.clear()
        for (t in tokens) {
            elements += ElementState(t.key, t.kind, spec, presence = 1.0).also { e ->
                e.layers += Layer(t.text, spec, 0.0, 1.0, 1.0)
            }
        }
        resetClock()
    }

    /** Applies a diff, continuing from the current visual state when already animating (FR-011). */
    fun apply(diff: DiffResult) {
        if (diff.isEmpty) return
        if (!isAnimating) resetClock()
        val byKey = elements.associateBy { it.key }
        val ordered = ArrayList<ElementState>(diff.elements.size)
        val mentioned = HashSet<String>()

        for (d in diff.elements) {
            mentioned += d.key
            val isEnter = d.action == DiffAction.EnterUp || d.action == DiffAction.EnterDown || d.action == DiffAction.EnterFade
            val element = byKey[d.key] ?: ElementState(d.key, d.kind, spec, presence = if (isEnter) 0.0 else 1.0)
            element.kind = d.kind
            when (d.action) {
                DiffAction.Stay, DiffAction.Crossfade, DiffAction.EnterFade ->
                    settleOn(element, requireNotNull(d.to), entryOffset = 0.0, exitOffset = null)
                DiffAction.RollUp, DiffAction.EnterUp ->
                    settleOn(element, requireNotNull(d.to), entryOffset = 1.0, exitOffset = -1.0)
                DiffAction.RollDown, DiffAction.EnterDown ->
                    settleOn(element, requireNotNull(d.to), entryOffset = -1.0, exitOffset = 1.0)
                DiffAction.ExitUp -> element.exit(spec, -1.0)
                DiffAction.ExitDown -> element.exit(spec, 1.0)
                DiffAction.ExitFade -> element.exit(spec, null)
            }
            ordered += element
        }

        // Elements still leaving from an earlier transition keep their place after their old predecessor.
        var previous: ElementState? = null
        for (e in elements) {
            if (e.key !in mentioned) {
                if (!e.exiting) e.exit(spec, null)
                val anchor = previous?.let { ordered.indexOf(it) } ?: -1
                ordered.add(anchor + 1, e)
            }
            previous = e
        }
        elements.clear()
        elements += ordered
    }

    /**
     * Makes [text] the resting layer of [element]. Reuses an existing layer with the same text
     * (no duplicates, FR-011); otherwise adds one at [entryOffset]. Other layers leave toward [exitOffset].
     */
    private fun settleOn(element: ElementState, text: String, entryOffset: Double, exitOffset: Double?) {
        element.show(spec)
        val existing = element.layers.lastOrNull { it.text == text }
        for (layer in element.layers) {
            if (layer === existing) continue
            if (!layer.leaving || exitOffset != null) layer.leave(spec, exitOffset)
        }
        if (existing != null) {
            existing.rest(spec)
            element.layers.remove(existing)
            element.layers += existing // resting layer drawn on top
        } else {
            val startWeight = if (element.layers.isEmpty()) 1.0 else 0.0
            element.layers += Layer(text, spec, entryOffset, 0.0, startWeight).also { it.rest(spec) }
        }
    }

    /** Advances to [nowNanos]. Returns true while animating. */
    fun step(nowNanos: Long): Boolean {
        val last = lastNanos
        lastNanos = nowNanos
        if (last == null || !isAnimating) return isAnimating
        accumulatedNanos += (nowNanos - last).coerceAtLeast(0)
        while (accumulatedNanos >= STEP_NANOS && isAnimating) {
            accumulatedNanos -= STEP_NANOS
            for (e in elements) e.step(STEP_SECONDS)
            elements.removeAll { it.prune() }
        }
        if (!isAnimating) accumulatedNanos = 0
        return isAnimating
    }

    /** Jumps every element to its final state. */
    fun snapToEnd() {
        elements.removeAll { it.exiting }
        elements.forEach { it.snapToRest() }
        resetClock()
    }

    private fun resetClock() {
        lastNanos = null
        accumulatedNanos = 0
    }

    /**
     * Lays elements out left to right. Element width = presence × (weight-blended text width).
     * Digit elements use [tabularDigitWidth] when given; other widths come from [measure].
     */
    fun frame(measure: (String) -> Float, tabularDigitWidth: Float?): RenderFrame {
        var x = 0f
        val items = ArrayList<RenderItem>(elements.size)
        for (e in elements) {
            var weighted = 0.0
            var totalWeight = 0.0
            var widest = 0f
            for (layer in e.layers) {
                val w = if (e.kind.isDigit && tabularDigitWidth != null) tabularDigitWidth else measure(layer.text)
                val weight = layer.weight.value.coerceIn(0.0, 1.0)
                weighted += w * weight
                totalWeight += weight
                widest = maxOf(widest, w)
            }
            val textWidth = if (totalWeight > 1e-6) (weighted / totalWeight).toFloat() else widest
            val width = textWidth * e.presence.value.toFloat().coerceIn(0f, 1f)
            items += RenderItem(
                key = e.key,
                kind = e.kind,
                x = x,
                width = width,
                layers = e.layers.map {
                    RenderLayer(it.text, it.offset.value.toFloat(), it.alpha.value.toFloat().coerceIn(0f, 1f))
                },
            )
            x += width
        }
        return RenderFrame(x, items, isAnimating)
    }
}
