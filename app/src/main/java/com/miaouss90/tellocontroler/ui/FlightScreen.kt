package com.miaouss90.tellocontroler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miaouss90.tellocontroler.FlightViewModel
import com.miaouss90.tellocontroler.tello.TelloConnectionState
import com.miaouss90.tellocontroler.tello.TelloTelemetry
import com.miaouss90.tellocontroler.ui.components.Metric
import com.miaouss90.tellocontroler.ui.components.StatusPill
import com.miaouss90.tellocontroler.ui.theme.HudColors
import com.miaouss90.tellocontroler.ui.theme.TelloTheme

@Composable
fun FlightScreen(vm: FlightViewModel) {
    val telemetry by vm.telemetry.collectAsState()
    val connection by vm.connection.collectAsState()
    val videoPackets by vm.videoPackets.collectAsState()
    val controllerConnected by vm.controllerConnected.collectAsState()
    val emergencyArming by vm.emergencyArming.collectAsState()
    val connected = connection == TelloConnectionState.CONNECTED
    val videoActive = videoPackets > 0

    TelloTheme {
        Box(Modifier.fillMaxSize().background(HudColors.Night)) {
            VideoSurface(
                onSurfaceReady = { vm.startVideo(it) },
                onSurfaceDestroyed = { vm.stopVideo() },
                modifier = Modifier.fillMaxSize(),
            )

            if (!videoActive) StandbyOverlay(connection)

            TopBar(connection, videoActive, controllerConnected, telemetry)

            FlightData(
                telemetry = telemetry,
                videoPackets = videoPackets,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )

            Row(
                Modifier.align(Alignment.BottomEnd).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingsButton(updateAllowed = !connected)
                if (!connected) Button(onClick = { vm.connect() }) { Text("CONNECT") }
                Button(
                    enabled = connected,
                    onClick = { vm.takeoff() },
                    colors = ButtonDefaults.buttonColors(containerColor = HudColors.Green, contentColor = HudColors.Night),
                ) { Text("A  TAKE OFF", fontWeight = FontWeight.Bold) }
                OutlinedButton(enabled = connected, onClick = { vm.land() }) { Text("B  LAND") }
            }

            if (emergencyArming) EmergencyBanner(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun StandbyOverlay(connection: TelloConnectionState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TELLO", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Light)
            Text(
                when (connection) {
                    TelloConnectionState.DISCONNECTED -> "CONNECT TO THE TELLO WI-FI, THEN PRESS CONNECT"
                    TelloConnectionState.CONNECTING -> "NEGOTIATING SDK CONNECTION…"
                    TelloConnectionState.CONNECTED -> "WAITING FOR VIDEO STREAM…"
                    TelloConnectionState.ERROR -> "TELLO NOT REACHABLE"
                },
                color = HudColors.Muted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun TopBar(
    connection: TelloConnectionState,
    videoActive: Boolean,
    controllerConnected: Boolean,
    telemetry: TelloTelemetry,
) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(
                "TELLO",
                ok = connection == TelloConnectionState.CONNECTED,
                okColor = if (connection == TelloConnectionState.ERROR) HudColors.Red else HudColors.Green,
            )
            StatusPill("VIDEO", videoActive, HudColors.Green)
            StatusPill("XBOX", controllerConnected, HudColors.Green)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("BAT", "${telemetry.batteryPercent}%")
            Metric("ALT", "${telemetry.heightCm} cm")
            Metric("TOF", "${telemetry.tofCm} cm")
        }
    }
}

@Composable
private fun FlightData(telemetry: TelloTelemetry, videoPackets: Long, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("FLIGHT DATA", color = HudColors.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(
            "YAW ${telemetry.yaw}°   •   PITCH ${telemetry.pitch}°   •   ROLL ${telemetry.roll}°",
            color = Color.White,
            fontSize = 12.sp,
        )
        if (videoPackets > 0) Text("VIDEO RX  $videoPackets packets", color = HudColors.Muted, fontSize = 9.sp)
    }
}

@Composable
private fun EmergencyBanner(modifier: Modifier = Modifier) {
    Surface(modifier, color = HudColors.Red, shape = RoundedCornerShape(12.dp)) {
        Text(
            "HOLD MENU — EMERGENCY MOTOR STOP",
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}
