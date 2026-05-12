# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.45` / `versionCode 117`
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.45-debug.apk`
- Latest feature round cleaned up desktop Web UI resource structure:
  1. Desktop HTML / CSS / JS moved to `app/src/main/assets/desktop-web/`.
  2. `DesktopSyncWebAssets.kt` is now only an Android asset loader plus fallback page.
  3. `DesktopSyncCoordinator.kt` still serves the same browser routes but reads the files from APK assets.
  4. `docs/current/DESKTOP_WEB_ARCHITECTURE.md` explains why the APK contains desktop UI files and how this should evolve later.

Previous feature round (`1.6.44`) refined the desktop web UI after desktop editing parity:
  1. Desktop todo and event editor modals use a bottom-sheet-like structure.
  2. Modal headers use left cancel, centered title/subtitle, and right save action.
  3. Editor fields are card-styled so the page feels less like a raw admin form.
  4. Todo timeline / all-day / timed event cards have lighter action buttons, denser spacing, and clearer accent borders.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncWebAssets.kt`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/desktop-web/app.js`
- `docs/current/DESKTOP_WEB_ARCHITECTURE.md`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`

## Current Verification Focus

1. Install `PaykiTodo-1.6.45-debug.apk`
2. Enable desktop sync and connect from a computer browser
3. Verify the desktop page loads normally, including CSS and JS
4. Edit an existing todo and an existing event to confirm the asset move did not break API calls
5. If browser UI fails to load, inspect `app/src/main/assets/desktop-web/` and `DesktopSyncWebAssets.kt` first

## Required Reading For A New Session

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/DESKTOP_WEB_ARCHITECTURE.md`
7. `docs/current/PAYKITODO_SESSION_LEDGER.md`

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
