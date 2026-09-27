package com.miaouss90.tellocontroler

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miaouss90.tellocontroler.controller.XboxController

class MainActivity:ComponentActivity() {
    private val vm by viewModels<FlightViewModel>()
    override fun onCreate(savedInstanceState:Bundle?){ super.onCreate(savedInstanceState); setContent { FlightScreen(vm) } }
    override fun onGenericMotionEvent(e:MotionEvent):Boolean {
        XboxController.motion(e)?.let { vm.rc(it.roll,it.pitch,it.throttle,it.yaw); return true }
        return super.onGenericMotionEvent(e)
    }
    override fun onKeyDown(keyCode:Int,event:KeyEvent):Boolean {
        when { XboxController.isTakeoff(event)->vm.takeoff(); XboxController.isLand(event)->vm.land(); else->return super.onKeyDown(keyCode,event) }
        return true
    }
}

@Composable fun FlightScreen(vm:FlightViewModel){
    val t by vm.telemetry.collectAsState()
    val connected by vm.connected.collectAsState()
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Text("TELLO CONTROLER",Modifier.align(Alignment.TopStart))
                Text(if(connected) "TELLO • SDK ACTIVE" else "DISCONNECTED",Modifier.align(Alignment.TopEnd))
                Text("VIDEO • decoder integration pending hardware validation",Modifier.align(Alignment.Center))
                Row(Modifier.align(Alignment.BottomStart),horizontalArrangement=Arrangement.spacedBy(24.dp)){
                    Text("BAT ${t.batteryPercent}%"); Text("ALT ${t.heightCm} cm"); Text("TOF ${t.tofCm} cm"); Text("YAW ${t.yaw}°")
                }
                Row(Modifier.align(Alignment.BottomEnd),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    if(!connected) Button(onClick={vm.connect()}){Text("CONNECT")}
                    Button(onClick={vm.takeoff()}){Text("TAKE OFF")}
                    Button(onClick={vm.land()}){Text("LAND")}
                }
            }
        }
    }
}
