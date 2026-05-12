# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.47` / `versionCode 119`
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.47-debug.apk`
- Latest feature round added reminder audio strategy and Settings restructuring:
  1. Reminder playback channel can be alarm, accessibility, notification, or media.
  2. PaykiTodo internal reminder volume percentage is stored and applied to self-played audio.
  3. Optional temporary system-channel volume boost can raise the selected global stream during playback, then restore it. It is off by default.
  4. Work / class quiet mode suppresses sound by default, strengthens vibration, and forces calendar reminders into full-screen / accessibility fallback.
  5. Settings is split into common and advanced sections.
  6. Wiki explains the new audio strategy and the Android limitation around true per-app system volume.

Previous feature round (`1.6.46`) refined Wiki, daily board, and drawer header visuals:
  1. In-app Wiki keeps a left menu / right article layout on phone-sized screens.
  2. Daily board distinguishes no schedule today from all of today's schedule already finished.
  3. Drawer header app icon is clipped into the circular header surface and enlarged.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/AppSettingsStore.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/alarm/ReminderAlertController.kt`
- `app/src/main/java/com/example/todoalarm/alarm/ReminderForegroundService.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/assets/wiki/index.html`

## Current Verification Focus

1. Install `PaykiTodo-1.6.47-debug.apk`
2. Open Settings -> 提醒声音策略
3. Verify playback channel choices are visible and persistent
4. Verify internal volume affects reminder / preview loudness
5. Verify quiet mode suppresses reminder sound and uses stronger vibration
6. Verify temporary volume boost restores the original system stream volume after the reminder ends
7. Verify the Settings common / advanced split is clearer than the previous mixed categories

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
