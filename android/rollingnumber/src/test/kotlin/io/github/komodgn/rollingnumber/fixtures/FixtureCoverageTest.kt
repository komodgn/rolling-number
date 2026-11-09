package io.github.komodgn.rollingnumber.fixtures

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every case id listed in contracts/fixtures.md "최소 수록 케이스" must exist in fixtures/diff/
 * (or fixtures/session/), so the spec's edge cases and scenarios stay covered on both platforms.
 */
class FixtureCoverageTest {
    @Test
    fun everyListedCaseExists() {
        val root = File(System.getProperty("rollingnumber.fixtures")!!)
        val contract = File(root.parentFile, "specs/001-rolling-number-display/contracts/fixtures.md").readText()
        val section = contract.substringAfter("### 최소 수록 케이스").substringBefore("\n## ")
        val listed = Regex("""^\|\s*([a-z][a-z0-9-]*(?:\s*/\s*[a-z][a-z0-9-]*)*)\s*\|""", RegexOption.MULTILINE)
            .findAll(section)
            .flatMap { m -> m.groupValues[1].split("/").map { it.trim() } }
            .filter { it != "id" }
            .toSet()
        assertTrue("no ids parsed from fixtures.md", listed.size >= 15)

        val existing = (Fixtures.diff().map { it.second.id } + Fixtures.session().map { it.second.id }).toSet()
        val missing = listed - existing
        assertTrue("fixture cases missing: $missing", missing.isEmpty())
    }
}
