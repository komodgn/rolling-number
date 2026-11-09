package io.github.komodgn.rollingnumber.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SC-002: frame timing while the coupon toggles 10 times on the checkout screen (the grouped
 * summary row animates up to 8 numbers at once). Run on a physical mid-range device:
 *
 *   ./gradlew :android:benchmark:connectedBenchmarkAndroidTest
 *
 * Target: frame drops under 1% of the measured frames.
 */
@RunWith(AndroidJUnit4::class)
class CheckoutBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun couponToggle() = rule.measureRepeated(
        packageName = "io.github.komodgn.rollingnumber.sample",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.textContains("쿠폰")), 5_000)
        },
    ) {
        repeat(10) {
            device.findObject(By.textContains("쿠폰"))?.click()
            Thread.sleep(250) // tap faster than the ~0.5 s animation so transitions interrupt
        }
        device.waitForIdle()
    }
}
