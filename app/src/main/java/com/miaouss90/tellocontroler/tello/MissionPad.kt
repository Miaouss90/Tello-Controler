package com.miaouss90.tellocontroler.tello

/**
 * A Tello EDU Mission Pad seen by the downward camera. x/y/z: aircraft position relative to the pad
 * center, in cm (SDK 2.0 `mid`, `x`, `y`, `z`; `mid` is -1 when none is seen, -2 when detection is off).
 */
data class MissionPad(val id: Int, val xCm: Int, val yCm: Int, val zCm: Int)
