package com.miaouss90.tellocontroler.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miaouss90.tellocontroler.tello.LinkLevel
import com.miaouss90.tellocontroler.ui.theme.HudColors

fun LinkLevel.color(): Color = when (this) {
    LinkLevel.NONE -> HudColors.Muted
    LinkLevel.GOOD -> HudColors.Green
    LinkLevel.DEGRADED -> HudColors.Amber
    LinkLevel.LOST -> HudColors.Red
}

@Composable
fun StatusPill(label: String, dotColor: Color) {
    Surface(color = HudColors.Panel, shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("●", color = dotColor, fontSize = 9.sp)
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Metric(name: String, value: String, valueColor: Color = Color.White) {
    Surface(color = HudColors.Panel, shape = RoundedCornerShape(11.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(name, color = HudColors.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun Banner(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, color = color, shape = RoundedCornerShape(12.dp)) {
        Text(
            text,
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

/** Live position of one RC stick pair (-100..100, y up = positive), as sent to the aircraft. */
@Composable
fun StickIndicator(label: String, x: Int, y: Int) {
    val box = 52.dp
    val dot = 10.dp
    Surface(color = HudColors.Panel, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(box).border(1.dp, HudColors.Muted, RoundedCornerShape(6.dp))) {
                Box(Modifier.align(Alignment.Center).size(width = box, height = 1.dp).background(HudColors.Muted.copy(alpha = 0.4f)))
                Box(Modifier.align(Alignment.Center).size(width = 1.dp, height = box).background(HudColors.Muted.copy(alpha = 0.4f)))
                val travel = (box - dot) / 2
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .offset(x = travel * (x / 100f), y = travel * (-y / 100f))
                        .size(dot)
                        .background(if (x != 0 || y != 0) HudColors.Cyan else HudColors.Muted, CircleShape),
                )
            }
            Text(label, color = HudColors.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
    }
}
