package com.miaouss90.tellocontroler.flight

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorStopDetectorTest {
    private val detector = MotorStopDetector(stoppedAfterMs = 3000)

    @Test
    fun `frozen counter that never ran does not trigger`() {
        assertFalse(detector.onTelemetry(0, 0))
        assertFalse(detector.onTelemetry(0, 10_000))
    }

    @Test
    fun `running counter then frozen for 3 s triggers`() {
        detector.onTelemetry(5, 0)
        detector.onTelemetry(6, 1000)
        detector.onTelemetry(7, 2000)
        assertFalse(detector.onTelemetry(7, 4999))
        assertTrue(detector.onTelemetry(7, 5000))
    }

    @Test
    fun `reset waits for motors to run again`() {
        detector.onTelemetry(1, 0)
        detector.onTelemetry(2, 1000)
        assertTrue(detector.onTelemetry(2, 4000))
        detector.reset()
        assertFalse(detector.onTelemetry(2, 9000))
        detector.onTelemetry(3, 10_000)
        assertTrue(detector.onTelemetry(3, 13_000))
    }

    @Test
    fun `motors running while the counter advances`() {
        detector.onTelemetry(1, 0)
        assertFalse(detector.motorsRunning(0))
        detector.onTelemetry(2, 1000)
        assertFalse(detector.motorsRunning(1500))
        detector.onTelemetry(3, 2000)
        assertTrue(detector.motorsRunning(2500))
        assertFalse(detector.motorsRunning(5000))
        detector.reset()
        detector.onTelemetry(4, 6000)
        assertFalse(detector.motorsRunning(6100))
    }
}
