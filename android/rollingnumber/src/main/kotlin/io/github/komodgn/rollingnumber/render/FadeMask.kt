package io.github.komodgn.rollingnumber.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Fades content out toward the top and bottom edges over [maskPx] (FR-022).
 * Must run inside an offscreen-composited layer so `DstIn` only affects this content.
 */
internal fun DrawScope.drawFadeMask(maskPx: Float) {
    if (maskPx <= 0f || size.height <= 0f) return
    val edge = (maskPx / size.height).coerceIn(0f, 0.5f)
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            edge to Color.Black,
            1f - edge to Color.Black,
            1f to Color.Transparent,
            startY = 0f,
            endY = size.height,
        ),
        topLeft = Offset.Zero,
        size = size,
        blendMode = BlendMode.DstIn,
    )
}
