# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.48` / `versionCode 120`
- Latest debug APK path after build should be `app/build/outputs/apk/debug/PaykiTodo-1.6.48-debug.apk`
- Latest feature round refined Settings and reminder audio behavior:
  1. Reminder playback channel is selected through a compact dropdown row rather than a button group.
  2. PaykiTodo internal volume uses a 0-100 slider plus numeric input.
  3. Temporary system-channel boost target uses the same slider + numeric input pattern.
  4. Calendar week-start and default calendar reminder mode are compact dropdown rows.
  5. 工作模式 keeps ring enabled, strengthens vibration, and keeps calendar reminders on the full-screen / accessibility fallback chain.
  6. Desktop sync is back in common Settings; diagnostics / backup / crash logs remain advanced maintenance surfaces.
  7. `docs/current/UI_DESIGN_RULES.md` records the project rule against large button-group option UIs.

Previous feature round (`1.6.47`) added the underlying reminder-audio strategy settings and playback support.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/alarm/ReminderAlertController.kt`
- `app/src/main/java/com/example/todoalarm/alarm/ReminderForegroundService.kt`
- `app/src/main/assets/wiki/index.html`
- `docs/current/UI_DESIGN_RULES.md`

## Current Verification Focus

1. Install `PaykiTodo-1.6.48-debug.apk`
2. Open Settings -> 提醒声音策略
3. Verify there is no wrapped playback-channel button group
4. Verify internal volume and temporary boost target use slider + numeric input
5. Verify 工作模式 does not suppress ringing for reminders with ringing enabled
6. Verify 工作模式 still uses stronger vibration and calendar full-screen / accessibility fallback
7. Open Settings -> 日历与提醒 and verify week-start / default reminder mode dropdowns
8. Verify desktop sync appears under common Settings rather than advanced maintenance settings

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
