package com.miaouss90.tellocontroler.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miaouss90.tellocontroler.ui.theme.HudColors
import com.miaouss90.tellocontroler.vision.TrackResult

/**
 * Covers exactly the 4:3 video area: tap selects a target, long-press clears it, the tracked box is drawn
 * on top. Green = locked, red = lost.
 */
@Composable
fun TargetLayer(
    target: TrackResult?,
    onSelect: (x: Float, y: Float) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val select by rememberUpdatedState(onSelect)
    val clear by rememberUpdatedState(onClear)
    Box(
        modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { select(it.x / size.width, it.y / size.height) },
                onLongPress = { clear() },
            )
        },
    ) {
        if (target != null) {
            val color = if (target.lost) HudColors.Red else HudColors.Green
            Canvas(Modifier.fillMaxSize()) {
                val w = target.boxWidth * size.width
                val h = target.boxHeight * size.height
                val left = target.centerX * size.width - w / 2
                val top = target.centerY * size.height - h / 2
                drawRect(color, topLeft = Offset(left, top), size = Size(w, h), style = Stroke(2.dp.toPx()))
                val c = Offset(target.centerX * size.width, target.centerY * size.height)
                drawLine(color, c - Offset(6.dp.toPx(), 0f), c + Offset(6.dp.toPx(), 0f), 2.dp.toPx())
                drawLine(color, c - Offset(0f, 6.dp.toPx()), c + Offset(0f, 6.dp.toPx()), 2.dp.toPx())
            }
            Text(
                if (target.lost) "TARGET LOST — searching" else "TARGET %.0f %%".format(java.util.Locale.ROOT, target.confidence * 100),
                Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
