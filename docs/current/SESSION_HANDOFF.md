# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.69` / `versionCode 141`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.6.69-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- Lunar support now includes display labels and a minimal yearly same-lunar-date recurrence rule. Do not claim the dedicated lunar date picker / lunar wheel is complete.

## Latest Fixes In 1.6.69

1. Desktop web existing event cards open the editor directly again; Node DOM simulation verified the click path and `YEARLY_LUNAR_DATE` field population.
2. Desktop-sync status is hardened so disabled sync reports no running service and no access addresses.
3. Daily-board normal schedule rows remain borderless with tighter left color-strip spacing; in-progress rows use lighter gold treatment.
4. Calendar header title/button sizing was adjusted to reduce month-title clipping.
5. `LunarCalendar.kt` now supports both display labels and resolving the same lunar month/day in a target Gregorian year.
6. Todo/event recurrence now supports `YEARLY_LUNAR_DATE` / 每年同农历月日 in phone editors, desktop web selects, generation, and preview.
7. Phone-side todo/event editor date rows append lunar labels in parentheses after the Gregorian date.
8. Desktop web todo/event editors show card-style date/time previews below segmented date inputs.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/LunarCalendar.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/*`

## Current Verification Focus

1. Install `PaykiTodo-1.6.69-debug.apk`.
2. Refresh desktop web after installing the APK and verify existing event cards open the editor directly.
3. Disable desktop sync and verify Settings shows no access address.
4. Check calendar timeline/month/agenda lunar labels for readability.
5. Create a yearly lunar recurrence test event/todo and verify preview/generated dates.

## Deferred Larger Work

- Lunar date picker.
- Dedicated lunar date picker / lunar wheel.
- Full desktop UI parity with phone UI.
- Full calendar rendering/performance optimization beyond the current-time tick scoping.
- Emulator-driven visual QA loop: `Pixel_8` AVD exists, but this round could not get a booted device listed in `adb devices` for install/screenshot verification.
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
