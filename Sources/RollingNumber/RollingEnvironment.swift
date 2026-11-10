import SwiftUI

/// Options set through `View` modifiers and read by every `RollingNumber` below them.
struct RollingOptions {
    var trend: RollingTrend = .auto
    var motion: RollingMotion = .default
    var tabularDigits = true
    var fadeMaskHeight: CGFloat = 4
    var itemKey: AnyHashable?
    var accessibilityLabel: String?
    var announcements = true
    var groupOrder: Int?
    var onStart: (() -> Void)?
    var onEnd: (() -> Void)?
}

private struct RollingOptionsKey: EnvironmentKey {
    static let defaultValue = RollingOptions()
}

extension EnvironmentValues {
    var rollingOptions: RollingOptions {
        get { self[RollingOptionsKey.self] }
        set { self[RollingOptionsKey.self] = newValue }
    }
}

extension View {
    func rollingOption(_ update: @escaping (inout RollingOptions) -> Void) -> some View {
        transformEnvironment(\.rollingOptions, transform: update)
    }
}
