# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.76` / `versionCode 148`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.6.76-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- Desktop web event cards now open preview first; editing is launched from the preview sheet.
- Lunar support includes display labels, yearly same-lunar-date recurrence, and an all-day event lunar start/end picker. Do not claim timed-event/todo lunar wheel support is complete.

## Latest Fixes In 1.6.76

1. Desktop web existing event cards open the event preview sheet first, aligning events with the phone-side and desktop-todo preview model.
2. Event preview `编辑` still opens the editor and saving continues through the existing `PUT /api/events/{id}` update path.
3. All-day event editing now exposes `农历开始` / `农历结束` buttons backed by a compact lunar date dialog using `LunarCalendar.findDate(...)`.
4. Calendar header month text now avoids hard clipping by using a smaller title style and wrapping/ellipsis under tight width.
5. Daily-board normal schedule rows remain borderless and the left color strip sits closer to the row edge; in-progress rows keep the gold treatment.
6. Desktop-sync Settings status now refreshes immediately after sync enable/disable and key rotation, reducing stale address display.
7. `1.6.76` was built successfully with Android Studio bundled JBR.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/java/com/example/todoalarm/ui/CalendarEventEditorDialog.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/*`

## Current Verification Focus

1. Install `PaykiTodo-1.6.76-debug.apk`.
2. Refresh desktop web and verify existing event click opens preview, `编辑` opens editor, and save updates the existing event.
3. Disable desktop sync and verify Settings shows no access address.
4. Check all-day event lunar start/end selection on device.
5. Check calendar header month text and daily-board schedule row spacing on the user's phone.

## Deferred Larger Work

- Full desktop UI parity with phone UI.
- Timed-event/todo lunar wheel picker.
- Full calendar rendering/performance optimization.
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