package io.github.komodgn.rollingnumber

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import io.github.komodgn.rollingnumber.a11y.announce
import io.github.komodgn.rollingnumber.logic.AnnouncementCoordinator

@Stable
internal class RollingNumberGroupState(val coordinator: AnnouncementCoordinator)

internal val LocalRollingNumberGroup = staticCompositionLocalOf<RollingNumberGroupState?> { null }

/**
 * Groups every [RollingNumber] inside [content] (FR-015, FR-019a).
 *
 * - Members changed in the same frame start in the same frame, and because each member's width
 *   animates every frame, neighbors in a `Row`/`Column` move along on the same curve.
 * - Members do not announce individually; once all changed members settle, one message is
 *   announced: "{label} {value}, …" in member order ([RollingNumber] `groupOrder`, then composition order).
 *
 * The group does not lay anything out; arrange members however you like.
 */
@Composable
public fun RollingNumberGroup(content: @Composable () -> Unit) {
    val view = LocalView.current
    val state = remember(view) { RollingNumberGroupState(AnnouncementCoordinator { text -> view.announce(text) }) }
    CompositionLocalProvider(LocalRollingNumberGroup provides state, content = content)
}
