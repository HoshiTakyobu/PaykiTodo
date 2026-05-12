# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.49` / `versionCode 121`
- Latest debug APK path after build should be `app/build/outputs/apk/debug/PaykiTodo-1.6.49-debug.apk`
- Latest feature round restored work-mode semantics while keeping the compact Settings UI:
  1. 工作模式 is a quiet strong-reminder mode: sound is suppressed by default, vibration is strengthened, and calendar reminders stay on full-screen / accessibility fallback.
  2. New reminder defaults again disable ringing when work mode is enabled.
  3. Reminder playback channel remains a compact dropdown row rather than a button group.
  4. PaykiTodo internal volume and temporary system-channel boost target remain 0-100 slider + numeric input controls.
  5. Calendar week-start and default calendar reminder mode remain compact dropdown rows.
  6. `docs/current/UI_DESIGN_RULES.md` now records both the no-large-button-group UI rule and the expected detailed Chinese commit-message style.

Previous feature round (`1.6.48`) introduced the compact Settings controls and moved desktop sync back into common Settings.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/alarm/ReminderAlertController.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/assets/wiki/index.html`
- `docs/current/UI_DESIGN_RULES.md`

## Current Verification Focus

1. Install `PaykiTodo-1.6.49-debug.apk`
2. Open Settings -> 提醒声音策略
3. Verify 工作模式 suppresses outward sound and strengthens vibration
4. Verify 工作模式 still forces calendar reminders through full-screen / accessibility fallback
5. Verify compact dropdown / slider controls from `1.6.48` were not regressed back to large button groups

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
