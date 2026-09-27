package com.miaouss90.tellocontroler

import android.view.Surface
import androidx.lifecycle.ViewModel
import com.miaouss90.tellocontroler.controller.RcInput
import com.miaouss90.tellocontroler.controller.RcSafetyLoop
import com.miaouss90.tellocontroler.tello.TelloClient
import com.miaouss90.tellocontroler.tello.TelloH264Decoder
import com.miaouss90.tellocontroler.tello.TelloVideoReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlightViewModel:ViewModel(){
    private val client=TelloClient()
    private var videoReceiver:TelloVideoReceiver?=null
    private var decoder:TelloH264Decoder?=null

    val telemetry=client.telemetry
    val connection=client.connection
    val lastResponse=client.lastResponse

    private val _videoPackets=MutableStateFlow(0L)
    val videoPackets=_videoPackets.asStateFlow()
    private val _controllerConnected=MutableStateFlow(false)
    val controllerConnected=_controllerConnected.asStateFlow()

    private val rcLoop=RcSafetyLoop { i -> client.rc(i.roll,i.pitch,i.throttle,i.yaw) }.also { it.start() }

    fun connect()=client.connect()

    fun startVideo(surface:Surface){
        stopVideo()
        _videoPackets.value=0
        decoder=TelloH264Decoder(surface).also{it.start()}
        videoReceiver=TelloVideoReceiver {
            _videoPackets.value=_videoPackets.value+1
            decoder?.offer(it)
        }.also{it.start()}
    }

    fun stopVideo(){
        videoReceiver?.stop(); videoReceiver=null
        decoder?.stop(); decoder=null
    }

    fun setControllerConnected(connected:Boolean){
        _controllerConnected.value=connected
        if(!connected) neutralControls()
    }

    fun controllerInput(i:RcInput){
        _controllerConnected.value=true
        rcLoop.update(i)
    }

    fun neutralControls()=rcLoop.neutral()
    fun takeoff()=client.takeoff()
    fun land()=client.land()
    fun emergency()=client.emergency()

    override fun onCleared(){
        stopVideo()
        rcLoop.stop()
        client.close()
        super.onCleared()
    }
}
