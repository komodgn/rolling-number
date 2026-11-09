package io.github.komodgn.rollingnumber.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.komodgn.rollingnumber.RollingNumber
import io.github.komodgn.rollingnumber.RollingNumberDefaults
import io.github.komodgn.rollingnumber.RollingNumberFormat
import java.math.BigDecimal
import java.util.Locale

private val UNIT_PRICE = BigDecimal(9_900)

/** Locales offered in the demo; the currency stays KRW so only the presentation changes (FR-014). */
private val DEMO_LOCALES = listOf(
    "ko-KR" to Locale.forLanguageTag("ko-KR"),
    "en-US" to Locale.forLanguageTag("en-US"),
    "de-DE" to Locale.forLanguageTag("de-DE"),
    "ja-JP" to Locale.forLanguageTag("ja-JP"),
)

@Composable
fun CheckoutScreen() {
    var quantity by remember { mutableIntStateOf(1) }
    var locale by remember { mutableStateOf(DEMO_LOCALES.first().second) }
    val total = UNIT_PRICE.multiply(BigDecimal(quantity))
    val krw = RollingNumberFormat.currency("KRW")

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("결제", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DEMO_LOCALES.forEach { (label, l) ->
                FilterChip(selected = locale == l, onClick = { locale = l }, label = { Text(label) })
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("기본 티셔츠", style = MaterialTheme.typography.titleMedium)
                RollingNumber(
                    value = UNIT_PRICE,
                    format = krw,
                    locale = locale,
                    style = RollingNumberDefaults.style(MaterialTheme.typography.bodyMedium),
                )
            }
            FilledTonalButton(onClick = { if (quantity > 1) quantity-- }) { Text("−") }
            Spacer(Modifier.width(8.dp))
            RollingNumber(value = quantity, style = RollingNumberDefaults.style(MaterialTheme.typography.titleMedium))
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = { if (quantity < 99) quantity++ }) { Text("+") }
        }

        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("최종 결제 금액", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            RollingNumber(
                value = total,
                format = krw,
                locale = locale,
                style = RollingNumberDefaults.style(
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
                ),
            )
        }
    }
}
