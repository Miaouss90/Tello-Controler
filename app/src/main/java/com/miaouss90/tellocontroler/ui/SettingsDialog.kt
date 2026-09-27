package com.miaouss90.tellocontroler.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miaouss90.tellocontroler.FlightViewModel
import com.miaouss90.tellocontroler.settings.FlightSettings
import com.miaouss90.tellocontroler.settings.RateProfile
import com.miaouss90.tellocontroler.ui.theme.HudColors
import com.miaouss90.tellocontroler.update.GitHubReleaseSource
import com.miaouss90.tellocontroler.update.UpdateState
import com.miaouss90.tellocontroler.update.UpdateViewModel
import kotlin.math.roundToInt

/**
 * @param updateAllowed false while linked to the Tello: installing restarts the app and would drop control.
 */
@Composable
fun SettingsButton(
    updateAllowed: Boolean,
    settings: FlightSettings,
    onSettingsChange: (FlightSettings) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("SETTINGS") }
    if (open) SettingsDialog(updateAllowed, settings, onSettingsChange, onDismiss = { open = false })
}

@Composable
private fun SettingsDialog(
    updateAllowed: Boolean,
    settings: FlightSettings,
    onSettingsChange: (FlightSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val updates: UpdateViewModel = viewModel()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlightSettingsSection(settings, onSettingsChange)
                HorizontalDivider()
                Text("Tello Controler v${updates.installedVersion}", fontWeight = FontWeight.Bold)
                Text("Left stick: yaw / throttle. Right stick: roll / pitch.")
                Text(
                    "A: take off. B: land. Y: cycle rate. " +
                        "Hold Menu ${FlightViewModel.EMERGENCY_HOLD_MS / 1000} s: EMERGENCY motor stop.",
                )
                UpdateSection(updates, updateAllowed)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } },
    )
}

@Composable
private fun FlightSettingsSection(settings: FlightSettings, onChange: (FlightSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Rate", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RateProfile.entries.forEach { rate ->
                if (rate == settings.rate) {
                    Button(onClick = {}) { Text(rate.name) }
                } else {
                    OutlinedButton(onClick = { onChange(settings.copy(rate = rate)) }) { Text(rate.name) }
                }
            }
        }

        Text("Stick dead-zone: ${(settings.deadZone * 100).roundToInt()} %", fontWeight = FontWeight.Bold)
        Slider(
            value = settings.deadZone,
            onValueChange = { onChange(settings.copy(deadZone = it)) },
            valueRange = FlightSettings.DEAD_ZONE_RANGE,
        )

        Text("Minimum battery for takeoff: ${settings.minTakeoffBatteryPercent} %", fontWeight = FontWeight.Bold)
        Slider(
            value = settings.minTakeoffBatteryPercent.toFloat(),
            onValueChange = { onChange(settings.copy(minTakeoffBatteryPercent = it.roundToInt())) },
            valueRange = FlightSettings.MIN_BATTERY_RANGE.first.toFloat()..FlightSettings.MIN_BATTERY_RANGE.last.toFloat(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Touch sticks (fallback without controller)", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Switch(checked = settings.touchSticks, onCheckedChange = { onChange(settings.copy(touchSticks = it)) })
        }
    }
}

@Composable
private fun UpdateSection(updates: UpdateViewModel, updateAllowed: Boolean) {
    val context = LocalContext.current
    val state by updates.state.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state is UpdateState.NeedsInstallPermission) {
                Button(onClick = { context.startActivity(updates.installPermissionIntent()) }) {
                    Text("ALLOW UPDATES")
                }
            } else {
                Button(enabled = updateAllowed && !state.busy, onClick = { updates.update() }) { Text("UPDATE") }
            }
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GitHubReleaseSource.RELEASES_PAGE)))
            }) { Text("RELEASES PAGE") }
        }
        val status = if (!updateAllowed) {
            "Updates are disabled while connected to the Tello. Restart the app on a Wi-Fi with Internet."
        } else when (val s = state) {
            UpdateState.Idle -> null
            UpdateState.Checking -> "Checking GitHub…"
            is UpdateState.UpToDate -> "Up to date (v${s.version})."
            UpdateState.NeedsInstallPermission -> "Allow Tello Controler to install updates, then come back and tap UPDATE."
            is UpdateState.Downloading -> "Downloading ${s.version}…"
            UpdateState.Installing -> "Installing…"
            UpdateState.AwaitingConfirmation -> "Confirm the installation in the system dialog."
            UpdateState.Installed -> "Updated. Reopen the app."
            is UpdateState.Failed -> "Update failed: ${s.message}"
        }
        if (state is UpdateState.Downloading) {
            val progress = (state as UpdateState.Downloading).progress
            if (progress != null) LinearProgressIndicator(progress = { progress }) else LinearProgressIndicator()
        }
        status?.let { Text(it, color = HudColors.Muted, fontSize = 12.sp) }
    }
}
