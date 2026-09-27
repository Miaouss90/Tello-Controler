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
}
