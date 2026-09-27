package com.miaouss90.tellocontroler

import android.content.Context
import android.content.Intent
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.miaouss90.tellocontroler.controller.XboxController
import com.miaouss90.tellocontroler.tello.TelloConnectionState

private val Night=Color(0xFF070B10)
private val Panel=Color(0xCC101820)
private val Cyan=Color(0xFF58D6FF)
private val Green=Color(0xFF58E39B)
private val Red=Color(0xFFFF6B6B)
private val Muted=Color(0xFF8C99A8)

class MainActivity:ComponentActivity(), InputManager.InputDeviceListener {
    private val vm by viewModels<FlightViewModel>()
    private lateinit var inputManager:InputManager

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        inputManager=getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(this,null)
        refreshControllerState()
        setContent { FlightScreen(vm) }
    }

    override fun onResume(){ super.onResume(); refreshControllerState() }
    override fun onPause(){ vm.neutralControls(); super.onPause() }
    override fun onDestroy(){ inputManager.unregisterInputDeviceListener(this); super.onDestroy() }

    override fun onInputDeviceAdded(deviceId:Int)=refreshControllerState()
    override fun onInputDeviceRemoved(deviceId:Int)=refreshControllerState()
    override fun onInputDeviceChanged(deviceId:Int)=refreshControllerState()

    private fun refreshControllerState(){
        val present=InputDevice.getDeviceIds().any { id ->
            val d=InputDevice.getDevice(id)
            d != null && ((d.sources and InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD ||
                    (d.sources and InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK)
        }
        vm.setControllerConnected(present)
    }

    override fun onGenericMotionEvent(e:MotionEvent):Boolean {
        XboxController.motion(e)?.let { vm.controllerInput(it); return true }
        return super.onGenericMotionEvent(e)
    }

    override fun onKeyDown(keyCode:Int,event:KeyEvent):Boolean {
        when {
            XboxController.isTakeoff(event)->vm.takeoff()
            XboxController.isLand(event)->vm.land()
            else->return super.onKeyDown(keyCode,event)
        }
        return true
    }
}

@Composable
fun FlightScreen(vm:FlightViewModel){
    val t by vm.telemetry.collectAsState()
    val connection by vm.connection.collectAsState()
    val videoPackets by vm.videoPackets.collectAsState()
    val controllerConnected by vm.controllerConnected.collectAsState()
    val connected=connection==TelloConnectionState.CONNECTED
    val videoActive=videoPackets>0

    MaterialTheme(colorScheme=darkColorScheme(primary=Cyan,background=Night,surface=Panel)){
        Box(Modifier.fillMaxSize().background(Night)){
            AndroidView(
                factory={ context -> SurfaceView(context).apply {
                    holder.addCallback(object:SurfaceHolder.Callback{
                        override fun surfaceCreated(h:SurfaceHolder){vm.startVideo(h.surface)}
                        override fun surfaceChanged(h:SurfaceHolder,f:Int,w:Int,hgt:Int){}
                        override fun surfaceDestroyed(h:SurfaceHolder){vm.stopVideo()}
                    })
                }},
                modifier=Modifier.fillMaxSize()
            )

            if(!videoActive){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    Column(horizontalAlignment=Alignment.CenterHorizontally){
                        Text("TELLO",color=Color.White,fontSize=40.sp,fontWeight=FontWeight.Light)
                        Text(
                            when(connection){
                                TelloConnectionState.DISCONNECTED -> "CONNECT TO THE TELLO WI-FI, THEN PRESS CONNECT"
                                TelloConnectionState.CONNECTING -> "NEGOTIATING SDK CONNECTION…"
                                TelloConnectionState.CONNECTED -> "WAITING FOR VIDEO STREAM…"
                                TelloConnectionState.ERROR -> "TELLO NOT REACHABLE"
                            },
                            color=Muted,fontSize=11.sp
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically
            ){
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    StatusPill("TELLO",connected,if(connection==TelloConnectionState.ERROR) Red else Green)
                    StatusPill("VIDEO",videoActive,Green)
                    StatusPill("XBOX",controllerConnected,Green)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Metric("BAT","${t.batteryPercent}%")
                    Metric("ALT","${t.heightCm} cm")
                    Metric("TOF","${t.tofCm} cm")
                }
            }

            Column(
                Modifier.align(Alignment.BottomStart).padding(16.dp),
                verticalArrangement=Arrangement.spacedBy(6.dp)
            ){
                Text("FLIGHT DATA",color=Muted,fontSize=9.sp,fontWeight=FontWeight.Bold)
                Text("YAW ${t.yaw}°   •   PITCH ${t.pitch}°   •   ROLL ${t.roll}°",color=Color.White,fontSize=12.sp)
                if(videoActive) Text("VIDEO RX  $videoPackets packets",color=Muted,fontSize=9.sp)
            }

            Row(
                Modifier.align(Alignment.BottomEnd).padding(16.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ){
                SettingsButton()
                if(!connected) Button(onClick={vm.connect()}){Text("CONNECT")}
                Button(
                    enabled=connected,
                    onClick={ vm.takeoff() },
                    colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Night)
                ){Text("A  TAKE OFF",fontWeight=FontWeight.Bold)}
                OutlinedButton(enabled=connected,onClick={ vm.land() }){Text("B  LAND")}
            }
        }
    }
}

@Composable private fun StatusPill(label:String,ok:Boolean,okColor:Color){
    Surface(color=Panel,shape=RoundedCornerShape(18.dp)){
        Row(
            Modifier.padding(horizontal=11.dp,vertical=7.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(6.dp)
        ){
            Text("●",color=if(ok) okColor else Muted,fontSize=9.sp)
            Text(label,color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable private fun Metric(name:String,value:String){
    Surface(color=Panel,shape=RoundedCornerShape(11.dp)){
        Column(Modifier.padding(horizontal=12.dp,vertical=6.dp)){
            Text(name,color=Muted,fontSize=8.sp,fontWeight=FontWeight.Bold)
            Text(value,color=Color.White,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingsButton(){
    val context=LocalContext.current
    var open by remember{mutableStateOf(false)}
    TextButton(onClick={open=true}){Text("SETTINGS")}
    if(open) AlertDialog(
        onDismissRequest={open=false},
        title={Text("Settings")},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("Tello Controler V0.2.0")
            Text("Xbox control: left stick = yaw/throttle, right stick = roll/pitch.")
            Text("Updates are distributed through GitHub Releases.")
            OutlinedButton(onClick={
                context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/Miaouss90/Tello-Controler/releases/latest")))
            }){Text("CHECK FOR UPDATES")}
        }},
        confirmButton={TextButton(onClick={open=false}){Text("CLOSE")}}
    )
}
