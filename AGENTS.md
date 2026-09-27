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
├── controller/              Input → RC command
│   ├── RcInput.kt           rc a b c d value object (-100..100)
│   ├── StickMapper.kt       PURE axis → RcInput (dead-zone, Mode 2 layout)      [unit-tested]
│   ├── XboxController.kt    Android MotionEvent/KeyEvent adapter → StickMapper
│   └── RcSafetyLoop.kt      SAFETY-CRITICAL fixed-rate sender + stale watchdog  [unit-tested]
├── tello/                   Aircraft protocol (see docs/PROTOCOL.md)
│   ├── TelloClient.kt       UDP 8889 commands / 8890 state, connection state
│   ├── TelloCommands.kt     PURE command string builders                         [unit-tested]
│   ├── TelloTelemetry.kt    PURE state packet parser                             [unit-tested]
│   ├── TelloVideoReceiver.kt UDP 11111 transport
│   ├── AnnexB.kt            PURE H.264 start-code scanner                        [unit-tested]
│   └── TelloH264Decoder.kt  MediaCodec → Surface
└── ui/
    ├── FlightScreen.kt      Landscape HUD composition
    ├── VideoSurface.kt      SurfaceView host for the decoder
    ├── SettingsDialog.kt
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
gradle assembleDebug       # APK → app/build/outputs/apk/debug/app-debug.apk
```
Local build without installing the SDK (Docker):
```bash
docker run --rm -v "$PWD":/w -w /w ghcr.io/cirruslabs/android-sdk:35 bash -c \
  'curl -sSLo /tmp/g.zip https://services.gradle.org/distributions/gradle-8.9-bin.zip && unzip -q /tmp/g.zip -d /opt &&
   /opt/gradle-8.9/bin/gradle --no-daemon testDebugUnitTest assembleDebug'
```
Note: AGP's `aapt2` is x86_64-only on Linux, so a full local build fails on ARM64 hosts (the owner's WSL is
aarch64). There, only pure-Kotlin logic can be checked locally; rely on CI for `assembleDebug`.

Releases: pushing a `v*` tag runs `release.yml` and publishes the APK to GitHub Releases. Bump
`versionCode`/`versionName` in `app/build.gradle.kts` first. Never tag without the owner's explicit request.

## Safety rules (must never regress)
1. **Only `RcSafetyLoop` sends `rc` commands.** Never call `TelloClient.rc` from UI or input handlers.
2. Input older than `RcSafetyLoop.STALE_MS` ⇒ neutral RC. Do not raise this limit without an ADR.
3. App pause, controller disconnect and ViewModel clear ⇒ `neutralControls()` (and cancel emergency arming).
4. `takeoff`/`land` fire on the **first** key press only (`repeatCount == 0`), never on repeat.
5. `emergency` (motor cut, the drone falls) requires holding Menu ≥ `FlightViewModel.EMERGENCY_HOLD_MS`.
   Never map it to a single tap or an on-screen button without a guard.
6. `CONNECTED` means the Tello acknowledged `command` with `ok` — never assume it.
7. Anything not verified on a real Tello is marked `HARDWARE-UNVERIFIED` in code/docs. Don't remove the mark
   unless the owner reports a successful hardware test.

Changes touching these areas must include/adjust unit tests and mention the safety impact in the commit body.

## Conventions
- Kotlin official style (`kotlin.code.style=official`), 4-space indent, one top-level concept per file,
  trailing commas in multi-line parameter lists. No compressed one-line classes.
- Composables: stateless, parameters in / lambdas out. Colors come from `HudColors`, not literals.
- Prefer lambdas `{ vm.takeoff() }` over method references for Compose callbacks (see commit df6cb66).
- Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `ci:`, `chore:`), English.
- Feature branches + PR to `main`; `main` must always build.
- No new dependency without a reason in the PR description. No backend, analytics or network calls
  other than the Tello and the GitHub Releases link.

## Definition of done
- [ ] `testDebugUnitTest` and `assembleDebug` pass (CI green).
- [ ] New pure logic has unit tests.
- [ ] `ROADMAP.md` checkboxes and relevant `docs/` updated; architectural choices recorded in `docs/DECISIONS.md`.
- [ ] Hardware-dependent assumptions are marked `HARDWARE-UNVERIFIED`.
- [ ] User-visible behavior changes are reflected in README (controls table) and the Settings dialog text.
