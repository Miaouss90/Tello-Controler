package com.miaouss90.tellocontroler.tello

import org.junit.Assert.assertEquals
import org.junit.Test

class AnnexBTest {
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun `finds three and four byte start codes`() {
        val data = bytes(0, 0, 0, 1, 0x67, 0x42, 0, 0, 1, 0x68, 0xCE, 0, 0, 0, 1, 0x65, 0x88)
        assertEquals(listOf(0, 6, 11), AnnexB.findStartCodes(data))
    }

    @Test
    fun `no start code in payload`() {
        assertEquals(emptyList<Int>(), AnnexB.findStartCodes(bytes(1, 2, 3, 4, 5)))
    }
}
