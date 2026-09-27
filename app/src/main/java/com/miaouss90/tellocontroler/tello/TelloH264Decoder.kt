package com.miaouss90.tellocontroler.tello

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Low-latency AVC decoder for the Tello H.264 elementary stream.
 * UDP chunks are accumulated and split into NAL units on Annex-B start codes.
 *
 * HARDWARE-UNVERIFIED: framing and latency must be validated on a real Tello.
 */
class TelloH264Decoder(private val surface: Surface) {
    companion object {
        const val WIDTH = 960
        const val HEIGHT = 720
        private const val FRAME_US = 33_333L
    }

    private var codec: MediaCodec? = null
    private val running = AtomicBoolean(false)
    private val buffer = ByteArrayOutputStream()
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

    @Synchronized
    fun offer(chunk: ByteArray) {
        if (!running.get()) return
        buffer.write(chunk)
        val data = buffer.toByteArray()
        val starts = AnnexB.findStartCodes(data)
        if (starts.size < 2) return
        for (i in 0 until starts.size - 1) queue(data.copyOfRange(starts[i], starts[i + 1]))
        val tail = data.copyOfRange(starts.last(), data.size)
        buffer.reset()
        buffer.write(tail)
        drain()
    }

    @Synchronized
    fun stop() {
        running.set(false)
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
        buffer.reset()
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
