import SwiftUI

public extension View {
    /// Which way changed digits roll (FR-005). Applies to every `RollingNumber` inside.
    func rollingTrend(_ trend: RollingTrend) -> some View {
        rollingOption { $0.trend = trend }
    }

    /// Animation curves, on/off, reduce-motion handling, first-display animation (FR-010, FR-016, FR-017).
    func rollingMotion(_ motion: RollingMotion) -> some View {
        rollingOption { $0.motion = motion }
    }

    /// Give every digit the width of the widest digit so rolling never wobbles (FR-021, default true).
    func rollingTabularDigits(_ enabled: Bool) -> some View {
        rollingOption { $0.tabularDigits = enabled }
    }

    /// Height of the top/bottom fade on rolling digits (FR-022, default 4; 0 disables).
    func rollingFadeMask(height: CGFloat) -> some View {
        rollingOption { $0.fadeMaskHeight = max(height, 0) }
    }

    /// List item identity; a different key shows the next value without animation (FR-013).
    func rollingItemKey<K: Hashable>(_ key: K) -> some View {
        rollingOption { $0.itemKey = AnyHashable(key) }
    }

    /// Name read before the value, e.g. "최종가" (FR-019).
    func rollingAccessibilityLabel(_ label: String) -> some View {
        rollingOption { $0.accessibilityLabel = label }
    }

    /// Announce the final value after each change settles (FR-019, default true).
    func rollingAnnouncements(_ enabled: Bool) -> some View {
        rollingOption { $0.announcements = enabled }
    }

    /// Order inside a `RollingNumberGroup` announcement (lower first; unset keeps appearance order).
    func rollingGroupOrder(_ order: Int) -> some View {
        rollingOption { $0.groupOrder = order }
    }

    /// Called once when a change starts animating from rest (FR-020). Runs on the main actor.
    func onRollingAnimationStart(_ action: @escaping () -> Void) -> some View {
        rollingOption { $0.onStart = action }
    }

    /// Called once when every element has settled on the latest value (FR-020). Runs on the main actor.
    func onRollingAnimationEnd(_ action: @escaping () -> Void) -> some View {
        rollingOption { $0.onEnd = action }
    }
}
