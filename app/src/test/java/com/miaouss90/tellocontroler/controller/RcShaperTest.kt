package com.miaouss90.tellocontroler.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RcShaperTest {
    private val shaper = RcShaper()
    private val climb = RcInput(throttle = 60, pitch = 20)

    @Test
    fun `no smoothing no limit is identity`() {
        assertEquals(climb, shaper.shape(climb, smoothing = 0f, maxHeightCm = null, heightCm = 500))
    }

    @Test
    fun `height limit blocks climbing but not descending`() {
        assertEquals(RcInput(pitch = 20), shaper.shape(climb, 0f, maxHeightCm = 150, heightCm = 150))
        assertTrue(shaper.heightLimited)
        val descend = RcInput(throttle = -40)
        assertEquals(descend, shaper.shape(descend, 0f, maxHeightCm = 150, heightCm = 200))
        assertFalse(shaper.heightLimited)
    }

    @Test
    fun `smoothing ramps toward the target and reset clears it`() {
        val first = shaper.shape(RcInput(roll = 100), smoothing = 0.5f, maxHeightCm = null, heightCm = 0)
        assertEquals(50, first.roll)
        val second = shaper.shape(RcInput(roll = 100), smoothing = 0.5f, maxHeightCm = null, heightCm = 0)
        assertEquals(75, second.roll)
        shaper.reset()
        assertEquals(50, shaper.shape(RcInput(roll = 100), 0.5f, null, 0).roll)
    }
}
