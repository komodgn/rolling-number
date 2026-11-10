package io.github.komodgn.rollingnumber.logic

import java.math.BigDecimal

/**
 * Exact decimal value used only for comparison (data-model §1).
 * Canonical form: no leading integer zeros (except "0"), no trailing fraction zeros, never "-0".
 */
internal class DecimalValue private constructor(
    val negative: Boolean,
    val integerDigits: String,
    val fractionDigits: String,
) : Comparable<DecimalValue> {

    val canonical: String =
        buildString {
            if (negative) append('-')
            append(integerDigits)
            if (fractionDigits.isNotEmpty()) append('.').append(fractionDigits)
        }

    override fun compareTo(other: DecimalValue): Int {
        if (negative != other.negative) return if (negative) -1 else 1
        val magnitude = compareMagnitude(this, other)
        return if (negative) -magnitude else magnitude
    }

    override fun equals(other: Any?): Boolean = other is DecimalValue && other.canonical == canonical

    override fun hashCode(): Int = canonical.hashCode()

    override fun toString(): String = canonical

    companion object {
        private val PATTERN = Regex("-?[0-9]+(\\.[0-9]+)?")

        fun parse(text: String): DecimalValue? {
            if (!PATTERN.matches(text)) return null
            val unsigned = text.removePrefix("-")
            val dot = unsigned.indexOf('.')
            val rawInt = if (dot < 0) unsigned else unsigned.substring(0, dot)
            val rawFrac = if (dot < 0) "" else unsigned.substring(dot + 1)
            val int = rawInt.trimStart('0').ifEmpty { "0" }
            val frac = rawFrac.trimEnd('0')
            val isZero = int == "0" && frac.isEmpty()
            return DecimalValue(text.startsWith("-") && !isZero, int, frac)
        }

        fun of(value: BigDecimal): DecimalValue = requireNotNull(parse(value.toPlainString()))

        private fun compareMagnitude(a: DecimalValue, b: DecimalValue): Int {
            if (a.integerDigits.length != b.integerDigits.length) {
                return a.integerDigits.length.compareTo(b.integerDigits.length)
            }
            val intCompare = a.integerDigits.compareTo(b.integerDigits)
            if (intCompare != 0) return intCompare.sign
            val width = maxOf(a.fractionDigits.length, b.fractionDigits.length)
            return a.fractionDigits.padEnd(width, '0').compareTo(b.fractionDigits.padEnd(width, '0')).sign
        }

        private val Int.sign: Int get() = if (this < 0) -1 else if (this > 0) 1 else 0
    }
}
