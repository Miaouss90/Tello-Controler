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
 * Axis directions validated on a real Tello EDU (2026-09-27).
 */
object StickMapper {
    const val DEFAULT_DEAD_ZONE = 0.15f

    fun map(
        leftX: Float,
        leftY: Float,
        rightX: Float,
        rightY: Float,
        deadZone: Float = DEFAULT_DEAD_ZONE,
        scale: Float = 1f,
        expo: Float = 0f,
    ) = RcInput(
        roll = toRc(curve(shape(rightX, deadZone), expo) * scale),
        pitch = toRc(-curve(shape(rightY, deadZone), expo) * scale),
        throttle = toRc(-curve(shape(leftY, deadZone), expo) * scale),
        yaw = toRc(curve(shape(leftX, deadZone), expo) * scale),
    )

    fun map(axes: StickAxes, deadZone: Float = DEFAULT_DEAD_ZONE, scale: Float = 1f, expo: Float = 0f) =
        map(axes.leftX, axes.leftY, axes.rightX, axes.rightY, deadZone, scale, expo)

    /** Expo curve: (1-e)·v + e·v³ — same end points, finer control around the center. */
    fun curve(v: Float, expo: Float): Float {
        val e = expo.coerceIn(0f, 1f)
        return (1 - e) * v + e * v * v * v
    }

    /** Dead-zone with rescaling, so output ramps smoothly from 0 instead of jumping to the dead-zone value. */
    fun shape(value: Float, deadZone: Float): Float {
        if (value.isNaN()) return 0f
        val v = value.coerceIn(-1f, 1f)
        if (abs(v) <= deadZone) return 0f
        return sign(v) * (abs(v) - deadZone) / (1f - deadZone)
    }

    private fun toRc(v: Float) = (v * 100f).roundToInt().coerceIn(-100, 100)
}
