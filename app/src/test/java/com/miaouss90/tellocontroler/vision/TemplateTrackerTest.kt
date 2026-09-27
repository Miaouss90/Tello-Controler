package com.miaouss90.tellocontroler.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateTrackerTest {
    private val w = GrayFrame.VISION_WIDTH
    private val h = GrayFrame.VISION_HEIGHT

    /** Flat gray background with a textured 20x20 "object" (checker + gradient) at (ox, oy). */
    private fun scene(ox: Int, oy: Int, brightness: Int = 0): GrayFrame {
        val px = IntArray(w * h) { 90 + brightness }
        for (y in 0 until 20) for (x in 0 until 20) {
            val checker = if ((x / 5 + y / 5) % 2 == 0) 200 else 40
            px[(oy + y) * w + ox + x] = (checker + x * 2 + brightness).coerceIn(0, 255)
        }
        return GrayFrame(w, h, px)
    }

    private fun centerOf(ox: Int, oy: Int) = (ox + 10f) / w to (oy + 10f) / h

    @Test
    fun `follows a moving object`() {
        val tracker = TemplateTracker()
        val (sx, sy) = centerOf(100, 80)
        assertTrue(tracker.select(scene(100, 80), sx, sy))
        var ox = 100
        var oy = 80
        repeat(6) {
            ox += 7
            oy -= 4
            val r = tracker.track(scene(ox, oy))
            assertFalse(r.lost)
            val (ex, ey) = centerOf(ox, oy)
            assertEquals(ex, r.centerX, 1.5f / w)
            assertEquals(ey, r.centerY, 1.5f / h)
        }
    }

    @Test
    fun `robust to a brightness change`() {
        val tracker = TemplateTracker()
        val (sx, sy) = centerOf(60, 60)
        tracker.select(scene(60, 60), sx, sy)
        val r = tracker.track(scene(64, 62, brightness = 20))
        assertFalse(r.lost)
        assertTrue(r.confidence > 0.9f)
    }

    @Test
    fun `flat area cannot be selected`() {
        assertFalse(TemplateTracker().select(scene(10, 10), 0.8f, 0.8f))
    }

    @Test
    fun `object gone is reported lost and the box stays put`() {
        val tracker = TemplateTracker()
        val (sx, sy) = centerOf(100, 80)
        tracker.select(scene(100, 80), sx, sy)
        val before = tracker.track(scene(100, 80))
        val flat = GrayFrame(w, h, IntArray(w * h) { 90 })
        val r = tracker.track(flat)
        assertTrue(r.lost)
        assertEquals(before.centerX, r.centerX, 1e-6f)
    }
}
