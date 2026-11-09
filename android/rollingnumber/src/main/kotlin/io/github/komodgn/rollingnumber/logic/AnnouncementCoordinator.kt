package io.github.komodgn.rollingnumber.logic

/**
 * Decides when and what to announce (FR-019, FR-019a, data-model §11).
 *
 * A standalone RollingNumber uses a coordinator with one member. In a group, members never announce
 * individually: once every member with a pending change has settled, one message is announced with
 * "{label} {text}" of the changed members in member order, joined by ", ".
 */
internal class AnnouncementCoordinator(private val announce: (String) -> Unit) {
    private class Member(val session: RollingSession, val order: Int?, val index: Int)

    private val members = ArrayList<Member>()
    private val pending = LinkedHashSet<RollingSession>()
    private var nextIndex = 0

    fun register(session: RollingSession, order: Int? = null) {
        if (members.any { it.session === session }) return
        members += Member(session, order, nextIndex++)
        session.coordinator = this
    }

    fun unregister(session: RollingSession) {
        members.removeAll { it.session === session }
        pending.remove(session)
        if (session.coordinator === this) session.coordinator = null
        flushIfReady()
    }

    fun setOrder(session: RollingSession, order: Int?) {
        val i = members.indexOfFirst { it.session === session }
        if (i >= 0) members[i] = Member(session, order, members[i].index)
    }

    /** A member has a change that will be announced after it settles. */
    internal fun onChangePending(session: RollingSession) {
        if (session.announce) pending += session
    }

    /** A member settled with a change to announce. */
    internal fun onSettled(session: RollingSession) {
        if (session.announce) pending += session
        flushIfReady()
    }

    /** A pending change was cancelled (item identity changed). */
    internal fun cancel(session: RollingSession) {
        pending.remove(session)
        flushIfReady()
    }

    private fun flushIfReady() {
        if (pending.isEmpty() || pending.any { it.isAnimating }) return
        val text = members
            .filter { it.session in pending }
            .sortedWith(compareBy<Member> { it.order ?: Int.MAX_VALUE }.thenBy { it.index })
            .joinToString(", ") { it.session.accessibilityText }
        pending.clear()
        if (text.isNotEmpty()) announce(text)
    }
}
