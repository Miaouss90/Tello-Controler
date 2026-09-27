# Roadmap

Milestones are **named**, not version numbers: the app version (`0.4.<build>`) only drives in-app updates.
Each item has two statuses — nothing counts as done for flight until it is validated on a real Tello EDU.

**Legend:** ✅ done · ⬜ to do · 🟡 partially validated · — not applicable

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
| Locked Tello Wi-Fi (no Internet needed), sockets bound to it, auto-reconnect when it returns | ✅ | 🟡 lock + loss alert ok; auto-reconnect untested |
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

## M2 — Comfortable manual flight
| Item | Code | Hardware |
|---|---|---|
| Slow / Normal / Sport rates (Y cycles, persisted) | ✅ | ⬜ |
| Configurable dead-zone | ✅ | ⬜ |
| Settings persistence | ✅ | — |
| Touch-stick fallback | ✅ | ⬜ |
| Controller remapping | ⬜ | ⬜ |
| Auto-land detection (low-battery landing reflected in flight state) | ⬜ | ⬜ |

## M3 — FPV experience
| Item | Code | Hardware |
|---|---|---|
| Low-latency video tuning | ⬜ | ⬜ |
| Photo capture | ⬜ | ⬜ |
| Local video recording | ⬜ | ⬜ |
| HUD polish (artificial horizon, flight timer) | ⬜ | — |

## M4 — EDU features
| Item | Code | Hardware |
|---|---|---|
| Mission Pad telemetry/control | ⬜ | ⬜ |
| Automated moves/missions + editor | ⬜ | ⬜ |
| Flight logs | ⬜ | — |

## Delivery (done)
| Item | Status |
|---|---|
| CI: unit tests + signed APK on every push/PR | ✅ |
| Automatic GitHub Release on every `main` build | ✅ |
| One-tap in-app update (disabled while linked to the Tello) | ✅ |
| AGENTS.md, package structure, ADRs | ✅ |

## Future / research
- QR / ArUco recognition
- Object detection/tracking, assisted visual flight
- Optional simulator
- Multiple Tello EDU
