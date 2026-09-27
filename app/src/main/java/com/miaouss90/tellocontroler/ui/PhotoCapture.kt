package com.miaouss90.tellocontroler.ui

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import com.miaouss90.tellocontroler.tello.TelloH264Decoder

/** Grabs the current video frame from the SurfaceView (the decoder renders straight to it). */
object PhotoCapture {
    fun capture(view: SurfaceView?, onResult: (Bitmap?) -> Unit) {
        if (view == null || !view.holder.surface.isValid) return onResult(null)
        val bitmap = Bitmap.createBitmap(TelloH264Decoder.WIDTH, TelloH264Decoder.HEIGHT, Bitmap.Config.ARGB_8888)
        runCatching {
            PixelCopy.request(view, bitmap, { result ->
                onResult(bitmap.takeIf { result == PixelCopy.SUCCESS })
            }, Handler(Looper.getMainLooper()))
        }.onFailure { onResult(null) }
    }
}
