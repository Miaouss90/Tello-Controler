package com.miaouss90.tellocontroler.flight

/**
 * Detects that the motors stopped from the Tello `time` telemetry field (motor-on time, +1 per second
 * while spinning). Only reports after the counter has been seen advancing, so firmware without the field
 * never triggers a false landing.
 */
class MotorStopDetector(private val stoppedAfterMs: Long = 3000) {
    private var lastMotorTime: Int? = null
    private var changedAt = 0L
    private var running = false

    /** Returns true when motors were running and the counter has been frozen for [stoppedAfterMs]. */
    fun onTelemetry(motorTimeSeconds: Int, now: Long): Boolean {
        val last = lastMotorTime
        if (last == null || motorTimeSeconds != last) {
            if (last != null) running = true
            changedAt = now
        }
        lastMotorTime = motorTimeSeconds
        return running && now - changedAt >= stoppedAfterMs
    }

    fun reset() {
        running = false
    }
}
