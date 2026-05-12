# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.43` / `versionCode 115`
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.43-debug.apk`
- Latest feature round improved desktop web editing parity:
  1. Desktop sync has `PUT /api/todos/{id}` for existing todo edits.
  2. Desktop todo cards include an explicit `编辑` action.
  3. The desktop todo modal supports edit mode, save, and delete.
  4. Editable todo fields include title, notes, DDL, reminder time, group, recurrence, ring, and vibration.
  5. Timed event and all-day event cards include explicit `编辑` actions.

Previous feature round fixed launch-screen icon and snooze / DDL behavior:
  1. Launch screen uses transparent `ic_launcher_art_transparent`.
  2. Custom snooze no longer has a 180-minute cap.
  3. Snoozing a todo later than its current DDL moves the DDL and pins the next reminder to that target.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncWebAssets.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`

## Current Verification Focus

1. Install `PaykiTodo-1.6.43-debug.apk`
2. Enable desktop sync and connect from a computer browser
3. Edit an existing todo from the desktop page and change DDL / reminder time
4. Verify the phone shows the updated todo and reminder timing
5. Edit a timed event and an all-day event from explicit `编辑` buttons
6. Verify desktop delete buttons still show browser confirmation before destructive actions

## Required Reading For A New Session

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/PAYKITODO_SESSION_LEDGER.md`

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
