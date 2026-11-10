package io.github.komodgn.rollingnumber.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TokenizerTest {
    private val en = NumberSymbols(decimalSeparator = ".", groupingSeparator = ",")
    private val de = NumberSymbols(decimalSeparator = ",", groupingSeparator = ".")

    private fun number(text: String, value: String, symbols: NumberSymbols = en, prefix: String = "", suffix: String = "") =
        FormattedNumber(text, DecimalValue.parse(value)!!, symbols, prefix, suffix)

    /** Compact notation: "kind:key:text" joined by spaces. */
    private fun render(tokens: List<Token>) = tokens.joinToString(" ") { "${it.kind.id}:${it.key}:${it.text}" }

    @Test
    fun krwWithGrouping() {
        assertEquals(
            "literal:Lp0:₩ int:I4:1 int:I3:2 group:G3:, int:I2:9 int:I1:0 int:I0:0",
            render(Tokenizer.tokenize(number("₩12,900", "12900"))),
        )
    }

    @Test
    fun signOutsideCurrencySymbolIsNotCountedAsLiteral() {
        assertEquals(
            "sign:S:- literal:Lp0:₩ int:I2:3 int:I1:0 int:I0:0",
            render(Tokenizer.tokenize(number("-₩300", "-300"))),
        )
    }

    @Test
    fun germanEuroWithTrailingSymbol() {
        assertEquals(
            "int:I3:1 group:G3:. int:I2:2 int:I1:3 int:I0:4 decimal:D:, frac:F0:5 frac:F1:6 literal:Ls0:\u00a0€",
            render(Tokenizer.tokenize(number("1.234,56\u00a0€", "1234.56", de))),
        )
    }

    @Test
    fun compactUnitIsTrailingLiteral() {
        assertEquals(
            "int:I0:1 decimal:D:. frac:F0:2 literal:Ls0:K",
            render(Tokenizer.tokenize(number("1.2K", "1200"))),
        )
    }

    @Test
    fun developerAffixesComeFirstAndLast() {
        assertEquals(
            "prefix:AP:~ literal:Lp0:₩ int:I4:1 int:I3:2 group:G3:, int:I2:9 int:I1:0 int:I0:0 suffix:AS:/mo",
            render(Tokenizer.tokenize(number("₩12,900", "12900", prefix = "~", suffix = "/mo"))),
        )
    }

    @Test
    fun unicodeMinusIsASign() {
        assertEquals(
            "sign:S:\u2212 int:I1:1 int:I0:2",
            render(Tokenizer.tokenize(number("\u221212", "-12"))),
        )
    }

    @Test
    fun literalSplitBySignBecomesTwoRuns() {
        // The sign splits literal runs; runs are numbered outward from the number area.
        assertEquals(
            "literal:Lp0:₩ sign:S:- int:I2:3 int:I1:0 int:I0:0",
            render(Tokenizer.tokenize(number("₩-300", "-300"))),
        )
        assertEquals(
            "literal:Lp1:( sign:S:- literal:Lp0:$ int:I0:5 literal:Ls0:)",
            render(Tokenizer.tokenize(number("(-$5)", "-5"))),
        )
    }

    @Test
    fun emptyAffixesProduceNoTokens() {
        val tokens = Tokenizer.tokenize(number("5", "5", prefix = "", suffix = ""))
        assertEquals("int:I0:5", render(tokens))
    }

    @Test
    fun manyGroups() {
        assertEquals(
            "int:I6:1 group:G6:, int:I5:2 int:I4:3 int:I3:4 group:G3:, int:I2:5 int:I1:6 int:I0:7 decimal:D:. frac:F0:8 frac:F1:9 frac:F2:1",
            render(Tokenizer.tokenize(number("1,234,567.891", "1234567.891"))),
        )
    }

    @Test
    fun textWithoutDigitsIsAnError() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize(FormattedNumber("₩", DecimalValue.parse("0")!!, en))
        }
    }

    @Test
    fun normalizeKeepsOnlyNumericPartsWithCanonicalSymbols() {
        assertEquals("1,234.56", Tokenizer.normalize(Tokenizer.tokenize(number("1.234,56\u00a0€", "1234.56", de))))
        assertEquals("-300", Tokenizer.normalize(Tokenizer.tokenize(number("-₩300", "-300"))))
        assertEquals("1.2", Tokenizer.normalize(Tokenizer.tokenize(number("1.2K", "1200"))))
    }
}
