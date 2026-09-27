# Architecture

## Components
- **FlightScreen** — landscape HUD and explicit actions.
- **FlightViewModel** — orchestration and UI state.
- **TelloClient** — UDP command transport and state listener.
- **TelloTelemetry** — state parser.
- **XboxController** — Android gamepad normalization.
- **TelloVideoReceiver** — UDP video transport.
- **H264Decoder (planned)** — access-unit assembly + MediaCodec + Surface.

## Design
The application is local-first and requires no Internet in flight. Blocking sockets run off the UI thread. State is exposed with StateFlow. Video packet receipt and decoding are separated so transport can be diagnosed independently.

## Testing
Parsers and controller transforms get unit tests. Network behavior can later use a simulator. Axis direction, H.264 framing, latency and all failsafes require validation on a real Tello EDU.
