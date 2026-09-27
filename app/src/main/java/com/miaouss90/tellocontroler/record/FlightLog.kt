package com.miaouss90.tellocontroler.record

import com.miaouss90.tellocontroler.controller.RcInput
import com.miaouss90.tellocontroler.flight.FlightState
import com.miaouss90.tellocontroler.tello.TelloTelemetry
import java.io.BufferedWriter
import java.io.File

/** Pure CSV format of the flight recorder: one row per sample or event. */
object FlightLog {
    const val HEADER =
        "t_ms,event,state,battery,height_cm,tof_cm,pitch,roll,yaw,vgx,vgy,vgz,motor_s,rc_roll,rc_pitch,rc_throttle,rc_yaw," +
            "pad_id,pad_x_cm,pad_y_cm,pad_z_cm"

    fun row(elapsedMs: Long, event: String?, state: FlightState, t: TelloTelemetry, rc: RcInput): String =
        listOf(
            elapsedMs, event?.replace(',', ';').orEmpty(), state,
            t.batteryPercent, t.heightCm, t.tofCm, t.pitch, t.roll, t.yaw,
            t.speedX, t.speedY, t.speedZ, t.flightTimeSeconds,
            rc.roll, rc.pitch, rc.throttle, rc.yaw,
            t.missionPad?.id ?: "", t.missionPad?.xCm ?: "", t.missionPad?.yCm ?: "", t.missionPad?.zCm ?: "",
        ).joinToString(",")
}

/** Writes one CSV file per flight into [directory]. Not thread-safe: call from one coroutine. */
class FlightRecorder(private val directory: File) {
    private var writer: BufferedWriter? = null
    private var file: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = writer != null

    fun start(now: Long, fileName: String) {
        stop()
        directory.mkdirs()
        val target = File(directory, fileName)
        file = target
        writer = target.bufferedWriter().also {
            it.write(FlightLog.HEADER)
            it.newLine()
        }
        startedAt = now
    }

    fun record(now: Long, event: String?, state: FlightState, telemetry: TelloTelemetry, rc: RcInput) {
        val w = writer ?: return
        w.write(FlightLog.row(now - startedAt, event, state, telemetry, rc))
        w.newLine()
    }

    /** Closes the current log and returns its file, or null if none was open. */
    fun stop(): File? {
        runCatching { writer?.close() }
        writer = null
        return file.also { file = null }
    }
}
