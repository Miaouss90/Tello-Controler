package com.miaouss90.tellocontroler.tello
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket

/** UDP/11111 H.264 transport. Decoding is intentionally a separate concern. */
class TelloVideoReceiver(private val onPacket:(ByteArray)->Unit) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var socket:DatagramSocket?=null
    fun start() { scope.launch { runCatching {
        socket=DatagramSocket(11111); val buf=ByteArray(2048)
        while(isActive) { val p=DatagramPacket(buf,buf.size); socket!!.receive(p); onPacket(p.data.copyOf(p.length)) }
    }}}
    fun stop(){ socket?.close(); scope.cancel() }
}
