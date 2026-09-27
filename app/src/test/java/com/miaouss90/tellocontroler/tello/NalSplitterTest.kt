package com.miaouss90.tellocontroler.tello

import org.junit.Assert.assertEquals
import org.junit.Test

class NalSplitterTest {
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun `emits complete NALs across chunks and keeps the tail`() {
        val s = NalSplitter()
        assertEquals(0, s.push(bytes(0, 0, 0, 1, 0x67, 0x42)).size)
        val nals = s.push(bytes(0, 0, 1, 0x68, 0xCE, 0, 0, 0, 1, 0x65))
        assertEquals(2, nals.size)
        assertEquals(6, nals[0].size)
        assertEquals(5, nals[1].size)
    }

    @Test
    fun `nal type and frame start`() {
        val idrFirst = bytes(0, 0, 0, 1, 0x65, 0x88)
        val sliceCont = bytes(0, 0, 1, 0x41, 0x08)
        assertEquals(AnnexB.NAL_IDR, AnnexB.nalType(idrFirst))
        assertEquals(true, AnnexB.startsNewFrame(idrFirst))
        assertEquals(AnnexB.NAL_SLICE, AnnexB.nalType(sliceCont))
        assertEquals(false, AnnexB.startsNewFrame(sliceCont))
        assertEquals(false, AnnexB.startsNewFrame(bytes(0, 0, 0, 1, 0x67, 0x88)))
    }
}
