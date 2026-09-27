package com.miaouss90.tellocontroler.controller

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.InputDevice

/**
 * Rumbles the connected gamepad. Silently does nothing when the controller or Android does not expose a
 * vibrator (Xbox rumble over Bluetooth depends on the Android version). HARDWARE-UNVERIFIED.
 */
object ControllerRumble {
    fun play(alert: FlightAlert) {
        val vibrator = gamepadVibrator() ?: return
        val timings = when (alert) {
            FlightAlert.EMERGENCY_ARMING -> longArrayOf(0, 120)
            FlightAlert.BATTERY_LOW -> longArrayOf(0, 200, 150, 200)
            FlightAlert.LINK_LOST -> longArrayOf(0, 400, 150, 400, 150, 400)
            FlightAlert.BATTERY_CRITICAL -> longArrayOf(0, 150, 100, 150, 100, 150, 100, 150)
        }
        runCatching { vibrator.vibrate(VibrationEffect.createWaveform(timings, -1)) }
    }

    private fun gamepadVibrator(): Vibrator? {
        val device = InputDevice.getDeviceIds()
            .mapNotNull { InputDevice.getDevice(it) }
            .firstOrNull { XboxController.isGamepad(it) }
            ?: return null
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            device.vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            device.vibrator
        }
        return vibrator.takeIf { it.hasVibrator() }
    }
}
