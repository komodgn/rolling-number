package io.github.komodgn.rollingnumber.logic.motion

import io.github.komodgn.rollingnumber.logic.TokenKind

/** Tolerances from research R5. */
internal const val OFFSET_TOLERANCE = 0.001
internal const val OFFSET_VELOCITY_TOLERANCE = 0.01
internal const val ALPHA_TOLERANCE = 0.005
internal const val ALPHA_VELOCITY_TOLERANCE = 0.05
internal const val PRESENCE_TOLERANCE = 0.001
internal const val PRESENCE_VELOCITY_TOLERANCE = 0.05

/**
 * One glyph layer inside an element.
 * - offset: vertical position in line heights (0 = resting, +1 = one line below, -1 = one line above)
 * - alpha: 0..1
 * - weight: 0..1, share of this layer's text width in the element width (layout curve).
 *   Blending by weight makes the width move smoothly between old and new text.
 */
internal class Layer(val text: String, spec: MotionSpec, offset: Double, alpha: Double, weight: Double) {
    val offset = AnimatedValue(offset, spec.roll, OFFSET_TOLERANCE, OFFSET_VELOCITY_TOLERANCE)
    val alpha = AnimatedValue(alpha, spec.fade, ALPHA_TOLERANCE, ALPHA_VELOCITY_TOLERANCE)
    val weight = AnimatedValue(weight, spec.layout, PRESENCE_TOLERANCE, PRESENCE_VELOCITY_TOLERANCE)

    /** True once the layer is on its way out; removed when settled. */
    var leaving: Boolean = false
        private set

    val isSettled: Boolean get() = offset.isSettled && alpha.isSettled && weight.isSettled

    fun rest(spec: MotionSpec) {
        leaving = false
        offset.animateTo(0.0, spec.roll)
        alpha.animateTo(1.0, spec.fade)
        weight.animateTo(1.0, spec.layout)
    }

    /** Leave toward [exitOffset]; null keeps the current vertical target (fade in place). */
    fun leave(spec: MotionSpec, exitOffset: Double?) {
        leaving = true
        if (exitOffset != null) offset.animateTo(exitOffset, spec.roll)
        alpha.animateTo(0.0, spec.fade)
        weight.animateTo(0.0, spec.layout)
    }

    fun snapToRest() {
        leaving = false
        offset.snapTo(0.0)
        alpha.snapTo(1.0)
        weight.snapTo(1.0)
    }

    fun step(dt: Double) {
        offset.step(dt)
        alpha.step(dt)
        weight.step(dt)
    }
}

/**
 * One on-screen element; at most one per key (data-model §8).
 * [presence] (0..1, layout curve) grows on enter and shrinks on exit.
 */
internal class ElementState(val key: String, var kind: TokenKind, spec: MotionSpec, presence: Double) {
    val presence = AnimatedValue(presence, spec.layout, PRESENCE_TOLERANCE, PRESENCE_VELOCITY_TOLERANCE)
    val layers: MutableList<Layer> = ArrayList(2)

    /** True while the element is on its way out. */
    var exiting: Boolean = false
        private set

    val isSettled: Boolean get() = presence.isSettled && layers.all { it.isSettled }

    fun show(spec: MotionSpec) {
        exiting = false
        presence.animateTo(1.0, spec.layout)
    }

    fun exit(spec: MotionSpec, exitOffset: Double?) {
        exiting = true
        presence.animateTo(0.0, spec.layout)
        layers.forEach { it.leave(spec, exitOffset) }
    }

    fun snapToRest() {
        exiting = false
        presence.snapTo(1.0)
        layers.removeAll { it.leaving }
        layers.forEach { it.snapToRest() }
    }

    fun step(dt: Double) {
        presence.step(dt)
        layers.forEach { it.step(dt) }
    }

    /** Drops finished layers. Returns true when the whole element has finished exiting. */
    fun prune(): Boolean {
        if (exiting) return isSettled
        if (layers.any { !it.leaving }) layers.removeAll { it.leaving && it.isSettled }
        return false
    }
}
