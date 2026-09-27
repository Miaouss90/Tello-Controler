package com.miaouss90.tellocontroler.flight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TakeoffGuardTest {
    private fun check(
        connected: Boolean = true,
        fresh: Boolean = true,
        battery: Int = 80,
        min: Int = 20,
        state: FlightState = FlightState.LANDED,
    ) = TakeoffGuard.check(connected, fresh, battery, min, state)

    @Test
    fun `all clear`() = assertNull(check())

    @Test
    fun `blocks in priority order`() {
        assertEquals(TakeoffBlock.NOT_CONNECTED, check(connected = false, fresh = false, battery = 0))
        assertEquals(TakeoffBlock.NO_TELEMETRY, check(fresh = false, battery = 0))
        assertEquals(TakeoffBlock.NOT_LANDED, check(state = FlightState.FLYING, battery = 0))
        assertEquals(TakeoffBlock.LOW_BATTERY, check(battery = 19))
    }

    @Test
    fun `battery at minimum is allowed`() = assertNull(check(battery = 20))
}
