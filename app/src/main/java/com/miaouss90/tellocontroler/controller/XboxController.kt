package com.miaouss90.tellocontroler.controller

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

/** Android adapter: reads gamepad events and delegates the math to [StickMapper]. */
object XboxController {
    fun motion(e: MotionEvent): RcInput? {
        if (e.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK) return null
        if (e.action != MotionEvent.ACTION_MOVE) return null
        return StickMapper.map(
            leftX = e.getAxisValue(MotionEvent.AXIS_X),
            leftY = e.getAxisValue(MotionEvent.AXIS_Y),
            rightX = e.getAxisValue(MotionEvent.AXIS_Z),
            rightY = e.getAxisValue(MotionEvent.AXIS_RZ),
        )
    }

    /** First press only: holding A/B must not re-send takeoff/land on key repeat. */
    fun isTakeoff(e: KeyEvent) = isFirstPress(e, KeyEvent.KEYCODE_BUTTON_A)
    fun isLand(e: KeyEvent) = isFirstPress(e, KeyEvent.KEYCODE_BUTTON_B)

    /** Menu / Start button; emergency requires holding it (see FlightViewModel). */
    fun isEmergencyButton(keyCode: Int) = keyCode == KeyEvent.KEYCODE_BUTTON_START

    fun isGamepad(device: InputDevice?): Boolean {
        val sources = device?.sources ?: return false
        return sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
    }

    private fun isFirstPress(e: KeyEvent, keyCode: Int) =
        e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0 && e.keyCode == keyCode
}
