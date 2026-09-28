# AGENTS.md

Guidance for AI coding agents (Claude Code, Codex, Copilot…) working on this repository.
Humans should read [README.md](README.md) and [CONTRIBUTING.md](CONTRIBUTING.md) first; this file is the operational summary.

## What this is
Native Android (Kotlin + Jetpack Compose) ground station for a **DJI/Ryze Tello EDU**: UDP control, telemetry,
H.264 live video, Xbox controller input. **It flies a real aircraft** — safety rules below are non-negotiable.

The owner does not write code and does not run Android Studio. Every change must be buildable by GitHub Actions
and understandable from its commit message and docs.

## Repository map
```text
app/src/main/java/com/miaouss90/tellocontroler/
├── MainActivity.kt          Android glue only: lifecycle + input events → FlightViewModel
├── FlightViewModel.kt       Orchestration & UI state (StateFlow). No Android UI references.
├── flight/                  PURE flight logic                                    [unit-tested]
│   ├── FlightStateMachine.kt landed/taking off/flying/landing reducer
│   ├── MotorStopDetector.kt motor-time counter → motors stopped / running
│   └── TakeoffGuard.kt      pre-takeoff checks (landing is never guarded)
├── settings/                FlightSettings + SharedPreferences repo
│   └── FlightMode.kt        PURE Standard/Indoor/Cinematic → FlightProfile      [unit-tested]
├── controller/              Input → RC command
│   ├── StickAxes.kt         raw stick positions
│   ├── RcInput.kt           rc a b c d value object (-100..100)
│   ├── StickMapper.kt       PURE axis → RcInput (dead-zone, expo, scale, Mode 2) [unit-tested]
│   ├── XboxController.kt    Android MotionEvent/KeyEvent adapter → StickMapper
│   ├── AlertMonitor.kt      PURE edge-triggered flight alerts                    [unit-tested]
│   ├── ControllerRumble.kt  gamepad vibration per alert
│   ├── RcShaper.kt          PURE height limit + smoothing inside the RC loop    [unit-tested]
│   └── RcSafetyLoop.kt      SAFETY-CRITICAL fixed-rate sender + stale watchdog  [unit-tested]
├── tello/                   Aircraft protocol (see docs/PROTOCOL.md)
│   ├── TelloClient.kt       UDP 8889 commands+acks / 8890 state, connection state, link watchdog
│   ├── LinkMonitor.kt       PURE packet freshness / rate tracker                 [unit-tested]
│   ├── TelloWifiManager.kt  explicit Internet-less TELLO-* Wi-Fi (WifiNetworkSpecifier), lost/locked state
│   ├── TelloCommands.kt     PURE command string builders                         [unit-tested]
│   ├── TelloTelemetry.kt    PURE state packet parser                             [unit-tested]
│   ├── MissionPad.kt        Mission Pad telemetry (mid, x/y/z)
│   ├── TelloVideoReceiver.kt UDP 11111 transport (short datagram = end of frame)
│   ├── NalSplitter.kt       PURE UDP chunks → NAL units                          [unit-tested]
│   ├── AnnexB.kt            PURE H.264 start codes / NAL types                   [unit-tested]
│   └── TelloH264Decoder.kt  dedicated decode thread, whole frames → MediaCodec → Surface
├── record/                  Photo, MP4 recording, flight recorder
│   ├── AccessUnitAssembler.kt PURE NAL stream → SPS/PPS config + whole frames      [unit-tested]
│   ├── FlightLog.kt         PURE CSV format + per-flight FlightRecorder            [unit-tested]
│   ├── VideoRecorder.kt     MediaMuxer MP4 without re-encoding
│   └── MediaStorage.kt      MediaStore (Movies/Pictures/Download › TelloControler)
├── vision/                  On-device vision
│   ├── GrayFrame.kt         PURE luminance image
│   ├── TemplateTracker.kt   PURE NCC template tracker                              [unit-tested]
│   ├── FollowController.kt  PURE FOLLOW yaw/throttle + InputArbiter (pilot wins)  [unit-tested]
│   └── VisionFrameGrabber.kt PixelCopy of the video at 240×180 on a vision thread
├── update/                  In-app update from GitHub Releases
│   ├── AppVersion.kt        PURE version comparison                              [unit-tested]
│   ├── ReleaseInfo.kt       GitHub release JSON → APK asset                      [unit-tested]
│   ├── GitHubReleaseSource.kt HTTP client (public repo, no token)
│   ├── ApkInstaller.kt      PackageInstaller session install
│   ├── UpdateInstallReceiver.kt install result / system confirmation
│   └── UpdateViewModel.kt   one-tap check → download → install state machine
└── ui/
    ├── FlightScreen.kt      Landscape HUD composition
    ├── VideoSurface.kt      SurfaceView host for the decoder
    ├── SettingsScreen.kt    full-screen settings LAYER (not a Dialog: see pitfalls)
    ├── SetupChecklist.kt    guided pre-flight setup
    ├── TargetLayer.kt       tap-to-track over the 4:3 video, tracked box
    ├── PhotoCapture.kt      PixelCopy photo of the video
    ├── hud/                 PURE HudMath + cockpit overlay (horizon, heading tape, reticle)
    ├── components/          Reusable HUD widgets
    └── theme/               Colors (HudColors) + TelloTheme
app/src/test/…               JVM unit tests (JUnit 4), mirror the main package layout
docs/                        REQUIREMENTS, ARCHITECTURE, PROTOCOL, SAFETY, DECISIONS (ADRs)
```

### Layering rules
- `tello/` and `controller/` must not depend on `ui/`.
- Put logic in **pure Kotlin** (no `android.*` imports) whenever possible, and unit-test it.
  Android classes (`MotionEvent`, `MediaCodec`, `DatagramSocket` wiring) are thin adapters around pure code.
- UI reads state only through `FlightViewModel` StateFlows; composables never touch sockets or codecs.
- Blocking I/O runs on `Dispatchers.IO`, never on the main thread.

## Build & test
No Gradle wrapper and no local SDK are assumed. CI (`.github/workflows/android.yml`) uses Gradle 8.9 + JDK 17:
```bash
gradle testDebugUnitTest   # JVM unit tests
gradle assembleRelease     # APK → app/build/outputs/apk/release/app-release.apk
```
**CI is the judge.** Don't spend time building locally: push the branch and read the CI result
(`gh run watch`, `gh run view --log-failed`). The owner's machine is ARM64 WSL, where AGP's x86_64 `aapt2` can't run anyway.

## Release pipeline (fully automatic)
- Every push/PR to `main`: unit tests + release APK uploaded as a workflow artifact.
- Every push to `main` (= every merged PR): GitHub Release `v<telloVersionBase>.<n>` with the APK,
  marked latest. **Merging to `main` ships to the owner's phone** — keep `main` flyable.
- Version: `versionCode = GITHUB_RUN_NUMBER` (drives Android updates), `versionName = telloVersionBase.n` where n =
  number of `v<base>.*` tags already published (0.5.0, 0.5.1…)
  (`gradle.properties`). Never hardcode versions; bump `telloVersionBase` for a new minor line.
- In-app: Settings → UPDATE queries `releases/latest`, downloads the `.apk` asset and installs it with
  PackageInstaller. Needs the one-time "install unknown apps" permission.
- **Signing:** releases are signed with a single long-lived key from repo secrets `TELLO_KEYSTORE_BASE64`,
  `TELLO_KEYSTORE_PASSWORD`, `TELLO_KEY_ALIAS`, `TELLO_KEY_PASSWORD`. Changing or losing it makes updates
  impossible (users must uninstall). Never regenerate it, never commit it. `main` builds fail without it.

## Safety rules (must never regress)
1. **Only `RcSafetyLoop` sends `rc` commands**, synchronously from its own thread (in order, every 50 ms).
   Never call `TelloClient.rc` from UI or input handlers, never send `rc` from per-packet coroutines.
2. Input older than `RcSafetyLoop.STALE_MS` ⇒ neutral RC. Do not raise this limit without an ADR.
   Held controller sticks are re-fed by the ViewModel input pump only while the controller is present and the
   app is in the foreground; touch sticks re-report while touched (ADR-005, ADR-009).
   Sources are arbitrated in `FlightViewModel.pumpInput` via `InputArbiter`: touch (only while dragged; greyed
   when a controller is connected) > controller > FOLLOW (only while the pilot's sticks are centered).
3. App pause, controller disconnect and ViewModel clear ⇒ `neutralControls()` (and cancel emergency arming).
4. `takeoff`/`land` fire on the **first** key press only (`repeatCount == 0`), never on repeat.
5. **Landing is never blocked**: no guard, no flight-state check, and `land` preempts pending acknowledgements.
   Takeoff always goes through `TakeoffGuard`.
6. `emergency` (motor cut, the drone falls) requires holding Menu ≥ `FlightViewModel.EMERGENCY_HOLD_MS`.
   Never map it to a single tap or an on-screen button without a guard.
7. `CONNECTED` means the Tello acknowledged `command` with `ok` — never assume it.
8. Tello sockets are bound **per socket** to the Tello `Network` (`network.bindSocket`), never with
   `bindProcessToNetwork` (the update needs Internet over mobile data). Wi-Fi lost ⇒ held input dropped,
   LINK_LOST, takeoff blocked.
9. In-app update is disabled while connected to the Tello (installing kills the app mid-flight).
10. Assisted/automatic features (flight modes, missions, replay, vision tracking) feed `RcSafetyLoop` like any
    input source: stick input or LAND overrides them instantly, they stop on link loss, and they respect the
    M2 speed/height limits.
11. Stateful shaping (Cinematic smoothing) never delays a stop: stale input and explicit neutral bypass `RcShaper`.
12. Anything not verified on a real Tello is marked `HARDWARE-UNVERIFIED` in code/docs. Don't remove the mark
   (or tick the ROADMAP "Hardware" column) unless the owner reports a successful hardware test.

Changes touching these areas must include/adjust unit tests and mention the safety impact in the commit body.

## Conventions
- Kotlin official style (`kotlin.code.style=official`), 4-space indent, one top-level concept per file,
  trailing commas in multi-line parameter lists. No compressed one-line classes.
- Composables: stateless, parameters in / lambdas out. Colors come from `HudColors`, not literals.
- Prefer lambdas `{ vm.takeoff() }` over method references for Compose callbacks (see commit df6cb66).
- Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `ci:`, `chore:`), English.
- Feature branches + PR to `main`; `main` must always build.
- No new dependency without a reason in the PR description. No backend, analytics or network calls
  other than the Tello and the GitHub Releases API.

## Working with the owner (process agreed on 2026-09-27)
- The owner speaks French; answer in French. Code, commits, PRs and docs stay in English.
- **One PR at a time, branched from `main`.** Never stack PRs that target `main` (it produced duplicated,
  conflicting PRs). If work must build on an open PR, open it against that PR's branch (GitHub retargets it
  after the merge) — CI then does not trigger on its own: run `gh workflow run "Android CI" --ref <branch>`.
- The owner authorized merging on green CI ("surveille la CI pour pousser sur main"): `gh pr merge --merge
  --delete-branch`, watch the `main` build, check that the release appeared (`gh release list`), delete branches.
  Exception: anything that steers the aircraft autonomously waits for the owner's hardware validation of its
  prerequisite (e.g. FOLLOW waited for the tracking preview).
- Docs-only changes (`**.md`) skip CI and publish no release (`paths-ignore`).
- A failed CI step with `ECONNRESET` / cache errors is GitHub infrastructure: `gh run rerun <id>`.
- Hardware results arrive as short messages ("alerte wifi ok", "le suivi fonctionne"): tick exactly what was
  reported in README › Hardware checklist and the ROADMAP Hardware column (🟡 when partial), nothing more.
- Owner-reported bugs are fixed first, before continuing the roadmap.

## Known pitfalls (learned the hard way)
- `StateFlow` drops equal values: a landed Tello sends identical state packets, so logic that must notice
  *time passing* (motor-stop detection) is **polled**, not driven by emissions.
- Android reports joystick axes only on change: held sticks need the input pump, and resting stick drift is
  then held too (check HUD indicator dots are grey at rest; raise the dead-zone if not).
- `WifiNetworkSpecifier` networks are not re-delivered after `onLost`: re-request (Android shows its dialog).
- A Compose `Dialog` is a separate window: offset by insets in landscape **and it steals gamepad focus**
  (sticks went neutral, B could not land). Use full-screen layers inside the flight screen instead.
- `MediaCodec.dequeueInputBuffer(0)` silently dropping NALs caused video artifacts: feed whole frames from a
  dedicated thread and wait for input buffers.
- Height alone is not "flying" (lifting the drone by hand changes it); motors running is.
- `IntArray` has no `mapNotNull` (`asList()` first); `String.format` needs `Locale.ROOT` for HUD digits.

## Status at end of session (2026-09-28, v0.5.6)
- Shipped: M1 safety code, Wi-Fi lock + reconnect, flight modes/expo/height limit, FPV cockpit HUD, rumble,
  photo / MP4 / flight log, Mission Pad detection, target tracking + FOLLOW.
- Hardware-validated: see README › Hardware checklist (Wi-Fi, controller, axes, takeoff/land, video,
  telemetry, Wi-Fi loss alert + reconnect, rumble, tracking select/follow box, hover stability).
- Next (ROADMAP › Next up): finish M1 failsafe validation; first FOLLOW flight (check yaw/throttle signs);
  mission editor (M6); advanced vision (orbit, gestures, person tracking).

## Definition of done
- [ ] `testDebugUnitTest` and `assembleRelease` pass (CI green).
- [ ] New pure logic has unit tests.
- [ ] `ROADMAP.md` "Code" column and relevant `docs/` updated; architectural choices recorded in `docs/DECISIONS.md`.
- [ ] Hardware-dependent assumptions are marked `HARDWARE-UNVERIFIED`.
- [ ] New hardware checks added (unticked) to README › Hardware checklist; ticked only on the owner's report.
- [ ] User-visible changes added to `CHANGELOG.md` under the current version line.
- [ ] User-visible behavior changes are reflected in README (controls table) and the Settings dialog text.
