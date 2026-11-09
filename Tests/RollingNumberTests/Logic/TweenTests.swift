import XCTest
@testable import RollingNumber

/// Same cases as Android TweenTest.kt.
final class TweenTests: XCTestCase {
    func testCubicBezierEndpointsAndLinearMidpoint() {
        XCTAssertEqual(CubicBezier.ease(0, 0.4, 0, 0.2, 1), 0)
        XCTAssertEqual(CubicBezier.ease(1, 0.4, 0, 0.2, 1), 1)
        XCTAssertEqual(CubicBezier.ease(0.5, 0, 0, 1, 1), 0.5, accuracy: 1e-6)
        XCTAssertGreaterThan(CubicBezier.ease(0.5, 0.4, 0, 0.2, 1), 0.5)
    }

    func testTweenCompletesExactlyAtItsDuration() {
        let v = AnimatedValue(0, curve: .tween(durationMs: 100, x1: 0, y1: 0, x2: 1, y2: 1), positionTolerance: 0.001, velocityTolerance: 0.01)
        v.animate(to: 1)
        for _ in 0..<24 { v.step(stepSeconds) }
        XCTAssertFalse(v.isSettled)
        XCTAssertEqual(v.value, 0.96, accuracy: 1e-6)
        v.step(stepSeconds)
        XCTAssertTrue(v.isSettled)
        XCTAssertEqual(v.value, 1)
    }

    func testInterruptedTweenRestartsFromCurrentValue() {
        let v = AnimatedValue(0, curve: .tween(durationMs: 100, x1: 0, y1: 0, x2: 1, y2: 1), positionTolerance: 0.001, velocityTolerance: 0.01)
        v.animate(to: 1)
        for _ in 0..<10 { v.step(stepSeconds) }
        let atInterrupt = v.value
        XCTAssertEqual(atInterrupt, 0.4, accuracy: 1e-6)
        v.animate(to: 0)
        v.step(stepSeconds)
        XCTAssertLessThan(abs(v.value - atInterrupt), 0.05, "no jump after retarget")
        for _ in 0..<30 { v.step(stepSeconds) }
        XCTAssertEqual(v.value, 0)
    }
}
