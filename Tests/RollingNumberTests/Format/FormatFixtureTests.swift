import XCTest
@testable import RollingNumber

/// Runs every `fixtures/format/` case through the Foundation formatter.
/// Comparison rules match Android `FormatChecks.kt` (contracts/fixtures.md §1).
final class FormatFixtureTests: XCTestCase {
    private static let looseRemoved: Set<Character> = [" ", "\u{00a0}", "\u{202f}", "\u{200e}", "\u{200f}", "\u{061c}"]

    static func loose(_ s: String) -> String { String(s.filter { !looseRemoved.contains($0) }) }

    static func toFormat(_ spec: FormatSpecJSON) throws -> RollingNumberFormat {
        let rounding: RollingRounding
        switch spec.rounding ?? "halfUp" {
        case "halfUp": rounding = .halfUp
        case "halfEven": rounding = .halfEven
        case "down": rounding = .down
        case "up": rounding = .up
        case let other: throw FixtureError("unknown rounding '\(other)'")
        }
        switch spec.style {
        case "number": return .number(minFraction: spec.minFraction, maxFraction: spec.maxFraction, grouping: spec.grouping ?? true, rounding: rounding)
        case "currency": return .currency(code: spec.currency ?? "", minFraction: spec.minFraction, maxFraction: spec.maxFraction, grouping: spec.grouping ?? true, rounding: rounding)
        case "percent": return .percent(minFraction: spec.minFraction, maxFraction: spec.maxFraction, rounding: rounding)
        case "compact": return .compact(maxFraction: spec.maxFraction ?? 1, rounding: rounding)
        default: throw FixtureError("unknown style '\(spec.style)'")
        }
    }

    static func check(_ c: FormatCase) throws -> [String] {
        guard let value = Decimal(string: c.input.value, locale: Locale(identifier: "en_US_POSIX")) else {
            return ["bad value '\(c.input.value)'"]
        }
        guard var formatted = FoundationFormatter.format(value, format: try toFormat(c.input.format), locale: Locale(identifier: c.input.locale)) else {
            return ["formatter returned nil"]
        }
        formatted.prefix = c.input.prefix ?? ""
        formatted.suffix = c.input.suffix ?? ""
        let tokens = try Tokenizer.tokenize(formatted)
        let shown = "'\(formatted.displayText)'"
        var problems: [String] = []

        let normalized = Tokenizer.normalize(tokens)
        if normalized != c.expect.normalized {
            problems.append("normalized: expected '\(c.expect.normalized)', got '\(normalized)' from \(shown)")
        }
        guard tokens.count == c.expect.tokens.count else {
            let got = tokens.map { "\($0.kind.rawValue):\($0.key):\($0.text)" }.joined(separator: " ")
            problems.append("token count: expected \(c.expect.tokens.count), got \(tokens.count): \(got) from \(shown)")
            return problems
        }
        for (i, pair) in zip(tokens, c.expect.tokens).enumerated() {
            let (actual, expected) = pair
            guard actual.kind.rawValue == expected.kind, actual.key == expected.key else {
                problems.append("token \(i): expected \(expected.kind):\(expected.key), got \(actual.kind.rawValue):\(actual.key) from \(shown)")
                continue
            }
            let exactKinds: Set<String> = ["sign", "prefix", "suffix"]
            let mode = expected.textMatch ?? ((actual.kind.isDigit || exactKinds.contains(expected.kind)) ? "exact" : "loose")
            let ok: Bool
            switch mode {
            case "exact": ok = actual.text == expected.text
            case "loose": ok = loose(actual.text) == loose(expected.text)
            case "any": ok = true
            default: ok = false; problems.append("token \(i): unknown textMatch '\(mode)'")
            }
            if !ok { problems.append("token \(i) (\(expected.key)): expected text '\(expected.text)', got '\(actual.text)' from \(shown)") }
        }
        return problems
    }

    func testAllFormatFixturesPass() throws {
        let cases = try Fixtures.format()
        XCTAssertFalse(cases.isEmpty, "no format fixtures found")
        var failures: [String] = []
        for (file, c) in cases {
            let problems: [String]
            do { problems = try Self.check(c) } catch { problems = ["threw \(error)"] }
            if !problems.isEmpty { failures.append("\(file)#\(c.id):\n  " + problems.joined(separator: "\n  ")) }
        }
        if !failures.isEmpty { XCTFail("\(failures.count) format fixture(s) failed:\n" + failures.joined(separator: "\n")) }
    }
}
