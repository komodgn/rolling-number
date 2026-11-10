package io.github.komodgn.rollingnumber.sample

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.komodgn.rollingnumber.RollingNumber
import io.github.komodgn.rollingnumber.RollingNumberDefaults
import io.github.komodgn.rollingnumber.RollingNumberFormat
import kotlinx.coroutines.delay
import java.math.BigDecimal
import kotlin.random.Random

private data class Order(val id: Int, val amount: BigDecimal)

/**
 * SC-007: 100 rows whose amounts change every second while scrolling. Each RollingNumber gets
 * `itemKey = order.id`, so a recycled row never animates from another order's value.
 */
@Composable
internal fun OrderHistoryScreen(settings: DemoSettings) {
    val orders = remember {
        mutableStateListOf<Order>().apply {
            repeat(100) { add(Order(it + 1, BigDecimal(Random.nextInt(5, 300) * 100))) }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            repeat(10) {
                val i = Random.nextInt(orders.size)
                orders[i] = orders[i].copy(amount = BigDecimal(Random.nextInt(5, 300) * 100))
            }
        }
    }
    val krw = RollingNumberFormat.currency("KRW")
    LazyColumn(Modifier.fillMaxSize()) {
        items(orders, key = { it.id }) { order ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Order #${order.id}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                RollingNumber(
                    value = order.amount,
                    format = krw,
                    locale = settings.locale,
                    motion = settings.motion,
                    itemKey = order.id,
                    announceChanges = false,
                    style = RollingNumberDefaults.style(MaterialTheme.typography.bodyLarge),
                )
            }
            HorizontalDivider()
        }
    }
}
