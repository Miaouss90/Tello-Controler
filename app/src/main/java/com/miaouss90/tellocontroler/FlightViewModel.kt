package com.miaouss90.tellocontroler

import android.app.Application
import android.graphics.Bitmap
import android.net.Network
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miaouss90.tellocontroler.controller.AlertInputs
import com.miaouss90.tellocontroler.controller.AlertMonitor
import com.miaouss90.tellocontroler.controller.ControllerRumble
import com.miaouss90.tellocontroler.controller.FlightAlert
import com.miaouss90.tellocontroler.controller.RcInput
import com.miaouss90.tellocontroler.controller.RcSafetyLoop
import com.miaouss90.tellocontroler.controller.RcShaper
import com.miaouss90.tellocontroler.controller.StickAxes
import com.miaouss90.tellocontroler.controller.StickMapper
import com.miaouss90.tellocontroler.flight.FlightEvent
import com.miaouss90.tellocontroler.flight.FlightState
import com.miaouss90.tellocontroler.flight.FlightStateMachine
import com.miaouss90.tellocontroler.flight.MotorStopDetector
import com.miaouss90.tellocontroler.flight.TakeoffBlock
import com.miaouss90.tellocontroler.flight.TakeoffGuard
import com.miaouss90.tellocontroler.record.FlightRecorder
import com.miaouss90.tellocontroler.record.MediaStorage
import com.miaouss90.tellocontroler.record.VideoRecorder
import com.miaouss90.tellocontroler.settings.FlightProfiles
import com.miaouss90.tellocontroler.settings.FlightSettings
import com.miaouss90.tellocontroler.settings.SettingsRepository
import com.miaouss90.tellocontroler.tello.CommandResult
import com.miaouss90.tellocontroler.tello.LinkLevel
import com.miaouss90.tellocontroler.tello.LinkMonitor
import com.miaouss90.tellocontroler.tello.LinkQuality
import com.miaouss90.tellocontroler.tello.NalSplitter
import com.miaouss90.tellocontroler.tello.TelloClient
import com.miaouss90.tellocontroler.tello.TelloConnectionState
import com.miaouss90.tellocontroler.tello.TelloH264Decoder
import com.miaouss90.tellocontroler.tello.TelloTelemetry
import com.miaouss90.tellocontroler.tello.TelloVideoReceiver
import com.miaouss90.tellocontroler.tello.TelloWifiManager
import com.miaouss90.tellocontroler.tello.TelloWifiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.net.DatagramSocket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Orchestrates transport, safety loop, flight state, video and settings. Holds no Android UI references. */
class FlightViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        /** Emergency cuts the motors mid-air: it must be a deliberate, sustained press. */
        const val EMERGENCY_HOLD_MS = 1000L
        private const val NOTICE_MS = 4000L
        private const val MONITOR_PERIOD_MS = 250L
        private const val FLIGHT_LOG_PERIOD_MS = 100L
    }

    private val client = TelloClient()
    private val wifi = TelloWifiManager(app)
    private val settingsRepository = SettingsRepository(app)
    private val videoMonitor = LinkMonitor()
    private val motorStopDetector = MotorStopDetector()
    private val nalSplitter = NalSplitter()
    private val storage = MediaStorage(app)
    private val flightRecorder = FlightRecorder(File(app.cacheDir, "flights"))
    @Volatile private var videoRecorder: VideoRecorder? = null
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

    /** The RC command actually sent to the aircraft (after staleness checks), for the HUD stick indicators. */
    private val _rcOutput = MutableStateFlow(RcInput.NEUTRAL)
    val rcOutput = _rcOutput.asStateFlow()

    private val _flightState = MutableStateFlow(FlightState.LANDED)
    val flightState = _flightState.asStateFlow()

    /** Wall-clock time the motors started for the current flight, null on the ground (HUD flight timer). */
    private val _flightStartedAt = MutableStateFlow<Long?>(null)
    val flightStartedAt = _flightStartedAt.asStateFlow()

    private val _controllerConnected = MutableStateFlow(false)
    val controllerConnected = _controllerConnected.asStateFlow()

    private val _emergencyArming = MutableStateFlow(false)
    val emergencyArming = _emergencyArming.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    /** Wall-clock start of the current video recording, null when not recording. */
    private val _recordingSince = MutableStateFlow<Long?>(null)
    val recordingSince = _recordingSince.asStateFlow()

    /** The UI owns the SurfaceView, so it performs the frame grab when asked. */
    private val _photoRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val photoRequests: SharedFlow<Unit> = _photoRequests

    val takeoffBlock: StateFlow<TakeoffBlock?> =
        combine(connection, stateLink, telemetry, settings, flightState) { _, _, _, _, _ -> currentTakeoffBlock() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TakeoffBlock.NOT_CONNECTED)

    private val rcShaper = RcShaper()
    private var wasHeightLimited = false

    private val rcLoop = RcSafetyLoop(
        send = {
            client.rc(it)
            _rcOutput.value = it
            if (rcShaper.heightLimited && !wasHeightLimited) {
                showNotice("Height limit ${FlightProfiles.resolve(settings.value).maxHeightCm} cm reached")
            }
            wasHeightLimited = rcShaper.heightLimited
        },
        shape = {
            val profile = FlightProfiles.resolve(settings.value)
            rcShaper.shape(it, profile.smoothing, profile.maxHeightCm, telemetry.value.heightCm)
        },
        onNeutralized = { rcShaper.reset() },
    ).also { it.start() }

    init {
        viewModelScope.launch { telemetry.collect { reduce(FlightEvent.Height(it.heightCm)) } }
        viewModelScope.launch { wifi.network.collect { onTelloNetwork(it) } }
        viewModelScope.launch {
            // Applied on every (re)connection and whenever the setting changes.
            combine(connection, settings) { c, s -> (c == TelloConnectionState.CONNECTED) to s.missionPads }
                .distinctUntilChanged()
                .collect { (connected, enabled) ->
                    if (!connected) return@collect
                    val result = client.setMissionPads(enabled)
                    if (enabled && result != CommandResult.Ok) {
                        showNotice("Mission Pads unavailable: ${describe(result)} (Tello EDU / SDK 2.0 only)")
                    }
                }
        }
        viewModelScope.launch {
            var previous: AlertInputs? = null
            combine(connection, wifi.state, telemetry, flightState, emergencyArming) { c, w, t, f, arming ->
                AlertInputs(
                    linkLost = c == TelloConnectionState.LINK_LOST || w == TelloWifiState.LOST,
                    airborne = f != FlightState.LANDED,
                    batteryPercent = t.batteryPercent,
                    lowBatteryPercent = settings.value.minTakeoffBatteryPercent,
                    emergencyArming = arming,
                )
            }.collect { current ->
                AlertMonitor.alerts(previous, current).forEach { onAlert(it) }
                previous = current
            }
        }
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
                checkMotorsStopped()
                delay(MONITOR_PERIOD_MS)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                pumpInput()
                delay(RcSafetyLoop.PERIOD_MS)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                recordFlightSample()
                delay(FLIGHT_LOG_PERIOD_MS)
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
        nalSplitter.reset()
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
        videoReceiver = TelloVideoReceiver(socketBinder) { chunk ->
            videoMonitor.onPacket()
            val endOfFrame = chunk.size < TelloVideoReceiver.FULL_PACKET_BYTES
            nalSplitter.push(chunk, endOfFrame).forEach { nal ->
                decoder?.offerNal(nal)
                videoRecorder?.onNal(nal)
            }
            if (endOfFrame) {
                decoder?.endOfFrame()
                videoRecorder?.endOfFrame()
            }
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

    /** Pilot says the aircraft is on the ground: re-enables takeoff when no automatic signal did. */
    fun markLanded() = reduce(FlightEvent.ManualLanded)

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

    fun requestPhoto() {
        if (videoLink.value.level != LinkLevel.GOOD) {
            showNotice("No video to photograph")
            return
        }
        _photoRequests.tryEmit(Unit)
    }

    fun onPhotoCaptured(bitmap: Bitmap?) {
        if (bitmap == null) {
            showNotice("Photo failed")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { storage.savePhoto(bitmap, "tello-${timestamp()}.jpg") }
                .onSuccess { showNotice("Photo saved: $it") }
                .onFailure { showNotice("Photo failed: ${it.message}") }
        }
    }

    fun toggleRecording() {
        if (videoRecorder != null) stopRecording() else startRecording()
    }

    private fun startRecording() {
        runCatching { VideoRecorder(storage.createVideo("tello-${timestamp()}.mp4")) }
            .onSuccess {
                videoRecorder = it
                _recordingSince.value = System.currentTimeMillis()
                showNotice("Recording (starts at the next key frame)")
            }
            .onFailure { showNotice("Cannot record: ${it.message}") }
    }

    private fun stopRecording() {
        val recorder = videoRecorder ?: return
        videoRecorder = null
        _recordingSince.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val saved = recorder.stop()
            showNotice(if (saved) "Video saved in Movies/${MediaStorage.FOLDER}" else "No video frames recorded")
        }
    }

    override fun onCleared() {
        videoRecorder?.stop()
        videoRecorder = null
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
        val profile = FlightProfiles.resolve(s)
        rcLoop.update(StickMapper.map(axes, s.deadZone, profile.scale, profile.expo))
    }

    /**
     * Polled, not driven by telemetry emissions: StateFlow drops identical packets, and a landed Tello sends
     * identical packets, so an emission-driven check would never see the counter stay frozen.
     */
    /** Flight recorder: one CSV per flight (motors on → landed), exported to Download/TelloControler. */
    private fun recordFlightSample(event: String? = null) {
        val state = flightState.value
        val now = System.currentTimeMillis()
        synchronized(flightRecorder) {
            if (state != FlightState.LANDED && settings.value.flightLogs && !flightRecorder.isRecording) {
                flightRecorder.start(now, "flight-${timestamp()}.csv")
            }
            flightRecorder.record(now, event, state, telemetry.value, rcOutput.value)
            if (state == FlightState.LANDED && flightRecorder.isRecording) {
                flightRecorder.stop()?.let(::exportFlightLog)
            }
        }
    }

    private fun exportFlightLog(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { storage.saveFile(file, file.name, "text/csv") }
                .onSuccess { showNotice("Flight log saved: $it") }
            file.delete()
        }
    }

    private fun timestamp(): String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())

    private fun onAlert(alert: FlightAlert) {
        viewModelScope.launch(Dispatchers.IO) { recordFlightSample("alert:${alert.name}") }
        if (settings.value.rumbleAlerts) ControllerRumble.play(alert)
        when (alert) {
            FlightAlert.BATTERY_LOW -> showNotice("Battery low: ${telemetry.value.batteryPercent}% — land soon")
            FlightAlert.BATTERY_CRITICAL -> showNotice("Battery critical: ${telemetry.value.batteryPercent}% — LAND NOW")
            FlightAlert.LINK_LOST, FlightAlert.EMERGENCY_ARMING -> Unit
        }
    }

    private fun checkMotorsStopped() {
        if (stateLink.value.level != LinkLevel.GOOD) return
        val now = System.currentTimeMillis()
        val stopped = motorStopDetector.onTelemetry(telemetry.value.flightTimeSeconds, now)
        if (!stopped && flightState.value == FlightState.LANDED && motorStopDetector.motorsRunning(now)) {
            reduce(FlightEvent.MotorsRunning)
        }
        if (stopped) {
            motorStopDetector.reset()
            if (flightState.value != FlightState.LANDED) {
                reduce(FlightEvent.MotorsStopped)
                showNotice("Landing detected (motors stopped)")
            }
        }
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

    private fun reduce(event: FlightEvent) {
        val before = _flightState.value
        val state = _flightState.updateAndGet { FlightStateMachine.reduce(it, event) }
        if (state == FlightState.LANDED && before != FlightState.LANDED) motorStopDetector.reset()
        if (state != before) viewModelScope.launch(Dispatchers.IO) { recordFlightSample("state:$state") }
        when {
            state == FlightState.LANDED -> _flightStartedAt.value = null
            _flightStartedAt.value == null -> _flightStartedAt.value = System.currentTimeMillis()
        }
    }

    @Synchronized
    private fun showNotice(message: String) {
        _notice.value = message
        noticeJob?.cancel()
        noticeJob = viewModelScope.launch {
            delay(NOTICE_MS)
            _notice.value = null
        }
    }
}
