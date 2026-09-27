package com.miaouss90.tellocontroler.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miaouss90.tellocontroler.ui.theme.HudColors

private val HudLine = Color.White.copy(alpha = 0.8f)

/**
 * Artificial horizon: rotates against roll, shifts with pitch, 10° ladder.
 * HARDWARE-UNVERIFIED: Tello pitch/roll sign conventions.
 */
@Composable
fun ArtificialHorizon(pitch: Int, roll: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val pxPerDegree = size.height / 60f
        val half = size.width * 0.18f
        val gap = half * 0.25f
        rotate(degrees = -roll.toFloat(), pivot = center) {
            val y = center.y + pitch * pxPerDegree
            drawLine(HudLine, Offset(center.x - half, y), Offset(center.x - gap, y), strokeWidth = 3.dp.toPx())
            drawLine(HudLine, Offset(center.x + gap, y), Offset(center.x + half, y), strokeWidth = 3.dp.toPx())
            for (step in listOf(-20, -10, 10, 20)) {
                val ladderY = y - step * pxPerDegree
                val width = half * if (step % 20 == 0) 0.4f else 0.25f
                drawLine(
                    HudLine.copy(alpha = 0.45f),
                    Offset(center.x - width, ladderY),
                    Offset(center.x + width, ladderY),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }
    }
}

@Composable
fun Reticle(modifier: Modifier = Modifier) {
    Canvas(modifier.size(44.dp)) {
        val stroke = 2.dp.toPx()
        val r = 6.dp.toPx()
        drawCircle(HudColors.Cyan, radius = r, center = center, style = Stroke(stroke))
        val arm = size.width / 2
        drawLine(HudColors.Cyan, Offset(center.x - arm, center.y), Offset(center.x - r * 2, center.y), stroke)
        drawLine(HudColors.Cyan, Offset(center.x + r * 2, center.y), Offset(center.x + arm, center.y), stroke)
        drawLine(HudColors.Cyan, Offset(center.x, center.y + r * 2), Offset(center.x, center.y + arm * 0.6f), stroke)
    }
}

/** Heading tape ±45°. Yaw is relative to the Tello's power-on orientation, not magnetic north. */
@Composable
fun HeadingTape(yaw: Int, modifier: Modifier = Modifier) {
    val heading = HudMath.heading(yaw)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = HudLine, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.width(260.dp).height(30.dp)) {
            val pxPerDegree = size.width / 90f
            HudMath.headingTicks(heading).forEach { tick ->
                val x = center.x + tick.offsetDegrees * pxPerDegree
                val length = if (tick.label != null) 10.dp.toPx() else 5.dp.toPx()
                drawLine(HudLine, Offset(x, size.height - length), Offset(x, size.height), 1.5.dp.toPx())
                tick.label?.let {
                    val layout = measurer.measure(it, labelStyle)
                    drawText(layout, topLeft = Offset(x - layout.size.width / 2f, 0f))
                }
            }
            val marker = Path().apply {
                moveTo(center.x, size.height - 12.dp.toPx())
                lineTo(center.x - 5.dp.toPx(), size.height)
                lineTo(center.x + 5.dp.toPx(), size.height)
                close()
            }
            drawPath(marker, HudColors.Cyan)
        }
        Text("HDG %03d°".format(java.util.Locale.ROOT, heading), color = HudColors.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
