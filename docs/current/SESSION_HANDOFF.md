# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.83` / `versionCode 155`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.6.83-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- Settings -> 提示音 has a compact panel with both built-in tone and system notification tone choices.
- Todo DDL now has wheel-style lunar date picking. Event lunar start/end picking still exists and now reuses the shared picker.
- Desktop web event cards open the editor directly on card click. Todo cards still use preview-first interaction.

## Latest Fixes In 1.6.83

1. Restored the built-in reminder-tone choice in Settings through a dedicated tone panel.
2. Added wheel-style `农历 DDL` to the todo editor; selected lunar dates preserve the existing DDL clock time.
3. Extracted the lunar picker to `LunarDatePickerDialog.kt` and reused it from event and todo editors.
4. Split the calendar header into a dedicated month-title row plus an action row so `2026年5月` is not squeezed by action buttons.
5. Increased daily-board event row text spacing from the left color strip and added vertical breathing room.
6. `1.6.83` was built successfully with Android Studio bundled JBR.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoEditorDialog.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarEventEditorDialog.kt`
- `app/src/main/java/com/example/todoalarm/ui/LunarDatePickerDialog.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/*`

## Current Verification Focus

1. Install `PaykiTodo-1.6.83-debug.apk`.
2. Verify Settings -> 提示音 shows both built-in and system notification tone choices.
3. Verify Todo edit/create -> 农历 DDL opens the wheel-style picker, converts lunar date correctly, and preserves time.
4. Check calendar header month text and daily-board schedule row spacing on the user's phone.
5. If the desktop browser still behaves as preview-first after installing `1.6.83`, check whether the page is still serving old `/app.js` assets from the old installed APK.

## Deferred Larger Work

- Full desktop UI parity with phone UI.
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
