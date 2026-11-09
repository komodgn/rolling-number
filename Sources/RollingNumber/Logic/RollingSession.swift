import Foundation

enum SessionPhase { case empty, idle, animating }

/// State of one RollingNumber (data-model §10). Mirrors Android `RollingSession`.
final class RollingSession {
    var onStart: (() -> Void)?
    var onEnd: (() -> Void)?

    /// Announcement coordinator this session belongs to (set by `AnnouncementCoordinator.register`).
    weak var coordinator: AnnouncementCoordinator?

    var spec: MotionSpec { didSet { engine.spec = spec } }
    var trend: Trend
    private let engine: MotionEngine

    private(set) var phase: SessionPhase = .empty
    private(set) var current: FormattedNumber?

    /// Accessibility label shown before the value (FR-019).
    var label: String?
    /// Whether value changes are announced (FR-019, default on).
    var announce = true
    /// True while a change waits to be announced after settling.
    private(set) var pendingAnnouncement = false

    private var animationAllowed = true
    private var itemKey: AnyHashable?
    private var itemKeyChanged = false

    init(spec: MotionSpec = MotionSpec(), trend: Trend = .auto) {
        self.spec = spec
        self.trend = trend
        engine = MotionEngine(spec: spec)
    }

    var isAnimating: Bool { phase == .animating }

    /// Label + full target text; never an intermediate value (FR-018).
    var accessibilityText: String {
        guard let text = current?.displayText else { return label ?? "" }
        if let label = label, !label.isEmpty { return "\(label) \(text)" }
        return text
    }

    /// Sets the list item identity (FR-013). A different key makes the next update a first display.
    func setItemKey(_ key: AnyHashable?) {
        if key == itemKey { return }
        let hadValue = current != nil
        itemKey = key
        if hadValue { itemKeyChanged = true }
    }

    /// enabled && !(reduceMotion && respectReduceMotion). Turning off completes a running animation.
    func setAnimationAllowed(_ allowed: Bool) {
        if animationAllowed == allowed { return }
        animationAllowed = allowed
        if !allowed && phase == .animating {
            engine.snapToEnd()
            finish()
        }
    }

    func update(_ number: FormattedNumber) {
        guard let previous = current, !itemKeyChanged else {
            let firstWithAnimation = current == nil && animationAllowed && spec.animateFirstDisplay
            if itemKeyChanged && phase == .animating { phase = .idle } // cancelled: no onEnd
            itemKeyChanged = false
            if pendingAnnouncement {
                pendingAnnouncement = false
                coordinator?.cancel(self)
            }
            current = number
            if firstWithAnimation, let diff = try? TransitionDiff.compute(from: nil, to: number, trend: trend, animateFirstDisplay: true) {
                engine.apply(diff)
                startIfNeeded()
            } else {
                engine.showImmediately((try? Tokenizer.tokenize(number)) ?? [])
                phase = .idle
            }
            return
        }

        if previous.displayText == number.displayText && previous.formatKey == number.formatKey { return }

        let valueOrTextChanged = previous.value != number.value || previous.displayText != number.displayText
        current = number

        let formatChanged = previous.formatKey != number.formatKey
        if formatChanged || !animationAllowed {
            // Locale/format change (FR-014) or animation off (FR-016, FR-017): replace immediately, no events.
            if phase == .animating { phase = .idle }
            engine.showImmediately((try? Tokenizer.tokenize(number)) ?? [])
            let announceNow = formatChanged ? previous.value != number.value : valueOrTextChanged
            if announceNow || pendingAnnouncement { requestAnnouncement() }
            return
        }

        guard let diff = try? TransitionDiff.compute(from: previous, to: number, trend: trend) else {
            engine.showImmediately((try? Tokenizer.tokenize(number)) ?? [])
            return
        }
        engine.apply(diff)
        if valueOrTextChanged && announce {
            pendingAnnouncement = true
            coordinator?.onChangePending(self)
        }
        startIfNeeded()
    }

    private func startIfNeeded() {
        if phase != .animating && engine.isAnimating {
            phase = .animating
            onStart?()
        } else if !engine.isAnimating {
            phase = .idle
            if pendingAnnouncement { requestAnnouncement() }
        }
    }

    /// Advances animation to `nowNanos`. Returns true while animating.
    @discardableResult
    func step(_ nowNanos: Int64) -> Bool {
        guard phase == .animating else { return false }
        if !engine.step(nowNanos) { finish() }
        return phase == .animating
    }

    private func finish() {
        phase = .idle
        onEnd?()
        if pendingAnnouncement { requestAnnouncement() }
    }

    private func requestAnnouncement() {
        pendingAnnouncement = false
        if announce { coordinator?.onSettled(self) }
    }

    func frame(measure: (String) -> Double, tabularDigitWidth: Double?) -> RenderFrame {
        engine.frame(measure: measure, tabularDigitWidth: tabularDigitWidth)
    }

    /// For tests.
    var engineForTest: MotionEngine { engine }
}
