import XCTest
@testable import RollingNumber

extension SymbolsJSON {
    var symbols: NumberSymbols {
        NumberSymbols(decimalSeparator: decimal ?? ".", groupingSeparator: group ?? ",", minusSigns: Set(minus ?? ["-", "\u{2212}"]))
    }
}

extension NumberJSON {
    func number(_ symbols: SymbolsJSON?) -> FormattedNumber? {
        guard let v = DecimalValue.parse(value) else { return nil }
        let s = symbols?.symbols ?? NumberSymbols(decimalSeparator: ".", groupingSeparator: ",")
        return FormattedNumber(text: text, value: v, symbols: s, prefix: prefix ?? "", suffix: suffix ?? "", formatKey: formatKey ?? "default")
    }
}

/// Runs every `fixtures/diff/` case; same comparison as Android DiffFixtureTest.kt.
final class DiffFixtureTests: XCTestCase {
    private func check(_ c: DiffCase) throws -> [String] {
        let from = c.from.flatMap { $0.number(c.symbols) }
        if c.from != nil && from == nil { return ["bad from value"] }
        guard let to = c.to.number(c.symbols) else { return ["bad to value"] }
        guard let trend = Trend(rawValue: c.trend ?? "auto") else { return ["unknown trend \(c.trend ?? "")"] }
        let result = try TransitionDiff.compute(from: from, to: to, trend: trend, animateFirstDisplay: c.animateFirstDisplay ?? false)
        var problems: [String] = []
        if result.direction.rawValue != c.expect.direction {
            problems.append("direction: expected \(c.expect.direction), got \(result.direction.rawValue)")
        }
        let actual = result.elements.map { "\($0.key) \($0.kind.rawValue) \($0.from ?? "null") -> \($0.to ?? "null") \($0.action.rawValue)" }
        let expected = c.expect.elements.map { "\($0.key) \($0.kind) \($0.from ?? "null") -> \($0.to ?? "null") \($0.action)" }
        if actual != expected {
            problems.append("elements:\n    expected: \(expected.joined(separator: "\n              "))\n    actual:   \(actual.joined(separator: "\n              "))")
        }
        return problems
    }

    func testAllDiffFixturesPass() throws {
        let cases = try Fixtures.diff()
        XCTAssertFalse(cases.isEmpty, "no diff fixtures found")
        var failures: [String] = []
        for (file, c) in cases {
            let problems: [String]
            do { problems = try check(c) } catch { problems = ["threw \(error)"] }
            if !problems.isEmpty { failures.append("\(file)#\(c.id):\n  " + problems.joined(separator: "\n  ")) }
        }
        if !failures.isEmpty { XCTFail("\(failures.count) diff fixture(s) failed:\n" + failures.joined(separator: "\n")) }
    }
}
