package com.miaouss90.tellocontroler.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miaouss90.tellocontroler.tello.TelloConnectionState
import com.miaouss90.tellocontroler.tello.TelloWifiManager
import com.miaouss90.tellocontroler.tello.TelloWifiState
import com.miaouss90.tellocontroler.ui.theme.HudColors

/** Guided pre-flight setup shown until video arrives. Every step reflects live state. */
@Composable
fun SetupChecklist(
    wifiState: TelloWifiState,
    connection: TelloConnectionState,
    controllerConnected: Boolean,
    touchSticks: Boolean,
    videoActive: Boolean,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    fun open(action: String) = context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    val connected = connection == TelloConnectionState.CONNECTED

    Surface(modifier, color = HudColors.Panel, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp).width(460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("PRE-FLIGHT SETUP", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Step(
                done = wifiState == TelloWifiState.LOCKED,
                title = "1. Power on the Tello. Wi-Fi ${TelloWifiManager.SSID_PREFIX}xxxxxx: " + when (wifiState) {
                    TelloWifiState.IDLE -> "press CONNECT"
                    TelloWifiState.SEARCHING -> "searching… approve it in the system dialog"
                    TelloWifiState.LOCKED -> "locked"
                    TelloWifiState.LOST -> "lost, waiting for it to come back…"
                    TelloWifiState.UNAVAILABLE -> "not found or refused, retry CONNECT"
                },
                action = "WI-FI SETTINGS" to { open(Settings.ACTION_WIFI_SETTINGS) },
            )
            Step(
                done = controllerConnected || touchSticks,
                title = if (touchSticks) "2. Touch sticks enabled" else "2. Pair the Xbox controller over Bluetooth",
                action = "BLUETOOTH" to { open(Settings.ACTION_BLUETOOTH_SETTINGS) },
            )
            Step(
                done = connected,
                title = "3. SDK link: " + when (connection) {
                    TelloConnectionState.DISCONNECTED -> "press CONNECT"
                    TelloConnectionState.CONNECTING -> "negotiating…"
                    TelloConnectionState.CONNECTED -> "connected"
                    TelloConnectionState.LINK_LOST -> "link lost, reconnect"
                    TelloConnectionState.ERROR -> "Tello not reachable, check Wi-Fi"
                },
                action = if (connected || connection == TelloConnectionState.CONNECTING) null else "CONNECT" to onConnect,
                primary = true,
            )
            Step(done = videoActive, title = "4. Video stream" + if (connected && !videoActive) ": waiting…" else "")
        }
    }
}

@Composable
private fun Step(done: Boolean, title: String, action: Pair<String, () -> Unit>? = null, primary: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (done) "✓" else "○", color = if (done) HudColors.Green else HudColors.Muted, fontSize = 16.sp)
        Text(title, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        if (action != null && !done) {
            if (primary) Button(onClick = action.second) { Text(action.first) }
            else TextButton(onClick = action.second) { Text(action.first) }
        }
    }
}
