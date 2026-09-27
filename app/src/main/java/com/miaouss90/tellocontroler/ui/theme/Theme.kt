package com.miaouss90.tellocontroler.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object HudColors {
    val Night = Color(0xFF070B10)
    val Panel = Color(0xCC101820)
    val Cyan = Color(0xFF58D6FF)
    val Green = Color(0xFF58E39B)
    val Red = Color(0xFFFF6B6B)
    val Amber = Color(0xFFFFC857)
    val Muted = Color(0xFF8C99A8)
}

@Composable
fun TelloTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = HudColors.Cyan,
            background = HudColors.Night,
            surface = HudColors.Panel,
            error = HudColors.Red,
        ),
        content = content,
    )
}
