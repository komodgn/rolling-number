package io.github.komodgn.rollingnumber.logic

/** Locale symbols used by one formatted string (data-model §3). */
internal data class NumberSymbols(
    val decimalSeparator: String,
    val groupingSeparator: String,
    val minusSigns: Set<String> = setOf("-", "−"),
)

/** One formatted value: the unit of input to the logic layer (data-model §4). */
internal data class FormattedNumber(
    val text: String,
    val value: DecimalValue,
    val symbols: NumberSymbols,
    val prefix: String = "",
    val suffix: String = "",
    val formatKey: String = "",
) {
    /** Everything shown on screen, affixes included. */
    val displayText: String get() = prefix + text + suffix
}

internal enum class TokenKind(val id: String) {
    IntDigit("int"),
    FracDigit("frac"),
    Decimal("decimal"),
    Group("group"),
    Sign("sign"),
    Literal("literal"),
    Prefix("prefix"),
    Suffix("suffix"),
    ;

    /** Digit slots roll; everything else fades in place. */
    val isDigit: Boolean get() = this == IntDigit || this == FracDigit

    companion object {
        fun fromId(id: String): TokenKind = entries.first { it.id == id }
    }
}

/** One display piece of a formatted number (data-model §5). */
internal data class Token(val kind: TokenKind, val text: String, val key: String)
