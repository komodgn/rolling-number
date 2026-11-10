package io.github.komodgn.rollingnumber.logic

import io.github.komodgn.rollingnumber.fixtures.ExpectJson
import io.github.komodgn.rollingnumber.fixtures.Fixtures
import io.github.komodgn.rollingnumber.fixtures.SessionCase
import io.github.komodgn.rollingnumber.logic.motion.MotionSpec
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Drives sessions with a virtual 16 ms frame clock per contracts/fixtures.md §3 and compares
 * events and announcements (kind, order, count exactly; times within one frame when given).
 */
class SessionFixtureTest {
    private val frameMs = 16L

    internal fun run(case: SessionCase): List<ExpectJson> {
        val actual = ArrayList<ExpectJson>()
        var nowMs = 0L
        val sessions = LinkedHashMap<String, RollingSession>()
        val coordinator = AnnouncementCoordinator { text -> actual += ExpectJson(announce = text, t = nowMs) }
        case.members.forEachIndexed { index, m ->
            val session = RollingSession(MotionSpec(animateFirstDisplay = m.animateFirstDisplay), Trend.fromId(case.trend))
            session.label = m.label
            session.announce = m.announce
            session.listener = object : RollingSession.Listener {
                override fun onStart() { actual += ExpectJson(event = "start", member = m.id, t = nowMs) }
                override fun onEnd() { actual += ExpectJson(event = "end", member = m.id, t = nowMs) }
            }
            coordinator.register(session, index)
            sessions[m.id] = session
        }

        fun advanceTo(targetMs: Long) {
            while (nowMs + frameMs <= targetMs) {
                nowMs += frameMs
                sessions.values.forEach { it.step(nowMs * 1_000_000) }
            }
            nowMs = targetMs
        }

        for (step in case.steps.sortedBy { it.t }) {
            advanceTo(step.t)
            step.reduceMotion?.let { reduce -> sessions.values.forEach { it.setAnimationAllowed(!reduce) } }
            step.disableAnimation?.let { sessions.getValue(it).setAnimationAllowed(false) }
            step.enableAnimation?.let { sessions.getValue(it).setAnimationAllowed(true) }
            step.itemKey?.forEach { (id, key) -> sessions.getValue(id).setItemKey(key) }
            // Apply sets in member order so results do not depend on map order (same as iOS).
            step.set?.let { set ->
                for ((id, session) in sessions) {
                    val number = set[id] ?: continue
                    // Invalid values are dropped by the UI layer; the last valid value stays.
                    number.toNumber(case.symbols)?.let { session.update(it) }
                }
            }
        }
        return actual
    }

    private fun matches(expected: ExpectJson, actual: ExpectJson): Boolean =
        expected.event == actual.event &&
            expected.member == (if (expected.event == null) expected.member else actual.member) &&
            expected.announce == actual.announce &&
            (expected.t == null || kotlin.math.abs(expected.t - (actual.t ?: Long.MIN_VALUE)) <= frameMs + 1)

    @Test
    fun allSessionFixturesPass() {
        val cases = Fixtures.session()
        assertTrue("no session fixtures found", cases.isNotEmpty())
        val failures = cases.mapNotNull { (file, case) ->
            val result = runCatching { run(case) }
            val problem = result.fold(
                onSuccess = { actual ->
                    val ok = actual.size == case.expect.size && case.expect.zip(actual).all { (e, a) -> matches(e, a) }
                    if (ok) null else "expected ${case.expect.map(::describe)}\n  actual   ${actual.map(::describe)}"
                },
                onFailure = { "threw ${it::class.simpleName}: ${it.message}" },
            )
            problem?.let { "$file#${case.id}:\n  $it" }
        }
        if (failures.isNotEmpty()) fail("${failures.size} session fixture(s) failed:\n" + failures.joinToString("\n"))
    }

    private fun describe(e: ExpectJson): String =
        if (e.announce != null) "announce('${e.announce}'@${e.t})" else "${e.event}(${e.member}@${e.t})"
}
