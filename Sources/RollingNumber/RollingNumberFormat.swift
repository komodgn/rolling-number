import Foundation

/// Rounding applied when a value has more fraction digits than the format allows.
public enum RollingRounding: Hashable {
    case halfUp, halfEven, down, up

    var rule: FloatingPointRoundingRule {
        switch self {
        case .halfUp: return .toNearestOrAwayFromZero
        case .halfEven: return .toNearestOrEven
        case .down: return .towardZero
        case .up: return .awayFromZero
        }
    }
}

/// How a `RollingNumber` value is turned into text.
/// Locale-specific symbols and placement come from the OS locale rules.
public struct RollingNumberFormat: Hashable {
    enum Style: String, Hashable { case number, currency, percent, compact }

    let style: Style
    let currencyCode: String?
    let minFraction: Int?
    let maxFraction: Int?
    let grouping: Bool
    let rounding: RollingRounding

    private static let maxAllowedFraction = 15

    private init(style: Style, currencyCode: String?, minFraction: Int?, maxFraction: Int?, grouping: Bool, rounding: RollingRounding) {
        var minF = minFraction
        var maxF = maxFraction
        var code = currencyCode
        // Invalid values: assert in debug, clamp to the nearest valid value in release.
        if let m = minF, !(0...Self.maxAllowedFraction).contains(m) {
            assertionFailure("minFraction must be 0...\(Self.maxAllowedFraction), was \(m)")
            minF = min(max(m, 0), Self.maxAllowedFraction)
        }
        if let m = maxF, !(0...Self.maxAllowedFraction).contains(m) {
            assertionFailure("maxFraction must be 0...\(Self.maxAllowedFraction), was \(m)")
            maxF = min(max(m, 0), Self.maxAllowedFraction)
        }
        if let a = minF, let b = maxF, a > b {
            assertionFailure("minFraction (\(a)) must be <= maxFraction (\(b))")
            maxF = a
        }
        if style == .currency {
            let c = code ?? ""
            let valid = c.count == 3 && c.allSatisfy { ("A"..."Z").contains($0) } && Locale.isoCurrencyCodes.contains(c)
            if !valid {
                assertionFailure("Unknown or invalid ISO 4217 currency code '\(c)'")
                code = "XXX"
            }
        }
        self.style = style
        self.currencyCode = code
        self.minFraction = minF
        self.maxFraction = maxF
        self.grouping = grouping
        self.rounding = rounding
    }

    /// Effective fraction digits, identical to Android `fractionRange()`.
    func fractionRange() -> ClosedRange<Int> {
        let defaultRange: ClosedRange<Int>
        switch style {
        case .number: defaultRange = 0...3
        case .currency:
            let f = NumberFormatter()
            f.numberStyle = .currency
            f.currencyCode = currencyCode
            let digits = max(f.maximumFractionDigits, 0)
            defaultRange = digits...digits
        case .percent: defaultRange = 0...0
        case .compact: defaultRange = 0...1
        }
        let lower = minFraction ?? min(defaultRange.lowerBound, maxFraction ?? defaultRange.lowerBound)
        let upper = maxFraction ?? max(defaultRange.upperBound, lower)
        return lower...upper
    }

    /// Identifies everything except the value; a change means "replace immediately" (FR-014).
    func formatKey(locale: Locale) -> String {
        let r = fractionRange()
        return [locale.identifier, style.rawValue, currencyCode ?? "", "\(r.lowerBound)", "\(r.upperBound)", "\(grouping)", "\(rounding)"]
            .joined(separator: "|")
    }

    /// Plain number. Default fraction digits 0...3.
    public static func number(minFraction: Int? = nil, maxFraction: Int? = nil, grouping: Bool = true, rounding: RollingRounding = .halfUp) -> Self {
        Self(style: .number, currencyCode: nil, minFraction: minFraction, maxFraction: maxFraction, grouping: grouping, rounding: rounding)
    }

    /// Currency with ISO 4217 `code`. Default fraction digits are the currency's own (KRW 0, USD 2).
    public static func currency(code: String, minFraction: Int? = nil, maxFraction: Int? = nil, grouping: Bool = true, rounding: RollingRounding = .halfUp) -> Self {
        Self(style: .currency, currencyCode: code, minFraction: minFraction, maxFraction: maxFraction, grouping: grouping, rounding: rounding)
    }

    /// Percent: 0.125 is shown as 12.5% (with one fraction digit). Default fraction digits 0...0.
    public static func percent(minFraction: Int? = nil, maxFraction: Int? = nil, rounding: RollingRounding = .halfUp) -> Self {
        Self(style: .percent, currencyCode: nil, minFraction: minFraction, maxFraction: maxFraction, grouping: true, rounding: rounding)
    }

    /// Short compact notation using the locale's units (1.2K, 1.2천).
    public static func compact(maxFraction: Int? = 1, rounding: RollingRounding = .halfUp) -> Self {
        Self(style: .compact, currencyCode: nil, minFraction: nil, maxFraction: maxFraction, grouping: true, rounding: rounding)
    }
}
