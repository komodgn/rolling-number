package io.github.komodgn.rollingnumber

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.Locale

/** US1 on a real device: a value change animates, settles on the new value, and fires events once. */
@RunWith(AndroidJUnit4::class)
class RollingNumberAnimationTest {
    @get:Rule
    val rule = createComposeRule()

    private fun ImageBitmap.signature(): List<Int> {
        val map = toPixelMap()
        return List(map.width * map.height) { i -> map[i % map.width, i / map.width].hashCode() }
    }

    @Test
    fun valueChangeAnimatesThenSettlesOnTheNewValue() {
        var value by mutableStateOf(BigDecimal(9_900))
        var starts = 0
        var ends = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(Modifier.background(Color.White).testTag("box")) {
                RollingNumber(
                    value = value,
                    format = RollingNumberFormat.currency("KRW"),
                    locale = Locale.KOREA,
                    style = RollingNumberDefaults.style(TextStyle(fontSize = 32.sp, color = Color.Black)),
                    onAnimationStart = { starts++ },
                    onAnimationEnd = { ends++ },
                )
            }
        }
        rule.mainClock.advanceTimeBy(100)
        val before = rule.onNodeWithTag("box").captureToImage()
        assertEquals("first display must not animate", 0, starts)

        value = BigDecimal(19_800)
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(120)
        val middle = rule.onNodeWithTag("box").captureToImage()
        assertEquals(1, starts)
        assertEquals(0, ends)

        rule.mainClock.advanceTimeBy(1_500)
        val after = rule.onNodeWithTag("box").captureToImage()
        assertEquals(1, starts)
        assertEquals(1, ends)

        assertTrue("width must grow for the extra digit", after.width > before.width)
        assertNotEquals("mid-animation frame must differ from the settled frame", after.signature(), middle.signature())
        assertTrue("mid-animation width must be between start and end", middle.width in before.width..after.width)
    }
}
