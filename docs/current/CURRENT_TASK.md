# Current Task

## Active Development Focus

The current round has produced a `1.6.38` baseline. The implemented focus was fixing picture-based launcher icon scale and white background.

Primary verification focus areas:

1. Todo editor reminder input accepts mixed entries such as `5,15,16:30,05-10 15:00,2026-05-10 14:30`
2. Calendar editor reminder input accepts the same syntax for timed events and all-day events
3. Invalid reminder entries show an error state and disable the save button
4. A normal todo with multiple future reminders schedules more than one trigger time
5. Todo batch import can import multiple comma-separated rows and blocks invalid rows
6. Batch buttons are visible from daily board / active todo surfaces and the calendar header
7. Full-screen and accessibility reminder custom snooze inputs accept minutes or concrete future times
8. Question-mark help buttons open the correct syntax help beside reminder, batch, and snooze fields
9. In-app Wiki describes the current 1.6.36 input syntax rather than older menu / picker behavior

## Immediate Practical Next Steps

When testing on a device, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.38-debug.apk`
2. create a todo with DDL in the future and reminder input `5,15`
3. create a calendar event and test `5,15,HH:mm` where `HH:mm` is before the event start
4. verify an illegal value later than DDL / event start turns the input red and disables save
5. open daily board or 我的任务 and test `批量添加待办` with `2026-05-12 18:00,写报告,5`
6. open 日历 and test the standalone `批量` button
7. trigger a reminder and test custom snooze input with `5` and a future `HH:mm`
8. tap the question-mark buttons next to these inputs and verify the displayed examples match the current syntax

## Repository-Verified Notes

The current code baseline includes these specific `1.6.34` changes:

1. `ReminderInputParser.kt` implements the shared comma-separated reminder syntax
2. `TodoDraft` now carries `reminderOffsetsMinutes` while preserving single-reminder compatibility
3. `TodoItem.reminderTriggerTimesMillis()` uses todo offset lists when present
4. `TodoRepository` persists normal todo reminder offsets to `reminderOffsetsCsv`
5. `TodoEditorDialog` and `CalendarEventEditorDialog` use the new text input and save-button validation
6. `TodoBatchImport.kt` uses lightweight `DDL时间,任务名称,提醒时间` todo batch syntax
7. `DashboardChrome.kt` and `CalendarPanel.kt` expose standalone batch buttons
8. Calendar batch `Remind=` and custom snooze inputs reuse the shared reminder-time parser
9. `InputSyntaxHelp.kt` adds a shared syntax help button/dialog
10. `app/src/main/assets/wiki/index.html` documents the current input syntax
11. launcher adaptive icons now use `@drawable/ic_launcher_art` and the old vector mark resources are deleted
12. launcher picture art is reprocessed with pure-white opaque background and smaller centered content
13. `app/build.gradle.kts` is bumped to `1.6.38 / 110`
14. `./gradlew assembleDebug` has succeeded for this version

## What Not To Do Immediately

- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`
- do not assume reminder behavior is fully verified until the user tests on device

## Current External Dependency

No external file is needed for the current `1.6.38` verification task.
