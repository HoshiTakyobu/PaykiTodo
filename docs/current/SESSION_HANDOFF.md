# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.46` / `versionCode 118`
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.46-debug.apk`
- Latest feature round refined Wiki, daily board, and drawer header visuals:
  1. In-app Wiki keeps a left menu / right article layout on phone-sized screens.
  2. Daily board distinguishes no schedule today from all of today's schedule already finished.
  3. Daily board schedule count now uses all events overlapping today.
  4. Drawer header app icon is clipped into the circular header surface and enlarged.

Previous feature round (`1.6.45`) cleaned up desktop Web UI resource structure:
  1. Desktop HTML / CSS / JS moved to `app/src/main/assets/desktop-web/`.
  2. `DesktopSyncWebAssets.kt` is now only an Android asset loader plus fallback page.
  3. `DesktopSyncCoordinator.kt` still serves the same browser routes but reads the files from APK assets.
  4. `docs/current/DESKTOP_WEB_ARCHITECTURE.md` explains why the APK contains desktop UI files and how this should evolve later.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/assets/wiki/index.html`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`

## Current Verification Focus

1. Install `PaykiTodo-1.6.46-debug.apk`
2. Open the in-app Wiki and verify the left navigation / right content layout in portrait
3. Tap several Wiki entries and verify only the right article changes
4. Verify Daily Board says `今天暂无日程` when there are no events today
5. Verify Daily Board says `太棒了！今天的日程都结束了~` after today's events have ended
6. Verify drawer header icon no longer shows a white rounded rectangle inside the circular header surface

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
