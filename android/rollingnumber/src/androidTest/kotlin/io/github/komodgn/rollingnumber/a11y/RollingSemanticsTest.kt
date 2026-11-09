package io.github.komodgn.rollingnumber.a11y

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.komodgn.rollingnumber.RollingNumber
import io.github.komodgn.rollingnumber.RollingNumberFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.Locale

/** FR-018: one node, label + final value only, even while animating. */
@RunWith(AndroidJUnit4::class)
class RollingSemanticsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun exposesOneNodeWithLabelAndTargetValueDuringAnimation() {
        var value by mutableStateOf(BigDecimal(15_000))
        rule.mainClock.autoAdvance = false
        rule.setContent {
            RollingNumber(
                value = value,
                format = RollingNumberFormat.currency("KRW"),
                locale = Locale.KOREA,
                accessibilityLabel = "최종가",
            )
        }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithContentDescription("최종가 ₩15,000").assertExists()

        value = BigDecimal(12_000)
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(100) // mid-animation
        rule.onNodeWithContentDescription("최종가 ₩12,000")
            .assertExists()
            .assert(SemanticsMatcher("has no children") { it.children.isEmpty() })
        assertEquals(0, rule.onAllNodesWithContentDescription("최종가 ₩15,000").fetchSemanticsNodes().size)
        rule.onNodeWithContentDescription("최종가 ₩12,000")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }
}
