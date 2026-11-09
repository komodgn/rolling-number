package io.github.komodgn.rollingnumber

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Locale

class RollingNumberFormatTest {
    @Test
    fun fractionDigitsMustBeWithin0To15() {
        assertThrows(IllegalArgumentException::class.java) { RollingNumberFormat.number(minFraction = -1) }
        assertThrows(IllegalArgumentException::class.java) { RollingNumberFormat.number(maxFraction = 16) }
        RollingNumberFormat.number(minFraction = 0, maxFraction = 15)
    }

    @Test
    fun minMustNotExceedMax() {
        assertThrows(IllegalArgumentException::class.java) { RollingNumberFormat.number(minFraction = 3, maxFraction = 2) }
    }

    @Test
    fun currencyCodeMustBeKnownIso4217() {
        assertThrows(IllegalArgumentException::class.java) { RollingNumberFormat.currency("krw") }
        assertThrows(IllegalArgumentException::class.java) { RollingNumberFormat.currency("KR") }
        assertThrows(IllegalArgumentException::class.java) { RollingNumberFormat.currency("ZZZ") }
        RollingNumberFormat.currency("KRW")
    }

    @Test
    fun defaultFractionDigitsFollowStyleAndCurrency() {
        assertEquals(0..0, RollingNumberFormat.currency("KRW").fractionRange())
        assertEquals(2..2, RollingNumberFormat.currency("USD").fractionRange())
        assertEquals(0..3, RollingNumberFormat.number().fractionRange())
        assertEquals(0..0, RollingNumberFormat.percent().fractionRange())
        assertEquals(1..1, RollingNumberFormat.percent(minFraction = 1).fractionRange())
        assertEquals(0..1, RollingNumberFormat.compact().fractionRange())
        assertEquals(2..3, RollingNumberFormat.number(minFraction = 2).fractionRange())
    }

    @Test
    fun formatKeyChangesWithLocaleAndOptionsOnly() {
        val f = RollingNumberFormat.currency("KRW")
        assertEquals(f.formatKey(Locale.KOREA), RollingNumberFormat.currency("KRW").formatKey(Locale.KOREA))
        assertNotEquals(f.formatKey(Locale.KOREA), f.formatKey(Locale.US))
        assertNotEquals(f.formatKey(Locale.KOREA), RollingNumberFormat.currency("USD").formatKey(Locale.KOREA))
        assertEquals(f, RollingNumberFormat.currency("KRW"))
    }
}
