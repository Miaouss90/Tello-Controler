package com.miaouss90.tellocontroler.vision

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean

/**
 * While [active], grabs a downscaled copy of the video (PixelCopy of the decoder's SurfaceView) every
 * [periodMs] and delivers it as a [GrayFrame] on a dedicated vision thread — never the UI thread.
 */
@Composable
fun VisionFrameGrabber(view: SurfaceView?, active: Boolean, onFrame: (GrayFrame) -> Unit, periodMs: Long = 100) {
    val thread = remember { HandlerThread("tello-vision").apply { start() } }
    DisposableEffect(thread) { onDispose { thread.quitSafely() } }

    LaunchedEffect(view, active) {
        if (!active || view == null) return@LaunchedEffect
        val handler = Handler(thread.looper)
        val bitmap = Bitmap.createBitmap(GrayFrame.VISION_WIDTH, GrayFrame.VISION_HEIGHT, Bitmap.Config.ARGB_8888)
        val argb = IntArray(GrayFrame.VISION_WIDTH * GrayFrame.VISION_HEIGHT)
        val busy = AtomicBoolean(false)
        while (isActive) {
            if (view.holder.surface.isValid && busy.compareAndSet(false, true)) {
                runCatching {
                    PixelCopy.request(view, bitmap, { result ->
                        if (result == PixelCopy.SUCCESS) {
                            bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                            runCatching { onFrame(GrayFrame.fromArgb(bitmap.width, bitmap.height, argb)) }
                        }
                        busy.set(false)
                    }, handler)
                }.onFailure { busy.set(false) }
            }
            delay(periodMs)
        }
    }
}
