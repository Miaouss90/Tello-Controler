package com.miaouss90.tellocontroler
import androidx.lifecycle.ViewModel
import com.miaouss90.tellocontroler.controller.RcInput
import com.miaouss90.tellocontroler.controller.RcSafetyLoop
import com.miaouss90.tellocontroler.tello.TelloClient

class FlightViewModel:ViewModel(){
 private val client=TelloClient(); val telemetry=client.telemetry; val connection=client.connection; val lastResponse=client.lastResponse
 private val rcLoop=RcSafetyLoop{ i->client.rc(i.roll,i.pitch,i.throttle,i.yaw) }.also{it.start()}
 fun connect()=client.connect(); fun controllerInput(i:RcInput)=rcLoop.update(i); fun neutralControls()=rcLoop.neutral()
 fun takeoff()=client.takeoff(); fun land()=client.land(); fun emergency()=client.emergency()
 override fun onCleared(){rcLoop.stop();client.close();super.onCleared()}
}