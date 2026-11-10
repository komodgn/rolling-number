package io.github.komodgn.rollingnumber.a11y

import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription

/**
 * Exposes exactly one accessibility node whose description is the label plus the full target
 * value (FR-018). Digits, separators and intermediate values are never exposed.
 */
internal fun Modifier.rollingSemantics(description: String): Modifier =
    clearAndSetSemantics { contentDescription = description }

/**
 * Speaks [text] through the screen reader (FR-019). `View.announceForAccessibility` is deprecated
 * in API 36 but still delivers TYPE_ANNOUNCEMENT on every supported level; see research R7.
 */
@Suppress("DEPRECATION")
internal fun View.announce(text: String) {
    val manager = context.getSystemService(AccessibilityManager::class.java)
    if (manager?.isEnabled != true) return
    announceForAccessibility(text)
}
