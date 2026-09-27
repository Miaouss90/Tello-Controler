package com.miaouss90.tellocontroler.controller

/** Raw stick positions in Android convention: -1..1, Y positive = stick pulled down. */
data class StickAxes(
    val leftX: Float = 0f,
    val leftY: Float = 0f,
    val rightX: Float = 0f,
    val rightY: Float = 0f,
) {
    companion object {
        val NEUTRAL = StickAxes()
    }
}
