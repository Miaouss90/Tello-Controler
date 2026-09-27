# Flight safety

This software controls a physical aircraft. Early builds are experimental.

Before normal flight: verify all four axes in a safe setup; verify button mapping; validate controller disconnect, app backgrounding and Wi-Fi loss; measure command/video latency.

## Mandatory software work before a flight-ready release
- Fixed-rate RC heartbeat.
- Input timestamp and stale-input watchdog.
- Send zero RC on controller disconnect/app pause.
- Connection state based on actual Tello acknowledgement.
- Guard emergency action against accidental activation.
- Optional takeoff battery threshold.
- Flight-state-aware UI.

Never treat a successful APK build as proof of safe flight behavior.
