package com.miaouss90.tellocontroler.tello

data class TelloTelemetry(
    val pitch: Int = 0, val roll: Int = 0, val yaw: Int = 0,
    val heightCm: Int = 0, val batteryPercent: Int = 0,
    val flightTimeSeconds: Int = 0, val tofCm: Int = 0,
    val temperatureC: Double = 0.0
) {
    companion object {
        fun parse(raw: String): TelloTelemetry {
            val v = raw.trim().split(";").mapNotNull {
                val p=it.split(":",limit=2); if(p.size==2) p[0] to p[1] else null
            }.toMap()
            fun i(k:String)=v[k]?.toIntOrNull() ?: 0
            return TelloTelemetry(i("pitch"),i("roll"),i("yaw"),i("h"),i("bat"),i("time"),i("tof"),
                listOf(i("templ"),i("temph")).average())
        }
    }
}
