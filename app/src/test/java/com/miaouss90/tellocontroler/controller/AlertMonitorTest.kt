package com.miaouss90.tellocontroler.controller

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertMonitorTest {
    private val base = AlertInputs(
        linkLost = false,
        airborne = true,
        batteryPercent = 50,
        lowBatteryPercent = 20,
        emergencyArming = false,
    )

    private fun alerts(previous: AlertInputs?, current: AlertInputs) = AlertMonitor.alerts(previous, current)

    @Test
    fun `first snapshot never alerts`() {
        assertEquals(emptyList<FlightAlert>(), alerts(null, base.copy(linkLost = true)))
    }

    @Test
    fun `link loss and arming fire once on the edge`() {
        val lost = base.copy(linkLost = true, emergencyArming = true)
        assertEquals(listOf(FlightAlert.LINK_LOST, FlightAlert.EMERGENCY_ARMING), alerts(base, lost))
        assertEquals(emptyList<FlightAlert>(), alerts(lost, lost))
    }

    @Test
    fun `battery low then critical while airborne`() {
        val low = base.copy(batteryPercent = 19)
        val critical = base.copy(batteryPercent = 9)
        assertEquals(listOf(FlightAlert.BATTERY_LOW), alerts(base, low))
        assertEquals(emptyList<FlightAlert>(), alerts(low, low.copy(batteryPercent = 18)))
        assertEquals(listOf(FlightAlert.BATTERY_CRITICAL), alerts(low, critical))
    }

    @Test
    fun `taking off with an already low battery alerts`() {
        val landedLow = base.copy(airborne = false, batteryPercent = 15)
        assertEquals(listOf(FlightAlert.BATTERY_LOW), alerts(landedLow, landedLow.copy(airborne = true)))
    }

    @Test
    fun `no battery alert on the ground or without telemetry`() {
        assertEquals(emptyList<FlightAlert>(), alerts(base.copy(airborne = false), base.copy(airborne = false, batteryPercent = 5)))
        assertEquals(emptyList<FlightAlert>(), alerts(base, base.copy(batteryPercent = 0)))
    }
}
