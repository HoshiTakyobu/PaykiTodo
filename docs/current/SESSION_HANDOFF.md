# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.62` / `versionCode 134`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.6.62-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- Lunar support is currently display-only. Do not claim lunar date picking or lunar recurrence is complete.

## Latest Fixes In 1.6.62

1. Added `LunarCalendar.kt`, backed by Android ICU `ChineseCalendar`.
2. Three-day / one-day timeline headers now show lunar labels below Gregorian day numbers.
3. Month-view day cells now show lunar labels.
4. Agenda/list week strip and day-group headers now show lunar labels.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/LunarCalendar.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/*`

## Current Verification Focus

1. Install `PaykiTodo-1.6.62-debug.apk`.
2. Check calendar timeline headers for readable lunar labels.
3. Check month-view cells for readable lunar labels without hiding event chips.
4. Check agenda/list week strip and daily section headers for readable lunar labels.

## Deferred Larger Work

- Lunar date picker.
- Lunar yearly recurrence and lunar birthday reminders.
- Full desktop UI parity with phone UI.
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
