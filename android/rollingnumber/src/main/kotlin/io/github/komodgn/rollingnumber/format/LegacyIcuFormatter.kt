package io.github.komodgn.rollingnumber.format

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import android.icu.util.Currency
import io.github.komodgn.rollingnumber.RollingNumberFormat
import io.github.komodgn.rollingnumber.RollingRounding
import io.github.komodgn.rollingnumber.logic.FormattedNumber
import java.math.BigDecimal
import java.util.Locale

/** API 24-29 path: `android.icu.text.NumberFormat` and `CompactDecimalFormat`. */
internal object LegacyIcuFormatter : IcuFormatter {
    override fun format(value: BigDecimal, format: RollingNumberFormat, locale: Locale): FormattedNumber {
        val uLocale = latinLocale(locale)
        val range = format.fractionRange()
        val nf: NumberFormat = when (format.style) {
            RollingNumberFormat.Style.Number -> NumberFormat.getInstance(uLocale)
            RollingNumberFormat.Style.Currency -> NumberFormat.getCurrencyInstance(uLocale).apply {
                currency = Currency.getInstance(format.currencyCode)
            }
            RollingNumberFormat.Style.Percent -> NumberFormat.getPercentInstance(uLocale)
            RollingNumberFormat.Style.Compact ->
                CompactDecimalFormat.getInstance(uLocale, CompactDecimalFormat.CompactStyle.SHORT).apply {
                    // Use fraction digits instead of the default significant-digit rounding.
                    setSignificantDigitsUsed(false)
                }
        }
        nf.minimumFractionDigits = range.first
        nf.maximumFractionDigits = range.last
        nf.isGroupingUsed = format.grouping
        nf.roundingMode = format.rounding.toIcuMode()
        return buildFormatted(nf.format(value), value, format, locale, uLocale)
    }

    /** ICU legacy rounding constants (same values as `java.math.BigDecimal.ROUND_*`). */
    private fun RollingRounding.toIcuMode(): Int = when (this) {
        RollingRounding.Up -> 0
        RollingRounding.Down -> 1
        RollingRounding.HalfUp -> 4
        RollingRounding.HalfEven -> 6
    }
}
