package io.github.komodgn.rollingnumber.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.komodgn.rollingnumber.RollingNumberStyle
import io.github.komodgn.rollingnumber.logic.RollingSession
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Sizes itself from the session's current frame and draws every layer. Animation state is read
 * only in the layout and draw phases (via [FrameTicker]), so frames never recompose callers.
 */
@Composable
internal fun RollingNumberLayout(
    session: RollingSession,
    ticker: FrameTicker,
    style: RollingNumberStyle,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val glyphs = remember(measurer, style.textStyle, density) { GlyphCache(measurer, style.textStyle) }
    val maskPx = with(density) { style.fadeMaskHeight.toPx() }
    val digitWidth: () -> Float? = { if (style.tabularDigits) glyphs.tabularDigitWidth else null }

    Layout(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer {
                compositingStrategy = if (maskPx > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            }
            .drawWithContent {
                ticker.read()
                val frame = session.frame(glyphs::width, digitWidth())
                val lineHeight = glyphs.lineHeight.toFloat()
                for (item in frame.items) {
                    for (layer in item.layers) {
                        if (layer.alpha <= 0f) continue
                        val text = glyphs.layout(layer.text)
                        val x = item.x + (item.width - glyphs.width(layer.text)) / 2f
                        drawText(text, topLeft = Offset(x, layer.offsetY * lineHeight), alpha = layer.alpha)
                    }
                }
                drawFadeMask(maskPx)
            },
    ) { _, constraints ->
        ticker.read()
        val frame = session.frame(glyphs::width, digitWidth())
        val width = ceil(frame.totalWidth).toInt().coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = glyphs.lineHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val baseline = glyphs.firstBaseline.roundToInt()
        layout(width, height, mapOf(FirstBaseline to baseline, LastBaseline to baseline)) {}
    }
}
