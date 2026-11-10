import Foundation

/// Formats values with the OS Foundation rules (research R2).
enum FoundationFormatter {
    static func format(_ value: Decimal, format: RollingNumberFormat, locale: Locale) -> FormattedNumber? {
        guard let decimalValue = DecimalValue(value) else { return nil }
        let l = latinLocale(locale)
        let range = format.fractionRange()
        let grouping: NumberFormatStyleConfiguration.Grouping = format.grouping ? .automatic : .never
        let currencyGrouping: CurrencyFormatStyleConfiguration.Grouping = format.grouping ? .automatic : .never
        let rule = format.rounding.rule

        let text: String
        switch format.style {
        case .number:
            text = value.formatted(
                Decimal.FormatStyle(locale: l)
                    .precision(.fractionLength(range))
                    .grouping(grouping)
                    .rounded(rule: rule)
            )
        case .currency:
            text = value.formatted(
                Decimal.FormatStyle.Currency(code: format.currencyCode ?? "XXX", locale: l)
                    .precision(.fractionLength(range))
                    .grouping(currencyGrouping)
                    .rounded(rule: rule)
            )
        case .percent:
            text = value.formatted(
                Decimal.FormatStyle.Percent(locale: l)
                    .precision(.fractionLength(range))
                    .rounded(rule: rule)
            )
        case .compact:
            text = value.formatted(
                Decimal.FormatStyle(locale: l)
                    .notation(.compactName)
                    .precision(.fractionLength(range))
                    .rounded(rule: rule)
            )
        }
        return FormattedNumber(
            text: text,
            value: decimalValue,
            symbols: symbols(locale: l, format: format),
            formatKey: format.formatKey(locale: locale)
        )
    }

    /// Locale with Latin digits forced (MVP supports only 0-9).
    static func latinLocale(_ locale: Locale) -> Locale {
        let parts = locale.identifier.split(separator: "@", maxSplits: 1).map(String.init)
        var keywords = parts.count > 1
            ? parts[1].split(separator: ";").map(String.init).filter { !$0.hasPrefix("numbers=") }
            : []
        keywords.append("numbers=latn")
        return Locale(identifier: parts[0] + "@" + keywords.joined(separator: ";"))
    }

    static func symbols(locale: Locale, format: RollingNumberFormat) -> NumberSymbols {
        let f = NumberFormatter()
        f.locale = locale
        switch format.style {
        case .currency:
            f.numberStyle = .currency
            f.currencyCode = format.currencyCode
            return NumberSymbols(
                decimalSeparator: f.currencyDecimalSeparator ?? f.decimalSeparator ?? ".",
                groupingSeparator: f.currencyGroupingSeparator ?? f.groupingSeparator ?? ",",
                minusSigns: ["-", "\u{2212}", f.minusSign ?? "-"]
            )
        case .percent:
            f.numberStyle = .percent
        default:
            f.numberStyle = .decimal
        }
        return NumberSymbols(
            decimalSeparator: f.decimalSeparator ?? ".",
            groupingSeparator: f.groupingSeparator ?? ",",
            minusSigns: ["-", "\u{2212}", f.minusSign ?? "-"]
        )
    }
}
