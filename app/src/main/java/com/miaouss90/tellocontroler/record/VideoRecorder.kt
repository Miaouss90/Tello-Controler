package com.miaouss90.tellocontroler.record

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import com.miaouss90.tellocontroler.tello.TelloH264Decoder
import java.nio.ByteBuffer

/**
 * Records the Tello H.264 stream to MP4 without re-encoding (MediaMuxer). Starts at the first key frame.
 * HARDWARE-UNVERIFIED: muxing of the Tello stream and playback in the Gallery.
 */
class VideoRecorder(private val target: MediaStorage.Target, private val clockNs: () -> Long = System::nanoTime) {
    private val assembler = AccessUnitAssembler()
    private val muxer = MediaMuxer(target.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private var track = -1
    private var startNs = 0L
    private var lastPtsUs = -1L
    private var frames = 0
    private var stopped = false

    @Synchronized
    fun onNal(nal: ByteArray) {
        if (stopped) return
        runCatching { assembler.push(nal).forEach(::write) }
    }

    /** Finalizes the file; returns true when at least one frame was saved. */
    @Synchronized
    fun stop(): Boolean {
        if (stopped) return false
        runCatching { assembler.flush().forEach(::write) }
        stopped = true
        val saved = track >= 0 && frames > 0
        if (track >= 0) runCatching { muxer.stop() }
        runCatching { muxer.release() }
        target.finish(keep = saved)
        return saved
    }

    private fun write(output: AccessUnitAssembler.Output) {
        when (output) {
            is AccessUnitAssembler.Output.Config -> {
                val format = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    TelloH264Decoder.WIDTH,
                    TelloH264Decoder.HEIGHT,
                ).apply {
                    setByteBuffer("csd-0", ByteBuffer.wrap(output.sps))
                    setByteBuffer("csd-1", ByteBuffer.wrap(output.pps))
                }
                track = muxer.addTrack(format)
                muxer.start()
                startNs = clockNs()
            }
            is AccessUnitAssembler.Output.Frame -> {
                if (track < 0) return
                val ptsUs = maxOf((clockNs() - startNs) / 1000, lastPtsUs + 1)
                lastPtsUs = ptsUs
                val info = MediaCodec.BufferInfo().apply {
                    set(0, output.data.size, ptsUs, if (output.keyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                }
                muxer.writeSampleData(track, ByteBuffer.wrap(output.data), info)
                frames++
            }
        }
    }
}
