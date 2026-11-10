package io.github.komodgn.rollingnumber

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import io.github.komodgn.rollingnumber.a11y.announce
import io.github.komodgn.rollingnumber.a11y.rollingSemantics
import io.github.komodgn.rollingnumber.format.IcuFormatter
import io.github.komodgn.rollingnumber.logic.AnnouncementCoordinator
import io.github.komodgn.rollingnumber.logic.RollingSession
import io.github.komodgn.rollingnumber.motion.FrameDriver
import io.github.komodgn.rollingnumber.motion.FrameRequests
import io.github.komodgn.rollingnumber.motion.rememberReduceMotion
import io.github.komodgn.rollingnumber.render.FrameTicker
import io.github.komodgn.rollingnumber.render.RollingNumberLayout
import java.math.BigDecimal
import java.util.Locale

private const val TAG = "RollingNumber"

/**
 * Shows [value] formatted with [format] and rolls changed digits when the value changes.
 *
 * ```
 * RollingNumber(value = total, format = RollingNumberFormat.currency("KRW"))
 * ```
 *
 * @param locale null follows the device locale (FR-003).
 * @param prefix text shown before the number, never rolled (FR-004).
 * @param suffix text shown after the number, never rolled (FR-004).
 * @param trend which way changed digits roll (FR-005).
 * @param itemKey identity of the list item; a different key shows the next value without animation (FR-013).
 * @param accessibilityLabel name read before the value, e.g. "Total" (FR-019).
 * @param announceChanges announce the final value after each change settles (FR-019).
 * @param groupOrder order inside a [RollingNumberGroup] announcement; null keeps composition order.
 * @param onAnimationStart called once when a change starts animating from rest (FR-020).
 * @param onAnimationEnd called once when every element has settled on the latest value (FR-020).
 */
@Composable
public fun RollingNumber(
    value: BigDecimal,
    format: RollingNumberFormat = RollingNumberFormat.number(),
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    prefix: String = "",
    suffix: String = "",
    trend: RollingTrend = RollingTrend.Auto,
    style: RollingNumberStyle = RollingNumberDefaults.style(),
    motion: RollingMotion = RollingNumberDefaults.motion(),
    itemKey: Any? = null,
    accessibilityLabel: String? = null,
    announceChanges: Boolean = true,
    groupOrder: Int? = null,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null,
) {
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0]
    val formatter = remember { IcuFormatter.forDevice() }
    val formatted = remember(value, format, resolvedLocale, prefix, suffix) {
        formatter.format(value, format, resolvedLocale).copy(prefix = prefix, suffix = suffix)
    }

    val session = remember { RollingSession() }
    val ticker = remember { FrameTicker() }
    val requests = remember { FrameRequests() }
    val latestStart by rememberUpdatedState(onAnimationStart)
    val latestEnd by rememberUpdatedState(onAnimationEnd)
    val reduceMotion by rememberReduceMotion()

    val view = LocalView.current
    val group = LocalRollingNumberGroup.current
    val standalone = remember(view) { AnnouncementCoordinator { text -> view.announce(text) } }
    val coordinator = group?.coordinator ?: standalone
    DisposableEffect(coordinator, session) {
        coordinator.register(session, groupOrder)
        onDispose { coordinator.unregister(session) }
    }

    SideEffect {
        session.spec = motion.toSpec()
        session.trend = trend.toInternal()
        session.label = accessibilityLabel
        session.announce = announceChanges
        coordinator.setOrder(session, groupOrder)
        session.listener = object : RollingSession.Listener {
            override fun onStart() { latestStart?.invoke() }
            override fun onEnd() { latestEnd?.invoke() }
        }
        session.setAnimationAllowed(motion.enabled && !(reduceMotion && motion.respectReduceMotion))
        session.setItemKey(itemKey)
        session.update(formatted)
        ticker.tick()
        requests.request()
    }

    val description = if (accessibilityLabel.isNullOrEmpty()) formatted.displayText else "$accessibilityLabel ${formatted.displayText}"
    FrameDriver(session, ticker, requests)
    RollingNumberLayout(session, ticker, style, modifier.rollingSemantics(description))
}

/** [Long] convenience overload of [RollingNumber]. */
@Composable
public fun RollingNumber(
    value: Long,
    format: RollingNumberFormat = RollingNumberFormat.number(),
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    prefix: String = "",
    suffix: String = "",
    trend: RollingTrend = RollingTrend.Auto,
    style: RollingNumberStyle = RollingNumberDefaults.style(),
    motion: RollingMotion = RollingNumberDefaults.motion(),
    itemKey: Any? = null,
    accessibilityLabel: String? = null,
    announceChanges: Boolean = true,
    groupOrder: Int? = null,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null,
) {
    val big = remember(value) { BigDecimal.valueOf(value) }
    RollingNumber(big, format, modifier, locale, prefix, suffix, trend, style, motion, itemKey,
        accessibilityLabel, announceChanges, groupOrder, onAnimationStart, onAnimationEnd)
}

/** [Int] convenience overload of [RollingNumber]. */
@Composable
public fun RollingNumber(
    value: Int,
    format: RollingNumberFormat = RollingNumberFormat.number(),
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    prefix: String = "",
    suffix: String = "",
    trend: RollingTrend = RollingTrend.Auto,
    style: RollingNumberStyle = RollingNumberDefaults.style(),
    motion: RollingMotion = RollingNumberDefaults.motion(),
    itemKey: Any? = null,
    accessibilityLabel: String? = null,
    announceChanges: Boolean = true,
    groupOrder: Int? = null,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null,
) {
    RollingNumber(value.toLong(), format, modifier, locale, prefix, suffix, trend, style, motion, itemKey,
        accessibilityLabel, announceChanges, groupOrder, onAnimationStart, onAnimationEnd)
}

/**
 * [Double] convenience overload of [RollingNumber]. Converted with `BigDecimal.valueOf`
 * (shortest decimal form). NaN and infinite values are ignored and the last valid value stays.
 */
@Composable
public fun RollingNumber(
    value: Double,
    format: RollingNumberFormat = RollingNumberFormat.number(),
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    prefix: String = "",
    suffix: String = "",
    trend: RollingTrend = RollingTrend.Auto,
    style: RollingNumberStyle = RollingNumberDefaults.style(),
    motion: RollingMotion = RollingNumberDefaults.motion(),
    itemKey: Any? = null,
    accessibilityLabel: String? = null,
    announceChanges: Boolean = true,
    groupOrder: Int? = null,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null,
) {
    var lastValid by remember { mutableStateOf(BigDecimal.ZERO) }
    val big = remember(value) {
        if (value.isNaN() || value.isInfinite()) {
            Log.w(TAG, "Ignoring non-finite value $value; keeping $lastValid")
            lastValid
        } else {
            BigDecimal.valueOf(value)
        }
    }
    SideEffect { lastValid = big }
    RollingNumber(big, format, modifier, locale, prefix, suffix, trend, style, motion, itemKey,
        accessibilityLabel, announceChanges, groupOrder, onAnimationStart, onAnimationEnd)
}
