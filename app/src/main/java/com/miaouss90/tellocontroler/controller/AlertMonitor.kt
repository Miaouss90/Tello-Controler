package com.miaouss90.tellocontroler.controller

enum class FlightAlert { LINK_LOST, BATTERY_LOW, BATTERY_CRITICAL, EMERGENCY_ARMING }

/** Snapshot of everything an alert can be derived from. */
data class AlertInputs(
    val linkLost: Boolean,
    val airborne: Boolean,
    val batteryPercent: Int,
    val lowBatteryPercent: Int,
    val emergencyArming: Boolean,
)

/** Pure edge-triggered alerts: each fires once when its condition becomes true. */
object AlertMonitor {
    const val CRITICAL_BATTERY_PERCENT = 10

    fun alerts(previous: AlertInputs?, current: AlertInputs): List<FlightAlert> {
        if (previous == null) return emptyList()
        val result = mutableListOf<FlightAlert>()
        if (current.linkLost && !previous.linkLost) result += FlightAlert.LINK_LOST
        if (current.emergencyArming && !previous.emergencyArming) result += FlightAlert.EMERGENCY_ARMING
        if (current.airborne && current.batteryPercent > 0) {
            val wasAbove = { limit: Int -> !previous.airborne || previous.batteryPercent >= limit }
            if (current.batteryPercent < CRITICAL_BATTERY_PERCENT && wasAbove(CRITICAL_BATTERY_PERCENT)) {
                result += FlightAlert.BATTERY_CRITICAL
            } else if (current.batteryPercent < current.lowBatteryPercent && wasAbove(current.lowBatteryPercent)) {
                result += FlightAlert.BATTERY_LOW
            }
        }
        return result
    }
}
