# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.61` / `versionCode 133`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.6.61-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- Desktop web still comes from the phone APK assets under `app/src/main/assets/desktop-web/`.
- New desktop web pages should show the installed version in the left brand block as `v1.6.61`.

## Latest Fixes In 1.6.61

1. Desktop todo reminder editing now uses one mixed-syntax input rather than a single concrete reminder date-time field.
2. Desktop event reminder editing uses the same mixed-syntax parser rather than only comma-separated minute values.
3. The parser accepts minutes, same-day `HH:mm`, current-year `MM-DD HH:mm`, and full `YYYY-MM-DD HH:mm` entries.
4. Concrete reminder times later than the todo DDL / event start are rejected and the input is marked invalid.
5. Desktop sync API now reads todo `reminderOffsetsMinutes`, so desktop-created / edited todos can persist multiple reminders.
6. Node parser simulation passed for valid mixed syntax and late-reminder rejection.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/*`

## Current Verification Focus

1. Install `PaykiTodo-1.6.61-debug.apk`.
2. Enable desktop sync and connect from a desktop browser.
3. Verify the desktop page shows `v1.6.61`; if not, the phone is still serving an old APK.
4. Edit a todo and enter mixed reminder specs such as `5,15,16:30`; verify multiple reminders persist.
5. Edit an event and enter mixed reminder specs such as `5,15,05-10 15:00`; verify offsets persist.
6. Enter a reminder later than the target time and verify the desktop field is marked invalid.

## Deferred Larger Work

- Full desktop UI parity with phone UI.
- Lunar calendar and lunar yearly recurrence.
- Full calendar rendering/performance optimization beyond the current-time tick scoping.
- Emulator-driven visual QA loop.
- Broader UI-copy cleanup beyond the concrete strings changed so far.

## Required Reading For A New Session

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/UI_DESIGN_RULES.md`
7. `docs/current/DESKTOP_WEB_ARCHITECTURE.md`
8. `docs/current/PAYKITODO_SESSION_LEDGER.md`

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
