import Foundation

/// Animation curve (research R5). Mirrors Android `MotionCurve`.
enum MotionCurve: Equatable {
    case spring(stiffness: Double, dampingRatio: Double)
    case tween(durationMs: Double, x1: Double, y1: Double, x2: Double, y2: Double)

    static let defaultLayout = MotionCurve.spring(stiffness: 300, dampingRatio: 1.0)
    static let defaultRoll = MotionCurve.spring(stiffness: 380, dampingRatio: 0.8)
    static let defaultFade = MotionCurve.spring(stiffness: 500, dampingRatio: 1.0)
}

struct MotionSpec: Equatable {
    var layout: MotionCurve = .defaultLayout
    var roll: MotionCurve = .defaultRoll
    var fade: MotionCurve = .defaultFade
    var enabled: Bool = true
    var respectReduceMotion: Bool = true
    var animateFirstDisplay: Bool = false
}

/// Fixed integration step: results depend only on elapsed time, not on frame rate.
let stepSeconds: Double = 0.004
let stepNanos: Int64 = 4_000_000

/// One animated scalar. Springs keep value and velocity across retargets (FR-011);
/// tweens restart from the current value.
final class AnimatedValue {
    private(set) var value: Double
    private(set) var velocity: Double = 0
    private(set) var target: Double
    var curve: MotionCurve
    private let positionTolerance: Double
    private let velocityTolerance: Double
    private var tweenFrom: Double
    private var tweenElapsed: Double = 0

    init(_ initial: Double, curve: MotionCurve, positionTolerance: Double, velocityTolerance: Double) {
        value = initial
        target = initial
        tweenFrom = initial
        self.curve = curve
        self.positionTolerance = positionTolerance
        self.velocityTolerance = velocityTolerance
    }

    var isSettled: Bool { value == target && velocity == 0 }

    func animate(to newTarget: Double, curve newCurve: MotionCurve? = nil) {
        if let c = newCurve { curve = c }
        if newTarget == target && !isSettled { return }
        target = newTarget
        tweenFrom = value
        tweenElapsed = 0
    }

    func snap(to newValue: Double) {
        value = newValue
        target = newValue
        velocity = 0
        tweenFrom = newValue
        tweenElapsed = 0
    }

    func step(_ dt: Double) {
        if isSettled { return }
        switch curve {
        case let .spring(stiffness, dampingRatio):
            let omega = stiffness.squareRoot()
            let damping = 2.0 * dampingRatio * omega
            let acceleration = -stiffness * (value - target) - damping * velocity
            velocity += acceleration * dt
            value += velocity * dt
            if abs(value - target) < positionTolerance && abs(velocity) < velocityTolerance {
                value = target
                velocity = 0
            }
        case let .tween(durationMs, x1, y1, x2, y2):
            tweenElapsed += dt
            let p = min(max(tweenElapsed * 1000.0 / durationMs, 0), 1)
            let previous = value
            value = tweenFrom + (target - tweenFrom) * CubicBezier.ease(p, x1, y1, x2, y2)
            velocity = (value - previous) / dt
            if p >= 1 {
                value = target
                velocity = 0
            }
        }
    }
}

/// CSS-style cubic-bezier easing (P0 = 0,0 and P3 = 1,1).
enum CubicBezier {
    static func ease(_ t: Double, _ x1: Double, _ y1: Double, _ x2: Double, _ y2: Double) -> Double {
        if t <= 0 { return 0 }
        if t >= 1 { return 1 }
        var lo = 0.0, hi = 1.0, s = t
        for _ in 0..<40 {
            let x = bezier(s, x1, x2)
            if abs(x - t) < 1e-7 { return bezier(s, y1, y2) }
            if x < t { lo = s } else { hi = s }
            s = (lo + hi) / 2
        }
        return bezier(s, y1, y2)
    }

    private static func bezier(_ s: Double, _ p1: Double, _ p2: Double) -> Double {
        let u = 1 - s
        return 3 * u * u * s * p1 + 3 * u * s * s * p2 + s * s * s
    }
}
