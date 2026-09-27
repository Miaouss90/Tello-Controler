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

    @Test
    fun `fresh input is shaped, stale and explicit neutral bypass shaping`() {
        var neutralized = 0
        val shaped = RcSafetyLoop(send = {}, clock = { now }, shape = { it.copy(yaw = 99) }, onNeutralized = { neutralized++ })
        shaped.update(forward)
        assertEquals(forward.copy(yaw = 99), shaped.nextCommand(now))
        assertEquals(RcInput.NEUTRAL, shaped.nextCommand(now + RcSafetyLoop.STALE_MS + 1))
        shaped.update(forward)
        shaped.neutral()
        assertEquals(RcInput.NEUTRAL, shaped.nextCommand(now))
        assertEquals(2, neutralized)
    }
}
