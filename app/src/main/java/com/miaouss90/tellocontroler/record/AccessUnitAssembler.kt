package com.miaouss90.tellocontroler.record

import com.miaouss90.tellocontroler.tello.AnnexB
import java.io.ByteArrayOutputStream

/**
 * Pure: turns a NAL stream into what an MP4 muxer needs — one codec config (SPS/PPS) then whole frames.
 * Recording starts at the first key frame; frames are emitted when the next one begins.
 */
class AccessUnitAssembler {
    sealed interface Output {
        class Config(val sps: ByteArray, val pps: ByteArray) : Output
        class Frame(val data: ByteArray, val keyFrame: Boolean) : Output
    }

    private var sps: ByteArray? = null
    private var pps: ByteArray? = null
    private var started = false
    private val frame = ByteArrayOutputStream()
    private var frameIsKey = false

    fun push(nal: ByteArray): List<Output> {
        val out = mutableListOf<Output>()
        when (AnnexB.nalType(nal)) {
            AnnexB.NAL_SPS -> sps = nal
            AnnexB.NAL_PPS -> pps = nal
            AnnexB.NAL_SLICE, AnnexB.NAL_IDR -> {
                val newFrame = AnnexB.startsNewFrame(nal)
                if (newFrame && frame.size() > 0) out += takeFrame()
                if (!started) {
                    val sps = sps
                    val pps = pps
                    if (!newFrame || AnnexB.nalType(nal) != AnnexB.NAL_IDR || sps == null || pps == null) return out
                    started = true
                    out += Output.Config(sps, pps)
                }
                frame.write(nal)
                if (AnnexB.nalType(nal) == AnnexB.NAL_IDR) frameIsKey = true
            }
        }
        return out
    }

    /** The last, possibly incomplete, frame when recording stops. */
    fun flush(): List<Output> = if (frame.size() > 0) listOf(takeFrame()) else emptyList()

    private fun takeFrame(): Output.Frame {
        val result = Output.Frame(frame.toByteArray(), frameIsKey)
        frame.reset()
        frameIsKey = false
        return result
    }
}
