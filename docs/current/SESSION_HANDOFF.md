# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.39` / `versionCode 111`
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.39-debug.apk`
- Minimal verification passed:
  - `./gradlew assembleDebug` using Android Studio bundled `jbr`
- Latest feature round fixed Daily Board and My Tasks entry placement:
  1. Daily Board no longer shows add / batch-add buttons; it is treated as a read-only board surface.
  2. My Tasks no longer shows the old top double-button row.
  3. My Tasks now exposes only `批量待办` beside the bottom-right `+` new-todo button.
  4. Calendar batch import remains on the calendar surface.
  5. Daily-board schedule event color strips now use intrinsic row height and fill the right-side text block height.

Previous feature round fixed the picture-based launcher icon art:
  1. `ic_launcher.xml` and `ic_launcher_round.xml` still use `@drawable/ic_launcher_art` as foreground
  2. `ic_launcher_art`, `ic_launcher_art_v2`, and `ic_launcher_art_dark` are opaque pure-white-background PNGs
  3. icon content was scaled down and centered to avoid over-full desktop rendering
  4. launcher adaptive background is pure white
  5. device-side verification should reinstall the APK and clear launcher cache if Android still shows a stale icon

Previous feature round addressed reminder input, batch import, custom snooze parsing, and syntax discoverability:
  1. Todo and calendar editors now share a comma-separated reminder input syntax
  2. Supported reminder tokens include relative minutes, same-day `HH:mm`, current-year `MM-DD HH:mm`, and full `YYYY-MM-DD HH:mm`
  3. Reminder entries later than DDL / event start, malformed entries, and new-item past reminders are rejected in the editor UI
  4. Normal todos now persist and schedule multiple reminder offsets through `reminderOffsetsCsv`
  5. Todo batch import now uses lightweight comma rows: `DDL时间,任务名称,提醒时间`
  6. Calendar batch `Remind=` and custom snooze inputs reuse the same reminder-time parser
  7. Question-mark help buttons now sit beside key input fields and open syntax examples
  8. In-app Wiki is updated for current reminder / batch / snooze syntax

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
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

Todo batch-import syntax uses lightweight comma rows:

```text
2026-05-12 18:00,写报告,5
05-13 09:30,给老师发消息,09:00
无DDL,整理 Obsidian 待办
```

Todo batch rows intentionally default group / ring / vibrate and accept only one reminder token to keep hand input fast.

## Smallest Safe Next Step

The next session should device-test the current APK rather than immediately refactor:

1. Install `PaykiTodo-1.6.39-debug.apk`
2. Open Daily Board and confirm no add / batch-add buttons are visible
3. Confirm each Daily Board schedule row's color strip height matches the event text block
4. Open My Tasks and confirm `批量待办` is beside the bottom-right `+` button
5. Open Calendar and confirm calendar batch import remains available there
6. Verify the existing reminder input, todo batch import, calendar batch import, and Wiki help still work

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
