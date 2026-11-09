package io.github.komodgn.rollingnumber.logic

import io.github.komodgn.rollingnumber.logic.motion.MotionEngine
import io.github.komodgn.rollingnumber.logic.motion.MotionSpec
import io.github.komodgn.rollingnumber.logic.motion.RenderFrame

internal enum class SessionPhase { Empty, Idle, Animating }

/**
 * State of one RollingNumber (data-model §10). Platform-free: the UI layer feeds formatted values
 * and frame times, and reads [frame] for drawing.
 */
internal class RollingSession(
    spec: MotionSpec = MotionSpec(),
    var trend: Trend = Trend.Auto,
) {
    interface Listener {
        fun onStart()
        fun onEnd()
    }

    var listener: Listener? = null

    /** Announcement coordinator this session belongs to (set by [AnnouncementCoordinator.register]). */
    internal var coordinator: AnnouncementCoordinator? = null

    var spec: MotionSpec = spec
        set(value) {
            field = value
            engine.spec = value
        }

    private val engine = MotionEngine(spec)

    var phase: SessionPhase = SessionPhase.Empty
        private set

    var current: FormattedNumber? = null
        private set

    /** Accessibility label shown before the value (FR-019). */
    var label: String? = null

    /** Whether value changes are announced (FR-019, default on). */
    var announce: Boolean = true

    /** True while a change waits to be announced after settling. */
    var pendingAnnouncement: Boolean = false
        private set

    private var animationAllowed = true
    private var itemKey: Any? = null
    private var itemKeyChanged = false

    val isAnimating: Boolean get() = phase == SessionPhase.Animating

    /** Label + full target text; never an intermediate value (FR-018). */
    val accessibilityText: String
        get() {
            val text = current?.displayText ?: return label.orEmpty()
            return if (label.isNullOrEmpty()) text else "$label $text"
        }

    /** Sets the list item identity (FR-013). A different key makes the next update a first display. */
    fun setItemKey(key: Any?) {
        if (key == itemKey) return
        val hadKey = current != null
        itemKey = key
        if (hadKey) itemKeyChanged = true
    }

    /** enabled && !(reduceMotion && respectReduceMotion). Turning off completes a running animation. */
    fun setAnimationAllowed(allowed: Boolean) {
        if (animationAllowed == allowed) return
        animationAllowed = allowed
        if (!allowed && phase == SessionPhase.Animating) {
            engine.snapToEnd()
            finish()
        }
    }

    fun update(number: FormattedNumber) {
        val previous = current

        if (previous == null || itemKeyChanged) {
            val firstWithAnimation = previous == null && animationAllowed && spec.animateFirstDisplay
            if (itemKeyChanged && phase == SessionPhase.Animating) phase = SessionPhase.Idle // cancelled: no onEnd
            itemKeyChanged = false
            if (pendingAnnouncement) {
                pendingAnnouncement = false
                coordinator?.cancel(this)
            }
            current = number
            if (firstWithAnimation) {
                engine.apply(TransitionDiff.compute(null, number, trend, animateFirstDisplay = true))
                startIfNeeded()
            } else {
                engine.showImmediately(Tokenizer.tokenize(number))
                phase = SessionPhase.Idle
            }
            return
        }

        if (previous.displayText == number.displayText && previous.formatKey == number.formatKey) return

        val valueOrTextChanged = previous.value != number.value || previous.displayText != number.displayText
        current = number

        val formatChanged = previous.formatKey != number.formatKey
        if (formatChanged || !animationAllowed) {
            // Locale/format change (FR-014) or animation off (FR-016, FR-017): replace immediately, no events.
            if (phase == SessionPhase.Animating) phase = SessionPhase.Idle
            engine.showImmediately(Tokenizer.tokenize(number))
            val announceNow = if (formatChanged) previous.value != number.value else valueOrTextChanged
            if (announceNow || pendingAnnouncement) requestAnnouncement()
            return
        }

        engine.apply(TransitionDiff.compute(previous, number, trend))
        if (valueOrTextChanged && announce) {
            pendingAnnouncement = true
            coordinator?.onChangePending(this)
        }
        startIfNeeded()
    }

    private fun startIfNeeded() {
        if (phase != SessionPhase.Animating && engine.isAnimating) {
            phase = SessionPhase.Animating
            listener?.onStart()
        } else if (!engine.isAnimating) {
            phase = SessionPhase.Idle
            if (pendingAnnouncement) requestAnnouncement()
        }
    }

    /** Advances animation to [nowNanos]. Returns true while animating. */
    fun step(nowNanos: Long): Boolean {
        if (phase != SessionPhase.Animating) return false
        if (!engine.step(nowNanos)) finish()
        return phase == SessionPhase.Animating
    }

    private fun finish() {
        phase = SessionPhase.Idle
        listener?.onEnd()
        if (pendingAnnouncement) requestAnnouncement()
    }

    private fun requestAnnouncement() {
        pendingAnnouncement = false
        if (announce) coordinator?.onSettled(this)
    }

    fun frame(measure: (String) -> Float, tabularDigitWidth: Float?): RenderFrame =
        engine.frame(measure, tabularDigitWidth)

    /** For tests. */
    internal val engineForTest: MotionEngine get() = engine
}
