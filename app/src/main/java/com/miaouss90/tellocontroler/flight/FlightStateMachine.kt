package com.miaouss90.tellocontroler.flight

enum class FlightState { LANDED, TAKING_OFF, FLYING, LANDING }

sealed interface FlightEvent {
    data object TakeoffSent : FlightEvent
    data class TakeoffDone(val airborne: Boolean) : FlightEvent
    data object LandSent : FlightEvent
    data class LandDone(val ok: Boolean) : FlightEvent
    data object EmergencySent : FlightEvent
    data class Height(val cm: Int) : FlightEvent

    /** Motor-on time stopped advancing: the aircraft is on the ground whatever the reason (auto-land, lost ack). */
    data object MotorsStopped : FlightEvent

    /** Motor-on time is advancing: motors spin even though we did not command a takeoff. */
    data object MotorsRunning : FlightEvent

    /** Pilot confirms the aircraft is on the ground (escape hatch when no signal can tell). */
    data object ManualLanded : FlightEvent
}

/**
 * Pure flight-state reducer driven by command acknowledgements and telemetry height.
 * Nominal takeoff/land transitions validated on a real Tello EDU (2026-09-27).
 * HARDWARE-UNVERIFIED: timeout/error paths and the airborne height threshold.
 */
object FlightStateMachine {
    const val AIRBORNE_HEIGHT_CM = 20

    fun reduce(state: FlightState, event: FlightEvent): FlightState = when (event) {
        FlightEvent.TakeoffSent -> if (state == FlightState.LANDED) FlightState.TAKING_OFF else state
        is FlightEvent.TakeoffDone -> when {
            state != FlightState.TAKING_OFF -> state
            event.airborne -> FlightState.FLYING
            else -> FlightState.LANDED
        }
        FlightEvent.LandSent -> if (state == FlightState.LANDED) state else FlightState.LANDING
        is FlightEvent.LandDone -> when {
            state != FlightState.LANDING -> state
            event.ok -> FlightState.LANDED
            else -> FlightState.FLYING
        }
        FlightEvent.EmergencySent, FlightEvent.MotorsStopped, FlightEvent.ManualLanded -> FlightState.LANDED
        FlightEvent.MotorsRunning -> if (state == FlightState.LANDED) FlightState.FLYING else state
        is FlightEvent.Height -> when {
            // Height alone never promotes LANDED: lifting the drone by hand changes it too.
            event.cm >= AIRBORNE_HEIGHT_CM && state == FlightState.TAKING_OFF -> FlightState.FLYING
            event.cm <= 0 && state == FlightState.LANDING -> FlightState.LANDED
            else -> state
        }
    }
}
