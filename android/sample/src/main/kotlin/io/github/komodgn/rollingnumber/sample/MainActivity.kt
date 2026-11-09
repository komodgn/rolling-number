package io.github.komodgn.rollingnumber.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val settings = remember { DemoSettings() }
                    var tab by remember { mutableIntStateOf(0) }
                    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                        TabRow(selectedTabIndex = tab) {
                            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("결제") })
                            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("주문 내역") })
                        }
                        when (tab) {
                            0 -> CheckoutScreen(settings)
                            else -> OrderHistoryScreen(settings)
                        }
                    }
                }
            }
        }
    }
}
