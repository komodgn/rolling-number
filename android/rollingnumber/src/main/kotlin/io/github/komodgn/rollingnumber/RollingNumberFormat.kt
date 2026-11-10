package io.github.komodgn.rollingnumber

import androidx.compose.runtime.Immutable
import java.util.Currency
import java.util.Locale

/** Rounding applied when a value has more fraction digits than the format allows. */
public enum class RollingRounding { HalfUp, HalfEven, Down, Up }

/**
 * How a [RollingNumber] value is turned into text.
 *
 * Create one with [number], [currency], [percent], or [compact]. Locale-specific symbols and
 * placement come from the OS locale rules.
 */
@Immutable
public class RollingNumberFormat private constructor(
    internal val style: Style,
    internal val currencyCode: String?,
    internal val minFraction: Int?,
    internal val maxFraction: Int?,
    internal val grouping: Boolean,
    internal val rounding: RollingRounding,
) {
    internal enum class Style { Number, Currency, Percent, Compact }

    init {
        minFraction?.let { require(it in 0..MAX_FRACTION) { "minFraction must be 0..$MAX_FRACTION, was $it" } }
        maxFraction?.let { require(it in 0..MAX_FRACTION) { "maxFraction must be 0..$MAX_FRACTION, was $it" } }
        if (minFraction != null && maxFraction != null) {
            require(minFraction <= maxFraction) { "minFraction ($minFraction) must be <= maxFraction ($maxFraction)" }
        }
        if (style == Style.Currency) {
            val code = requireNotNull(currencyCode)
            require(CURRENCY_CODE.matches(code)) { "Currency code must be 3 uppercase letters (ISO 4217), was '$code'" }
            try {
                Currency.getInstance(code)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Unknown currency code '$code'", e)
            }
        }
    }

    /** Effective (min, max) fraction digits, identical on every formatter path. */
    internal fun fractionRange(): IntRange {
        val default = when (style) {
            Style.Number -> 0..3
            Style.Currency -> Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0).let { it..it }
            Style.Percent -> 0..0
            Style.Compact -> 0..1
        }
        val min = minFraction ?: minOf(default.first, maxFraction ?: default.first)
        val max = maxFraction ?: maxOf(default.last, min)
        return min..max
    }

    /** Identifies everything except the value; a change means "replace immediately" (FR-014). */
    internal fun formatKey(locale: Locale): String {
        val range = fractionRange()
        return listOf(locale.toLanguageTag(), style, currencyCode.orEmpty(), range.first, range.last, grouping, rounding)
            .joinToString("|")
    }

    override fun equals(other: Any?): Boolean =
        other is RollingNumberFormat &&
            other.style == style &&
            other.currencyCode == currencyCode &&
            other.minFraction == minFraction &&
            other.maxFraction == maxFraction &&
            other.grouping == grouping &&
            other.rounding == rounding

    override fun hashCode(): Int =
        listOf(style, currencyCode, minFraction, maxFraction, grouping, rounding).hashCode()

    override fun toString(): String =
        "RollingNumberFormat(style=$style, currency=$currencyCode, fraction=$minFraction..$maxFraction, " +
            "grouping=$grouping, rounding=$rounding)"

    public companion object {
        private const val MAX_FRACTION = 15
        private val CURRENCY_CODE = Regex("[A-Z]{3}")

        /** Plain number. Default fraction digits 0..3. */
        public fun number(
            minFraction: Int? = null,
            maxFraction: Int? = null,
            grouping: Boolean = true,
            rounding: RollingRounding = RollingRounding.HalfUp,
        ): RollingNumberFormat = RollingNumberFormat(Style.Number, null, minFraction, maxFraction, grouping, rounding)

        /** Currency with ISO 4217 [code]. Default fraction digits are the currency's own (KRW 0, USD 2). */
        public fun currency(
            code: String,
            minFraction: Int? = null,
            maxFraction: Int? = null,
            grouping: Boolean = true,
            rounding: RollingRounding = RollingRounding.HalfUp,
        ): RollingNumberFormat = RollingNumberFormat(Style.Currency, code, minFraction, maxFraction, grouping, rounding)

        /** Percent: 0.125 is shown as 12.5% (with one fraction digit). Default fraction digits 0..0. */
        public fun percent(
            minFraction: Int? = null,
            maxFraction: Int? = null,
            rounding: RollingRounding = RollingRounding.HalfUp,
        ): RollingNumberFormat = RollingNumberFormat(Style.Percent, null, minFraction, maxFraction, true, rounding)

        /** Short compact notation using the locale's units (1.2K, 1.2천). */
        public fun compact(
            maxFraction: Int? = 1,
            rounding: RollingRounding = RollingRounding.HalfUp,
        ): RollingNumberFormat = RollingNumberFormat(Style.Compact, null, null, maxFraction, true, rounding)
    }
}
