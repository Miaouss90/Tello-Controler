package com.miaouss90.tellocontroler.tello

enum class LinkLevel { NONE, GOOD, DEGRADED, LOST }

data class LinkQuality(
    val level: LinkLevel,
    val lastPacketAgeMs: Long?,
    val packetsPerSecond: Int,
) {
    companion object {
        val NONE = LinkQuality(LinkLevel.NONE, null, 0)
    }
}

/** Pure packet-freshness tracker for a UDP stream (Tello state or video). Thread-safe. */
class LinkMonitor(
    private val clock: () -> Long = System::currentTimeMillis,
    private val degradedAfterMs: Long = 500,
    private val lostAfterMs: Long = 2000,
) {
    private val window = ArrayDeque<Long>()
    private var last: Long? = null

    @Synchronized
    fun onPacket() {
        val now = clock()
        last = now
        window.addLast(now)
        trim(now)
    }

    /** Starts the freshness timer now, without packets: silence from here on counts as link loss. */
    @Synchronized
    fun restart() {
        window.clear()
        last = clock()
    }

    @Synchronized
    fun reset() {
        window.clear()
        last = null
    }

    @Synchronized
    fun quality(): LinkQuality {
        val now = clock()
        trim(now)
        val lastAt = last ?: return LinkQuality.NONE
        val age = now - lastAt
        val level = when {
            age >= lostAfterMs -> LinkLevel.LOST
            age >= degradedAfterMs -> LinkLevel.DEGRADED
            else -> LinkLevel.GOOD
        }
        return LinkQuality(level, age, window.size)
    }

    private fun trim(now: Long) {
        while (window.isNotEmpty() && now - window.first() > 1000) window.removeFirst()
    }
}
