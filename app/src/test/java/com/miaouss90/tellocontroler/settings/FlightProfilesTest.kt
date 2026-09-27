package com.miaouss90.tellocontroler.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class FlightProfilesTest {
    private val sport = FlightSettings(rate = RateProfile.SPORT, expo = 0.1f, maxHeightCm = 0)

    @Test
    fun `standard uses the user settings`() {
        assertEquals(FlightProfile(1f, 0.1f, 0f, null), FlightProfiles.resolve(sport))
        assertEquals(300, FlightProfiles.resolve(sport.copy(maxHeightCm = 300)).maxHeightCm)
    }

    @Test
    fun `indoor caps speed and height, softens sticks`() {
        val p = FlightProfiles.resolve(sport.copy(mode = FlightMode.INDOOR, maxHeightCm = 400))
        assertEquals(FlightProfile(0.35f, 0.4f, 0f, 150), p)
        assertEquals(100, FlightProfiles.resolve(sport.copy(mode = FlightMode.INDOOR, maxHeightCm = 100)).maxHeightCm)
    }

    @Test
    fun `modes never make flight more aggressive`() {
        val slow = FlightSettings(rate = RateProfile.SLOW, expo = 0.7f, mode = FlightMode.CINEMATIC)
        assertEquals(FlightProfile(0.30f, 0.7f, 0.85f, null), FlightProfiles.resolve(slow))
    }
}
