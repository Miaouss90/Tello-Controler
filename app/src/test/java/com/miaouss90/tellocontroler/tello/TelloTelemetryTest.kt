package com.miaouss90.tellocontroler.tello

import org.junit.Assert.assertEquals
import org.junit.Test

class TelloTelemetryTest {
    @Test
    fun `parses SDK state packet`() {
        val raw = "pitch:1;roll:-2;yaw:45;vgx:0;vgy:0;vgz:0;templ:60;temph:64;tof:120;h:80;bat:87;" +
            "baro:12.34;time:15;agx:0.00;agy:0.00;agz:-1000.00;\r\n"
        val t = TelloTelemetry.parse(raw)
        assertEquals(TelloTelemetry(1, -2, 45, 80, 87, 15, 120, 62.0), t)
    }

    @Test
    fun `garbage yields defaults`() {
        assertEquals(TelloTelemetry(), TelloTelemetry.parse("not a state packet"))
    }
}
