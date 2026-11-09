import XCTest
@testable import RollingNumber

/// Same cases as Android MotionContinuityTest.kt (US3 / SC-006).
final class MotionContinuityTests: XCTestCase {
    private let symbols = NumberSymbols(decimalSeparator: ".", groupingSeparator: ",")
    private func n(_ text: String) -> FormattedNumber {
        FormattedNumber(text: text, value: DecimalValue.parse(String(text.filter { $0.isNumber }))!, symbols: symbols)
    }

    private let digitWidth = 20.0
    private let limit = 0.35

    private struct Sample { let offset: Double; let alpha: Double }
    private typealias Snapshot = (layers: [String: Sample], widths: [String: Double])

    private func snapshot(_ e: MotionEngine) -> Snapshot {
        var layers: [String: Sample] = [:]
        for el in e.elements {
            let texts = el.layers.map { $0.text }
            XCTAssertEqual(texts.count, Set(texts).count, "element \(el.key) has duplicate layers \(texts)")
            for l in el.layers { layers["\(el.key)/\(l.text)"] = Sample(offset: l.offset.value, alpha: l.alpha.value) }
        }
        let keys = e.elements.map { $0.key }
        XCTAssertEqual(keys.count, Set(keys).count, "duplicate element keys \(keys)")
        var widths: [String: Double] = [:]
        for item in e.frame(measure: { Double($0.count) * self.digitWidth }, tabularDigitWidth: digitWidth).items { widths[item.key] = item.width }
        return (layers, widths)
    }

    private func assertContinuous(_ before: Snapshot, _ after: Snapshot, _ t: Int64) {
        for (k, s) in after.layers {
            if let p = before.layers[k] {
                XCTAssertLessThanOrEqual(abs(s.offset - p.offset), limit, "t=\(t) layer \(k) offset jumped")
                XCTAssertLessThanOrEqual(abs(s.alpha - p.alpha), limit, "t=\(t) layer \(k) alpha jumped")
            } else {
                XCTAssertLessThanOrEqual(s.alpha, limit, "t=\(t) new layer \(k) appeared with alpha \(s.alpha)")
            }
        }
        for (k, w) in after.widths {
            XCTAssertLessThanOrEqual(abs(w - (before.widths[k] ?? 0)), limit * digitWidth, "t=\(t) element \(k) width jumped")
        }
    }

    @discardableResult
    private func run(_ from: String, _ mid: String, _ back: String, reverseAtMs: Int64, onReverse: (MotionEngine) -> Void = { _ in }) throws -> MotionEngine {
        let e = MotionEngine()
        let a = n(from), b = n(mid)
        e.showImmediately(try Tokenizer.tokenize(a))
        var prev = snapshot(e)
        e.apply(try TransitionDiff.compute(from: a, to: b, trend: .auto))
        e.step(0)
        var t: Int64 = 0
        var reversed = false
        while true {
            t += 16
            if !reversed && t >= reverseAtMs {
                e.apply(try TransitionDiff.compute(from: b, to: n(back), trend: .auto))
                reversed = true
                onReverse(e)
            }
            e.step(t * 1_000_000)
            let now = snapshot(e)
            assertContinuous(prev, now, t)
            prev = now
            if reversed && !e.isAnimating { break }
            precondition(t < 5_000, "did not settle")
        }
        return e
    }

    func testReversalAt40PercentRollsBackWithoutJumps() throws {
        let e = try run("₩15,000", "₩12,000", "₩15,000", reverseAtMs: 200)
        XCTAssertEqual(e.elements.first { $0.key == "I3" }!.layers.map { $0.text }, ["5"])
    }

    func testReversedEnteringDigitFadesOutFromItsCurrentAlpha() throws {
        var alphaAtReverse = -1.0
        let e = try run("₩9,900", "₩12,900", "₩9,900", reverseAtMs: 200) { e in
            let one = e.elements.first { $0.key == "I4" }!
            XCTAssertEqual(one.layers.count, 1, "no new layer for the reversed digit")
            alphaAtReverse = one.layers[0].alpha.value
            XCTAssertTrue(one.exiting)
        }
        XCTAssertTrue((0.05...0.99).contains(alphaAtReverse), "the '1' was partly visible when reversed: \(alphaAtReverse)")
        XCTAssertFalse(e.elements.contains { $0.key == "I4" })
    }

    func testManyRapidRetargetsStayContinuous() throws {
        let e = MotionEngine()
        let values = ["₩10,000", "₩11,000", "₩9,900", "₩12,900", "₩900", "₩19,800"]
        e.showImmediately(try Tokenizer.tokenize(n(values[0])))
        var prev = snapshot(e)
        var current = n(values[0])
        e.step(0)
        var t: Int64 = 0
        var next = 1
        while next < values.count || e.isAnimating {
            t += 16
            if next < values.count && t % 64 == 0 {
                let target = n(values[next]); next += 1
                e.apply(try TransitionDiff.compute(from: current, to: target, trend: .auto))
                current = target
            }
            e.step(t * 1_000_000)
            let now = snapshot(e)
            assertContinuous(prev, now, t)
            prev = now
            precondition(t < 5_000, "did not settle")
        }
        XCTAssertEqual(e.elements.count, "₩19,800".count)
    }
}
