package com.miaouss90.tellocontroler.tello

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.*

class TelloClient {
    companion object { const val HOST="192.168.10.1"; const val COMMAND_PORT=8889; const val STATE_PORT=8890 }
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var commandSocket: DatagramSocket?=null
    private var stateSocket: DatagramSocket?=null
    private val _telemetry=MutableStateFlow(TelloTelemetry())
    val telemetry: StateFlow<TelloTelemetry> = _telemetry

    fun connect() {
        if(commandSocket!=null) return
        commandSocket=DatagramSocket()
        send("command")
        send("streamon")
        scope.launch { listenState() }
    }
    fun send(command:String) {
        scope.launch {
            runCatching {
                val data=command.toByteArray()
                commandSocket?.send(DatagramPacket(data,data.size,InetAddress.getByName(HOST),COMMAND_PORT))
            }
        }
    }
    fun rc(roll:Int,pitch:Int,throttle:Int,yaw:Int)=
        send("rc ${roll.coerceIn(-100,100)} ${pitch.coerceIn(-100,100)} ${throttle.coerceIn(-100,100)} ${yaw.coerceIn(-100,100)}")
    fun takeoff()=send("takeoff")
    fun land()=send("land")
    fun emergency()=send("emergency")

    private suspend fun listenState()=withContext(Dispatchers.IO) {
        runCatching {
            stateSocket=DatagramSocket(STATE_PORT)
            val buf=ByteArray(2048)
            while(isActive) {
                val p=DatagramPacket(buf,buf.size); stateSocket!!.receive(p)
                _telemetry.value=TelloTelemetry.parse(String(p.data,0,p.length))
            }
        }
    }
    fun close() { runCatching { rc(0,0,0,0) }; commandSocket?.close(); stateSocket?.close(); scope.cancel() }
}
