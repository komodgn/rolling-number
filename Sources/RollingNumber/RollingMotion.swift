import Foundation

/// Which way changed digits roll (FR-005).
public enum RollingTrend: Hashable {
    /// Up when the value increases, down when it decreases.
    case auto
    /// Always roll up.
    case alwaysUp
    /// Always roll down.
    case alwaysDown
    /// Each digit rolls by its own change; entering/leaving digits follow the value.
    case perDigit

    var internalTrend: Trend {
        switch self {
        case .auto: return .auto
        case .alwaysUp: return .alwaysUp
        case .alwaysDown: return .alwaysDown
        case .perDigit: return .perDigit
        }
    }
}

/// Speed and shape of one animated property (FR-010).
public enum RollingCurve: Hashable {
    /// Physics spring; keeps velocity when interrupted.
    case spring(stiffness: Double, dampingRatio: Double)
    /// Fixed duration (seconds) with CSS-style cubic-bezier control points.
    case tween(duration: TimeInterval, x1: Double = 0.4, y1: Double = 0, x2: Double = 0.2, y2: Double = 1)

    var internalCurve: MotionCurve {
        switch self {
        case let .spring(k, z): return .spring(stiffness: max(k, 0.0001), dampingRatio: max(z, 0.0001))
        case let .tween(d, x1, y1, x2, y2): return .tween(durationMs: max(d * 1000, 1), x1: x1, y1: y1, x2: x2, y2: y2)
        }
    }
}

/// Animation settings of a `RollingNumber`.
public struct RollingMotion: Hashable {
    /// Width and position changes.
    public var layout: RollingCurve
    /// Vertical digit rolling.
    public var roll: RollingCurve
    /// Appearing and disappearing.
    public var fade: RollingCurve
    /// false shows changes immediately and completes a running animation (FR-016).
    public var enabled: Bool
    /// Follow the OS Reduce Motion setting (FR-017).
    public var respectReduceMotion: Bool
    /// Animate the very first value instead of showing it immediately.
    public var animateFirstDisplay: Bool

    public init(
        layout: RollingCurve = .spring(stiffness: 300, dampingRatio: 1.0),
        roll: RollingCurve = .spring(stiffness: 380, dampingRatio: 0.8),
        fade: RollingCurve = .spring(stiffness: 500, dampingRatio: 1.0),
        enabled: Bool = true,
        respectReduceMotion: Bool = true,
        animateFirstDisplay: Bool = false
    ) {
        self.layout = layout
        self.roll = roll
        self.fade = fade
        self.enabled = enabled
        self.respectReduceMotion = respectReduceMotion
        self.animateFirstDisplay = animateFirstDisplay
    }

    /// Default spring curves; settles in about 0.3–0.6 s.
    public static let `default` = RollingMotion()

    var spec: MotionSpec {
        MotionSpec(layout: layout.internalCurve, roll: roll.internalCurve, fade: fade.internalCurve,
                   enabled: enabled, respectReduceMotion: respectReduceMotion, animateFirstDisplay: animateFirstDisplay)
    }
}
