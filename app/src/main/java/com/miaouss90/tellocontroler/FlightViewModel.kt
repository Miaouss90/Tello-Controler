package com.miaouss90.tellocontroler

import androidx.lifecycle.ViewModel
import com.miaouss90.tellocontroler.tello.TelloClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlightViewModel:ViewModel() {
    private val client=TelloClient()
    val telemetry=client.telemetry
    private val _connected=MutableStateFlow(false); val connected=_connected.asStateFlow()
    fun connect(){ client.connect(); _connected.value=true }
    fun rc(r:Int,p:Int,t:Int,y:Int)=client.rc(r,p,t,y)
    fun takeoff()=client.takeoff()
    fun land()=client.land()
    fun emergency()=client.emergency()
    override fun onCleared(){ client.close(); super.onCleared() }
}
