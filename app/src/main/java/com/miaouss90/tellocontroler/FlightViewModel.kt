package com.miaouss90.tellocontroler

import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaouss90.tellocontroler.controller.RcInput
import com.miaouss90.tellocontroler.controller.RcSafetyLoop
import com.miaouss90.tellocontroler.tello.TelloClient
import com.miaouss90.tellocontroler.tello.TelloH264Decoder
import com.miaouss90.tellocontroler.tello.TelloVideoReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Orchestrates transport, safety loop, video and UI state. Holds no Android UI references. */
class FlightViewModel : ViewModel() {
    companion object {
        /** Emergency cuts the motors mid-air: it must be a deliberate, sustained press. */
        const val EMERGENCY_HOLD_MS = 1000L
    }

    private val client = TelloClient()
    private var videoReceiver: TelloVideoReceiver? = null
    private var decoder: TelloH264Decoder? = null
    private var emergencyJob: Job? = null

    val telemetry = client.telemetry
    val connection = client.connection
    val lastResponse = client.lastResponse

    private val _videoPackets = MutableStateFlow(0L)
    val videoPackets = _videoPackets.asStateFlow()

    private val _controllerConnected = MutableStateFlow(false)
    val controllerConnected = _controllerConnected.asStateFlow()

    private val _emergencyArming = MutableStateFlow(false)
    val emergencyArming = _emergencyArming.asStateFlow()

    private val rcLoop = RcSafetyLoop(send = { client.rc(it) }).also { it.start() }

    fun connect() = client.connect()

    fun startVideo(surface: Surface) {
        stopVideo()
        _videoPackets.value = 0
        decoder = TelloH264Decoder(surface).also { it.start() }
        videoReceiver = TelloVideoReceiver {
            _videoPackets.value = _videoPackets.value + 1
            decoder?.offer(it)
        }.also { it.start() }
    }

    fun stopVideo() {
        videoReceiver?.stop()
        videoReceiver = null
        decoder?.stop()
        decoder = null
    }

    fun setControllerConnected(connected: Boolean) {
        _controllerConnected.value = connected
        if (!connected) {
            neutralControls()
            emergencyReleased()
        }
    }

    fun controllerInput(input: RcInput) {
        _controllerConnected.value = true
        rcLoop.update(input)
    }

    fun neutralControls() = rcLoop.neutral()

    fun takeoff() = client.takeoff()
    fun land() = client.land()

    fun emergencyPressed() {
        if (emergencyJob?.isActive == true) return
        _emergencyArming.value = true
        emergencyJob = viewModelScope.launch {
            delay(EMERGENCY_HOLD_MS)
            client.emergency()
            _emergencyArming.value = false
        }
    }

    fun emergencyReleased() {
        emergencyJob?.cancel()
        emergencyJob = null
        _emergencyArming.value = false
    }

    override fun onCleared() {
        emergencyReleased()
        stopVideo()
        rcLoop.stop()
        client.close()
        super.onCleared()
    }
}
