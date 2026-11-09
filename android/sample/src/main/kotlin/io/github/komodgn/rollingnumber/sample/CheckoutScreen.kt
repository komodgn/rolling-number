package io.github.komodgn.rollingnumber.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.komodgn.rollingnumber.RollingMotion
import io.github.komodgn.rollingnumber.RollingNumber
import io.github.komodgn.rollingnumber.RollingNumberDefaults
import io.github.komodgn.rollingnumber.RollingNumberFormat
import io.github.komodgn.rollingnumber.RollingNumberGroup
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

private val UNIT_PRICE = BigDecimal(9_900)
private val FREE_SHIPPING_FROM = BigDecimal(30_000)
private val SHIPPING_FEE = BigDecimal(3_000)
private val COUPON_RATE = BigDecimal("0.10")

/** Locales offered in the demo; the currency stays KRW so only the presentation changes (FR-014). */
internal val DEMO_LOCALES = listOf(
    "ko-KR" to Locale.forLanguageTag("ko-KR"),
    "en-US" to Locale.forLanguageTag("en-US"),
    "de-DE" to Locale.forLanguageTag("de-DE"),
    "ja-JP" to Locale.forLanguageTag("ja-JP"),
)

/** Settings shared by the demo screens. */
internal class DemoSettings {
    var locale by mutableStateOf(DEMO_LOCALES.first().second)
    var animationEnabled by mutableStateOf(true)
    var ignoreReduceMotion by mutableStateOf(false)

    val motion: RollingMotion
        get() = RollingNumberDefaults.motion(enabled = animationEnabled, respectReduceMotion = !ignoreReduceMotion)
}

@Composable
internal fun CheckoutScreen(settings: DemoSettings) {
    var quantity by remember { mutableIntStateOf(1) }
    var couponApplied by remember { mutableStateOf(false) }
    var debugAdjustment by remember { mutableStateOf(BigDecimal.ZERO) }

    // The app computes prices; the component only displays them.
    val subtotal = UNIT_PRICE.multiply(BigDecimal(quantity))
    val discount = if (couponApplied) subtotal.multiply(COUPON_RATE).setScale(0, RoundingMode.DOWN) else BigDecimal.ZERO
    val afterDiscount = subtotal - discount
    val shipping = if (afterDiscount >= FREE_SHIPPING_FROM) BigDecimal.ZERO else SHIPPING_FEE
    val total = afterDiscount + shipping + debugAdjustment

    val krw = RollingNumberFormat.currency("KRW")
    val body = RollingNumberDefaults.style(MaterialTheme.typography.bodyLarge)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("결제", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DEMO_LOCALES.forEach { (label, l) ->
                FilterChip(selected = settings.locale == l, onClick = { settings.locale = l }, label = { Text(label) })
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("기본 티셔츠", style = MaterialTheme.typography.titleMedium)
                RollingNumber(value = UNIT_PRICE, format = krw, locale = settings.locale, motion = settings.motion,
                    style = RollingNumberDefaults.style(MaterialTheme.typography.bodyMedium))
            }
            FilledTonalButton(onClick = { if (quantity > 1) quantity-- }) { Text("−") }
            Spacer(Modifier.width(8.dp))
            RollingNumber(value = quantity, motion = settings.motion, accessibilityLabel = "수량",
                style = RollingNumberDefaults.style(MaterialTheme.typography.titleMedium))
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = { if (quantity < 99) quantity++ }) { Text("+") }
        }

        Button(onClick = { couponApplied = !couponApplied }, modifier = Modifier.fillMaxWidth()) {
            Text(if (couponApplied) "10% 쿠폰 해제" else "10% 쿠폰 적용")
        }

        HorizontalDivider()

        // US4: one group. Members changed together start together, neighbors in the Row move with
        // the width change, and the screen reader hears one merged announcement.
        RollingNumberGroup {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledAmount("상품 금액") {
                    RollingNumber(subtotal, krw, locale = settings.locale, motion = settings.motion, style = body,
                        accessibilityLabel = "상품 금액", groupOrder = 0)
                }
                LabeledAmount("쿠폰 할인") {
                    RollingNumber(discount.negate(), krw, locale = settings.locale, motion = settings.motion, style = body,
                        accessibilityLabel = "할인액", groupOrder = 1)
                }
                LabeledAmount("배송비") {
                    RollingNumber(shipping, krw, locale = settings.locale, motion = settings.motion, style = body,
                        accessibilityLabel = "배송비", groupOrder = 2)
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("최종 결제 금액", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    RollingNumber(total, krw, locale = settings.locale, motion = settings.motion,
                        style = RollingNumberDefaults.style(
                            MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
                        ),
                        accessibilityLabel = "최종가", groupOrder = 3)
                }

                // Same numbers on one line: when one widens, the ones to its right slide along.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val small = RollingNumberDefaults.style(TextStyle(fontSize = 14.sp))
                    RollingNumber(subtotal, krw, locale = settings.locale, motion = settings.motion, style = small, announceChanges = false)
                    Text(" − ", fontSize = 14.sp)
                    RollingNumber(discount, krw, locale = settings.locale, motion = settings.motion, style = small, announceChanges = false)
                    Text(" + ", fontSize = 14.sp)
                    RollingNumber(shipping, krw, locale = settings.locale, motion = settings.motion, style = small, announceChanges = false)
                    Text(" = ", fontSize = 14.sp)
                    RollingNumber(total, krw, locale = settings.locale, motion = settings.motion, style = small, announceChanges = false)
                }
            }
        }

        HorizontalDivider()

        SettingSwitch("애니메이션", settings.animationEnabled) { settings.animationEnabled = it }
        SettingSwitch("동작 줄이기 설정 무시", settings.ignoreReduceMotion) { settings.ignoreReduceMotion = it }
        OutlinedButton(
            onClick = { debugAdjustment = if (debugAdjustment.signum() == 0) BigDecimal(-50_000) else BigDecimal.ZERO },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (debugAdjustment.signum() == 0) "디버그: 금액을 음수로" else "디버그: 음수 해제") }
    }
}

@Composable
private fun LabeledAmount(label: String, amount: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        amount()
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
