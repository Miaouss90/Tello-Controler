# Contributing

Keep changes small and hardware-testable. Safety-critical behavior (RC loop, lifecycle, disconnect handling, takeoff/land/emergency) should be isolated and reviewed carefully.

## Branches and commits
Use feature branches for non-trivial work. Prefer Conventional Commit-style messages (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).

## Definition of done
Code builds in CI, relevant docs/tests are updated, and hardware-dependent assumptions are explicitly marked until verified on a real Tello.
