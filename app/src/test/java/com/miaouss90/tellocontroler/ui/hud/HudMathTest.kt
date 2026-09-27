package com.miaouss90.tellocontroler.ui.hud

import org.junit.Assert.assertEquals
import org.junit.Test

class HudMathTest {
    @Test
    fun `heading wraps to 0 to 359`() {
        assertEquals(0, HudMath.heading(0))
        assertEquals(270, HudMath.heading(-90))
        assertEquals(180, HudMath.heading(-180))
        assertEquals(10, HudMath.heading(370))
    }

    @Test
    fun `speeds convert from dm per s`() {
        assertEquals(5.0, HudMath.horizontalSpeedMs(30, 40), 1e-9)
        assertEquals(1.5, HudMath.verticalSpeedMs(-15), 1e-9)
    }

    @Test
    fun `one decimal formatting`() {
        assertEquals("1.5", HudMath.oneDecimal(1.46))
        assertEquals("-0.3", HudMath.oneDecimal(-0.3))
        assertEquals("0.0", HudMath.oneDecimal(0.0))
    }

    @Test
    fun `flight time is mm ss`() {
        assertEquals("00:00", HudMath.flightTime(-5))
        assertEquals("01:05", HudMath.flightTime(65_400))
    }

    @Test
    fun `heading ticks around north wrap and label cardinals`() {
        val ticks = HudMath.headingTicks(heading = 5, halfSpan = 20)
        assertEquals(listOf(-15f, -5f, 5f, 15f), ticks.map { it.offsetDegrees })
        assertEquals(listOf(350, 0, 10, 20), ticks.map { it.degrees })
        assertEquals("N", ticks[1].label)
    }
}
