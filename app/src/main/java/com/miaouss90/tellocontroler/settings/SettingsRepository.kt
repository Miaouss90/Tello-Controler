package com.miaouss90.tellocontroler.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists [FlightSettings] in SharedPreferences. */
class SettingsRepository(context: Context) {
    private companion object {
        const val FILE = "flight_settings"
        const val KEY_RATE = "rate"
        const val KEY_DEAD_ZONE = "dead_zone"
        const val KEY_MIN_BATTERY = "min_takeoff_battery"
        const val KEY_TOUCH_STICKS = "touch_sticks"
        const val KEY_HUD_HORIZON = "hud_horizon"
        const val KEY_HUD_HEADING = "hud_heading"
        const val KEY_HUD_RETICLE = "hud_reticle"
    }

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<FlightSettings> = _settings.asStateFlow()

    fun update(transform: (FlightSettings) -> FlightSettings) {
        val next = transform(_settings.value).sanitized()
        _settings.value = next
        prefs.edit()
            .putString(KEY_RATE, next.rate.name)
            .putFloat(KEY_DEAD_ZONE, next.deadZone)
            .putInt(KEY_MIN_BATTERY, next.minTakeoffBatteryPercent)
            .putBoolean(KEY_TOUCH_STICKS, next.touchSticks)
            .putBoolean(KEY_HUD_HORIZON, next.hudHorizon)
            .putBoolean(KEY_HUD_HEADING, next.hudHeading)
            .putBoolean(KEY_HUD_RETICLE, next.hudReticle)
            .apply()
    }

    private fun load(): FlightSettings {
        val defaults = FlightSettings()
        return FlightSettings(
            rate = RateProfile.entries.firstOrNull { it.name == prefs.getString(KEY_RATE, null) } ?: defaults.rate,
            deadZone = prefs.getFloat(KEY_DEAD_ZONE, defaults.deadZone),
            minTakeoffBatteryPercent = prefs.getInt(KEY_MIN_BATTERY, defaults.minTakeoffBatteryPercent),
            touchSticks = prefs.getBoolean(KEY_TOUCH_STICKS, defaults.touchSticks),
            hudHorizon = prefs.getBoolean(KEY_HUD_HORIZON, defaults.hudHorizon),
            hudHeading = prefs.getBoolean(KEY_HUD_HEADING, defaults.hudHeading),
            hudReticle = prefs.getBoolean(KEY_HUD_RETICLE, defaults.hudReticle),
        ).sanitized()
    }
}
