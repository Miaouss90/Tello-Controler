package com.miaouss90.tellocontroler.tello

/** Pure builders for Tello SDK command strings. */
object TelloCommands {
    const val RC_MIN = -100
    const val RC_MAX = 100

    /** `rc a b c d` = left/right (roll), forward/back (pitch), up/down (throttle), yaw. */
    fun rc(roll: Int, pitch: Int, throttle: Int, yaw: Int) =
        "rc ${clamp(roll)} ${clamp(pitch)} ${clamp(throttle)} ${clamp(yaw)}"

    private fun clamp(v: Int) = v.coerceIn(RC_MIN, RC_MAX)
}
