package io.github.komodgn.rollingnumber.logic

import io.github.komodgn.rollingnumber.fixtures.DiffCase
import io.github.komodgn.rollingnumber.fixtures.Fixtures
import io.github.komodgn.rollingnumber.fixtures.NumberJson
import io.github.komodgn.rollingnumber.fixtures.SymbolsJson
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

internal fun SymbolsJson.toSymbols() = NumberSymbols(decimal, group, minus.toSet())

internal fun NumberJson.toNumber(symbols: SymbolsJson): FormattedNumber? {
    val v = DecimalValue.parse(value) ?: return null
    return FormattedNumber(text, v, symbols.toSymbols(), prefix, suffix, formatKey)
}

/** Runs every `fixtures/diff/` case and compares direction and the full ordered element list. */
class DiffFixtureTest {
    private fun check(case: DiffCase): List<String> {
        val from = case.from?.let { requireNotNull(it.toNumber(case.symbols)) { "bad from value" } }
        val to = requireNotNull(case.to.toNumber(case.symbols)) { "bad to value" }
        val result = TransitionDiff.compute(from, to, Trend.fromId(case.trend), case.animateFirstDisplay)
        val problems = ArrayList<String>()
        if (result.direction.id != case.expect.direction) {
            problems += "direction: expected ${case.expect.direction}, got ${result.direction.id}"
        }
        val actual = result.elements.map { "${it.key} ${it.kind.id} ${it.from} -> ${it.to} ${it.action.id}" }
        val expected = case.expect.elements.map { "${it.key} ${it.kind} ${it.from} -> ${it.to} ${it.action}" }
        if (actual != expected) {
            problems += "elements:\n    expected: ${expected.joinToString("\n              ")}\n    actual:   ${actual.joinToString("\n              ")}"
        }
        return problems
    }

    @Test
    fun allDiffFixturesPass() {
        val cases = Fixtures.diff()
        assertTrue("no diff fixtures found", cases.isNotEmpty())
        val failures = cases.mapNotNull { (file, case) ->
            val problems = runCatching { check(case) }.getOrElse { listOf("threw ${it::class.simpleName}: ${it.message}") }
            if (problems.isEmpty()) null else "$file#${case.id}:\n  " + problems.joinToString("\n  ")
        }
        if (failures.isNotEmpty()) fail("${failures.size} diff fixture(s) failed:\n" + failures.joinToString("\n"))
    }
}
