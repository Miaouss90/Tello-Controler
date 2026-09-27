package com.miaouss90.tellocontroler.controller

/** One Tello `rc a b c d` command. Every channel is in the SDK range -100..100. */
data class RcInput(
    val roll: Int = 0,
    val pitch: Int = 0,
    val throttle: Int = 0,
    val yaw: Int = 0,
) {
    val isNeutral: Boolean get() = roll == 0 && pitch == 0 && throttle == 0 && yaw == 0

    companion object {
        val NEUTRAL = RcInput()
    }
}
