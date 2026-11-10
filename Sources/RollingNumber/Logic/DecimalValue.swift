import Foundation

/// Exact decimal value used only for comparison (data-model §1).
/// Canonical form: no leading integer zeros (except "0"), no trailing fraction zeros, never "-0".
struct DecimalValue: Comparable, Hashable, CustomStringConvertible {
    let negative: Bool
    let integerDigits: String
    let fractionDigits: String

    var canonical: String {
        var s = negative ? "-" : ""
        s += integerDigits
        if !fractionDigits.isEmpty { s += "." + fractionDigits }
        return s
    }

    var description: String { canonical }

    /// Accepts only `-?[0-9]+(\.[0-9]+)?`.
    static func parse(_ text: String) -> DecimalValue? {
        var chars = Substring(text)
        var negative = false
        if chars.first == "-" {
            negative = true
            chars = chars.dropFirst()
        }
        let parts = chars.split(separator: ".", omittingEmptySubsequences: false)
        guard parts.count == 1 || parts.count == 2 else { return nil }
        let isDigits: (Substring) -> Bool = { s in !s.isEmpty && s.allSatisfy { ("0"..."9").contains($0) } }
        guard isDigits(parts[0]) else { return nil }
        if parts.count == 2 { guard isDigits(parts[1]) else { return nil } }

        var int = String(parts[0].drop { $0 == "0" })
        if int.isEmpty { int = "0" }
        var frac = parts.count == 2 ? String(parts[1]) : ""
        while frac.last == "0" { frac.removeLast() }
        let isZero = int == "0" && frac.isEmpty
        return DecimalValue(negative: negative && !isZero, integerDigits: int, fractionDigits: frac)
    }

    /// `Decimal.description` is locale-independent ("12.5").
    init?(_ decimal: Decimal) {
        guard decimal.isFinite, let parsed = DecimalValue.parse(decimal.description) else { return nil }
        self = parsed
    }

    private init(negative: Bool, integerDigits: String, fractionDigits: String) {
        self.negative = negative
        self.integerDigits = integerDigits
        self.fractionDigits = fractionDigits
    }

    /// Order: sign → integer digit count → integer digits → fraction digits.
    static func compare(_ a: DecimalValue, _ b: DecimalValue) -> Int {
        if a.negative != b.negative { return a.negative ? -1 : 1 }
        let magnitude = compareMagnitude(a, b)
        return a.negative ? -magnitude : magnitude
    }

    static func < (lhs: DecimalValue, rhs: DecimalValue) -> Bool { compare(lhs, rhs) < 0 }

    private static func compareMagnitude(_ a: DecimalValue, _ b: DecimalValue) -> Int {
        if a.integerDigits.count != b.integerDigits.count {
            return a.integerDigits.count < b.integerDigits.count ? -1 : 1
        }
        if a.integerDigits != b.integerDigits { return a.integerDigits < b.integerDigits ? -1 : 1 }
        let width = max(a.fractionDigits.count, b.fractionDigits.count)
        let fa = a.fractionDigits.padding(toLength: width, withPad: "0", startingAt: 0)
        let fb = b.fractionDigits.padding(toLength: width, withPad: "0", startingAt: 0)
        if fa == fb { return 0 }
        return fa < fb ? -1 : 1
    }
}
