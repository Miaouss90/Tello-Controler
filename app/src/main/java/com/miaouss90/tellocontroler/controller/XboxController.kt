package com.miaouss90.tellocontroler.controller

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

data class RcInput(val roll:Int=0,val pitch:Int=0,val throttle:Int=0,val yaw:Int=0)

object XboxController {
    private const val DEAD_ZONE=.08f
    private fun axis(e:MotionEvent,a:Int):Float {
        val v=e.getAxisValue(a)
        return if(abs(v)<DEAD_ZONE) 0f else v
    }
    fun motion(e:MotionEvent):RcInput? {
        if(e.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK) return null
        return RcInput(
            roll=(axis(e,MotionEvent.AXIS_X)*100).toInt(),
            pitch=(-axis(e,MotionEvent.AXIS_Y)*100).toInt(),
            throttle=(-axis(e,MotionEvent.AXIS_RZ)*100).toInt(),
            yaw=(axis(e,MotionEvent.AXIS_Z)*100).toInt()
        )
    }
    fun isTakeoff(e:KeyEvent)=e.action==KeyEvent.ACTION_DOWN && e.keyCode==KeyEvent.KEYCODE_BUTTON_A
    fun isLand(e:KeyEvent)=e.action==KeyEvent.ACTION_DOWN && e.keyCode==KeyEvent.KEYCODE_BUTTON_B
}
