package com.miaouss90.tellocontroler.tello
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.*

enum class TelloConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

class TelloClient {
 companion object { const val HOST="192.168.10.1"; const val COMMAND_PORT=8889; const val STATE_PORT=8890 }
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private var commandSocket:DatagramSocket?=null; private var stateSocket:DatagramSocket?=null
 private val _telemetry=MutableStateFlow(TelloTelemetry()); val telemetry:StateFlow<TelloTelemetry> = _telemetry
 private val _connection=MutableStateFlow(TelloConnectionState.DISCONNECTED); val connection:StateFlow<TelloConnectionState> = _connection
 private val _lastResponse=MutableStateFlow(""); val lastResponse:StateFlow<String> = _lastResponse
 fun connect(){ if(_connection.value==TelloConnectionState.CONNECTING||_connection.value==TelloConnectionState.CONNECTED)return; scope.launch {
  _connection.value=TelloConnectionState.CONNECTING
  try { val s=DatagramSocket(); s.soTimeout=3000; commandSocket=s; val response=sendAndWait("command"); _lastResponse.value=response
   if(response.trim().equals("ok",true)){ _connection.value=TelloConnectionState.CONNECTED; launch{listenState()}; send("streamon") }
   else _connection.value=TelloConnectionState.ERROR
  } catch(_:Exception){ _connection.value=TelloConnectionState.ERROR }
 }}
 private fun sendAndWait(command:String):String { val s=commandSocket?:error("No socket"); val d=command.toByteArray(); s.send(DatagramPacket(d,d.size,InetAddress.getByName(HOST),COMMAND_PORT)); val b=ByteArray(1024); val p=DatagramPacket(b,b.size); s.receive(p); s.soTimeout=0; return String(p.data,0,p.length) }
 fun send(command:String){ scope.launch{ runCatching{ val d=command.toByteArray(); commandSocket?.send(DatagramPacket(d,d.size,InetAddress.getByName(HOST),COMMAND_PORT)) }}}
 fun rc(r:Int,p:Int,t:Int,y:Int)=send("rc ${r.coerceIn(-100,100)} ${p.coerceIn(-100,100)} ${t.coerceIn(-100,100)} ${y.coerceIn(-100,100)}")
 fun takeoff()=send("takeoff"); fun land()=send("land"); fun emergency()=send("emergency")
 private suspend fun listenState()=withContext(Dispatchers.IO){ runCatching{ stateSocket=DatagramSocket(STATE_PORT); val b=ByteArray(2048); while(isActive){ val p=DatagramPacket(b,b.size); stateSocket!!.receive(p); _telemetry.value=TelloTelemetry.parse(String(p.data,0,p.length)) }}}
 fun close(){ runCatching{rc(0,0,0,0)}; commandSocket?.close(); stateSocket?.close(); _connection.value=TelloConnectionState.DISCONNECTED; scope.cancel() }
}