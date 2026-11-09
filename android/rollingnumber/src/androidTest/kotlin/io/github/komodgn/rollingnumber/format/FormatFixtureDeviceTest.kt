package io.github.komodgn.rollingnumber.format

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.komodgn.rollingnumber.fixtures.AssetFixtures
import io.github.komodgn.rollingnumber.fixtures.checkFormatCase
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Runs `fixtures/format/` on a real device. Run on API 24, 29, 30, and latest (quickstart §2). */
@RunWith(AndroidJUnit4::class)
class FormatFixtureDeviceTest {
    @Test
    fun allFormatFixturesPass() {
        val cases = AssetFixtures.format()
        assertTrue("no format fixtures found", cases.isNotEmpty())
        val failures = cases.mapNotNull { (file, case) ->
            val problems = runCatching { checkFormatCase(case, IcuFormatter.forDevice()) }
                .getOrElse { listOf("threw ${it::class.simpleName}: ${it.message}") }
            if (problems.isEmpty()) null else "$file#${case.id}:\n  " + problems.joinToString("\n  ")
        }
        if (failures.isNotEmpty()) fail("${failures.size} format fixture(s) failed:\n" + failures.joinToString("\n"))
    }
}
