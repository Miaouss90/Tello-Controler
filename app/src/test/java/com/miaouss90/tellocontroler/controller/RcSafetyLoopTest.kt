package com.miaouss90.tellocontroler.controller

import org.junit.Assert.assertEquals
import org.junit.Test

class RcSafetyLoopTest {
    private var now = 10_000L
    private val loop = RcSafetyLoop(send = {}, clock = { now })
    private val forward = RcInput(pitch = 50)

    @Test
    fun `fresh input is forwarded`() {
        loop.update(forward)
        assertEquals(forward, loop.commandAt(now + RcSafetyLoop.STALE_MS))
    }

    @Test
    fun `stale input becomes neutral`() {
        loop.update(forward)
        assertEquals(RcInput.NEUTRAL, loop.commandAt(now + RcSafetyLoop.STALE_MS + 1))
    }

    @Test
    fun `neutral overrides active input immediately`() {
        loop.update(forward)
        loop.neutral()
        assertEquals(RcInput.NEUTRAL, loop.commandAt(now))
    }

    @Test
    fun `no input ever received is neutral`() {
        assertEquals(RcInput.NEUTRAL, loop.commandAt(now))
    }
}
