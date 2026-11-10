import Foundation

/// Locale symbols used by one formatted string (data-model §3).
struct NumberSymbols: Hashable {
    var decimalSeparator: String
    var groupingSeparator: String
    var minusSigns: Set<String> = ["-", "\u{2212}"]
}

/// One formatted value: the unit of input to the logic layer (data-model §4).
struct FormattedNumber: Hashable {
    var text: String
    var value: DecimalValue
    var symbols: NumberSymbols
    var prefix: String = ""
    var suffix: String = ""
    var formatKey: String = ""

    /// Everything shown on screen, affixes included.
    var displayText: String { prefix + text + suffix }
}

enum TokenKind: String, Hashable {
    case intDigit = "int"
    case fracDigit = "frac"
    case decimal
    case group
    case sign
    case literal
    case prefix
    case suffix

    /// Digit slots roll; everything else fades in place.
    var isDigit: Bool { self == .intDigit || self == .fracDigit }
}

/// One display piece of a formatted number (data-model §5).
struct Token: Hashable {
    var kind: TokenKind
    var text: String
    var key: String
}
