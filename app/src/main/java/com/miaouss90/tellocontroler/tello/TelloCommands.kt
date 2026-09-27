package com.miaouss90.tellocontroler.tello

sealed interface CommandResult {
    data object Ok : CommandResult
    data class Error(val message: String) : CommandResult
    data object Timeout : CommandResult

    /** Sent immediately without waiting for an answer (another command held the ack channel). */
    data object Unconfirmed : CommandResult
}

/** Pure builders/parsers for Tello SDK command strings. */
object TelloCommands {
    const val RC_MIN = -100
    const val RC_MAX = 100

    /** `rc a b c d` = left/right (roll), forward/back (pitch), up/down (throttle), yaw. */
    fun rc(roll: Int, pitch: Int, throttle: Int, yaw: Int) =
        "rc ${clamp(roll)} ${clamp(pitch)} ${clamp(throttle)} ${clamp(yaw)}"

    fun parseResponse(response: String): CommandResult {
        val text = response.trim()
        return if (text.equals("ok", ignoreCase = true)) CommandResult.Ok else CommandResult.Error(text.ifEmpty { "empty response" })
    }

    private fun clamp(v: Int) = v.coerceIn(RC_MIN, RC_MAX)
}
