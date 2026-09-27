package com.miaouss90.tellocontroler.ui.hud

import kotlin.math.hypot
import kotlin.math.roundToInt

/** Pure HUD computations (no Compose), unit-tested. */
object HudMath {
    /** Tello yaw (-180..180, relative to power-on) to a 0..359 heading. */
    fun heading(yaw: Int): Int = ((yaw % 360) + 360) % 360

    /** Horizontal ground speed in m/s from `vgx/vgy` (dm/s). */
    fun horizontalSpeedMs(speedX: Int, speedY: Int): Double = hypot(speedX.toDouble(), speedY.toDouble()) / 10.0

    /** Vertical speed in m/s from `vgz`, positive = climbing. HARDWARE-UNVERIFIED: `vgz` sign (assumed down). */
    fun verticalSpeedMs(speedZ: Int): Double = -speedZ / 10.0

    fun oneDecimal(value: Double): String {
        val tenths = (value * 10).roundToInt()
        val sign = if (tenths < 0) "-" else ""
        val abs = kotlin.math.abs(tenths)
        return "$sign${abs / 10}.${abs % 10}"
    }

    fun flightTime(elapsedMs: Long): String {
        val totalSeconds = (elapsedMs.coerceAtLeast(0) / 1000).toInt()
        return String.format(java.util.Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    data class HeadingTick(val offsetDegrees: Float, val degrees: Int, val label: String?)

    /** Ticks every 10° within ±[halfSpan]° of [heading]; cardinal points and every 30° get a label. */
    fun headingTicks(heading: Int, halfSpan: Int = 45): List<HeadingTick> {
        val first = ((heading - halfSpan) / 10.0).let { kotlin.math.ceil(it).toInt() } * 10
        return (first..heading + halfSpan step 10).map { raw ->
            val degrees = heading(raw)
            val label = when (degrees) {
                0 -> "N"
                90 -> "E"
                180 -> "S"
                270 -> "W"
                else -> if (degrees % 30 == 0) (degrees / 10).toString() else null
            }
            HeadingTick((raw - heading).toFloat(), degrees, label)
        }
    }
}
