package com.miaouss90.tellocontroler.flight

import com.miaouss90.tellocontroler.flight.FlightState.FLYING
import com.miaouss90.tellocontroler.flight.FlightState.LANDED
import com.miaouss90.tellocontroler.flight.FlightState.LANDING
import com.miaouss90.tellocontroler.flight.FlightState.TAKING_OFF
import org.junit.Assert.assertEquals
import org.junit.Test

class FlightStateMachineTest {
    private fun run(start: FlightState, vararg events: FlightEvent) =
        events.fold(start) { s, e -> FlightStateMachine.reduce(s, e) }

    @Test
    fun `nominal takeoff and landing`() {
        assertEquals(FLYING, run(LANDED, FlightEvent.TakeoffSent, FlightEvent.TakeoffDone(airborne = true)))
        assertEquals(LANDED, run(FLYING, FlightEvent.LandSent, FlightEvent.LandDone(ok = true)))
    }

    @Test
    fun `failed takeoff returns to landed`() {
        assertEquals(LANDED, run(LANDED, FlightEvent.TakeoffSent, FlightEvent.TakeoffDone(airborne = false)))
    }

    @Test
    fun `land preempting a pending takeoff wins`() {
        assertEquals(
            LANDING,
            run(LANDED, FlightEvent.TakeoffSent, FlightEvent.LandSent, FlightEvent.TakeoffDone(airborne = true)),
        )
    }

    @Test
    fun `telemetry height corrects state`() {
        assertEquals(LANDED, run(LANDED, FlightEvent.Height(80)))
        assertEquals(FLYING, run(TAKING_OFF, FlightEvent.Height(30)))
        assertEquals(LANDED, run(LANDING, FlightEvent.Height(0)))
        assertEquals(FLYING, run(FLYING, FlightEvent.Height(0)))
    }

    @Test
    fun `emergency always ends landed`() {
        FlightState.entries.forEach { assertEquals(LANDED, run(it, FlightEvent.EmergencySent)) }
    }

    @Test
    fun `failed land keeps flying`() {
        assertEquals(FLYING, run(FLYING, FlightEvent.LandSent, FlightEvent.LandDone(ok = false)))
    }

    @Test
    fun `motors stopped always ends landed`() {
        FlightState.entries.forEach { assertEquals(LANDED, run(it, FlightEvent.MotorsStopped)) }
    }

    @Test
    fun `motors running promotes landed only`() {
        assertEquals(FLYING, run(LANDED, FlightEvent.MotorsRunning))
        assertEquals(LANDING, run(LANDING, FlightEvent.MotorsRunning))
    }

    @Test
    fun `manual landed always ends landed`() {
        FlightState.entries.forEach { assertEquals(LANDED, run(it, FlightEvent.ManualLanded)) }
    }
}
