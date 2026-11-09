package io.github.komodgn.rollingnumber.fixtures

import io.github.komodgn.rollingnumber.RollingNumberFormat
import io.github.komodgn.rollingnumber.RollingRounding
import io.github.komodgn.rollingnumber.format.IcuFormatter
import io.github.komodgn.rollingnumber.logic.Tokenizer
import java.math.BigDecimal
import java.util.Locale

/** Builds the public format type from a fixture spec. */
fun FormatSpecJson.toFormat(): RollingNumberFormat {
    val r = when (rounding) {
        "halfUp" -> RollingRounding.HalfUp
        "halfEven" -> RollingRounding.HalfEven
        "down" -> RollingRounding.Down
        "up" -> RollingRounding.Up
        else -> error("unknown rounding '$rounding'")
    }
    return when (style) {
        "number" -> RollingNumberFormat.number(minFraction, maxFraction, grouping, r)
        "currency" -> RollingNumberFormat.currency(requireNotNull(currency), minFraction, maxFraction, grouping, r)
        "percent" -> RollingNumberFormat.percent(minFraction, maxFraction, r)
        "compact" -> RollingNumberFormat.compact(maxFraction ?: 1, r)
        else -> error("unknown style '$style'")
    }
}

private val LOOSE_REMOVED = setOf(' ', ' ', ' ', '‎', '‏', '؜')

/** Removes spacing and bidi marks the OS may vary on (FR-003 allows these differences). */
fun loose(text: String): String = text.filterNot { it in LOOSE_REMOVED }

/**
 * Formats one fixture case with [formatter] and returns a list of mismatch messages (empty = pass).
 * Comparison rules: contracts/fixtures.md §1.
 */
internal fun checkFormatCase(case: FormatCase, formatter: IcuFormatter): List<String> {
    val input = case.input
    val formatted = formatter.format(BigDecimal(input.value), input.format.toFormat(), Locale.forLanguageTag(input.locale))
        .copy(prefix = input.prefix, suffix = input.suffix)
    val tokens = Tokenizer.tokenize(formatted)
    val problems = ArrayList<String>()
    val shown = "'${formatted.displayText}'"

    val normalized = Tokenizer.normalize(tokens)
    if (normalized != case.expect.normalized) {
        problems += "normalized: expected '${case.expect.normalized}', got '$normalized' from $shown"
    }
    if (tokens.size != case.expect.tokens.size) {
        problems += "token count: expected ${case.expect.tokens.size}, got ${tokens.size}: " +
            tokens.joinToString(" ") { "${it.kind.id}:${it.key}:${it.text}" } + " from $shown"
        return problems
    }
    tokens.zip(case.expect.tokens).forEachIndexed { i, (actual, expected) ->
        if (actual.kind.id != expected.kind || actual.key != expected.key) {
            problems += "token $i: expected ${expected.kind}:${expected.key}, got ${actual.kind.id}:${actual.key} from $shown"
            return@forEachIndexed
        }
        val mode = expected.textMatch ?: if (actual.kind.isDigit || expected.kind in setOf("sign", "prefix", "suffix")) "exact" else "loose"
        val ok = when (mode) {
            "exact" -> actual.text == expected.text
            "loose" -> loose(actual.text) == loose(expected.text)
            "any" -> true
            else -> false.also { problems += "token $i: unknown textMatch '$mode'" }
        }
        if (!ok) problems += "token $i (${expected.key}): expected text '${expected.text}', got '${actual.text}' from $shown"
    }
    return problems
}
