import XCTest
@testable import RollingNumber

/// Same cases as Android DecimalValueTest.kt.
final class DecimalValueTests: XCTestCase {
    private func d(_ text: String) -> DecimalValue {
        guard let v = DecimalValue.parse(text) else { fatalError("parse failed: \(text)") }
        return v
    }

    func testRejectsTextOutsideTheDecimalPattern() {
        for text in ["NaN", "Infinity", "-Infinity", "", "1e5", "1,000", "+1", ".5", "5.", "1.2.3", " 1"] {
            XCTAssertNil(DecimalValue.parse(text), "should reject '\(text)'")
        }
    }

    func testNegativeZeroIsNotNegative() {
        XCTAssertFalse(d("-0").negative)
        XCTAssertFalse(d("-0.00").negative)
        XCTAssertEqual(d("-0.00").canonical, "0")
    }

    func testStripsLeadingAndTrailingZeros() {
        XCTAssertEqual(d("0012").integerDigits, "12")
        XCTAssertEqual(d("000").integerDigits, "0")
        XCTAssertEqual(d("1.500").fractionDigits, "5")
        XCTAssertEqual(d("3.000").fractionDigits, "")
        XCTAssertEqual(d("-001.0500").canonical, "-1.05")
    }

    func testTrailingZerosAreTheSameValue() {
        XCTAssertEqual(d("12.5"), d("12.50"))
        XCTAssertEqual(DecimalValue.compare(d("12.5"), d("12.50")), 0)
    }

    func testComparesSignThenLengthThenDigits() {
        XCTAssertLessThan(d("-1"), d("0"))
        XCTAssertLessThan(d("-100"), d("-99"))
        XCTAssertLessThan(d("99"), d("100"))
        XCTAssertLessThan(d("12.99"), d("13"))
        XCTAssertLessThan(d("12.5"), d("12.51"))
        XCTAssertGreaterThan(d("-12.5"), d("-12.51"))
        XCTAssertLessThan(d("9900"), d("12900"))
    }

    func testComparesValuesBeyondDoublePrecision() {
        XCTAssertLessThan(d("12345678901234567890"), d("12345678901234567891"))
        XCTAssertGreaterThan(d("0.10000000000000000001"), d("0.1"))
    }
}
