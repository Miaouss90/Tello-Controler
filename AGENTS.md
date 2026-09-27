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
│   └── TakeoffGuard.kt      pre-takeoff checks (landing is never guarded)
├── settings/                FlightSettings (rates, dead-zone, battery min, touch) + SharedPreferences repo
├── controller/              Input → RC command
│   ├── StickAxes.kt         raw stick positions
│   ├── RcInput.kt           rc a b c d value object (-100..100)
│   ├── StickMapper.kt       PURE axis → RcInput (dead-zone, Mode 2 layout)      [unit-tested]
│   ├── XboxController.kt    Android MotionEvent/KeyEvent adapter → StickMapper
│   └── RcSafetyLoop.kt      SAFETY-CRITICAL fixed-rate sender + stale watchdog  [unit-tested]
├── tello/                   Aircraft protocol (see docs/PROTOCOL.md)
│   ├── TelloClient.kt       UDP 8889 commands+acks / 8890 state, connection state, link watchdog
│   ├── LinkMonitor.kt       PURE packet freshness / rate tracker                 [unit-tested]
│   ├── TelloWifiManager.kt  explicit Internet-less TELLO-* Wi-Fi (WifiNetworkSpecifier), lost/locked state
│   ├── TelloCommands.kt     PURE command string builders                         [unit-tested]
│   ├── TelloTelemetry.kt    PURE state packet parser                             [unit-tested]
│   ├── TelloVideoReceiver.kt UDP 11111 transport
│   ├── AnnexB.kt            PURE H.264 start-code scanner                        [unit-tested]
│   └── TelloH264Decoder.kt  MediaCodec → Surface
├── record/                  Photo, MP4 recording, flight recorder
│   ├── AccessUnitAssembler.kt PURE NAL stream → SPS/PPS config + whole frames      [unit-tested]
│   ├── FlightLog.kt         PURE CSV format + per-flight FlightRecorder            [unit-tested]
│   ├── VideoRecorder.kt     MediaMuxer MP4 without re-encoding
│   └── MediaStorage.kt      MediaStore (Movies/Pictures/Download › TelloControler)
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
    ├── SettingsDialog.kt    flight settings + update
    ├── SetupChecklist.kt    guided pre-flight setup
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
1. **Only `RcSafetyLoop` sends `rc` commands.** Never call `TelloClient.rc` from UI or input handlers.
2. Input older than `RcSafetyLoop.STALE_MS` ⇒ neutral RC. Do not raise this limit without an ADR.
   Held controller sticks are re-fed by the ViewModel input pump only while the controller is present and the
   app is in the foreground; touch sticks re-report while touched (ADR-005).
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
11. Anything not verified on a real Tello is marked `HARDWARE-UNVERIFIED` in code/docs. Don't remove the mark
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

## Definition of done
- [ ] `testDebugUnitTest` and `assembleDebug` pass (CI green).
- [ ] New pure logic has unit tests.
- [ ] `ROADMAP.md` "Code" column and relevant `docs/` updated; architectural choices recorded in `docs/DECISIONS.md`.
- [ ] Hardware-dependent assumptions are marked `HARDWARE-UNVERIFIED`.
- [ ] New hardware checks added (unticked) to README › Hardware checklist; ticked only on the owner's report.
- [ ] User-visible behavior changes are reflected in README (controls table) and the Settings dialog text.
