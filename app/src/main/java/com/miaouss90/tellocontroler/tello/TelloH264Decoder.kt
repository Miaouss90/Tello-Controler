package com.miaouss90.tellocontroler.tello

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import com.miaouss90.tellocontroler.record.AccessUnitAssembler
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Low-latency AVC decoder for the Tello H.264 stream.
 *
 * The receiver thread only assembles whole frames and enqueues them (never blocks, so UDP is never starved).
 * A dedicated thread feeds MediaCodec and waits for input buffers instead of dropping data. If the decoder
 * falls behind, the backlog is discarded and decoding resumes cleanly at the next key frame — a short freeze
 * instead of smeared artifacts. Framing validated on a real Tello EDU (2026-09-27).
 */
class TelloH264Decoder(private val surface: Surface) {
    companion object {
        const val WIDTH = 960
        const val HEIGHT = 720
        private const val FRAME_US = 33_333L
        private const val QUEUE_CAPACITY = 30
        private const val INPUT_TIMEOUT_US = 10_000L
    }

    private val assembler = AccessUnitAssembler()
    private val queue = ArrayBlockingQueue<AccessUnitAssembler.Output>(QUEUE_CAPACITY)
    private val running = AtomicBoolean(false)
    @Volatile private var waitingForKeyFrame = false
    @Volatile private var codec: MediaCodec? = null
    private var thread: Thread? = null

    /** Frames discarded to resynchronize (diagnostics). */
    val droppedFrames = AtomicLong()

    fun start() {
        if (running.getAndSet(true)) return
        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024)
                setInteger(MediaFormat.KEY_PRIORITY, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            }
            configure(format, surface, null, 0)
            start()
        }
        thread = Thread(::decodeLoop, "tello-decoder").apply { start() }
    }

    /** Receiver thread: one start-code-prefixed NAL unit. Never blocks. */
    fun offerNal(nal: ByteArray) {
        if (!running.get()) return
        synchronized(assembler) { assembler.push(nal) }.forEach(::enqueue)
    }

    /** Receiver thread: the current frame is complete (short UDP packet), decode it without waiting for the next. */
    fun endOfFrame() {
        if (!running.get()) return
        synchronized(assembler) { assembler.flush() }.forEach(::enqueue)
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        thread?.join(500)
        thread = null
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
        queue.clear()
    }

    private fun enqueue(output: AccessUnitAssembler.Output) {
        if (output is AccessUnitAssembler.Output.Frame && waitingForKeyFrame) {
            if (!output.keyFrame) {
                droppedFrames.incrementAndGet()
                return
            }
            waitingForKeyFrame = false
        }
        if (!queue.offer(output)) {
            droppedFrames.addAndGet(queue.size.toLong())
            queue.clear()
            val resumesHere = output !is AccessUnitAssembler.Output.Frame || output.keyFrame
            if (resumesHere) queue.offer(output) else waitingForKeyFrame = true
        }
    }

    private fun decodeLoop() {
        val info = MediaCodec.BufferInfo()
        var pts = 0L
        runCatching {
            while (running.get()) {
                val c = codec ?: break
                val item = queue.poll(20, TimeUnit.MILLISECONDS)
                if (item != null) {
                    val (data, flags) = when (item) {
                        is AccessUnitAssembler.Output.Config ->
                            (item.sps + item.pps) to MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                        is AccessUnitAssembler.Output.Frame ->
                            item.data to if (item.keyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    }
                    var index = -1
                    while (running.get() && index < 0) {
                        index = c.dequeueInputBuffer(INPUT_TIMEOUT_US)
                        if (index < 0) drain(c, info)
                    }
                    if (index >= 0) {
                        c.getInputBuffer(index)?.apply {
                            clear()
                            put(data)
                        }
                        c.queueInputBuffer(index, 0, data.size, pts, flags)
                        if (item is AccessUnitAssembler.Output.Frame) pts += FRAME_US
                    }
                }
                drain(c, info)
            }
        }
    }

    private fun drain(c: MediaCodec, info: MediaCodec.BufferInfo) {
        while (true) {
            val out = c.dequeueOutputBuffer(info, 0)
            if (out < 0) break
            c.releaseOutputBuffer(out, true)
        }
    }
}
