package io.github.komodgn.rollingnumber.format

import android.icu.number.Notation
import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.number.Scale
import android.icu.util.Currency
import android.icu.util.MeasureUnit
import androidx.annotation.RequiresApi
import io.github.komodgn.rollingnumber.RollingNumberFormat
import io.github.komodgn.rollingnumber.logic.FormattedNumber
import java.math.BigDecimal
import java.util.Locale

/** API 30+ path: `android.icu.number.NumberFormatter`. */
@RequiresApi(30)
internal object ModernIcuFormatter : IcuFormatter {
    override fun format(value: BigDecimal, format: RollingNumberFormat, locale: Locale): FormattedNumber {
        val uLocale = latinLocale(locale)
        val range = format.fractionRange()
        var f = NumberFormatter.withLocale(uLocale)
            .precision(Precision.minMaxFraction(range.first, range.last))
            .roundingMode(format.rounding.toJava())
            .grouping(
                if (format.grouping) NumberFormatter.GroupingStrategy.ON_ALIGNED
                else NumberFormatter.GroupingStrategy.OFF,
            )
        f = when (format.style) {
            RollingNumberFormat.Style.Number -> f
            RollingNumberFormat.Style.Currency -> f.unit(Currency.getInstance(format.currencyCode))
            RollingNumberFormat.Style.Percent -> f.unit(MeasureUnit.PERCENT).scale(Scale.powerOfTen(2))
            RollingNumberFormat.Style.Compact -> f.notation(Notation.compactShort())
        }
        return buildFormatted(f.format(value).toString(), value, format, locale, uLocale)
    }
}
