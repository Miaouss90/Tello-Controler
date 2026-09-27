# Changelog

Grouped by version line (`telloVersionBase`). Every merge to `main` publishes a release in the current line
(`0.5.0`, `0.5.1`…); each PR adds its user-visible changes under the current line.
Hardware validation status: [README › Hardware checklist](README.md#hardware-checklist).

## 0.5 — 2026-09-27

### Added
- **FOLLOW mode** (RB / HUD): yaw and altitude corrections keep the tracked target centered; any stick input
  takes over, and follow stops on landing, link loss or a target lost for 2 s (with rumble).
- **Target tracking (preview)**: tap an object in the video, a box tracks it (template matching on a
  downscaled frame, ~10 Hz, off the UI thread); long-press or ✕ TARGET clears. Does not steer the drone yet.
- **Flight modes**: Standard, **Indoor** (≤ 35 % speed, soft sticks, height ≤ 150 cm) and **Cinematic** (≤ 30 %,
  smoothed movements); modes never make flight more aggressive than the user settings.
- **Stick expo** and **height limit** (climb blocked at the limit, descent always allowed).
- **Mission Pads** (Tello EDU): detection toggle in Settings (`mon` / `mdirection 0`), `PAD #n x y z` in the HUD,
  pad columns in the flight log.
- **Photo** (X button / HUD `PHOTO`) → `Pictures/TelloControler`.
- **Video recording** to MP4 without re-encoding (View button / HUD `REC` with timer) → `Movies/TelloControler`.
- **Flight recorder**: CSV per flight at 10 Hz (telemetry, RC sent, state changes, alerts) → `Download/TelloControler`.
- **FPV cockpit HUD**: artificial horizon, heading tape, central reticle (each toggleable), ground speed,
  vertical speed, flight timer.
- **Controller rumble on alerts**: emergency arming, link/Wi-Fi loss, battery low / critical (+ HUD notices).
- **MARK LANDED** escape hatch when the state says airborne but the drone is at ground level.
- README **Hardware checklist** of every on-drone verification.

### Changed
- **Settings** is a full-screen two-column layer (usable in landscape, not offset); the controller keeps working
  while it is open.
- Versions are sequential within a line (`0.5.0`, `0.5.1`…) instead of the CI run number.

### Fixed
- `rc` commands sent in order and evenly spaced (they could bunch up or be reordered).
- With a controller connected, touch sticks are greyed; dragging one is an explicit takeover (notice).
- **Video pixel artifacts**: dedicated decode thread, no silently dropped data, clean resync on key frames,
  frame end detected on the short last datagram, larger socket buffer, Wi-Fi low-latency lock.
- **TAKE OFF never came back** after lifting the drone by hand: height alone no longer means "flying".
- Screen kept on during flights (sleep paused the app and neutralized the sticks).

## 0.4 — 2026-09-27

### Added
- **Locked Tello Wi-Fi**: explicit Internet-less network request, sockets bound to it, mobile data kept for the rest.
- Automatic reconnection when the Tello Wi-Fi comes back; red Wi-Fi-lost banner.
- **Landing detection** from the motor-time counter.
- **Touch sticks** (automatic without a controller) and HUD stick indicators.
- Link watchdog (`LINK_LOST`), connection-quality and last-packet indicators.
- Acknowledged takeoff/land, flight state machine, takeoff guard (battery minimum, fresh telemetry).
- Rates Slow / Normal / Sport (Y), configurable dead-zone, persisted settings.
- Guided pre-flight checklist.

### Changed
- ROADMAP reorganized into named milestones with Code / Hardware status.

### Fixed
- **Held sticks were reset to neutral after 250 ms** (Android only reports stick changes).
- Video stretched on wide phones (now 4:3 letterboxed).
- Reconnect after Wi-Fi loss did nothing.

## 0.3 — 2026-09-27

### Added
- `AGENTS.md`, package structure (`controller` / `tello` / `ui` / `update`), unit tests in CI.
- Automatic signed GitHub Release on every `main` build; one-tap in-app **UPDATE**.
- Guarded emergency stop (hold Menu 1 s).
- Adaptive app icon.

### Fixed
- Stick layout now matches Mode 2 (left = yaw/throttle, right = roll/pitch).
- Takeoff/land no longer repeat while a button is held; dead-zone rescaled.

## 0.2 — 2026-09-27
- Settings screen, "Check for updates" link, controller status, lifecycle failsafe, video diagnostics.

## 0.1
- First native Kotlin/Compose build: UDP control, telemetry, Xbox input, video receiver, landscape HUD.
