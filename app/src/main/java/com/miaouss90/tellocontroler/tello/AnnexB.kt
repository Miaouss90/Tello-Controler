package com.miaouss90.tellocontroler.tello

/** Pure helpers for H.264 Annex-B byte streams. */
object AnnexB {
    /** Offsets of every 3-byte (00 00 01) or 4-byte (00 00 00 01) start code in [data]. */
    fun findStartCodes(data: ByteArray): List<Int> {
        val result = mutableListOf<Int>()
        var i = 0
        while (i < data.size - 3) {
            val zeroZero = data[i].toInt() == 0 && data[i + 1].toInt() == 0
            val threeByte = zeroZero && data[i + 2].toInt() == 1
            val fourByte = zeroZero && data[i + 2].toInt() == 0 && data[i + 3].toInt() == 1
            if (threeByte || fourByte) {
                result += i
                i += 3
            } else {
                i++
            }
        }
        return result
    }

    const val NAL_SLICE = 1
    const val NAL_IDR = 5
    const val NAL_SPS = 7
    const val NAL_PPS = 8

    /** Index of the NAL header byte in a start-code-prefixed NAL unit. */
    fun headerOffset(nal: ByteArray): Int = if (nal.size > 2 && nal[2].toInt() == 1) 3 else 4

    fun nalType(nal: ByteArray): Int {
        val offset = headerOffset(nal)
        return if (nal.size > offset) nal[offset].toInt() and 0x1F else -1
    }

    fun isVideoSlice(nal: ByteArray): Boolean = nalType(nal).let { it == NAL_SLICE || it == NAL_IDR }

    /** first_mb_in_slice == 0 (ue(v) "1" bit): this slice starts a new picture. */
    fun startsNewFrame(nal: ByteArray): Boolean {
        val offset = headerOffset(nal) + 1
        return isVideoSlice(nal) && nal.size > offset && (nal[offset].toInt() and 0x80) != 0
    }
}
