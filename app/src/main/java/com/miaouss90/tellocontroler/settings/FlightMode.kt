package com.miaouss90.tellocontroler.settings

import kotlin.math.max
import kotlin.math.min

enum class FlightMode { STANDARD, INDOOR, CINEMATIC }

/** Effective shaping once the flight mode is applied on top of the user settings. */
data class FlightProfile(
    val scale: Float,
    val expo: Float,
    val smoothing: Float,
    val maxHeightCm: Int?,
)

/** Pure: modes can only make flight softer/safer than the user settings, never more aggressive. */
object FlightProfiles {
    const val INDOOR_MAX_HEIGHT_CM = 150
    const val INDOOR_MAX_SCALE = 0.35f
    const val INDOOR_MIN_EXPO = 0.4f
    const val CINEMATIC_MAX_SCALE = 0.30f
    const val CINEMATIC_MIN_EXPO = 0.5f
    const val CINEMATIC_SMOOTHING = 0.85f

    fun resolve(s: FlightSettings): FlightProfile {
        val limit = s.maxHeightCm.takeIf { it > 0 }
        return when (s.mode) {
            FlightMode.STANDARD -> FlightProfile(s.rate.scale, s.expo, 0f, limit)
            FlightMode.INDOOR -> FlightProfile(
                scale = min(s.rate.scale, INDOOR_MAX_SCALE),
                expo = max(s.expo, INDOOR_MIN_EXPO),
                smoothing = 0f,
                maxHeightCm = min(limit ?: INDOOR_MAX_HEIGHT_CM, INDOOR_MAX_HEIGHT_CM),
            )
            FlightMode.CINEMATIC -> FlightProfile(
                scale = min(s.rate.scale, CINEMATIC_MAX_SCALE),
                expo = max(s.expo, CINEMATIC_MIN_EXPO),
                smoothing = CINEMATIC_SMOOTHING,
                maxHeightCm = limit,
            )
        }
    }
}
