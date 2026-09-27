package com.miaouss90.tellocontroler.tello

import java.io.ByteArrayOutputStream

/** Pure: accumulates UDP video chunks and emits complete start-code-prefixed NAL units. */
class NalSplitter {
    private val buffer = ByteArrayOutputStream()

    /**
     * @param endOfFrame the chunk completes a frame (Tello: last UDP fragment is shorter than
     * [TelloVideoReceiver.FULL_PACKET_BYTES]); the pending NAL is emitted now instead of when the next one starts.
     */
    @Synchronized
    fun push(chunk: ByteArray, endOfFrame: Boolean = false): List<ByteArray> {
        buffer.write(chunk)
        val data = buffer.toByteArray()
        val starts = AnnexB.findStartCodes(data)
        if (starts.isEmpty()) {
            if (endOfFrame) buffer.reset()
            return emptyList()
        }
        val nals = (0 until starts.size - 1).map { data.copyOfRange(starts[it], starts[it + 1]) }.toMutableList()
        val tail = data.copyOfRange(starts.last(), data.size)
        buffer.reset()
        if (endOfFrame) nals += tail else buffer.write(tail)
        return nals
    }

    @Synchronized
    fun reset() = buffer.reset()
}
