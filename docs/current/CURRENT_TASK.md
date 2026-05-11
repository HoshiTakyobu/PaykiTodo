# Current Task

## Active Development Focus

The current round has produced a `1.6.32` baseline. The next work should start from repository facts rather than old chat memory.

Primary active focus areas:

1. Device-test daily-board missed todo inclusion: missed active todos should appear in the board todo block
2. Device-test the refined phone-side delete confirmation sheet on todo, event, group, and schedule-template deletion paths
3. Regression-test daily-board schedule filtering: finished timed events should no longer appear in today's schedule
4. Regression-test running-event highlight: currently running board events should show a gold outline / glow
5. Continue board / dashboard and calendar polish without regressing existing flows
6. Keep version metadata and docs aligned with the actual code state

## Immediate Practical Next Steps

When a new session takes over, it should usually do these in order:

1. run `git status --short --branch`
2. verify current version number and APK naming
3. if testing on device, install `app/build/outputs/apk/debug/PaykiTodo-1.6.32-debug.apk`
4. create or keep a missed active todo and verify it appears on the daily board
5. open delete flows for todo, event, group, and schedule template and verify the refined dangerous-action bottom sheet appears
6. create a today event that has already ended and verify it does not appear on the daily board
7. create a currently running today event and verify the gold outline / glow appears
8. decide the next smallest UI polish item based on device observations

## Repository-Verified Notes

The current code baseline includes these specific 1.6.32 changes:

1. `DashboardChrome.kt` builds the board todo list from `missedItems + todayItems`, deduplicated by id and sorted with missed items first
2. `EditorBottomSheet.kt` refines `PaykiDecisionBottomSheet` into the shared dangerous-action confirmation UI
3. `app/build.gradle.kts` is bumped to `1.6.32 / 104`
4. `./gradlew assembleDebug` has succeeded for this version

## What Not To Do Immediately

- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`
- do not assume device behavior is fixed until the user actually tests it

## Current External Dependency

No external file is needed for the current 1.6.32 verification task.

If future work touches icon generation or adaptation, verify the intended in-repo resource chain before reprocessing any external image.
