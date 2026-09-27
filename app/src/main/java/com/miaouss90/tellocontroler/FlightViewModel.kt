package com.miaouss90.tellocontroler

import android.app.Application
import android.net.Network
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miaouss90.tellocontroler.controller.RcSafetyLoop
import com.miaouss90.tellocontroler.controller.StickAxes
import com.miaouss90.tellocontroler.controller.StickMapper
import com.miaouss90.tellocontroler.flight.FlightEvent
import com.miaouss90.tellocontroler.flight.FlightState
import com.miaouss90.tellocontroler.flight.FlightStateMachine
import com.miaouss90.tellocontroler.flight.TakeoffBlock
import com.miaouss90.tellocontroler.flight.TakeoffGuard
import com.miaouss90.tellocontroler.settings.FlightSettings
import com.miaouss90.tellocontroler.settings.SettingsRepository
import com.miaouss90.tellocontroler.tello.CommandResult
import com.miaouss90.tellocontroler.tello.LinkLevel
import com.miaouss90.tellocontroler.tello.LinkMonitor
import com.miaouss90.tellocontroler.tello.LinkQuality
import com.miaouss90.tellocontroler.tello.TelloClient
import com.miaouss90.tellocontroler.tello.TelloConnectionState
import com.miaouss90.tellocontroler.tello.TelloH264Decoder
import com.miaouss90.tellocontroler.tello.TelloTelemetry
import com.miaouss90.tellocontroler.tello.TelloVideoReceiver
import com.miaouss90.tellocontroler.tello.TelloWifiManager
import com.miaouss90.tellocontroler.tello.TelloWifiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramSocket

/** Orchestrates transport, safety loop, flight state, video and settings. Holds no Android UI references. */
class FlightViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        /** Emergency cuts the motors mid-air: it must be a deliberate, sustained press. */
        const val EMERGENCY_HOLD_MS = 1000L
        private const val NOTICE_MS = 4000L
        private const val MONITOR_PERIOD_MS = 250L
    }

    private val client = TelloClient()
    private val wifi = TelloWifiManager(app)
    private val settingsRepository = SettingsRepository(app)
    private val videoMonitor = LinkMonitor()
    private var videoReceiver: TelloVideoReceiver? = null
    private var decoder: TelloH264Decoder? = null
    private var emergencyJob: Job? = null
    private var noticeJob: Job? = null
    private var videoSurface: Surface? = null
    private var socketBinder: ((DatagramSocket) -> Unit)? = null

    /** The user asked to fly: keep reconnecting the SDK whenever the Tello Wi-Fi comes back. */
    @Volatile private var wantConnected = false

    // Input sources. Android only reports stick *changes*, so the controller position is held and
    // re-sent by the input pump while the controller is present and the app is in the foreground.
    @Volatile private var controllerAxes = StickAxes.NEUTRAL
    @Volatile private var touchAxes = StickAxes.NEUTRAL
    @Volatile private var touchLeftActive = false
    @Volatile private var touchRightActive = false
    @Volatile private var foreground = true

    val telemetry: StateFlow<TelloTelemetry> = client.telemetry
    val connection: StateFlow<TelloConnectionState> = client.connection
    val lastResponse: StateFlow<String> = client.lastResponse
    val stateLink: StateFlow<LinkQuality> = client.stateLink
    val settings: StateFlow<FlightSettings> = settingsRepository.settings
    val wifiState: StateFlow<TelloWifiState> = wifi.state

    private val _videoLink = MutableStateFlow(LinkQuality.NONE)
    val videoLink = _videoLink.asStateFlow()

    private val _flightState = MutableStateFlow(FlightState.LANDED)
    val flightState = _flightState.asStateFlow()

    private val _controllerConnected = MutableStateFlow(false)
    val controllerConnected = _controllerConnected.asStateFlow()

    private val _emergencyArming = MutableStateFlow(false)
    val emergencyArming = _emergencyArming.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    val takeoffBlock: StateFlow<TakeoffBlock?> =
        combine(connection, stateLink, telemetry, settings, flightState) { _, _, _, _, _ -> currentTakeoffBlock() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TakeoffBlock.NOT_CONNECTED)

    private val rcLoop = RcSafetyLoop(send = { client.rc(it) }).also { it.start() }

    init {
        viewModelScope.launch { telemetry.collect { reduce(FlightEvent.Height(it.heightCm)) } }
        viewModelScope.launch { wifi.network.collect { onTelloNetwork(it) } }
        viewModelScope.launch {
            wifi.state.collect {
                when (it) {
                    TelloWifiState.UNAVAILABLE -> {
                        wantConnected = false
                        showNotice("Tello Wi-Fi not found or not approved")
                    }
                    // Re-arm at once: the new request waits for the Tello access point to come back.
                    TelloWifiState.LOST -> if (wantConnected) wifi.request()
                    else -> Unit
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                _videoLink.value = videoMonitor.quality()
                delay(MONITOR_PERIOD_MS)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                pumpInput()
                delay(RcSafetyLoop.PERIOD_MS)
            }
        }
    }

    /** Locks the Tello Wi-Fi first; the SDK handshake starts once the network is available. */
    fun connect() {
        wantConnected = true
        if (wifi.state.value == TelloWifiState.LOCKED) client.connect() else wifi.request()
    }

    fun startVideo(surface: Surface) {
        stopVideo()
        videoSurface = surface
        videoMonitor.reset()
        decoder = TelloH264Decoder(surface).also { it.start() }
        startVideoReceiver()
    }

    fun stopVideo() {
        videoSurface = null
        videoReceiver?.stop()
        videoReceiver = null
        decoder?.stop()
        decoder = null
    }

    private fun startVideoReceiver() {
        videoReceiver?.stop()
        videoReceiver = TelloVideoReceiver(socketBinder) {
            videoMonitor.onPacket()
            decoder?.offer(it)
        }.also { it.start() }
    }

    private fun onTelloNetwork(network: Network?) {
        if (network == null) {
            if (socketBinder == null) return
            // SAFETY: the aircraft is unreachable; drop held input so nothing resumes on reconnection.
            socketBinder = null
            controllerAxes = StickAxes.NEUTRAL
            neutralControls()
            client.onNetworkLost()
            if (wantConnected) showNotice("Tello Wi-Fi lost, waiting for it to come back…")
            return
        }
        val binder: (DatagramSocket) -> Unit = { network.bindSocket(it) }
        socketBinder = binder
        client.useNetwork(binder)
        if (videoSurface != null) startVideoReceiver()
        if (wantConnected) client.connect()
    }

    fun setForeground(value: Boolean) {
        foreground = value
        if (!value) {
            controllerAxes = StickAxes.NEUTRAL
            touchAxes = StickAxes.NEUTRAL
            touchLeftActive = false
            touchRightActive = false
            neutralControls()
            emergencyReleased()
        }
    }

    fun setControllerConnected(connected: Boolean) {
        _controllerConnected.value = connected
        if (!connected) {
            controllerAxes = StickAxes.NEUTRAL
            neutralControls()
            emergencyReleased()
        }
    }

    fun controllerInput(axes: StickAxes) {
        _controllerConnected.value = true
        controllerAxes = axes
        pumpInput()
    }

    fun touchLeft(x: Float, y: Float, active: Boolean) {
        touchLeftActive = active
        touchAxes = touchAxes.copy(leftX = if (active) x else 0f, leftY = if (active) y else 0f)
        pumpInput()
    }

    fun touchRight(x: Float, y: Float, active: Boolean) {
        touchRightActive = active
        touchAxes = touchAxes.copy(rightX = if (active) x else 0f, rightY = if (active) y else 0f)
        pumpInput()
    }

    fun neutralControls() = rcLoop.neutral()

    fun updateSettings(settings: FlightSettings) = settingsRepository.update { settings }

    fun cycleRate() {
        settingsRepository.update { it.copy(rate = it.rate.next()) }
        showNotice("Rate: ${settings.value.rate.name}")
    }

    fun takeoff() {
        currentTakeoffBlock()?.let {
            showNotice(describe(it))
            return
        }
        reduce(FlightEvent.TakeoffSent)
        viewModelScope.launch {
            val result = client.takeoff()
            val airborne = result == CommandResult.Ok ||
                telemetry.value.heightCm >= FlightStateMachine.AIRBORNE_HEIGHT_CM
            reduce(FlightEvent.TakeoffDone(airborne))
            if (result != CommandResult.Ok) showNotice("Takeoff: ${describe(result)}")
        }
    }

    /** SAFETY: landing is never blocked by flight state or guards. */
    fun land() {
        reduce(FlightEvent.LandSent)
        viewModelScope.launch {
            when (val result = client.land()) {
                CommandResult.Ok -> reduce(FlightEvent.LandDone(ok = true))
                is CommandResult.Error -> {
                    reduce(FlightEvent.LandDone(ok = false))
                    showNotice("Land: ${describe(result)}")
                }
                CommandResult.Timeout, CommandResult.Unconfirmed -> Unit
            }
        }
    }

    fun emergencyPressed() {
        if (emergencyJob?.isActive == true) return
        _emergencyArming.value = true
        emergencyJob = viewModelScope.launch {
            delay(EMERGENCY_HOLD_MS)
            client.emergency()
            reduce(FlightEvent.EmergencySent)
            _emergencyArming.value = false
            showNotice("EMERGENCY motor stop sent")
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
        wifi.release()
        super.onCleared()
    }

    private fun pumpInput() {
        if (!foreground) return
        val s = settings.value
        val axes = when {
            touchLeftActive || touchRightActive -> touchAxes
            _controllerConnected.value -> controllerAxes
            else -> return
        }
        rcLoop.update(StickMapper.map(axes, s.deadZone, s.rate.scale))
    }

    private fun currentTakeoffBlock(): TakeoffBlock? {
        val link = stateLink.value.level
        return TakeoffGuard.check(
            connected = connection.value == TelloConnectionState.CONNECTED,
            telemetryFresh = link == LinkLevel.GOOD || link == LinkLevel.DEGRADED,
            batteryPercent = telemetry.value.batteryPercent,
            minBatteryPercent = settings.value.minTakeoffBatteryPercent,
            state = flightState.value,
        )
    }

    private fun describe(block: TakeoffBlock) = when (block) {
        TakeoffBlock.NOT_CONNECTED -> "Connect to the Tello first"
        TakeoffBlock.NO_TELEMETRY -> "No telemetry from the Tello"
        TakeoffBlock.NOT_LANDED -> "Already airborne"
        TakeoffBlock.LOW_BATTERY ->
            "Battery ${telemetry.value.batteryPercent}% below takeoff minimum ${settings.value.minTakeoffBatteryPercent}%"
    }

    private fun describe(result: CommandResult) = when (result) {
        CommandResult.Ok -> "ok"
        is CommandResult.Error -> result.message
        CommandResult.Timeout -> "no answer from the Tello"
        CommandResult.Unconfirmed -> "sent, not confirmed"
    }

    private fun reduce(event: FlightEvent) = _flightState.update { FlightStateMachine.reduce(it, event) }

    private fun showNotice(message: String) {
        _notice.value = message
        noticeJob?.cancel()
        noticeJob = viewModelScope.launch {
            delay(NOTICE_MS)
            _notice.value = null
        }
    }
}
