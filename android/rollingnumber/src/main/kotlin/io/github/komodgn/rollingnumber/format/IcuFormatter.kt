package io.github.komodgn.rollingnumber.format

import android.icu.text.DecimalFormatSymbols
import android.icu.util.ULocale
import android.os.Build
import io.github.komodgn.rollingnumber.RollingNumberFormat
import io.github.komodgn.rollingnumber.RollingRounding
import io.github.komodgn.rollingnumber.logic.DecimalValue
import io.github.komodgn.rollingnumber.logic.FormattedNumber
import io.github.komodgn.rollingnumber.logic.NumberSymbols
import java.math.BigDecimal
import java.util.Locale

/** Formats values with the OS ICU rules (research R2). */
internal interface IcuFormatter {
    fun format(value: BigDecimal, format: RollingNumberFormat, locale: Locale): FormattedNumber

    companion object {
        /** API 30+ uses `android.icu.number.NumberFormatter`; older devices use `android.icu.text`. */
        fun forDevice(): IcuFormatter =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ModernIcuFormatter else LegacyIcuFormatter
    }
}

/** Locale with Latin digits forced (MVP supports only 0-9). */
internal fun latinLocale(locale: Locale): ULocale =
    ULocale.forLocale(Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("nu", "latn").build())

internal fun symbolsFor(locale: ULocale, format: RollingNumberFormat): NumberSymbols {
    val dfs = DecimalFormatSymbols.getInstance(locale)
    val monetary = format.style == RollingNumberFormat.Style.Currency
    val decimal = if (monetary) dfs.monetaryDecimalSeparator else dfs.decimalSeparator
    val group = if (monetary) dfs.monetaryGroupingSeparator else dfs.groupingSeparator
    return NumberSymbols(
        decimalSeparator = decimal.toString(),
        groupingSeparator = group.toString(),
        minusSigns = setOf("-", "−", dfs.minusSign.toString()),
    )
}

internal fun RollingRounding.toJava(): java.math.RoundingMode = when (this) {
    RollingRounding.HalfUp -> java.math.RoundingMode.HALF_UP
    RollingRounding.HalfEven -> java.math.RoundingMode.HALF_EVEN
    RollingRounding.Down -> java.math.RoundingMode.DOWN
    RollingRounding.Up -> java.math.RoundingMode.UP
}

internal fun buildFormatted(
    text: String,
    value: BigDecimal,
    format: RollingNumberFormat,
    locale: Locale,
    uLocale: ULocale,
): FormattedNumber = FormattedNumber(
    text = text,
    value = DecimalValue.of(value),
    symbols = symbolsFor(uLocale, format),
    formatKey = format.formatKey(locale),
)
