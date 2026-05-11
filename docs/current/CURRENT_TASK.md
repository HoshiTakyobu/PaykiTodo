# Current Task

## Active Development Focus

The current round has produced a `1.6.40` baseline. The implemented focus was fixing todo batch-import DDL input:

1. `16:30` in the DDL field now means today 16:30.
2. `16：30` with a Chinese colon is normalized and accepted the same way.
3. Existing complete date-time formats still work: `05-13 09:30` and `2026-05-12 18:00`.
4. If the resolved same-day DDL is not later than the current time, the existing `DDL 必须晚于当前时间` validation still blocks it.

## Immediate Practical Next Steps

When testing on a device, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.40-debug.apk`
2. open 我的任务 -> `批量待办`
3. enter `16:30,写报告,5` when 16:30 is still in the future and verify it previews/imports successfully
4. enter `16：30,写报告,5` and verify the Chinese colon variant also works
5. enter a same-day time that has already passed and verify the row is rejected as a past DDL
6. verify `05-13 09:30,给老师发消息,09:00` and `无DDL,整理 Obsidian 待办` still behave as before

## Repository-Verified Notes

The current code baseline includes these specific `1.6.40` changes:

1. `TodoBatchImport.kt` normalizes Chinese colon to English colon before DDL parsing.
2. `TodoBatchImport.kt` added an `HH:mm` DDL regex that resolves to today's date.
3. Todo batch-import dialog help and default sample now show `16:30` as a valid DDL.
4. `InputSyntaxHelp.kt` and the in-app Wiki document same-day DDL input.
5. `app/build.gradle.kts` is bumped to `1.6.40 / 112`.
6. `./gradlew assembleDebug` has succeeded for this version using Android Studio bundled `jbr`.

## What Not To Do Immediately

- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`
- do not assume device UI polish is fully verified until the user tests on device

## Current External Dependency

No external file is needed for the current `1.6.40` verification task.
