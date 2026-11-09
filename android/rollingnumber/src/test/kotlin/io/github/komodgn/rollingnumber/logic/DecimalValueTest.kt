package io.github.komodgn.rollingnumber.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecimalValueTest {
    private fun d(text: String) = requireNotNull(DecimalValue.parse(text)) { "parse failed: $text" }

    @Test
    fun rejectsTextOutsideTheDecimalPattern() {
        listOf("NaN", "Infinity", "-Infinity", "", "1e5", "1,000", "+1", ".5", "5.", "1.2.3", " 1").forEach {
            assertNull("should reject '$it'", DecimalValue.parse(it))
        }
    }

    @Test
    fun negativeZeroIsNotNegative() {
        assertFalse(d("-0").negative)
        assertFalse(d("-0.00").negative)
        assertEquals("0", d("-0.00").canonical)
    }

    @Test
    fun stripsLeadingAndTrailingZeros() {
        assertEquals("12", d("0012").integerDigits)
        assertEquals("0", d("000").integerDigits)
        assertEquals("5", d("1.500").fractionDigits)
        assertEquals("", d("3.000").fractionDigits)
        assertEquals("-1.05", d("-001.0500").canonical)
    }

    @Test
    fun trailingZerosAreTheSameValue() {
        assertEquals(d("12.5"), d("12.50"))
        assertEquals(0, d("12.5").compareTo(d("12.50")))
    }

    @Test
    fun comparesSignThenLengthThenDigits() {
        assertTrue(d("-1") < d("0"))
        assertTrue(d("-100") < d("-99"))
        assertTrue(d("99") < d("100"))
        assertTrue(d("12.99") < d("13"))
        assertTrue(d("12.5") < d("12.51"))
        assertTrue(d("-12.5") > d("-12.51"))
        assertTrue(d("9900") < d("12900"))
    }

    @Test
    fun comparesValuesBeyondDoublePrecision() {
        assertTrue(d("12345678901234567890") < d("12345678901234567891"))
        assertTrue(d("0.10000000000000000001") > d("0.1"))
    }
}
