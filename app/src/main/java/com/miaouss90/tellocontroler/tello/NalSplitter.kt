package com.miaouss90.tellocontroler.tello

import java.io.ByteArrayOutputStream

/** Pure: accumulates UDP video chunks and emits complete start-code-prefixed NAL units. */
class NalSplitter {
    private val buffer = ByteArrayOutputStream()

    @Synchronized
    fun push(chunk: ByteArray): List<ByteArray> {
        buffer.write(chunk)
        val data = buffer.toByteArray()
        val starts = AnnexB.findStartCodes(data)
        if (starts.size < 2) return emptyList()
        val nals = (0 until starts.size - 1).map { data.copyOfRange(starts[it], starts[it + 1]) }
        val tail = data.copyOfRange(starts.last(), data.size)
        buffer.reset()
        buffer.write(tail)
        return nals
    }

    @Synchronized
    fun reset() = buffer.reset()
}
