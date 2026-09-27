# Architecture

## Layers
```text
ui/ (Compose HUD) ──reads StateFlow──> FlightViewModel <──events── MainActivity (lifecycle, gamepad)
                                           │
              ┌────────────────────────────┼─────────────────────────────┐
              ▼                            ▼                             ▼
   controller/RcSafetyLoop ──rc──> tello/TelloClient          tello/TelloVideoReceiver
   (fixed 20 Hz, stale→neutral)    (UDP 8889 / 8890)           (UDP 11111) ─> TelloH264Decoder ─> Surface
              ▲
   controller/XboxController ─> StickMapper (pure)
```

## Components
- **MainActivity** — Android glue: lifecycle failsafes, input device detection, key/motion routing.
- **FlightViewModel** — orchestration, UI state, guarded emergency (hold Menu).
- **ui/** — `FlightScreen` (HUD), `VideoSurface`, `SettingsDialog`, `components/`, `theme/`.
- **StickMapper** — pure Mode 2 mapping with rescaled dead-zone.
- **XboxController** — Android gamepad adapter.
- **RcSafetyLoop** — the only RC emitter; fixed rate, stale-input watchdog (ADR-005).
- **TelloClient** — UDP command transport, acknowledged connection state, telemetry listener.
- **TelloCommands / TelloTelemetry / AnnexB** — pure protocol helpers.
- **TelloVideoReceiver / TelloH264Decoder** — video transport and MediaCodec decoding (ADR-004).

## Design
The application is local-first and requires no Internet in flight. Blocking sockets run off the UI thread. State is exposed with StateFlow. Logic is kept in pure Kotlin classes so it can be unit-tested on the JVM; Android-specific classes are thin adapters.

## Testing
JVM unit tests (`app/src/test`) cover the stick mapping, safety loop staleness, telemetry parsing, command building and H.264 start-code scanning; CI runs them before building the APK. Network behavior can later use a simulator. Axis direction, H.264 framing, latency and all failsafes require validation on a real Tello EDU.
