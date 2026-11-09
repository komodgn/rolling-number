package io.github.komodgn.rollingnumber.logic

/**
 * Splits a [FormattedNumber] into display pieces with decimal-point-relative keys (data-model §5).
 *
 * - Number area: first digit through last digit.
 * - Inside: decimal `D`; integer digits `I0`, `I1`… leftward from the decimal point (or the area end);
 *   fraction digits `F0`, `F1`… rightward; group separators `G{k}` with k = integer digits to the right.
 * - Outside: minus signs `S`; other runs are literals, `Lp0`… outward before the number, `Ls0`… after it.
 *   Signs are not counted and split literal runs.
 * - Developer prefix `AP`, suffix `AS` (no token when empty).
 */
internal object Tokenizer {

    fun tokenize(number: FormattedNumber): List<Token> {
        val text = number.text
        val first = text.indexOfFirst { it in '0'..'9' }
        val last = text.indexOfLast { it in '0'..'9' }
        require(first >= 0) { "Formatted text has no digit: '$text'" }

        val tokens = ArrayList<Token>()
        if (number.prefix.isNotEmpty()) tokens += Token(TokenKind.Prefix, number.prefix, "AP")
        tokens += outside(text.substring(0, first), number.symbols, leading = true)
        tokens += core(text.substring(first, last + 1), number.symbols)
        tokens += outside(text.substring(last + 1), number.symbols, leading = false)
        if (number.suffix.isNotEmpty()) tokens += Token(TokenKind.Suffix, number.suffix, "AS")
        return tokens
    }

    /** Numeric parts only, with canonical symbols: digits, "." decimal, "," group, "-" sign. */
    fun normalize(tokens: List<Token>): String = buildString {
        for (token in tokens) {
            when (token.kind) {
                TokenKind.IntDigit, TokenKind.FracDigit -> append(token.text)
                TokenKind.Decimal -> append('.')
                TokenKind.Group -> append(',')
                TokenKind.Sign -> append('-')
                else -> Unit
            }
        }
    }

    private class Piece(val kind: TokenKind, val text: String)

    private fun core(area: String, symbols: NumberSymbols): List<Token> {
        // 1. Split into pieces in text order.
        val pieces = ArrayList<Piece>()
        var i = 0
        var decimalSeen = false
        while (i < area.length) {
            val c = area[i]
            when {
                c in '0'..'9' -> {
                    pieces += Piece(TokenKind.IntDigit, c.toString()); i++
                }
                !decimalSeen && area.startsWith(symbols.decimalSeparator, i) -> {
                    pieces += Piece(TokenKind.Decimal, symbols.decimalSeparator)
                    decimalSeen = true
                    i += symbols.decimalSeparator.length
                }
                area.startsWith(symbols.groupingSeparator, i) -> {
                    pieces += Piece(TokenKind.Group, symbols.groupingSeparator)
                    i += symbols.groupingSeparator.length
                }
                else -> {
                    // Unexpected character inside the number area: keep it as a literal piece.
                    pieces += Piece(TokenKind.Literal, c.toString()); i++
                }
            }
        }

        // 2. Assign keys relative to the decimal point.
        val decimalIndex = pieces.indexOfFirst { it.kind == TokenKind.Decimal }
        val integerEnd = if (decimalIndex >= 0) decimalIndex else pieces.size
        val keys = arrayOfNulls<Token>(pieces.size)

        var digitsToRight = 0
        for (p in integerEnd - 1 downTo 0) {
            val piece = pieces[p]
            keys[p] = when (piece.kind) {
                TokenKind.IntDigit -> Token(TokenKind.IntDigit, piece.text, "I$digitsToRight").also { digitsToRight++ }
                TokenKind.Group -> Token(TokenKind.Group, piece.text, "G$digitsToRight")
                else -> Token(TokenKind.Literal, piece.text, "Li$digitsToRight")
            }
        }
        if (decimalIndex >= 0) {
            keys[decimalIndex] = Token(TokenKind.Decimal, pieces[decimalIndex].text, "D")
            var fraction = 0
            for (p in decimalIndex + 1 until pieces.size) {
                val piece = pieces[p]
                keys[p] = when (piece.kind) {
                    TokenKind.IntDigit -> Token(TokenKind.FracDigit, piece.text, "F$fraction").also { fraction++ }
                    TokenKind.Group -> Token(TokenKind.Group, piece.text, "H$fraction")
                    else -> Token(TokenKind.Literal, piece.text, "Lf$fraction")
                }
            }
        }
        return keys.map { requireNotNull(it) }
    }

    private fun outside(region: String, symbols: NumberSymbols, leading: Boolean): List<Token> {
        if (region.isEmpty()) return emptyList()
        // Split into sign tokens and literal runs, in text order.
        val parts = ArrayList<Pair<Boolean, String>>() // (isSign, text)
        val run = StringBuilder()
        var i = 0
        while (i < region.length) {
            val sign = symbols.minusSigns.filter { it.isNotEmpty() && region.startsWith(it, i) }.maxByOrNull { it.length }
            if (sign != null) {
                if (run.isNotEmpty()) { parts += false to run.toString(); run.clear() }
                parts += true to sign
                i += sign.length
            } else {
                run.append(region[i]); i++
            }
        }
        if (run.isNotEmpty()) parts += false to run.toString()

        // Literal runs are numbered outward from the number area; signs are not counted.
        val literalCount = parts.count { !it.first }
        var literalSeen = 0
        var signSeen = 0
        return parts.map { (isSign, text) ->
            if (isSign) {
                val key = if (signSeen == 0) "S" else "S$signSeen"
                signSeen++
                Token(TokenKind.Sign, text, key)
            } else {
                val index = if (leading) literalCount - 1 - literalSeen else literalSeen
                literalSeen++
                Token(TokenKind.Literal, text, if (leading) "Lp$index" else "Ls$index")
            }
        }
    }
}
