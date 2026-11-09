package io.github.komodgn.rollingnumber

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import io.github.komodgn.rollingnumber.logic.motion.MotionCurve
import io.github.komodgn.rollingnumber.logic.motion.MotionSpec

/** Speed and shape of one animated property (FR-010). */
@Immutable
public sealed interface RollingCurve {
    /** Physics spring; keeps velocity when interrupted. */
    public data class Spring(val stiffness: Float, val dampingRatio: Float) : RollingCurve {
        init {
            require(stiffness > 0f) { "stiffness must be > 0" }
            require(dampingRatio > 0f) { "dampingRatio must be > 0" }
        }
    }

    /** Fixed-duration curve with CSS-style cubic-bezier control points. */
    public data class Tween(
        val durationMillis: Int,
        val x1: Float = 0.4f,
        val y1: Float = 0f,
        val x2: Float = 0.2f,
        val y2: Float = 1f,
    ) : RollingCurve {
        init {
            require(durationMillis > 0) { "durationMillis must be > 0" }
        }
    }
}

/**
 * Animation settings of a [RollingNumber].
 *
 * @property layout width and position changes.
 * @property roll vertical digit rolling.
 * @property fade appearing and disappearing.
 * @property enabled false shows changes immediately and completes a running animation (FR-016).
 * @property respectReduceMotion follow the OS reduce-motion setting (FR-017).
 * @property animateFirstDisplay animate the very first value instead of showing it immediately.
 */
@Immutable
public data class RollingMotion(
    val layout: RollingCurve,
    val roll: RollingCurve,
    val fade: RollingCurve,
    val enabled: Boolean = true,
    val respectReduceMotion: Boolean = true,
    val animateFirstDisplay: Boolean = false,
) {
    internal fun toSpec(): MotionSpec = MotionSpec(
        layout = layout.toInternal(),
        roll = roll.toInternal(),
        fade = fade.toInternal(),
        enabled = enabled,
        respectReduceMotion = respectReduceMotion,
        animateFirstDisplay = animateFirstDisplay,
    )
}

internal fun RollingCurve.toInternal(): MotionCurve = when (this) {
    is RollingCurve.Spring -> MotionCurve.Spring(stiffness.toDouble(), dampingRatio.toDouble())
    is RollingCurve.Tween -> MotionCurve.Tween(durationMillis.toDouble(), x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
}

/** Defaults for [RollingNumber]. */
public object RollingNumberDefaults {
    /** Layout spring: stiffness 300, damping 1.0. */
    public val LayoutCurve: RollingCurve = RollingCurve.Spring(300f, 1.0f)

    /** Roll spring: stiffness 380, damping 0.8 (slight overshoot). */
    public val RollCurve: RollingCurve = RollingCurve.Spring(380f, 0.8f)

    /** Fade spring: stiffness 500, damping 1.0. */
    public val FadeCurve: RollingCurve = RollingCurve.Spring(500f, 1.0f)

    /** 16sp text, tabular digits, 4dp fade mask. */
    public fun style(textStyle: TextStyle = TextStyle(fontSize = 16.sp)): RollingNumberStyle =
        RollingNumberStyle(textStyle = textStyle)

    /** Default spring curves; settles in about 0.3–0.6 s. */
    public fun motion(
        enabled: Boolean = true,
        respectReduceMotion: Boolean = true,
        animateFirstDisplay: Boolean = false,
    ): RollingMotion = RollingMotion(LayoutCurve, RollCurve, FadeCurve, enabled, respectReduceMotion, animateFirstDisplay)
}
