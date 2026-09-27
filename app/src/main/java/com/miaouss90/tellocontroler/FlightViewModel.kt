package com.miaouss90.tellocontroler
import androidx.lifecycle.ViewModel
import android.view.Surface
import com.miaouss90.tellocontroler.tello.TelloH264Decoder
import com.miaouss90.tellocontroler.tello.TelloVideoReceiver
import com.miaouss90.tellocontroler.controller.RcInput
import com.miaouss90.tellocontroler.controller.RcSafetyLoop
import com.miaouss90.tellocontroler.tello.TelloClient

class FlightViewModel:ViewModel(){
 private val client=TelloClient(); private var videoReceiver:TelloVideoReceiver?=null; private var decoder:TelloH264Decoder?=null; val telemetry=client.telemetry; val connection=client.connection; val lastResponse=client.lastResponse
 private val rcLoop=RcSafetyLoop{ i->client.rc(i.roll,i.pitch,i.throttle,i.yaw) }.also{it.start()}
 fun connect()=client.connect()
 fun startVideo(surface:Surface){ stopVideo(); decoder=TelloH264Decoder(surface).also{it.start()}; videoReceiver=TelloVideoReceiver{decoder?.offer(it)}.also{it.start()} }
 fun stopVideo(){videoReceiver?.stop();videoReceiver=null;decoder?.stop();decoder=null}
 fun controllerInput(i:RcInput)=rcLoop.update(i); fun neutralControls()=rcLoop.neutral()
 fun takeoff()=client.takeoff(); fun land()=client.land(); fun emergency()=client.emergency()
 override fun onCleared(){stopVideo();rcLoop.stop();client.close();super.onCleared()}
}