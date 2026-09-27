package com.miaouss90.tellocontroler.tello

/** Parsed Tello state packet (UDP 8890), e.g. `pitch:0;roll:0;yaw:0;...;bat:87;...`. */
data class TelloTelemetry(
    val pitch: Int = 0,
    val roll: Int = 0,
    val yaw: Int = 0,
    val heightCm: Int = 0,
    val batteryPercent: Int = 0,
    val flightTimeSeconds: Int = 0,
    val tofCm: Int = 0,
    val temperatureC: Double = 0.0,
    /** Velocity `vgx/vgy/vgz`, raw SDK units (dm/s per SDK 3.0). HARDWARE-UNVERIFIED unit. */
    val speedX: Int = 0,
    val speedY: Int = 0,
    val speedZ: Int = 0,
) {
    companion object {
        fun parse(raw: String): TelloTelemetry {
            val values = raw.trim().split(";").mapNotNull {
                val pair = it.split(":", limit = 2)
                if (pair.size == 2) pair[0].trim() to pair[1].trim() else null
            }.toMap()

            fun int(key: String) = values[key]?.toIntOrNull() ?: 0

            return TelloTelemetry(
                pitch = int("pitch"),
                roll = int("roll"),
                yaw = int("yaw"),
                heightCm = int("h"),
                batteryPercent = int("bat"),
                flightTimeSeconds = int("time"),
                tofCm = int("tof"),
                temperatureC = listOf(int("templ"), int("temph")).average(),
                speedX = int("vgx"),
                speedY = int("vgy"),
                speedZ = int("vgz"),
            )
        }
    }
}
