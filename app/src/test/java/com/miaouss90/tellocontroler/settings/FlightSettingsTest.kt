package com.miaouss90.tellocontroler.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class FlightSettingsTest {
    @Test
    fun `rate cycles slow normal sport`() {
        assertEquals(RateProfile.NORMAL, RateProfile.SLOW.next())
        assertEquals(RateProfile.SPORT, RateProfile.NORMAL.next())
        assertEquals(RateProfile.SLOW, RateProfile.SPORT.next())
    }

    @Test
    fun `sanitized clamps out of range values`() {
        val s = FlightSettings(deadZone = 0.9f, minTakeoffBatteryPercent = 1).sanitized()
        assertEquals(0.25f, s.deadZone)
        assertEquals(10, s.minTakeoffBatteryPercent)
    }

    @Test
    fun `sanitized clamps expo and height limit`() {
        val s = FlightSettings(expo = 2f, maxHeightCm = 5000).sanitized()
        assertEquals(0.8f, s.expo)
        assertEquals(800, s.maxHeightCm)
    }
}
