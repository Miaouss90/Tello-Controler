package com.miaouss90.tellocontroler.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.miaouss90.tellocontroler.controller.RcSafetyLoop
import com.miaouss90.tellocontroler.ui.theme.HudColors
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Virtual stick reporting (x, y) in -1..1 (Android convention, y down = positive).
 * While touched it re-reports its position every RC period so a held stick is not treated as stale;
 * on release it reports inactive and the aircraft input returns to neutral.
 */
@Composable
fun TouchStick(
    onChange: (x: Float, y: Float, active: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** Greyed out while another input (the controller) is in charge; still usable to take over. */
    dimmed: Boolean = false,
) {
    val diameter = 150.dp
    val knob = 56.dp
    var position by remember { mutableStateOf(Offset.Zero) }
    var active by remember { mutableStateOf(false) }
    val report by rememberUpdatedState(onChange)

    LaunchedEffect(active) {
        while (active) {
            report(position.x, position.y, true)
            delay(RcSafetyLoop.PERIOD_MS)
        }
        report(0f, 0f, false)
    }

    Box(
        modifier
            .alpha(if (dimmed && !active) 0.3f else 1f)
            .size(diameter)
            .background(HudColors.Panel, CircleShape)
            .border(1.dp, HudColors.Muted, CircleShape)
            .pointerInput(Unit) {
                val radius = size.width / 2f
                fun normalize(p: Offset): Offset {
                    val v = Offset((p.x - radius) / radius, (p.y - radius) / radius)
                    val length = hypot(v.x, v.y)
                    return if (length > 1f) v / length else v
                }
                detectDragGestures(
                    onDragStart = {
                        position = normalize(it)
                        active = true
                    },
                    onDragEnd = {
                        active = false
                        position = Offset.Zero
                    },
                    onDragCancel = {
                        active = false
                        position = Offset.Zero
                    },
                ) { change, _ -> position = normalize(change.position) }
            },
        contentAlignment = Alignment.Center,
    ) {
        val travel = (diameter - knob) / 2
        Box(
            Modifier
                .offset { IntOffset((position.x * travel.toPx()).roundToInt(), (position.y * travel.toPx()).roundToInt()) }
                .size(knob)
                .background(if (active) HudColors.Cyan else HudColors.Muted, CircleShape),
        )
    }
}
