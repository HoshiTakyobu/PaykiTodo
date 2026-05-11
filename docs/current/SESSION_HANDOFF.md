# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.35` / `versionCode 107`
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.35-debug.apk`
- Minimal verification passed:
  - `./gradlew assembleDebug` using Android Studio bundled `jbr`
- Latest feature round addressed reminder input, batch import, and custom snooze parsing:
  1. Todo and calendar editors now share a comma-separated reminder input syntax
  2. Supported reminder tokens include relative minutes, same-day `HH:mm`, current-year `MM-DD HH:mm`, and full `YYYY-MM-DD HH:mm`
  3. Reminder entries later than DDL / event start, malformed entries, and new-item past reminders are rejected in the editor UI
  4. Normal todos now persist and schedule multiple reminder offsets through `reminderOffsetsCsv`
  5. Todo batch import now uses lightweight comma rows: `DDL时间,任务名称,提醒时间`
  6. Batch-add buttons are visible on daily board / active todo surfaces and in the calendar header
  7. Calendar batch `Remind=` and custom snooze inputs reuse the same reminder-time parser

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/ui/ReminderInputParser.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoBatchImport.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoEditorDialog.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarEventEditorDialog.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRecurrence.kt`
- `app/src/main/java/com/example/todoalarm/data/ReminderOffsetCodec.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`

## Reminder Syntax Note

Shared reminder input examples:

```text
5,15,16:30,05-10 15:00,2026-05-10 14:30
```

- `5` / `15`: remind 5 or 15 minutes before DDL / event start
- `16:30`: remind at 16:30 on the DDL / event-start date
- `05-10 15:00`: remind at that date-time in the current year
- `2026-05-10 14:30`: remind at the full date-time
- All tokens are separated by English commas

Todo batch-import syntax uses `|` between fields so the reminder comma syntax remains unambiguous:

```text
2026-05-12 18:00,写报告,5
05-13 09:30,给老师发消息,09:00
无DDL,整理 Obsidian 待办
```

Todo batch rows intentionally default group / ring / vibrate and accept only one reminder token to keep hand input fast.

## Smallest Safe Next Step

The next session should device-test the current APK rather than immediately refactor:

1. Install `PaykiTodo-1.6.35-debug.apk`
2. Verify todo reminder input accepts multiple valid entries and blocks invalid entries
3. Verify calendar reminder input does the same for timed and all-day events
4. Verify a todo with `5,15` schedules and fires both reminder points
5. Verify `批量添加待办` parses comma rows and reports invalid rows
6. Verify standalone batch buttons are visible and not hidden only in overflow menus
7. Verify custom snooze input accepts both minutes and a future clock time

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
