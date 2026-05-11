# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.40` / `versionCode 112`
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.40-debug.apk`
- Minimal verification passed:
  - `./gradlew assembleDebug` using Android Studio bundled `jbr`
- Latest feature round fixed todo batch-import DDL parsing:
  1. `16:30` in the DDL field now resolves to today 16:30.
  2. `16：30` with a Chinese colon is normalized and accepted.
  3. `05-13 09:30`, `2026-05-12 18:00`, and `无DDL` remain supported.
  4. Same-day times that have already passed still fail the existing future-DDL validation.
  5. Todo batch-import help text, shared input help, and Wiki examples now document the same-day DDL form.

Previous feature round fixed Daily Board and My Tasks entry placement:
  1. Daily Board no longer shows add / batch-add buttons; it is treated as a read-only board surface.
  2. My Tasks no longer shows the old top double-button row.
  3. My Tasks now exposes only `批量待办` beside the bottom-right `+` new-todo button.
  4. Calendar batch import remains on the calendar surface.
  5. Daily-board schedule event color strips now use intrinsic row height and fill the right-side text block height.

Previous feature round addressed reminder input, batch import, custom snooze parsing, and syntax discoverability:
  1. Todo and calendar editors now share a comma-separated reminder input syntax
  2. Supported reminder tokens include relative minutes, same-day `HH:mm`, current-year `MM-DD HH:mm`, and full `YYYY-MM-DD HH:mm`
  3. Normal todos now persist and schedule multiple reminder offsets through `reminderOffsetsCsv`
  4. Todo batch import uses lightweight comma rows: `DDL时间,任务名称,提醒时间`
  5. Calendar batch `Remind=` and custom snooze inputs reuse the same reminder-time parser
  6. Question-mark help buttons sit beside key input fields and open syntax examples

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/ui/TodoBatchImport.kt`
- `app/src/main/java/com/example/todoalarm/ui/InputSyntaxHelp.kt`
- `app/src/main/assets/wiki/index.html`
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
16:30,写报告,5
05-13 09:30,给老师发消息,09:00
无DDL,整理 Obsidian 待办
```

- The first DDL field can be `HH:mm`; it means today at that time.
- A Chinese colon is accepted in the DDL field, so `16：30` is treated like `16:30`.
- Todo batch rows intentionally default group / ring / vibrate and accept only one reminder token to keep hand input fast.

## Smallest Safe Next Step

The next session should device-test the current APK rather than immediately refactor:

1. Install `PaykiTodo-1.6.40-debug.apk`
2. Open My Tasks -> `批量待办`
3. Test `16:30,写报告,5` while 16:30 is still in the future
4. Test `16：30,写报告,5`
5. Test a same-day time that has already passed and verify it is rejected
6. Verify previous Daily Board / My Tasks entry placement from 1.6.39 still looks correct

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
