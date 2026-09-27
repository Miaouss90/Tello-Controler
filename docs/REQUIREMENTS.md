# Product requirements

## Origin
This is the durable record of the initial product discussion; it summarizes decisions rather than reproducing chat transcripts.

## Goal
Operate a Tello EDU from an Android phone while viewing live video. The owner does not want to write code or maintain a local Android toolchain; builds and installable artifacts should be produced by cloud CI.

## Confirmed decisions
- Android only initially; native Kotlin + Jetpack Compose.
- Xbox controller is first-class: Bluetooth to phone while phone uses Wi-Fi for Tello.
- No backend is required for flight.
- GitHub Actions should build the APK; Android Studio is optional.
- V0.1: control, telemetry, live video and safety.
- Touch controls, recording and computer vision are later features.

## First flight-capable acceptance criteria
1. Enter SDK mode and verify a response.
2. Receive telemetry continuously.
3. Display live camera video at usable latency.
4. Map all four Xbox flight axes and verify directions.
5. Deliberate takeoff/land actions only.
6. Neutralize RC when input becomes stale.
7. Handle app pause/controller or Wi-Fi loss safely.
8. Produce an installable APK from CI.
