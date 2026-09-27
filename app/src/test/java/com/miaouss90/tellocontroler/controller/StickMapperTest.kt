package com.miaouss90.tellocontroler.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StickMapperTest {
    @Test
    fun `centered sticks inside dead zone are neutral`() {
        assertTrue(StickMapper.map(0.05f, -0.07f, 0.02f, 0.08f).isNeutral)
    }

    @Test
    fun `mode 2 layout matches README`() {
        // Android: stick pushed up = negative Y.
        assertEquals(RcInput(yaw = 100), StickMapper.map(1f, 0f, 0f, 0f))
        assertEquals(RcInput(throttle = 100), StickMapper.map(0f, -1f, 0f, 0f))
        assertEquals(RcInput(roll = 100), StickMapper.map(0f, 0f, 1f, 0f))
        assertEquals(RcInput(pitch = 100), StickMapper.map(0f, 0f, 0f, -1f))
    }

    @Test
    fun `output ramps from zero just outside dead zone`() {
        assertEquals(1, StickMapper.map(0.09f, 0f, 0f, 0f).yaw)
    }

    @Test
    fun `out of range and NaN axes are clamped`() {
        assertEquals(RcInput(yaw = -100, roll = 100), StickMapper.map(-3f, Float.NaN, 2f, 0f))
    }

    @Test
    fun `rate scale limits output`() {
        assertEquals(RcInput(yaw = 35, throttle = 35), StickMapper.map(StickAxes(leftX = 1f, leftY = -1f), scale = 0.35f))
    }
}
