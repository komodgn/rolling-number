import XCTest
@testable import RollingNumber

/// Same cases as Android TokenizerTest.kt.
final class TokenizerTests: XCTestCase {
    private let en = NumberSymbols(decimalSeparator: ".", groupingSeparator: ",")
    private let de = NumberSymbols(decimalSeparator: ",", groupingSeparator: ".")

    private func number(_ text: String, _ value: String, _ symbols: NumberSymbols? = nil, prefix: String = "", suffix: String = "") -> FormattedNumber {
        FormattedNumber(text: text, value: DecimalValue.parse(value)!, symbols: symbols ?? en, prefix: prefix, suffix: suffix)
    }

    private func render(_ tokens: [Token]) -> String {
        tokens.map { "\($0.kind.rawValue):\($0.key):\($0.text)" }.joined(separator: " ")
    }

    func testKrwWithGrouping() throws {
        XCTAssertEqual(render(try Tokenizer.tokenize(number("₩12,900", "12900"))),
                       "literal:Lp0:₩ int:I4:1 int:I3:2 group:G3:, int:I2:9 int:I1:0 int:I0:0")
    }

    func testSignOutsideCurrencySymbolIsNotCountedAsLiteral() throws {
        XCTAssertEqual(render(try Tokenizer.tokenize(number("-₩300", "-300"))),
                       "sign:S:- literal:Lp0:₩ int:I2:3 int:I1:0 int:I0:0")
    }

    func testGermanEuroWithTrailingSymbol() throws {
        XCTAssertEqual(render(try Tokenizer.tokenize(number("1.234,56\u{00a0}€", "1234.56", de))),
                       "int:I3:1 group:G3:. int:I2:2 int:I1:3 int:I0:4 decimal:D:, frac:F0:5 frac:F1:6 literal:Ls0:\u{00a0}€")
    }

    func testCompactUnitIsTrailingLiteral() throws {
        XCTAssertEqual(render(try Tokenizer.tokenize(number("1.2K", "1200"))),
                       "int:I0:1 decimal:D:. frac:F0:2 literal:Ls0:K")
    }

    func testDeveloperAffixesComeFirstAndLast() throws {
        XCTAssertEqual(render(try Tokenizer.tokenize(number("₩12,900", "12900", prefix: "~", suffix: "/월"))),
                       "prefix:AP:~ literal:Lp0:₩ int:I4:1 int:I3:2 group:G3:, int:I2:9 int:I1:0 int:I0:0 suffix:AS:/월")
    }

    func testUnicodeMinusIsASign() throws {
        XCTAssertEqual(render(try Tokenizer.tokenize(number("\u{2212}12", "-12"))), "sign:S:\u{2212} int:I1:1 int:I0:2")
    }

    func testLiteralSplitBySignBecomesTwoRuns() throws {
        XCTAssertEqual(render(try Tokenizer.tokenize(number("₩-300", "-300"))),
                       "literal:Lp0:₩ sign:S:- int:I2:3 int:I1:0 int:I0:0")
        XCTAssertEqual(render(try Tokenizer.tokenize(number("(-$5)", "-5"))),
                       "literal:Lp1:( sign:S:- literal:Lp0:$ int:I0:5 literal:Ls0:)")
    }

    func testEmptyAffixesProduceNoTokens() throws {
        XCTAssertEqual(render(try Tokenizer.tokenize(number("5", "5"))), "int:I0:5")
    }

    func testManyGroups() throws {
        XCTAssertEqual(render(try Tokenizer.tokenize(number("1,234,567.891", "1234567.891"))),
                       "int:I6:1 group:G6:, int:I5:2 int:I4:3 int:I3:4 group:G3:, int:I2:5 int:I1:6 int:I0:7 decimal:D:. frac:F0:8 frac:F1:9 frac:F2:1")
    }

    func testTextWithoutDigitsIsAnError() {
        XCTAssertThrowsError(try Tokenizer.tokenize(FormattedNumber(text: "₩", value: DecimalValue.parse("0")!, symbols: en)))
    }

    func testNormalizeKeepsOnlyNumericPartsWithCanonicalSymbols() throws {
        XCTAssertEqual(Tokenizer.normalize(try Tokenizer.tokenize(number("1.234,56\u{00a0}€", "1234.56", de))), "1,234.56")
        XCTAssertEqual(Tokenizer.normalize(try Tokenizer.tokenize(number("-₩300", "-300"))), "-300")
        XCTAssertEqual(Tokenizer.normalize(try Tokenizer.tokenize(number("1.2K", "1200"))), "1.2")
    }
}
