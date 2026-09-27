package com.miaouss90.tellocontroler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import com.miaouss90.tellocontroler.flight.FlightState
import com.miaouss90.tellocontroler.tello.LinkLevel
import com.miaouss90.tellocontroler.tello.LinkQuality
import com.miaouss90.tellocontroler.tello.TelloConnectionState
import com.miaouss90.tellocontroler.tello.TelloTelemetry
import com.miaouss90.tellocontroler.ui.components.Banner
import com.miaouss90.tellocontroler.ui.components.Metric
import com.miaouss90.tellocontroler.ui.components.StatusPill
import com.miaouss90.tellocontroler.ui.components.TouchStick
import com.miaouss90.tellocontroler.ui.components.color
import com.miaouss90.tellocontroler.ui.theme.HudColors
import com.miaouss90.tellocontroler.ui.theme.TelloTheme

@Composable
fun FlightScreen(vm: FlightViewModel) {
    val telemetry by vm.telemetry.collectAsState()
    val connection by vm.connection.collectAsState()
    val stateLink by vm.stateLink.collectAsState()
    val videoLink by vm.videoLink.collectAsState()
    val lastResponse by vm.lastResponse.collectAsState()
    val controllerConnected by vm.controllerConnected.collectAsState()
    val emergencyArming by vm.emergencyArming.collectAsState()
    val flightState by vm.flightState.collectAsState()
    val takeoffBlock by vm.takeoffBlock.collectAsState()
    val settings by vm.settings.collectAsState()
    val notice by vm.notice.collectAsState()
    val connected = connection == TelloConnectionState.CONNECTED
    val videoActive = videoLink.level == LinkLevel.GOOD || videoLink.level == LinkLevel.DEGRADED

    TelloTheme {
        Box(Modifier.fillMaxSize().background(HudColors.Night)) {
            VideoSurface(
                onSurfaceReady = { vm.startVideo(it) },
                onSurfaceDestroyed = { vm.stopVideo() },
                modifier = Modifier.fillMaxSize(),
            )

            if (!videoActive) {
                SetupChecklist(
                    connection = connection,
                    controllerConnected = controllerConnected,
                    touchSticks = settings.touchSticks,
                    videoActive = videoActive,
                    onConnect = { vm.connect() },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            TopBar(connection, stateLink, videoLink, controllerConnected, telemetry, settings.minTakeoffBatteryPercent)

            if (settings.touchSticks) {
                TouchStick(
                    onChange = { x, y, active -> vm.touchLeft(x, y, active) },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp, top = 40.dp),
                )
                TouchStick(
                    onChange = { x, y, active -> vm.touchRight(x, y, active) },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp, top = 40.dp),
                )
            }

            FlightData(
                telemetry = telemetry,
                flightState = flightState,
                rate = settings.rate.name,
                stateLink = stateLink,
                videoLink = videoLink,
                lastResponse = lastResponse,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )

            Row(
                Modifier.align(Alignment.BottomEnd).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingsButton(
                    updateAllowed = connection == TelloConnectionState.DISCONNECTED || connection == TelloConnectionState.ERROR,
                    settings = settings,
                    onSettingsChange = { vm.updateSettings(it) },
                )
                if (!connected && connection != TelloConnectionState.CONNECTING) {
                    Button(onClick = { vm.connect() }) {
                        Text(if (connection == TelloConnectionState.LINK_LOST) "RECONNECT" else "CONNECT")
                    }
                }
                Button(
                    enabled = takeoffBlock == null,
                    onClick = { vm.takeoff() },
                    colors = ButtonDefaults.buttonColors(containerColor = HudColors.Green, contentColor = HudColors.Night),
                ) {
                    Text(
                        if (flightState == FlightState.TAKING_OFF) "TAKING OFF…" else "A  TAKE OFF",
                        fontWeight = FontWeight.Bold,
                    )
                }
                // SAFETY: LAND stays enabled whenever a command link may exist; it is never guarded.
                OutlinedButton(enabled = connection != TelloConnectionState.DISCONNECTED, onClick = { vm.land() }) {
                    Text(if (flightState == FlightState.LANDING) "LANDING…" else "B  LAND")
                }
            }

            Column(
                Modifier.align(Alignment.TopCenter).padding(top = 70.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (connection == TelloConnectionState.LINK_LOST) {
                    Banner("TELLO LINK LOST — no telemetry for ${(stateLink.lastPacketAgeMs ?: 0) / 1000} s", HudColors.Red)
                }
                if (emergencyArming) Banner("HOLD MENU — EMERGENCY MOTOR STOP", HudColors.Red)
                notice?.let { Banner(it, HudColors.Panel) }
            }
        }
    }
}

@Composable
private fun TopBar(
    connection: TelloConnectionState,
    stateLink: LinkQuality,
    videoLink: LinkQuality,
    controllerConnected: Boolean,
    telemetry: TelloTelemetry,
    minBattery: Int,
) {
    val tello = when (connection) {
        TelloConnectionState.CONNECTED, TelloConnectionState.LINK_LOST -> stateLink.level.color()
        TelloConnectionState.ERROR -> HudColors.Red
        TelloConnectionState.CONNECTING -> HudColors.Amber
        TelloConnectionState.DISCONNECTED -> HudColors.Muted
    }
    val battery = telemetry.batteryPercent
    val batteryColor = when {
        connection != TelloConnectionState.CONNECTED -> Color.White
        battery < minBattery -> HudColors.Red
        battery < minBattery + 10 -> HudColors.Amber
        else -> Color.White
    }
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("TELLO", tello)
            StatusPill("VIDEO", videoLink.level.color())
            StatusPill("XBOX", if (controllerConnected) HudColors.Green else HudColors.Muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("BAT", "$battery%", batteryColor)
            Metric("ALT", "${telemetry.heightCm} cm")
            Metric("TOF", "${telemetry.tofCm} cm")
        }
    }
}

@Composable
private fun FlightData(
    telemetry: TelloTelemetry,
    flightState: FlightState,
    rate: String,
    stateLink: LinkQuality,
    videoLink: LinkQuality,
    lastResponse: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$flightState  •  RATE $rate", color = HudColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            "YAW ${telemetry.yaw}°   •   PITCH ${telemetry.pitch}°   •   ROLL ${telemetry.roll}°",
            color = Color.White,
            fontSize = 12.sp,
        )
        Text(
            "LINK ${stateLink.describe()}   VIDEO ${videoLink.describe()}   LAST ${lastResponse.ifEmpty { "—" }}",
            color = HudColors.Muted,
            fontSize = 9.sp,
        )
    }
}

private fun LinkQuality.describe(): String =
    lastPacketAgeMs?.let { "$packetsPerSecond/s · ${it} ms" } ?: "—"
