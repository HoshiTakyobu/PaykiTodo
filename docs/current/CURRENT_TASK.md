# Current Task

## Active Development Focus

The current round has produced a `1.6.39` baseline. The implemented focus was daily-board and My Tasks entry cleanup:

1. Daily Board is a read-only board surface and no longer shows add / batch-add buttons.
2. My Tasks exposes only todo batch import beside the bottom-right new-todo button.
3. Calendar batch import remains available from the calendar surface.
4. Daily-board schedule rows now align the left color strip to the measured height of the event text block.

## Immediate Practical Next Steps

When testing on a device, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.39-debug.apk`
2. open 每日看板 and verify there are no add / batch-add buttons
3. check the 今日日程 rows and confirm the colored strip matches the right-side text block height
4. open 我的任务 and verify the bottom-right area shows `批量待办` next to the `+` new-todo button
5. open 日历 and verify the calendar-side batch import entry is still available
6. verify the previous reminder input and batch import syntax still behaves as expected

## Repository-Verified Notes

The current code baseline includes these specific `1.6.39` changes:

1. `DashboardChrome.kt` removed the old top `DashboardQuickActionRow` from Daily Board and My Tasks.
2. `DashboardScreen.kt` moved todo batch import into the My Tasks floating action button area.
3. `BoardScheduleEventRow` uses intrinsic row height so the event color strip fills the text block height.
4. `app/build.gradle.kts` is bumped to `1.6.39 / 111`.
5. `./gradlew assembleDebug` has succeeded for this version using Android Studio bundled `jbr`.

## What Not To Do Immediately

- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`
- do not assume device UI polish is fully verified until the user tests on device

## Current External Dependency

No external file is needed for the current `1.6.39` verification task.
