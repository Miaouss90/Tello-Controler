package com.miaouss90.tellocontroler.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
