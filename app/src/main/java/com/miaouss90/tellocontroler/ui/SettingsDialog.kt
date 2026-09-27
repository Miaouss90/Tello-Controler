package com.miaouss90.tellocontroler.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    // Full screen, two columns: a small AlertDialog is far too short in landscape.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = HudColors.Night, contentColor = Color.White) {
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Settings", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("v${updates.installedVersion}", color = HudColors.Muted, fontSize = 12.sp)
                    TextButton(onClick = onDismiss) { Text("CLOSE") }
                }
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    SettingsColumn(Modifier.weight(1f)) {
                        FlightSettingsSection(settings, onSettingsChange)
                        SectionTitle("Recording")
                        ToggleRow("Flight log (CSV per flight, saved in Download/TelloControler)", settings.flightLogs) {
                            onSettingsChange(settings.copy(flightLogs = it))
                        }
                    }
                    SettingsColumn(Modifier.weight(1f)) {
                        HudSettingsSection(settings, onSettingsChange)
                        SectionTitle("Controls")
                        Text("Left stick: yaw / throttle · Right stick: roll / pitch", fontSize = 13.sp)
                        Text(
                            "A take off · B land · Y rate · X photo · View record · " +
                                "hold Menu ${FlightViewModel.EMERGENCY_HOLD_MS / 1000} s = EMERGENCY motor stop",
                            fontSize = 13.sp,
                        )
                        SectionTitle("Update")
                        UpdateSection(updates, updateAllowed)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        Modifier.padding(top = 8.dp),
        color = HudColors.Cyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun FlightSettingsSection(settings: FlightSettings, onChange: (FlightSettings) -> Unit) {
    SectionTitle("Flight")
    Text("Rate")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RateProfile.entries.forEach { rate ->
            if (rate == settings.rate) {
                Button(onClick = {}) { Text(rate.name) }
            } else {
                OutlinedButton(onClick = { onChange(settings.copy(rate = rate)) }) { Text(rate.name) }
            }
        }
    }
    Text("Stick dead-zone: ${(settings.deadZone * 100).roundToInt()} %")
    Slider(
        value = settings.deadZone,
        onValueChange = { onChange(settings.copy(deadZone = it)) },
        valueRange = FlightSettings.DEAD_ZONE_RANGE,
    )
    Text("Minimum battery for takeoff: ${settings.minTakeoffBatteryPercent} %")
    Slider(
        value = settings.minTakeoffBatteryPercent.toFloat(),
        onValueChange = { onChange(settings.copy(minTakeoffBatteryPercent = it.roundToInt())) },
        valueRange = FlightSettings.MIN_BATTERY_RANGE.first.toFloat()..FlightSettings.MIN_BATTERY_RANGE.last.toFloat(),
    )
}

@Composable
private fun HudSettingsSection(settings: FlightSettings, onChange: (FlightSettings) -> Unit) {
    SectionTitle("HUD & controller")
    ToggleRow("Artificial horizon", settings.hudHorizon) { onChange(settings.copy(hudHorizon = it)) }
    ToggleRow("Heading tape", settings.hudHeading) { onChange(settings.copy(hudHeading = it)) }
    ToggleRow("Central reticle", settings.hudReticle) { onChange(settings.copy(hudReticle = it)) }
    ToggleRow("Controller rumble on alerts", settings.rumbleAlerts) { onChange(settings.copy(rumbleAlerts = it)) }
    ToggleRow("Always show touch sticks (automatic without a controller)", settings.touchSticks) {
        onChange(settings.copy(touchSticks = it))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
