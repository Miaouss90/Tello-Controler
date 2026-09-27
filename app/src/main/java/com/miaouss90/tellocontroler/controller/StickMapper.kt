package com.miaouss90.tellocontroler.controller

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Pure (Android-free) transform from raw stick axes to an [RcInput].
 *
 * Mode 2 layout, as documented in README.md:
 * left stick X = yaw, left stick Y = throttle, right stick X = roll, right stick Y = pitch.
 * Android reports stick "up" as negative Y, so Y axes are inverted.
 *
 * HARDWARE-UNVERIFIED: axis directions must be confirmed on a real Tello (see docs/SAFETY.md).
 */
object StickMapper {
    const val DEFAULT_DEAD_ZONE = 0.08f

    fun map(
        leftX: Float,
        leftY: Float,
        rightX: Float,
        rightY: Float,
        deadZone: Float = DEFAULT_DEAD_ZONE,
        scale: Float = 1f,
    ) = RcInput(
        roll = toRc(shape(rightX, deadZone) * scale),
        pitch = toRc(-shape(rightY, deadZone) * scale),
        throttle = toRc(-shape(leftY, deadZone) * scale),
        yaw = toRc(shape(leftX, deadZone) * scale),
    )

    fun map(axes: StickAxes, deadZone: Float = DEFAULT_DEAD_ZONE, scale: Float = 1f) =
        map(axes.leftX, axes.leftY, axes.rightX, axes.rightY, deadZone, scale)

    /** Dead-zone with rescaling, so output ramps smoothly from 0 instead of jumping to the dead-zone value. */
    fun shape(value: Float, deadZone: Float): Float {
        if (value.isNaN()) return 0f
        val v = value.coerceIn(-1f, 1f)
        if (abs(v) <= deadZone) return 0f
        return sign(v) * (abs(v) - deadZone) / (1f - deadZone)
    }

    private fun toRc(v: Float) = (v * 100f).roundToInt().coerceIn(-100, 100)
}
