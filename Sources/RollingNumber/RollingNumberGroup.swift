import SwiftUI

/// Shared state of one group: a single announcement coordinator (FR-019a).
final class RollingGroupState: ObservableObject {
    let coordinator = AnnouncementCoordinator { text in Announcer.announce(text) }
}

private struct RollingGroupKey: EnvironmentKey {
    static let defaultValue: RollingGroupState? = nil
}

extension EnvironmentValues {
    var rollingGroup: RollingGroupState? {
        get { self[RollingGroupKey.self] }
        set { self[RollingGroupKey.self] = newValue }
    }
}

/// Groups every `RollingNumber` inside `content` (FR-015, FR-019a).
///
/// - Members changed together start together, and because each member's width animates every
///   frame inside one SwiftUI transaction, neighbors in an `HStack`/`VStack` move along with it.
/// - Members do not announce individually; once all changed members settle, one message is
///   announced: "{label} {value}, …" in member order (`rollingGroupOrder`, then appearance order).
///
/// The group does not lay anything out; arrange members however you like.
public struct RollingNumberGroup<Content: View>: View {
    @StateObject private var state = RollingGroupState()
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        content.environment(\.rollingGroup, state)
    }
}
