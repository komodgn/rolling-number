package io.github.komodgn.rollingnumber.render

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

/** Measures and caches single-line text layouts for one text style. */
internal class GlyphCache(private val measurer: TextMeasurer, private val style: TextStyle) {
    private val layouts = HashMap<String, TextLayoutResult>()

    fun layout(text: String): TextLayoutResult =
        layouts.getOrPut(text) { measurer.measure(text, style, softWrap = false, maxLines = 1) }

    fun width(text: String): Float = layout(text).size.width.toFloat()

    /** Width of the widest digit; every digit slot uses it when tabular digits are on (FR-021). */
    val tabularDigitWidth: Float by lazy { (0..9).maxOf { width(it.toString()) } }

    val lineHeight: Int by lazy { layout("0").size.height }

    val firstBaseline: Float by lazy { layout("0").firstBaseline }
}
