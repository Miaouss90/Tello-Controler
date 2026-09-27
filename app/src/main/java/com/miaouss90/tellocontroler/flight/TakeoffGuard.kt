package com.miaouss90.tellocontroler.flight

enum class TakeoffBlock { NOT_CONNECTED, NO_TELEMETRY, LOW_BATTERY, NOT_LANDED }

/** Pure pre-takeoff checks. Landing is never guarded. */
object TakeoffGuard {
    fun check(
        connected: Boolean,
        telemetryFresh: Boolean,
        batteryPercent: Int,
        minBatteryPercent: Int,
        state: FlightState,
    ): TakeoffBlock? = when {
        !connected -> TakeoffBlock.NOT_CONNECTED
        !telemetryFresh -> TakeoffBlock.NO_TELEMETRY
        state != FlightState.LANDED -> TakeoffBlock.NOT_LANDED
        batteryPercent < minBatteryPercent -> TakeoffBlock.LOW_BATTERY
        else -> null
    }
}
