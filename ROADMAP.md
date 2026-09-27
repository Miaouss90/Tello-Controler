# Roadmap

Milestones are **named**, not version numbers: the app version (`0.5.x`) only drives in-app updates.
Each item has two statuses — nothing counts as done for flight until it is validated on a real Tello EDU.
The step-by-step checks live in [README › Hardware checklist](README.md#hardware-checklist).

**Legend:** ✅ done · ⬜ to do · 🟡 partially validated · — not applicable

## Next up
Best "wow / effort" ratio, in order (Slow/Normal/Sport rates already shipped):
1. **Finish M1 hardware validation** (failsafes) — prerequisite for everything below.
2. ~~Rich FPV HUD (M3)~~ — coded, awaiting hardware check.
3. ~~Controller rumble on alerts (M4)~~ — validated (arming, Wi-Fi loss).
4. ~~Video + telemetry recording, flight recorder (M5)~~ — coded, awaiting hardware check.
5. ~~Mission Pads (M6)~~ — detection coded, awaiting hardware check.
6. Visual target tracking (M7).
7. Mission editor (M6).

**Rules for every assisted/automatic feature (M2, M6, M7):** stick input or LAND always overrides it instantly,
it stops on link loss, it respects the safety limits of M2, and it goes through `RcSafetyLoop` — never raw `rc`.

First hardware session: 2026-09-27 (Wi-Fi, Xbox controller, takeoff/land, 4 axes, video, telemetry, Wi-Fi loss alert).

## M1 — First safe flight
Everything `docs/SAFETY.md` requires before flying, plus the bench test that validates it.

| Item | Code | Hardware |
|---|---|---|
| Native Kotlin/Compose app, landscape HUD, adaptive icon | ✅ | — |
| UDP command transport, `command` acknowledgement | ✅ | ✅ |
| Command acknowledgements (`takeoff`/`land`), `land` preempts pending acks | ✅ | 🟡 nominal acks; preemption untested |
| Flight state machine (landed / taking off / flying / landing) from acks + height | ✅ | 🟡 nominal path; timeouts untested |
| Telemetry parser | ✅ | ✅ |
| Link watchdog: LINK_LOST after 2 s without state, auto-recover, reconnect | ✅ | ⬜ |
| Locked Tello Wi-Fi (no Internet needed), sockets bound to it, auto-reconnect when it returns | ✅ | 🟡 lock, loss alert, reconnect ok (Android dialog must be tapped again) |
| Connection quality + last-packet indicators (state & video) | ✅ | 🟡 nominal (green) only |
| Xbox Mode 2 mapping, rescaled dead-zone | ✅ | ✅ |
| Held-stick input pump (a steady stick is not treated as stale) | ✅ | ✅ |
| Fixed-rate RC loop + stale-input watchdog | ✅ | 🟡 loop ok; watchdog untested |
| Zero RC on controller disconnect / app pause | ✅ | ⬜ |
| Guarded emergency stop (hold Menu 1 s) | ✅ | ⬜ |
| Takeoff guard: connection, fresh telemetry, battery minimum, landed | ✅ | ⬜ |
| Flight-state-aware UI (takeoff disabled when airborne, land never blocked) | ✅ | ⬜ |
| Guided pre-flight setup checklist (Wi-Fi, controller, SDK link, video) | ✅ | ⬜ |
| H.264 decoder to full-screen Surface | ✅ | 🟡 video ok; latency not measured |
| **Bench test on a real Tello EDU (props off, then tethered hover)** | — | 🟡 flight ok; failsafes pending |

## M2 — Comfortable & assisted manual flight
| Item | Code | Hardware |
|---|---|---|
| Slow / Normal / Sport rates (Y cycles, persisted) | ✅ | ⬜ |
| Configurable dead-zone | ✅ | ⬜ |
| Settings persistence | ✅ | — |
| Touch sticks (automatic without a controller) | ✅ | ⬜ |
| HUD stick indicators (RC actually sent) | ✅ | ⬜ |
| Landing detection (motor-time freeze ⇒ landed) | ✅ | ⬜ |
| Neutral RC on controller loss | ✅ | ⬜ |
| Stick expo curves (fine control around center) | ✅ | ⬜ |
| Flight modes: **Indoor** (slow, soft sticks, height ≤ 1.5 m) and **Cinematic** (filtered, very slow moves) | ✅ | ⬜ |
| Speed & height limits (safety bubble, height part; speed via rate/mode caps) | ✅ | ⬜ |
| Simplified altitude hold (throttle stick centered = hold, relies on Tello's own hold) | ⬜ | ⬜ |

## M3 — FPV cockpit
| Item | Code | Hardware |
|---|---|---|
| Video 4:3 letterbox (no stretching on wide phones) | ✅ | ⬜ |
| Rich, configurable HUD: speed (`vgx/vgy/vgz`), altitude, battery, flight timer, link quality | ✅ | ⬜ speed units/signs |
| Virtual cockpit: artificial horizon, heading tape, central reticle (toggles in Settings) | ✅ | ⬜ pitch/roll signs |
| Photo capture (frame grab from the decoder, X button) | ✅ | ⬜ |
| Local video recording (H.264 stream to MP4, no re-encode, View button) | ✅ | ⬜ |
| Video reliability: dedicated decode thread, whole frames, no dropped input, clean resync on key frame, 1 MB socket buffer, Wi-Fi low-latency lock | ✅ | ⬜ |
| Latency measurement | ⬜ | ⬜ |

## M4 — Advanced controller
| Item | Code | Hardware |
|---|---|---|
| Full Xbox button remapping | ⬜ | ⬜ |
| Saved controller profiles | ⬜ | — |
| Rumble on alerts (low battery, link loss, emergency arming) — depends on Android/controller rumble support | ✅ | 🟡 arming + Wi-Fi loss ok; battery untested |
| Triggers as analog speed control / software tilt | ⬜ | ⬜ |

## M5 — Flight recorder & replay
| Item | Code | Hardware |
|---|---|---|
| Flight recorder: telemetry, RC commands, events (CSV per flight, 10 Hz) | ✅ | ⬜ |
| Video synchronized with the flight log | ⬜ | ⬜ |
| Flight log viewer (battery, altitude, attitude charts) | ⬜ | — |
| Replay: re-fly recorded RC commands (supervised, stick override) | ⬜ | ⬜ |

## M6 — Tello EDU missions
| Item | Code | Hardware |
|---|---|---|
| Mission Pad telemetry (`mid`, `x/y/z`) and detection (`mon`, HUD, flight log) | ✅ | ⬜ |
| Mission editor timeline: takeoff → forward 1 m → rotate 90° → wait 2 s → photo → land | ⬜ | ⬜ |
| Recorded sequences and predefined trajectories | ⬜ | ⬜ |
| Distance limit / return to a central zone (needs Mission Pads for position) | ⬜ | ⬜ |

## Backlog
| Item | Code | Hardware |
|---|---|---|
| Reconnect without the Android approval dialog (specifier with the exact SSID/BSSID of the approved Tello; BSSID needs location permission) | ⬜ | ⬜ |

## M7 — Vision-assisted flight
The feature that makes the app original rather than a remote-control clone.

| Item | Code | Hardware |
|---|---|---|
| Frame pipeline from the decoder for on-device vision | ⬜ | — |
| QR / ArUco detection | ⬜ | ⬜ |
| Color recognition | ⬜ | ⬜ |
| **Follow target**: tap an object, gentle yaw/pitch corrections keep it centered | ⬜ | ⬜ |
| Person tracking | ⬜ | ⬜ |
| Orbit mode around a selected target (radius/speed) | ⬜ | ⬜ |
| Gesture control (takeoff / land / photo) | ⬜ | ⬜ |
| Camera-triggered actions | ⬜ | ⬜ |

## Delivery (done)
| Item | Status |
|---|---|
| CI: unit tests + signed APK on every push/PR | ✅ |
| Automatic GitHub Release on every `main` build | ✅ |
| One-tap in-app update (disabled while linked to the Tello) | ✅ |
| AGENTS.md, package structure, ADRs | ✅ |

## Future / research
- Head tracking: tilt the phone to steer yaw/pitch slightly
- Mini-map of movements (needs position: Mission Pads or velocity integration, which drifts)
- Multiple Tello EDU (swarm mode requires the Tellos in station mode on a router)
- Optional simulator
