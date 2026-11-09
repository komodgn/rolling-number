package io.github.komodgn.rollingnumber

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.Locale

/** Animation frames are layout/draw only: callers and the component itself do not recompose per frame. */
@RunWith(AndroidJUnit4::class)
class RecompositionTest {
    @get:Rule
    val rule = createComposeRule()

    private var callerCompositions = 0
    private var innerCompositions = 0

    @Composable
    private fun Caller(value: BigDecimal) {
        SideEffect { callerCompositions++ }
        CountingWrapper { RollingNumber(value, RollingNumberFormat.currency("KRW"), locale = Locale.KOREA) }
    }

    @Composable
    private fun CountingWrapper(content: @Composable () -> Unit) {
        SideEffect { innerCompositions++ }
        content()
    }

    @Test
    fun animationFramesDoNotRecompose() {
        var value by mutableStateOf(BigDecimal(15_000))
        rule.mainClock.autoAdvance = false
        rule.setContent { Caller(value) }
        rule.mainClock.advanceTimeBy(100)
        val callerBefore = callerCompositions

        value = BigDecimal(12_000)
        rule.mainClock.advanceTimeByFrame()
        var animatedFrames = 0
        repeat(40) { // ~640 ms, longer than the default ~0.5 s animation
            rule.mainClock.advanceTimeByFrame()
            animatedFrames++
        }
        assertTrue(animatedFrames > 20)
        assertEquals("caller recomposes once per value change", callerBefore + 1, callerCompositions)
        assertTrue("wrapper recomposed $innerCompositions times", innerCompositions <= callerCompositions)
    }
}
