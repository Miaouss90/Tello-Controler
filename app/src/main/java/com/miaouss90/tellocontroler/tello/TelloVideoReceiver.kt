package com.miaouss90.tellocontroler.tello

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket

/** UDP/11111 H.264 transport. Decoding is intentionally a separate concern (ADR-004). */
class TelloVideoReceiver(private val onPacket: (ByteArray) -> Unit) {
    companion object {
        const val VIDEO_PORT = 11111
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: DatagramSocket? = null

    fun start() {
        scope.launch {
            runCatching {
                val s = DatagramSocket(VIDEO_PORT).also { socket = it }
                val buffer = ByteArray(2048)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    s.receive(packet)
                    onPacket(packet.data.copyOf(packet.length))
                }
            }
        }
    }

    fun stop() {
        socket?.close()
        scope.cancel()
    }
}
