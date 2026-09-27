package com.miaouss90.tellocontroler.tello

import com.miaouss90.tellocontroler.controller.RcInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/** Tello SDK UDP transport: commands on 8889, state on 8890. See docs/PROTOCOL.md. */
class TelloClient {
    companion object {
        const val HOST = "192.168.10.1"
        const val COMMAND_PORT = 8889
        const val STATE_PORT = 8890
        const val ACK_TIMEOUT_MS = 3000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val host by lazy { InetAddress.getByName(HOST) }
    private var commandSocket: DatagramSocket? = null
    private var stateSocket: DatagramSocket? = null

    private val _telemetry = MutableStateFlow(TelloTelemetry())
    val telemetry: StateFlow<TelloTelemetry> = _telemetry.asStateFlow()

    private val _connection = MutableStateFlow(TelloConnectionState.DISCONNECTED)
    val connection: StateFlow<TelloConnectionState> = _connection.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    /** Enters SDK mode; CONNECTED only after the Tello acknowledges `command` with `ok`. */
    fun connect() {
        val state = _connection.value
        if (state == TelloConnectionState.CONNECTING || state == TelloConnectionState.CONNECTED) return
        scope.launch {
            _connection.value = TelloConnectionState.CONNECTING
            try {
                commandSocket?.close()
                commandSocket = DatagramSocket().apply { soTimeout = ACK_TIMEOUT_MS }
                val response = sendAndWait("command")
                _lastResponse.value = response
                if (response.trim().equals("ok", ignoreCase = true)) {
                    _connection.value = TelloConnectionState.CONNECTED
                    launch { listenState() }
                    send("streamon")
                } else {
                    _connection.value = TelloConnectionState.ERROR
                }
            } catch (_: Exception) {
                _connection.value = TelloConnectionState.ERROR
            }
        }
    }

    fun rc(input: RcInput) =
        send(TelloCommands.rc(input.roll, input.pitch, input.throttle, input.yaw))

    fun takeoff() = send("takeoff")
    fun land() = send("land")
    fun emergency() = send("emergency")

    fun send(command: String) {
        scope.launch {
            runCatching {
                val data = command.toByteArray()
                commandSocket?.send(DatagramPacket(data, data.size, host, COMMAND_PORT))
            }
        }
    }

    fun close() {
        runCatching { send(TelloCommands.rc(0, 0, 0, 0)) }
        commandSocket?.close()
        stateSocket?.close()
        _connection.value = TelloConnectionState.DISCONNECTED
        scope.cancel()
    }

    private fun sendAndWait(command: String): String {
        val socket = commandSocket ?: error("No command socket")
        val data = command.toByteArray()
        socket.send(DatagramPacket(data, data.size, host, COMMAND_PORT))
        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)
        socket.soTimeout = 0
        return String(packet.data, 0, packet.length)
    }

    private suspend fun listenState() = withContext(Dispatchers.IO) {
        runCatching {
            val socket = DatagramSocket(STATE_PORT).also { stateSocket = it }
            val buffer = ByteArray(2048)
            while (isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                _telemetry.value = TelloTelemetry.parse(String(packet.data, 0, packet.length))
            }
        }
    }
}
