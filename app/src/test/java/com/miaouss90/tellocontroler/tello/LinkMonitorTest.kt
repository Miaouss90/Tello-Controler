package com.miaouss90.tellocontroler.tello

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkMonitorTest {
    private var now = 0L
    private val monitor = LinkMonitor(clock = { now }, degradedAfterMs = 500, lostAfterMs = 2000)

    @Test
    fun `no packet ever is NONE`() {
        assertEquals(LinkQuality.NONE, monitor.quality())
    }

    @Test
    fun `levels follow last packet age`() {
        monitor.onPacket()
        now = 499
        assertEquals(LinkLevel.GOOD, monitor.quality().level)
        now = 500
        assertEquals(LinkLevel.DEGRADED, monitor.quality().level)
        now = 2000
        assertEquals(LinkLevel.LOST, monitor.quality().level)
    }

    @Test
    fun `rate counts packets in the last second`() {
        repeat(10) {
            monitor.onPacket()
            now += 100
        }
        assertEquals(10, monitor.quality().packetsPerSecond)
        now += 1000
        assertEquals(0, monitor.quality().packetsPerSecond)
    }

    @Test
    fun `restart without packets becomes LOST after timeout`() {
        now = 10_000
        monitor.restart()
        assertEquals(LinkLevel.GOOD, monitor.quality().level)
        now = 12_000
        assertEquals(LinkLevel.LOST, monitor.quality().level)
    }
}
