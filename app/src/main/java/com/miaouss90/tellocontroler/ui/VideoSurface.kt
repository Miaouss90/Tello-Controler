package com.miaouss90.tellocontroler.ui

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Hosts the MediaCodec output Surface inside Compose. */
@Composable
fun VideoSurface(
    onSurfaceReady: (Surface) -> Unit,
    onSurfaceDestroyed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(h: SurfaceHolder) = onSurfaceReady(h.surface)
                    override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {}
                    override fun surfaceDestroyed(h: SurfaceHolder) = onSurfaceDestroyed()
                })
            }
        },
        modifier = modifier,
    )
}
