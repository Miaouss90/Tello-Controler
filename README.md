<img src="docs/assets/app-icon-source.png" width="96" align="right" alt="App icon">

# Tello-Controler 🚁🎮

Native Android ground-control application for the **DJI/Ryze Tello EDU**, focused on low-latency manual flight, live video and Xbox controller support.

> Status: **M1 code complete / hardware validation required** (see [ROADMAP](ROADMAP.md)). Do not fly near people or obstacles until command mapping, failsafes and latency have been validated on the real aircraft.

## Product vision

Tello-Controler turns an Android phone into a lightweight FPV control station. The phone connects to the Tello over Wi-Fi and to an Xbox controller over Bluetooth. The project deliberately uses native Android/Kotlin so networking, gamepad input and hardware H.264 decoding can use Android APIs directly.

### M1 scope (first safe flight)
- Tello SDK mode over UDP
- Takeoff, land, emergency and RC control
- Continuous Xbox stick input with dead-zone
- Tello state/telemetry parsing
- Video transport receiver and native decoder integration point
- Landscape Compose flight HUD
- Connection/safety state
- Cloud APK build with GitHub Actions
- No backend, account or Play Store dependency

## Architecture

```text
Xbox controller ─Bluetooth─> Android input ─┐
                                           ├─> FlightViewModel ─> TelloClient ─UDP:8889─> Tello
Tello state ─────────────UDP:8890──────────>│
Tello H.264 ────────────UDP:11111──────────> TelloVideoReceiver ─> decoder ─> display
```

The Tello SDK endpoint is `192.168.10.1:8889`; state is received on UDP `8890` and video on UDP `11111`. SDK mode is entered with `command`, and video is enabled with `streamon`.

## Controls

| Xbox control | Default action |
|---|---|
| Left stick X | Yaw |
| Left stick Y | Throttle |
| Right stick X | Roll |
| Right stick Y | Pitch |
| A | Takeoff |
| B | Land (never blocked) |
| RB | FOLLOW the selected target on/off (sticks always override) |
| Y | Cycle rate: Slow / Normal / Sport |
| X | Photo (saved in Pictures/TelloControler) |
| View | Start / stop video recording (Movies/TelloControler) |
| Menu (hold 1 s) | Emergency motor stop — release early to cancel |

RC values are normalized to the Tello `rc a b c d` range, with a configurable dead-zone (default 15 %) and rate profiles (Slow 35 %, Normal 65 %, Sport 100 %). Takeoff is refused without fresh telemetry or below the minimum battery (default 20 %). On-screen touch sticks appear automatically when no controller is connected (or always, via Settings); with a controller, the HUD shows the RC values actually sent. Axis directions were validated on a real Tello EDU; failsafes still need a hardware test (see ROADMAP).

## Hardware checklist

Checks to run on the real Tello EDU + phone. Ticked only when the owner reports a successful test
(agents: add new checks with every hardware-dependent change, never tick them yourself).
Props off first, then open space. Status mirrors the "Hardware" column of the [ROADMAP](ROADMAP.md).

### Connection & link
- [x] CONNECT locks the `TELLO-xxxx` Wi-Fi (system dialog) and the SDK answers
- [x] Wi-Fi loss shows the red banner
- [x] Tello switched off then on → reconnection after tapping **Connect** in the Android system dialog
- [ ] Reconnection without the system dialog (Android auto-approval — may need the exact SSID/BSSID, see ROADMAP)
- [ ] Mobile data still works in another app while connected to the Tello
- [ ] Second session: the Android Wi-Fi approval dialog is remembered (or note that it is not)
- [ ] LINK LOST banner when telemetry stops, RECONNECT works

### Controls
- [x] Xbox controller detected over Bluetooth
- [x] 4 axes in the right direction (left = yaw/throttle, right = roll/pitch)
- [x] Stick held steady keeps its command (not reset after 250 ms)
- [x] Hover stability back to normal with v0.5.4+ (in-order `rc` sending)
- [ ] Sticks released: HUD indicator dots grey and centered (no controller drift above the dead-zone)
- [ ] Controller switched off while pushing a stick → command back to neutral
- [ ] Y cycles Slow / Normal / Sport and the difference is felt
- [ ] Dead-zone slider changes stick feel
- [ ] Touch sticks appear without a controller and fly correctly
- [ ] With the controller connected, touch sticks are greyed and do nothing until dragged; dragging one takes over (notice), releasing gives control back
- [ ] HUD stick indicators follow the controller
- [ ] Expo slider: finer control around the center, full deflection unchanged
- [ ] INDOOR mode: slow, soft, climbing stops at 150 cm (HUD notice)
- [ ] CINEMATIC mode: very slow, movements ease in/out; releasing the controller (off) still stops at once
- [ ] Height limit slider: climbing blocked at the limit, descending still works

### Flight & safety
- [x] A takes off, B lands, flight state follows in the HUD
- [ ] Short Menu press does nothing; Menu held 1 s = emergency motor stop
- [ ] B during takeoff lands immediately
- [X] Takeoff refused below the battery minimum / without telemetry (message shown)
- [X] Landing detected ~3 s after touchdown (TAKE OFF available again), `MOTOR` counter frozen on the ground
- [X] Drone lifted by hand → TAKE OFF stays available; MARK LANDED appears if the state gets stuck
- [X] Low-battery auto-landing reflected in the flight state

### Video
- [x] Live video displayed
- [X] No stretching on a wide phone (4:3 with side bands)
- [ ] Fewer / no pixel artifacts, including at distance
- [ ] Latency measured (film a stopwatch through the app): ____ ms

### HUD
- [X] Artificial horizon tilts the right way (roll) and moves the right way (pitch)
- [ ] Heading tape turns with yaw
- [ ] SPD / V/S plausible (units and sign), TIME runs in flight and resets on landing
- [ ] HUD toggles in Settings work

### Alerts
- [x] Menu hold → single rumble
- [x] Wi-Fi loss → triple rumble
- [X] Battery low / critical in flight → rumble + HUD notice

### Recording
- [X] Photo (X / PHOTO) appears in Gallery › Pictures/TelloControler
- [X] 10 s video (View / REC) plays in the Gallery (Movies/TelloControler)
- [X] Flight log CSV in Download/TelloControler after a flight, columns filled

### Mission Pads (Tello EDU)
- [ ] Settings › Mission Pad detection enabled → no error message
- [ ] Hovering over a pad shows `PAD #n x y z` in the HUD, values change when moving
- [ ] Pad columns filled in the flight log

### Target tracking
- [X] Tap a textured object in the video → green box on it; a flat area shows "Nothing to track there"
- [X] Box follows when the object or the drone moves slowly; TARGET % stays high
- [X] Object hidden → red "TARGET LOST", box re-acquires when it comes back
- [ ] Long-press on the video or ✕ TARGET clears it
- [ ] Video stays smooth while tracking (no added lag)
- [ ] FOLLOW (RB / HUD) refused on the ground or without a locked target (message)
- [ ] In a hover, FOLLOW turns toward a target moving left/right and climbs/descends to keep it centered (right direction!)
- [ ] Any stick input takes over instantly; releasing the sticks resumes following
- [ ] Target hidden > 2 s → "Follow off: target lost" + rumble, drone holds position
- [ ] Wi-Fi loss / landing / B → follow off

### App & updates
- [X] Settings page full screen in landscape (not offset), two columns, back button closes it
- [ ] With Settings open, the controller still works (B lands)
- [ ] Settings kept after restarting the app
- [X] UPDATE installs the next release (phone on a Wi-Fi with Internet)
- [X] Screen stays on during a flight

## Build, release & update

Everything is automated by GitHub Actions — **Android Studio is not required**.

- **Every push / pull request** runs the unit tests and builds a signed APK (workflow artifact).
- **Every push to `main`** publishes a GitHub Release `v0.5.0`, `v0.5.1`… with the APK.
- **On the phone:** Settings → **UPDATE** downloads the latest release and installs it (phone on a Wi-Fi with Internet, not the Tello Wi-Fi). Android asks once to allow the app to install updates.

**First install:** open the [latest release](https://github.com/Miaouss90/Tello-Controler/releases/latest) on the phone, download the APK and open it. Builds up to v0.2.0 were signed with a throw-away debug key: uninstall that version once before installing v0.3 or later.

## Documentation

- [Requirements & product decisions](docs/REQUIREMENTS.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Safety](docs/SAFETY.md)
- [Roadmap](ROADMAP.md)
- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)
- [AI agent guide](AGENTS.md)

## Development principles

1. **Safety before features** — stale controller input must never remain an active command.
2. **Native where latency matters** — UDP, gamepad and video stay close to Android APIs.
3. **Hardware-testable increments** — networking, control, telemetry and video are independently diagnosable.
4. **No unnecessary cloud dependency** — the phone talks directly to the aircraft.
5. **Document decisions** — product requirements from the design discussion are maintained in `docs/`.

## References

The protocol implementation follows the official RoboMaster TT / Tello SDK 3.0 documentation. See `docs/PROTOCOL.md`.

## License

No license has been selected yet. All rights reserved until a license is explicitly added.
