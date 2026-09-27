# Flight safety

This software controls a physical aircraft. Early builds are experimental.

Before normal flight: verify all four axes in a safe setup; verify button mapping; validate controller disconnect, app backgrounding and Wi-Fi loss; measure command/video latency.

## Mandatory software work before a flight-ready release
All implemented; **none validated on hardware yet** (see ROADMAP M1).
- Fixed-rate RC heartbeat — `RcSafetyLoop`.
- Input timestamp and stale-input watchdog; held sticks re-fed only while the controller is present and the app is in the foreground.
- Send zero RC on controller disconnect/app pause.
- Connection state based on actual Tello acknowledgement, and LINK_LOST when telemetry stops for 2 s.
- Guard emergency action against accidental activation (hold Menu 1 s).
- Takeoff battery threshold (settings, default 20 %), fresh telemetry required.
- Flight-state-aware UI; landing is never blocked.

## Bench test procedure (M1)
1. Props off: connect, verify telemetry, video, LINK_LOST when the Tello is switched off, reconnect.
2. Props off: verify each stick axis direction and each rate (SLOW/NORMAL/SPORT) with `rc` values in the HUD diagnostics.
3. Props off: hold a stick steady for 5 s and verify the command is maintained; switch the controller off and verify neutral.
4. Tethered/open space: takeoff, hover, land; verify flight state transitions in the HUD.
5. Verify emergency hold (short press must do nothing).

Never treat a successful APK build as proof of safe flight behavior.
