package com.miaouss90.tellocontroler.record

import com.miaouss90.tellocontroler.controller.RcInput
import com.miaouss90.tellocontroler.flight.FlightState
import com.miaouss90.tellocontroler.tello.MissionPad
import com.miaouss90.tellocontroler.tello.TelloTelemetry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class FlightLogTest {
    private val telemetry = TelloTelemetry(pitch = 1, roll = 2, yaw = 3, heightCm = 80, batteryPercent = 70, flightTimeSeconds = 12, tofCm = 90)

    @Test
    fun `row matches header columns and escapes commas`() {
        val row = FlightLog.row(1500, "alert, low", FlightState.FLYING, telemetry, RcInput(roll = 10, yaw = -5))
        assertEquals(FlightLog.HEADER.split(",").size, row.split(",").size)
        assertEquals("1500,alert; low,FLYING,70,80,90,1,2,3,0,0,0,12,10,0,0,-5,,,,", row)
        val withPad = FlightLog.row(0, null, FlightState.FLYING, telemetry.copy(missionPad = MissionPad(2, 5, -6, 70)), RcInput.NEUTRAL)
        assertEquals("2,5,-6,70", withPad.split(",").takeLast(4).joinToString(","))
    }

    @Test
    fun `recorder writes header, rows and closes`() {
        val dir = Files.createTempDirectory("flights").toFile()
        val recorder = FlightRecorder(dir)
        recorder.start(now = 1000, fileName = "f.csv")
        recorder.record(1100, null, FlightState.FLYING, telemetry, RcInput.NEUTRAL)
        recorder.record(1200, "landed", FlightState.LANDED, telemetry, RcInput.NEUTRAL)
        val file = recorder.stop()!!
        val lines = file.readLines()
        assertEquals(FlightLog.HEADER, lines[0])
        assertEquals(3, lines.size)
        assertEquals("200,landed", lines[2].substringBefore(",LANDED"))
        assertEquals(false, recorder.isRecording)
    }
}
