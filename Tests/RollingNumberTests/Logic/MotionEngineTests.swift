import XCTest
@testable import RollingNumber

/// Same cases as Android MotionEngineTest.kt.
final class MotionEngineTests: XCTestCase {
    private let symbols = NumberSymbols(decimalSeparator: ".", groupingSeparator: ",")
    private func n(_ text: String, _ value: String) -> FormattedNumber {
        FormattedNumber(text: text, value: DecimalValue.parse(value)!, symbols: symbols)
    }

    private func engine(_ from: String, _ fromValue: String, _ to: String, _ toValue: String) throws -> MotionEngine {
        let e = MotionEngine()
        let a = n(from, fromValue)
        e.showImmediately(try Tokenizer.tokenize(a))
        e.apply(try TransitionDiff.compute(from: a, to: n(to, toValue), trend: .auto))
        return e
    }

    @discardableResult
    private func settleSeconds(_ e: MotionEngine, frameMs: Int64 = 16) -> Double {
        var t: Int64 = 0
        e.step(0)
        while e.isAnimating {
            t += frameMs
            e.step(t * 1_000_000)
            precondition(t < 5_000, "did not settle")
        }
        return Double(t) / 1000
    }

    func testDefaultCurvesSettleWithinTargetRange() throws {
        let seconds = settleSeconds(try engine("₩9,900", "9900", "₩12,900", "12900"))
        XCTAssertTrue((0.3...0.6).contains(seconds), "settled in \(seconds) s, expected 0.3...0.6")
    }

    func testResultsDependOnElapsedTimeOnlyNotFrameRate() throws {
        let e8 = try engine("₩9,900", "9900", "₩12,900", "12900")
        let e16 = try engine("₩9,900", "9900", "₩12,900", "12900")
        let ej = try engine("₩9,900", "9900", "₩12,900", "12900")
        [e8, e16, ej].forEach { $0.step(0) }
        let jitter: [Int64] = [5, 13, 7, 19, 4, 12, 20, 16]
        var tj: Int64 = 0
        var i = 0
        for t in stride(from: Int64(48), through: 480, by: 48) {
            var t8 = t - 48
            while t8 < t { t8 += 8; e8.step(t8 * 1_000_000) }
            var t16 = t - 48
            while t16 < t { t16 += 16; e16.step(t16 * 1_000_000) }
            while tj < t { tj = min(t, tj + jitter[i % jitter.count]); i += 1; ej.step(tj * 1_000_000) }
            let a = e8.frame(measure: { _ in 10 }, tabularDigitWidth: 10)
            XCTAssertEqual(a, e16.frame(measure: { _ in 10 }, tabularDigitWidth: 10), "t=\(t) 8 vs 16 ms")
            XCTAssertEqual(a, ej.frame(measure: { _ in 10 }, tabularDigitWidth: 10), "t=\(t) 8 ms vs jitter")
        }
    }

    func testEnteringDigitStartsBelowTransparentAndRests() throws {
        let e = try engine("₩9,900", "9900", "₩12,900", "12900")
        let entering = e.elements.first { $0.key == "I4" }!
        let layer = entering.layers[0]
        XCTAssertEqual(layer.offset.value, 1)
        XCTAssertEqual(layer.alpha.value, 0)
        XCTAssertEqual(entering.presence.value, 0)
        settleSeconds(e)
        XCTAssertEqual(layer.offset.value, 0)
        XCTAssertEqual(layer.alpha.value, 1)
        XCTAssertEqual(entering.presence.value, 1)
    }

    func testExitingDigitLeavesDownwardAndIsRemoved() throws {
        let e = try engine("₩12,900", "12900", "₩9,900", "9900")
        let exiting = e.elements.first { $0.key == "I4" }!
        XCTAssertTrue(exiting.exiting)
        XCTAssertEqual(exiting.layers[0].offset.target, 1)
        settleSeconds(e)
        XCTAssertNil(e.elements.first { $0.key == "I4" })
    }

    func testRollingDigitKeepsConstantTabularWidth() throws {
        let e = try engine("₩15,000", "15000", "₩12,000", "12000")
        e.step(0)
        var t: Int64 = 0
        while e.isAnimating {
            t += 16
            e.step(t * 1_000_000)
            let item = e.frame(measure: { _ in 10 }, tabularDigitWidth: 12).items.first { $0.key == "I3" }!
            XCTAssertEqual(item.width, 12, accuracy: 1e-4, "t=\(t)")
        }
    }

    func testCrossfadeWidthMovesContinuouslyBetweenTexts() throws {
        let e = try engine("1.2K", "1200", "1.2MM", "1200000")
        let measure: (String) -> Double = { Double($0.count) * 10 }
        var last = e.frame(measure: measure, tabularDigitWidth: nil).items.first { $0.key == "Ls0" }!.width
        XCTAssertEqual(last, 10, accuracy: 1e-4)
        e.step(0)
        var t: Int64 = 0
        while e.isAnimating {
            t += 16
            e.step(t * 1_000_000)
            let w = e.frame(measure: measure, tabularDigitWidth: nil).items.first { $0.key == "Ls0" }!.width
            XCTAssertLessThanOrEqual(abs(w - last), 3.5, "jump at t=\(t)")
            last = w
        }
        XCTAssertEqual(last, 20, accuracy: 1e-4)
    }

    func testSettledValuesSnapExactlyToTargets() throws {
        let e = try engine("₩15,000", "15000", "₩12,000", "12000")
        settleSeconds(e)
        XCTAssertFalse(e.isAnimating)
        for el in e.elements {
            XCTAssertEqual(el.presence.value, 1)
            XCTAssertEqual(el.layers.count, 1)
            XCTAssertEqual(el.layers[0].offset.value, 0)
            XCTAssertEqual(el.layers[0].alpha.value, 1)
        }
    }
}
