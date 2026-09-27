package com.miaouss90.tellocontroler.tello

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Low-latency AVC decoder for the Tello H.264 elementary stream.
 * NAL units come from [NalSplitter], shared with the video recorder.
 *
 * Framing validated on a real Tello EDU (2026-09-27). HARDWARE-UNVERIFIED: latency not measured.
 */
class TelloH264Decoder(private val surface: Surface) {
    companion object {
        const val WIDTH = 960
        const val HEIGHT = 720
        private const val FRAME_US = 33_333L
    }

    private var codec: MediaCodec? = null
    private val running = AtomicBoolean(false)
    private var pts = 0L

    fun start() {
        if (running.getAndSet(true)) return
        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024)
            configure(format, surface, null, 0)
            start()
        }
    }

    /** Feeds one start-code-prefixed NAL unit (see [NalSplitter]). */
    @Synchronized
    fun offerNal(nal: ByteArray) {
        if (!running.get()) return
        queue(nal)
        drain()
    }

    @Synchronized
    fun stop() {
        running.set(false)
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
    }

    private fun queue(nal: ByteArray) {
        val c = codec ?: return
        val index = c.dequeueInputBuffer(0)
        if (index < 0) return
        c.getInputBuffer(index)?.apply { clear(); put(nal) }
        c.queueInputBuffer(index, 0, nal.size, pts, 0)
        pts += FRAME_US
    }

    private fun drain() {
        val c = codec ?: return
        val info = MediaCodec.BufferInfo()
        while (true) {
            val out = c.dequeueOutputBuffer(info, 0)
            if (out < 0) break
            c.releaseOutputBuffer(out, true)
        }
    }
}
