package io.github.komodgn.rollingnumber.logic.motion

/** Animation curve (research R5). */
internal sealed interface MotionCurve {
    data class Spring(val stiffness: Double, val dampingRatio: Double) : MotionCurve {
        init {
            require(stiffness > 0) { "stiffness must be > 0" }
            require(dampingRatio > 0) { "dampingRatio must be > 0" }
        }
    }

    data class Tween(val durationMs: Double, val x1: Double, val y1: Double, val x2: Double, val y2: Double) : MotionCurve {
        init {
            require(durationMs > 0) { "durationMs must be > 0" }
        }
    }

    companion object {
        val DefaultLayout: MotionCurve = Spring(stiffness = 300.0, dampingRatio = 1.0)
        val DefaultRoll: MotionCurve = Spring(stiffness = 380.0, dampingRatio = 0.8)
        val DefaultFade: MotionCurve = Spring(stiffness = 500.0, dampingRatio = 1.0)
    }
}

internal data class MotionSpec(
    val layout: MotionCurve = MotionCurve.DefaultLayout,
    val roll: MotionCurve = MotionCurve.DefaultRoll,
    val fade: MotionCurve = MotionCurve.DefaultFade,
    val enabled: Boolean = true,
    val respectReduceMotion: Boolean = true,
    val animateFirstDisplay: Boolean = false,
)

/** Fixed integration step: results depend only on elapsed time, not on frame rate. */
internal const val STEP_SECONDS: Double = 0.004
internal const val STEP_NANOS: Long = 4_000_000L

/**
 * One animated scalar. Springs keep value and velocity across retargets (FR-011);
 * tweens restart from the current value.
 */
internal class AnimatedValue(
    initial: Double,
    var curve: MotionCurve,
    private val positionTolerance: Double,
    private val velocityTolerance: Double,
) {
    var value: Double = initial
        private set
    var velocity: Double = 0.0
        private set
    var target: Double = initial
        private set
    private var tweenFrom = initial
    private var tweenElapsed = 0.0

    val isSettled: Boolean get() = value == target && velocity == 0.0

    fun animateTo(newTarget: Double, newCurve: MotionCurve = curve) {
        curve = newCurve
        if (newTarget == target && !isSettled) return
        target = newTarget
        tweenFrom = value
        tweenElapsed = 0.0
    }

    fun snapTo(newValue: Double) {
        value = newValue
        target = newValue
        velocity = 0.0
        tweenFrom = newValue
        tweenElapsed = 0.0
    }

    fun step(dt: Double) {
        if (isSettled) return
        when (val c = curve) {
            is MotionCurve.Spring -> {
                val omega = kotlin.math.sqrt(c.stiffness)
                val damping = 2.0 * c.dampingRatio * omega
                val acceleration = -c.stiffness * (value - target) - damping * velocity
                velocity += acceleration * dt
                value += velocity * dt
                if (kotlin.math.abs(value - target) < positionTolerance && kotlin.math.abs(velocity) < velocityTolerance) {
                    value = target
                    velocity = 0.0
                }
            }
            is MotionCurve.Tween -> {
                tweenElapsed += dt
                val p = (tweenElapsed * 1000.0 / c.durationMs).coerceIn(0.0, 1.0)
                val previous = value
                value = tweenFrom + (target - tweenFrom) * CubicBezier.ease(p, c.x1, c.y1, c.x2, c.y2)
                velocity = (value - previous) / dt
                if (p >= 1.0) {
                    value = target
                    velocity = 0.0
                }
            }
        }
    }
}

/** CSS-style cubic-bezier easing (P0 = 0,0 and P3 = 1,1). */
internal object CubicBezier {
    fun ease(t: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        if (t <= 0.0) return 0.0
        if (t >= 1.0) return 1.0
        // Solve x(s) = t for s with bisection (monotonic for x1, x2 in [0, 1]).
        var lo = 0.0
        var hi = 1.0
        var s = t
        repeat(40) {
            val x = bezier(s, x1, x2)
            if (kotlin.math.abs(x - t) < 1e-7) return bezier(s, y1, y2)
            if (x < t) lo = s else hi = s
            s = (lo + hi) / 2
        }
        return bezier(s, y1, y2)
    }

    private fun bezier(s: Double, p1: Double, p2: Double): Double {
        val u = 1 - s
        return 3 * u * u * s * p1 + 3 * u * s * s * p2 + s * s * s
    }
}
