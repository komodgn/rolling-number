package io.github.komodgn.rollingnumber

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual style of a [RollingNumber] (FR-021, FR-022).
 *
 * @property textStyle font family, size, color, weight.
 * @property tabularDigits give every digit the width of the widest digit so rolling never wobbles.
 * @property fadeMaskHeight height of the top/bottom fade mask on rolling digits; 0.dp disables it.
 */
@Immutable
public data class RollingNumberStyle(
    val textStyle: TextStyle,
    val tabularDigits: Boolean = true,
    val fadeMaskHeight: Dp = 4.dp,
)
