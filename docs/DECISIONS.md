# Engineering decisions

## ADR-001 — Native Android
**Decision:** Kotlin + Jetpack Compose.  
**Why:** Android-only target; direct access to UDP, MediaCodec and gamepad APIs; no cross-platform bridge is needed.

## ADR-002 — Cloud build
**Decision:** GitHub Actions generates installable APK artifacts. (Extended by ADR-008.)  
**Why:** the owner should not need Android Studio or a local Android SDK for routine testing.

## ADR-003 — Xbox as first-class input
**Decision:** use Android game-controller events; Xbox connects over Bluetooth while Wi-Fi remains dedicated to Tello.  
**Why:** physical sticks are preferable for manual FPV flight.

## ADR-004 — Separate video transport/decoder
**Decision:** UDP reception and H.264 decoding are separate components.  
**Why:** Tello video framing can be hardware-sensitive; separation improves diagnosis and testability.

## ADR-005 — Safety architecture
**Decision:** RC commands will ultimately be emitted by a fixed-rate safety loop using the latest fresh input, not directly by arbitrary UI events.  
**Why:** a lost/stale controller event must not leave a non-zero flight command active.

## ADR-006 — Pure core, thin Android adapters
**Decision:** protocol, mapping and safety logic live in Android-free Kotlin (`StickMapper`, `TelloCommands`, `TelloTelemetry`, `AnnexB`, `RcSafetyLoop` with injectable clock); Android classes only adapt events and I/O.
**Why:** safety-relevant behavior must be unit-testable in CI without a device or an aircraft.

## ADR-007 — Guarded emergency stop
**Decision:** emergency is triggered by holding the controller Menu button for 1 s; releasing earlier cancels it. A HUD banner shows arming.
**Why:** `emergency` cuts the motors and the drone falls; a single accidental press must not trigger it.

## ADR-008 — Continuous delivery with in-app self-update
**Decision:** every push to `main` publishes a GitHub Release (`v<base>.<run_number>`) with an APK signed by one stable release key stored in repository secrets; the app installs updates itself through the PackageInstaller session API.
**Why:** the owner wants zero manual steps between a merge and the phone. A stable key is mandatory because Android rejects updates signed by a different key. Updates are disabled while connected to the Tello because installing restarts the app.
