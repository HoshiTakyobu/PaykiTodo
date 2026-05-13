# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.81` / `versionCode 153`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.6.81-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- Desktop web event cards now open the editor directly on card click. Todo cards still use preview-first interaction.
- Lunar support includes display labels, yearly same-lunar-date recurrence, and event lunar start/end picking for all-day and timed events. Do not claim todo lunar wheel support is complete.

## Latest Fixes In 1.6.81

1. Desktop web existing timed event cards open the event editor directly on click, avoiding the previous preview-first path that made editing hard to discover.
2. Node DOM simulation with the live phone snapshot verified event `15`: card click opens `event-modal`, fills the title, sets `editingEventId`, and does not open `event-preview-modal`.
3. Settings -> 电脑同步 hides the entire `连接地址` section while desktop sync is disabled.
4. Daily-board normal schedule rows remain borderless and the left color strip is closer to the row edge; in-progress rows keep the gold treatment.
5. Calendar header uses a left flexible year/month pill and compact right-side action buttons to reduce month-title clipping.
6. `1.6.81` was built successfully with Android Studio bundled JBR.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/*`

## Current Verification Focus

1. Install `PaykiTodo-1.6.81-debug.apk`.
2. Refresh the desktop browser page after install, then verify clicking an existing event opens the editor directly and saving updates the existing event.
3. Disable desktop sync and verify Settings shows no access-address section.
4. Check calendar header month text and daily-board schedule row spacing on the user's phone.
5. If the desktop browser still behaves as preview-first after installing `1.6.81`, check whether the page is still serving old `/app.js` assets from the old installed APK.

## Deferred Larger Work

- Full desktop UI parity with phone UI.
- Todo lunar wheel picker.
- Full calendar rendering/performance optimization.
- Emulator-driven visual QA loop: `Pixel_8` exists, but the latest hidden launch attempt did not produce an `adb devices` entry within 90 seconds.
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