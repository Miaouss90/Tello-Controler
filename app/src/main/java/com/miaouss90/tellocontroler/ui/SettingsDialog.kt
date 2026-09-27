package com.miaouss90.tellocontroler.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.miaouss90.tellocontroler.FlightViewModel

private const val RELEASES_URL = "https://github.com/Miaouss90/Tello-Controler/releases/latest"

@Composable
fun SettingsButton() {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("SETTINGS") }
    if (open) SettingsDialog(onDismiss = { open = false })
}

@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tello Controler v${context.versionName()}")
                Text("Left stick: yaw / throttle. Right stick: roll / pitch.")
                Text("A: take off. B: land. Hold Menu ${FlightViewModel.EMERGENCY_HOLD_MS / 1000} s: EMERGENCY motor stop.")
                Text("Updates are distributed through GitHub Releases.")
                Row {
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)))
                    }) { Text("CHECK FOR UPDATES") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } },
    )
}

private fun Context.versionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "?"
