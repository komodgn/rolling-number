package io.github.komodgn.rollingnumber.logic

/** Roll direction rule (FR-005). */
internal enum class Trend(val id: String) {
    Auto("auto"),
    AlwaysUp("alwaysUp"),
    AlwaysDown("alwaysDown"),
    PerDigit("perDigit"),
    ;

    companion object {
        fun fromId(id: String): Trend = entries.first { it.id == id }
    }
}

internal enum class Direction(val id: String) { Up("up"), Down("down"), None("none") }

internal enum class DiffAction(val id: String) {
    Stay("stay"),
    RollUp("roll-up"),
    RollDown("roll-down"),
    Crossfade("crossfade"),
    EnterUp("enter-up"),
    EnterDown("enter-down"),
    EnterFade("enter-fade"),
    ExitUp("exit-up"),
    ExitDown("exit-down"),
    ExitFade("exit-fade"),
    ;

    val isExit: Boolean get() = this == ExitUp || this == ExitDown || this == ExitFade
}

internal data class DiffElement(
    val key: String,
    val kind: TokenKind,
    val from: String?,
    val to: String?,
    val action: DiffAction,
)

internal data class DiffResult(val direction: Direction, val elements: List<DiffElement>) {
    val isEmpty: Boolean get() = elements.isEmpty()
}

/** Matches previous and next tokens by key and decides each element's action (data-model §6). */
internal object TransitionDiff {

    fun compute(
        from: FormattedNumber?,
        to: FormattedNumber,
        trend: Trend,
        animateFirstDisplay: Boolean = false,
    ): DiffResult {
        val newTokens = Tokenizer.tokenize(to)

        if (from == null) {
            if (!animateFirstDisplay) return DiffResult(Direction.None, emptyList())
            val d = if (trend == Trend.AlwaysDown) Direction.Down else Direction.Up
            return DiffResult(
                d,
                newTokens.map { DiffElement(it.key, it.kind, null, it.text, enterAction(it.kind, d)) },
            )
        }

        val direction = when (to.value.compareTo(from.value)) {
            1 -> Direction.Up
            -1 -> Direction.Down
            else -> Direction.None
        }
        if (from.displayText == to.displayText) return DiffResult(direction, emptyList())

        val oldTokens = Tokenizer.tokenize(from)
        val oldByKey = oldTokens.associateBy { it.key }
        val newKeys = newTokens.mapTo(HashSet()) { it.key }

        val result = ArrayList<DiffElement>(newTokens.size + oldTokens.size)
        for (token in newTokens) {
            val old = oldByKey[token.key]
            result += when {
                old == null -> DiffElement(token.key, token.kind, null, token.text, enterAction(token.kind, elementDirection(trend, direction)))
                old.text == token.text -> DiffElement(token.key, token.kind, old.text, token.text, DiffAction.Stay)
                token.kind.isDigit && old.kind.isDigit -> {
                    val d = matchedDigitDirection(trend, direction, old.text, token.text)
                    DiffElement(token.key, token.kind, old.text, token.text, rollAction(d))
                }
                else -> DiffElement(token.key, token.kind, old.text, token.text, DiffAction.Crossfade)
            }
        }

        // Exiting elements go right after their predecessor in the previous list (or first).
        var previousKey: String? = null
        for (old in oldTokens) {
            if (old.key !in newKeys) {
                val element = DiffElement(old.key, old.kind, old.text, null, exitAction(old.kind, elementDirection(trend, direction)))
                val anchor = previousKey?.let { key -> result.indexOfFirst { it.key == key } } ?: -1
                result.add(anchor + 1, element)
            }
            previousKey = old.key
        }
        return DiffResult(direction, result)
    }

    /** Direction for entering/exiting elements: the overall direction, or the forced one. */
    private fun elementDirection(trend: Trend, direction: Direction): Direction = when {
        direction == Direction.None -> Direction.None
        trend == Trend.AlwaysUp -> Direction.Up
        trend == Trend.AlwaysDown -> Direction.Down
        else -> direction
    }

    private fun matchedDigitDirection(trend: Trend, direction: Direction, old: String, new: String): Direction = when {
        direction == Direction.None -> Direction.None
        trend == Trend.PerDigit -> if (new > old) Direction.Up else Direction.Down
        else -> elementDirection(trend, direction)
    }

    private fun rollAction(d: Direction) = when (d) {
        Direction.Up -> DiffAction.RollUp
        Direction.Down -> DiffAction.RollDown
        Direction.None -> DiffAction.Crossfade
    }

    private fun enterAction(kind: TokenKind, d: Direction) = when {
        !kind.isDigit || d == Direction.None -> DiffAction.EnterFade
        d == Direction.Up -> DiffAction.EnterUp
        else -> DiffAction.EnterDown
    }

    private fun exitAction(kind: TokenKind, d: Direction) = when {
        !kind.isDigit || d == Direction.None -> DiffAction.ExitFade
        d == Direction.Up -> DiffAction.ExitUp
        else -> DiffAction.ExitDown
    }
}
