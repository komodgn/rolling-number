package io.github.komodgn.rollingnumber

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.abs

/** US4: when a grouped number widens, its right neighbor slides smoothly, never jumps. */
@RunWith(AndroidJUnit4::class)
class RollingNumberGroupTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rightNeighborMovesContinuouslyWhenMiddleNumberWidens() {
        var discount by mutableStateOf(BigDecimal(-900))
        val style = RollingNumberDefaults.style(TextStyle(fontSize = 24.sp))
        val krw = RollingNumberFormat.currency("KRW")
        rule.mainClock.autoAdvance = false
        rule.setContent {
            RollingNumberGroup {
                Row {
                    RollingNumber(BigDecimal(13_900), krw, locale = Locale.KOREA, style = style)
                    RollingNumber(discount, krw, locale = Locale.KOREA, style = style, modifier = Modifier.testTag("middle"))
                    RollingNumber(BigDecimal(12_900), krw, locale = Locale.KOREA, style = style, modifier = Modifier.testTag("right"))
                }
            }
        }
        rule.mainClock.advanceTimeBy(100)
        fun rightX(): Dp = rule.onNodeWithTag("right").getUnclippedBoundsInRoot().left
        fun middleWidth(): Dp = rule.onNodeWithTag("middle").getUnclippedBoundsInRoot().let { it.right - it.left }

        val startX = rightX()
        val startWidth = middleWidth()
        discount = BigDecimal(-1_900)
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1_500)
        val endX = rightX()
        val delta = endX - startX
        assertTrue("middle must widen", middleWidth() > startWidth)
        assertEquals("neighbor ends right after the widened number", (middleWidth() - startWidth).value, delta.value, 1.5f)

        // Replay and sample every frame: the neighbor never jumps by more than 35% of the total move.
        discount = BigDecimal(-900)
        rule.mainClock.advanceTimeBy(1_500)
        var last = rightX()
        discount = BigDecimal(-1_900)
        repeat(60) {
            rule.mainClock.advanceTimeByFrame()
            val x = rightX()
            assertTrue("frame $it jumped ${last.value} -> ${x.value}", abs((x - last).value) <= 0.35f * abs(delta.value) + 0.5f)
            last = x
        }
        assertEquals(endX.value, last.value, 0.5f)
    }
}
