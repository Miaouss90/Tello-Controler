package com.miaouss90.tellocontroler.controller

import kotlin.math.roundToInt

/**
 * Pure, stateful RC post-processing run inside RcSafetyLoop at a fixed rate:
 * - height limit: climbing is blocked at or above the limit (descending always allowed);
 * - smoothing: exponential low-pass per tick (Cinematic mode).
 * Not thread-safe: used only from the RC loop.
 */
class RcShaper {
    private var roll = 0f
    private var pitch = 0f
    private var throttle = 0f
    private var yaw = 0f

    var heightLimited = false
        private set

    fun shape(input: RcInput, smoothing: Float, maxHeightCm: Int?, heightCm: Int): RcInput {
        heightLimited = maxHeightCm != null && heightCm >= maxHeightCm && input.throttle > 0
        val throttleTarget = if (heightLimited) 0 else input.throttle
        val k = 1f - smoothing.coerceIn(0f, 0.95f)
        roll += (input.roll - roll) * k
        pitch += (input.pitch - pitch) * k
        throttle += (throttleTarget - throttle) * k
        yaw += (input.yaw - yaw) * k
        // A smoothed climb must not keep climbing once the limit is hit.
        if (heightLimited && throttle > 0f) throttle = 0f
        return RcInput(roll.roundToInt(), pitch.roundToInt(), throttle.roundToInt(), yaw.roundToInt())
    }

    fun reset() {
        roll = 0f
        pitch = 0f
        throttle = 0f
        yaw = 0f
        heightLimited = false
    }
}
