package io.github.komodgn.rollingnumber.format

import io.github.komodgn.rollingnumber.fixtures.Fixtures
import io.github.komodgn.rollingnumber.fixtures.checkFormatCase
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs every `fixtures/format/` case through the formatter the device would pick. */
abstract class FormatFixtureTestBase {
    @Test
    fun allFormatFixturesPass() {
        val cases = Fixtures.format()
        assertTrue("no format fixtures found", cases.isNotEmpty())
        val failures = cases.mapNotNull { (file, case) ->
            val problems = runCatching { checkFormatCase(case, IcuFormatter.forDevice()) }
                .getOrElse { listOf("threw ${it::class.simpleName}: ${it.message}") }
            if (problems.isEmpty()) null else "$file#${case.id}:\n  " + problems.joinToString("\n  ")
        }
        if (failures.isNotEmpty()) fail("${failures.size} format fixture(s) failed:\n" + failures.joinToString("\n"))
    }
}

/** API 29: legacy `android.icu.text` path. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class FormatFixtureLegacyTest : FormatFixtureTestBase()

/** API 33: `android.icu.number.NumberFormatter` path. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FormatFixtureModernTest : FormatFixtureTestBase()
