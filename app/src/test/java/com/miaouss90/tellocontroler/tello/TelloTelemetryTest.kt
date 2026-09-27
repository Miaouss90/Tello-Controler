package com.miaouss90.tellocontroler.tello

import org.junit.Assert.assertEquals
import org.junit.Test

class TelloTelemetryTest {
    @Test
    fun `parses SDK state packet`() {
        val raw = "pitch:1;roll:-2;yaw:45;vgx:3;vgy:-4;vgz:1;templ:60;temph:64;tof:120;h:80;bat:87;" +
            "baro:12.34;time:15;agx:0.00;agy:0.00;agz:-1000.00;\r\n"
        val t = TelloTelemetry.parse(raw)
        assertEquals(TelloTelemetry(1, -2, 45, 80, 87, 15, 120, 62.0, speedX = 3, speedY = -4, speedZ = 1), t)
    }

    @Test
    fun `garbage yields defaults`() {
        assertEquals(TelloTelemetry(), TelloTelemetry.parse("not a state packet"))
    }

    @Test
    fun `mission pad parsed only when detected`() {
        val seen = TelloTelemetry.parse("mid:3;x:12;y:-4;z:80;mpry:0,0,0;pitch:0;")
        assertEquals(MissionPad(id = 3, xCm = 12, yCm = -4, zCm = 80), seen.missionPad)
        assertEquals(null, TelloTelemetry.parse("mid:-1;x:0;y:0;z:0;").missionPad)
        assertEquals(null, TelloTelemetry.parse("mid:-2;").missionPad)
        assertEquals(null, TelloTelemetry.parse("pitch:0;").missionPad)
    }
}
