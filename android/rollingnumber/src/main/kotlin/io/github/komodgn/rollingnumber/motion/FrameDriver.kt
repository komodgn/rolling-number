package io.github.komodgn.rollingnumber.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import io.github.komodgn.rollingnumber.logic.RollingSession
import io.github.komodgn.rollingnumber.render.FrameTicker
import kotlinx.coroutines.flow.first

/** Wakes the frame loop when a new animation may have started. */
@Stable
internal class FrameRequests {
    private val counter = mutableIntStateOf(0)
    fun request() { counter.intValue++ }
    internal fun read(): Int = counter.intValue
}

/**
 * Steps [session] on every frame while it animates and ticks [ticker] so layout/draw re-read
 * the frame. Sleeps (no frame callbacks) while idle.
 */
@Composable
internal fun FrameDriver(session: RollingSession, ticker: FrameTicker, requests: FrameRequests) {
    LaunchedEffect(session, ticker, requests) {
        while (true) {
            snapshotFlow { requests.read() }.first { session.isAnimating }
            while (session.isAnimating) {
                withFrameNanos { now -> session.step(now) }
                ticker.tick()
            }
        }
    }
}
