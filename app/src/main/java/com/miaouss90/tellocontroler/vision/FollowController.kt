package com.miaouss90.tellocontroler.vision

import com.miaouss90.tellocontroler.controller.RcInput
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure proportional follow: turns (yaw) and climbs/descends (throttle) to bring the tracked target back to the
 * image center. No forward motion (the tracker has no distance estimate). Gentle, capped commands; they still go
 * through RcSafetyLoop (height limit, smoothing, stale watchdog). HARDWARE-UNVERIFIED gains and signs.
 */
object FollowController {
    const val MAX_YAW = 30
    const val MAX_THROTTLE = 25
    const val YAW_GAIN = 80f
    const val THROTTLE_GAIN = 60f
    /** No correction within this distance from the center (fraction of the frame), avoids hunting. */
    const val DEAD_BAND = 0.04f

    fun command(target: TrackResult): RcInput {
        if (target.lost) return RcInput.NEUTRAL
        val errorX = target.centerX - 0.5f // > 0: target right of center → turn right (positive yaw)
        val errorY = target.centerY - 0.5f // > 0: target below center → descend (negative throttle)
        return RcInput(
            yaw = correction(errorX, YAW_GAIN, MAX_YAW),
            throttle = -correction(errorY, THROTTLE_GAIN, MAX_THROTTLE),
        )
    }

    private fun correction(error: Float, gain: Float, max: Int): Int =
        if (abs(error) < DEAD_BAND) 0 else (error * gain).roundToInt().coerceIn(-max, max)
}

/** Pure input arbitration: any pilot stick input wins; otherwise follow (if active); otherwise pilot neutral. */
object InputArbiter {
    fun choose(pilot: RcInput?, follow: RcInput?): RcInput? = when {
        pilot != null && !pilot.isNeutral -> pilot
        follow != null -> follow
        else -> pilot
    }
}
