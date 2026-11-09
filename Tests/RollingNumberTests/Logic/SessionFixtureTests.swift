import XCTest
@testable import RollingNumber

/// Drives sessions with a virtual 16 ms frame clock per contracts/fixtures.md §3.
/// Same procedure and comparison as Android SessionFixtureTest.kt.
final class SessionFixtureTests: XCTestCase {
    private let frameMs: Int64 = 16

    func run(_ c: SessionCase) throws -> [ExpectJSON] {
        var actual: [ExpectJSON] = []
        var nowMs: Int64 = 0
        var sessions: [(id: String, session: RollingSession)] = []
        let coordinator = AnnouncementCoordinator { text in actual.append(ExpectJSON(event: nil, member: nil, t: nowMs, announce: text)) }
        guard let trend = Trend(rawValue: c.trend ?? "auto") else { throw FixtureError("unknown trend") }

        for (index, m) in c.members.enumerated() {
            let s = RollingSession(spec: MotionSpec(animateFirstDisplay: m.animateFirstDisplay ?? false), trend: trend)
            s.label = m.label
            s.announce = m.announce ?? true
            s.onStart = { actual.append(ExpectJSON(event: "start", member: m.id, t: nowMs, announce: nil)) }
            s.onEnd = { actual.append(ExpectJSON(event: "end", member: m.id, t: nowMs, announce: nil)) }
            coordinator.register(s, order: index)
            sessions.append((m.id, s))
        }
        func session(_ id: String) -> RollingSession { sessions.first { $0.id == id }!.session }

        func advance(to target: Int64) {
            while nowMs + frameMs <= target {
                nowMs += frameMs
                sessions.forEach { $0.session.step(nowMs * 1_000_000) }
            }
            nowMs = target
        }

        for step in c.steps.sorted(by: { $0.t < $1.t }) {
            advance(to: step.t)
            if let reduce = step.reduceMotion { sessions.forEach { $0.session.setAnimationAllowed(!reduce) } }
            if let id = step.disableAnimation { session(id).setAnimationAllowed(false) }
            if let id = step.enableAnimation { session(id).setAnimationAllowed(true) }
            step.itemKey?.forEach { id, key in session(id).setItemKey(key) }
            // Apply sets in member order so results do not depend on dictionary order.
            if let set = step.set {
                for (id, s) in sessions {
                    guard let n = set[id] else { continue }
                    // Invalid values are dropped by the UI layer; the last valid value stays.
                    if let number = n.number(c.symbols) { s.update(number) }
                }
            }
        }
        _ = coordinator
        return actual
    }

    private func matches(_ e: ExpectJSON, _ a: ExpectJSON) -> Bool {
        e.event == a.event &&
            (e.event == nil || e.member == a.member) &&
            e.announce == a.announce &&
            (e.t == nil || abs(e.t! - (a.t ?? Int64.min / 2)) <= frameMs + 1)
    }

    private func describe(_ e: ExpectJSON) -> String {
        e.announce != nil ? "announce('\(e.announce!)'@\(e.t.map(String.init) ?? "nil"))" : "\(e.event ?? "")(\(e.member ?? "")@\(e.t.map(String.init) ?? "nil"))"
    }

    func testAllSessionFixturesPass() throws {
        let cases = try Fixtures.session()
        XCTAssertFalse(cases.isEmpty, "no session fixtures found")
        var failures: [String] = []
        for (file, c) in cases {
            do {
                let actual = try run(c)
                let ok = actual.count == c.expect.count && zip(c.expect, actual).allSatisfy { matches($0, $1) }
                if !ok {
                    failures.append("\(file)#\(c.id):\n  expected \(c.expect.map(describe))\n  actual   \(actual.map(describe))")
                }
            } catch {
                failures.append("\(file)#\(c.id): threw \(error)")
            }
        }
        if !failures.isEmpty { XCTFail("\(failures.count) session fixture(s) failed:\n" + failures.joined(separator: "\n")) }
    }
}
