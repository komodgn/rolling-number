package io.github.komodgn.rollingnumber.render

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf

/**
 * Snapshot state read only by layout and draw. Ticking it re-runs those phases without
 * recomposing anything.
 */
@Stable
internal class FrameTicker {
    private val frame = mutableLongStateOf(0L)

    fun tick() {
        frame.longValue++
    }

    fun read(): Long = frame.longValue
}
