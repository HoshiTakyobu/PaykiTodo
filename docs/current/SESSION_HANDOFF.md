# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.42` / `versionCode 114`
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.42-debug.apk`
- Minimal verification passed:
  - `./gradlew assembleDebug` using Android Studio bundled `jbr`
- Latest feature round fixed launch-screen icon and snooze / DDL behavior:
  1. Launch screen uses transparent `ic_launcher_art_transparent`; no white square should appear behind the logo.
  2. Launcher and install icons still use the white-background adaptive icon art.
  3. Custom snooze no longer has a 180-minute cap.
  4. Snooze targets must still be in the future.
  5. For todos, if the snooze target is later than the current DDL, the DDL is moved to that target.
  6. Snoozed todos store a zero-minute reminder offset so the next reminder fires at the new target time.

Previous feature round refined Daily Board schedule-row visuals:
  1. Gold in-progress rows and normal rows use the same card/padding structure.
  2. Left vertical color bars align in the same column across row states.
  3. Normal rows have a thin same-color border instead of only a clipped side arc.
  4. In-progress rows use a gold border and subtle inner highlight, not a broad yellow overlay.

Previous feature round fixed todo batch-import DDL parsing:
  1. `16:30` in the DDL field resolves to today 16:30.
  2. `16：30` with a Chinese colon is normalized and accepted.
  3. `05-13 09:30`, `2026-05-12 18:00`, and `无DDL` remain supported.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/res/drawable-nodpi/ic_launcher_art_transparent.png`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/ReminderInputParser.kt`
- `app/src/main/java/com/example/todoalarm/ui/ReminderActivity.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/ui/InputSyntaxHelp.kt`
- `app/src/main/assets/wiki/index.html`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`

## Current Verification Focus

1. Install `PaykiTodo-1.6.42-debug.apk`
2. Verify the launch-screen logo has no white square background
3. Trigger a todo reminder and input a future custom snooze beyond 180 minutes, for example `18:00`
4. Verify the input is accepted and no 180-minute error appears
5. Verify the todo DDL changes to the snooze target when the target is later than the old DDL
6. Verify the reminder is scheduled at the new target time

## Required Reading For A New Session

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/PAYKITODO_SESSION_LEDGER.md`

## Recommended First Output From A New Session

Before editing code, the new session should output:

1. a 5-10 line summary of project background
2. a 5-10 line summary of current repository state
3. the smallest next coding step it plans to take

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
