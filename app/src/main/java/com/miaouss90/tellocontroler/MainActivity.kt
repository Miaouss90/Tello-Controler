package com.miaouss90.tellocontroler

import android.content.Context
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.miaouss90.tellocontroler.controller.XboxController
import com.miaouss90.tellocontroler.ui.FlightScreen

/** Routes Android input/lifecycle events to [FlightViewModel]. UI lives in the `ui` package. */
class MainActivity : ComponentActivity(), InputManager.InputDeviceListener {
    private val vm by viewModels<FlightViewModel>()
    private lateinit var inputManager: InputManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(this, null)
        refreshControllerState()
        setContent { FlightScreen(vm) }
    }

    override fun onResume() {
        super.onResume()
        refreshControllerState()
    }

    // SAFETY: never keep flying on stale sticks while the app is not in the foreground.
    override fun onPause() {
        vm.neutralControls()
        vm.emergencyReleased()
        super.onPause()
    }

    override fun onDestroy() {
        inputManager.unregisterInputDeviceListener(this)
        super.onDestroy()
    }

    override fun onInputDeviceAdded(deviceId: Int) = refreshControllerState()
    override fun onInputDeviceRemoved(deviceId: Int) = refreshControllerState()
    override fun onInputDeviceChanged(deviceId: Int) = refreshControllerState()

    private fun refreshControllerState() {
        val present = InputDevice.getDeviceIds().any { XboxController.isGamepad(InputDevice.getDevice(it)) }
        vm.setControllerConnected(present)
    }

    override fun onGenericMotionEvent(e: MotionEvent): Boolean {
        XboxController.motion(e)?.let {
            vm.controllerInput(it)
            return true
        }
        return super.onGenericMotionEvent(e)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when {
            XboxController.isEmergencyButton(keyCode) -> vm.emergencyPressed()
            XboxController.isTakeoff(event) -> vm.takeoff()
            XboxController.isLand(event) -> vm.land()
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (XboxController.isEmergencyButton(keyCode)) {
            vm.emergencyReleased()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
