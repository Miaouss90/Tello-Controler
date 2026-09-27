package com.miaouss90.tellocontroler.record

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessUnitAssemblerTest {
    private fun nal(type: Int, firstSlice: Boolean = true) =
        byteArrayOf(0, 0, 0, 1, (0x60 or type).toByte(), if (firstSlice) 0x88.toByte() else 0x08, 0x42)

    private val sps = nal(7)
    private val pps = nal(8)

    @Test
    fun `waits for SPS, PPS and a key frame`() {
        val a = AccessUnitAssembler()
        assertTrue(a.push(nal(1)).isEmpty())
        assertTrue(a.push(sps).isEmpty())
        assertTrue(a.push(pps).isEmpty())
        assertTrue(a.push(nal(1)).isEmpty())
        val out = a.push(nal(5))
        assertEquals(1, out.size)
        val config = out[0] as AccessUnitAssembler.Output.Config
        assertArrayEquals(sps, config.sps)
        assertArrayEquals(pps, config.pps)
    }

    @Test
    fun `groups slices of one picture and flags key frames`() {
        val a = AccessUnitAssembler()
        a.push(sps)
        a.push(pps)
        a.push(nal(5))
        assertTrue(a.push(nal(5, firstSlice = false)).isEmpty())
        val first = a.push(nal(1)).single() as AccessUnitAssembler.Output.Frame
        assertTrue(first.keyFrame)
        assertEquals(14, first.data.size)
        val last = a.flush().single() as AccessUnitAssembler.Output.Frame
        assertEquals(false, last.keyFrame)
    }
}
