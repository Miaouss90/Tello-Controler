package com.miaouss90.tellocontroler.settings

import com.miaouss90.tellocontroler.controller.StickMapper

/** Output scale applied to every RC channel. */
enum class RateProfile(val scale: Float) {
    SLOW(0.35f),
    NORMAL(0.65f),
    SPORT(1f),
    ;

    fun next(): RateProfile = entries[(ordinal + 1) % entries.size]
}

data class FlightSettings(
    val rate: RateProfile = RateProfile.NORMAL,
    val deadZone: Float = StickMapper.DEFAULT_DEAD_ZONE,
    val minTakeoffBatteryPercent: Int = 20,
    val touchSticks: Boolean = false,
    val hudHorizon: Boolean = true,
    val hudHeading: Boolean = true,
    val hudReticle: Boolean = true,
) {
    companion object {
        val DEAD_ZONE_RANGE = 0.02f..0.25f
        val MIN_BATTERY_RANGE = 10..50
    }

    fun sanitized() = copy(
        deadZone = deadZone.coerceIn(DEAD_ZONE_RANGE),
        minTakeoffBatteryPercent = minTakeoffBatteryPercent.coerceIn(MIN_BATTERY_RANGE),
    )
}
