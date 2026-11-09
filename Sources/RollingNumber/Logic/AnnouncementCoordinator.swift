import Foundation

/// Decides when and what to announce (FR-019, FR-019a, data-model §11). Mirrors Android
/// `AnnouncementCoordinator`: in a group, one message "{label} {text}, …" is announced once every
/// member with a pending change has settled.
final class AnnouncementCoordinator {
    private struct Member {
        let session: RollingSession
        var order: Int?
        let index: Int
    }

    private let announce: (String) -> Void
    private var members: [Member] = []
    private var pending: [ObjectIdentifier: RollingSession] = [:]
    private var nextIndex = 0

    init(announce: @escaping (String) -> Void) { self.announce = announce }

    func register(_ session: RollingSession, order: Int? = nil) {
        if members.contains(where: { $0.session === session }) { return }
        members.append(Member(session: session, order: order, index: nextIndex))
        nextIndex += 1
        session.coordinator = self
    }

    func unregister(_ session: RollingSession) {
        members.removeAll { $0.session === session }
        pending[ObjectIdentifier(session)] = nil
        if session.coordinator === self { session.coordinator = nil }
        flushIfReady()
    }

    func setOrder(_ session: RollingSession, _ order: Int?) {
        if let i = members.firstIndex(where: { $0.session === session }) { members[i].order = order }
    }

    func onChangePending(_ session: RollingSession) {
        if session.announce { pending[ObjectIdentifier(session)] = session }
    }

    func onSettled(_ session: RollingSession) {
        if session.announce { pending[ObjectIdentifier(session)] = session }
        flushIfReady()
    }

    func cancel(_ session: RollingSession) {
        pending[ObjectIdentifier(session)] = nil
        flushIfReady()
    }

    private func flushIfReady() {
        if pending.isEmpty || pending.values.contains(where: { $0.isAnimating }) { return }
        let text = members
            .filter { pending[ObjectIdentifier($0.session)] != nil }
            .sorted { ($0.order ?? Int.max, $0.index) < ($1.order ?? Int.max, $1.index) }
            .map { $0.session.accessibilityText }
            .joined(separator: ", ")
        pending.removeAll()
        if !text.isEmpty { announce(text) }
    }
}
