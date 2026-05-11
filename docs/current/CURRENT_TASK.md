# Current Task

## Active Development Focus

The current round has produced a `1.6.31` baseline. The next work should start from repository facts rather than old chat memory.

Primary active focus areas:

1. Device-test daily-board schedule filtering: finished timed events should no longer appear in today's schedule
2. Device-test running-event highlight: currently running board events should show a gold outline / glow
3. Device-test calendar pending draft behavior: long-press blank timeline space should cancel the pending "new event" state, and opening an existing event should clear it
4. Regression-test the `1.6.30` fixes: todo preview should not complete items, destructive actions should confirm, desktop-sync notification should route correctly
5. Continue board / dashboard and calendar polish without regressing existing flows
6. Keep version metadata and docs aligned with the actual code state

## Immediate Practical Next Steps

When a new session takes over, it should usually do these in order:

1. run `git status --short --branch`
2. verify current version number and APK naming
3. if testing on device, install `app/build/outputs/apk/debug/PaykiTodo-1.6.31-debug.apk`
4. create a today event that has already ended and verify it does not appear on the daily board
5. create a currently running today event and verify the gold outline / glow appears
6. tap a blank calendar time slot to show the pending new-event card, then long-press blank time-axis space to cancel it
7. tap a blank calendar time slot, then open an existing event and verify the pending card disappears
8. decide the next smallest UI polish item based on device observations

## Repository-Verified Notes

The current code baseline includes these specific 1.6.31 changes:

1. `DashboardChrome.kt` computes `boardMoment` and filters today's schedule through `boardEventVisibleForToday`
2. `DashboardChrome.kt` highlights currently running board events through `boardEventInProgress`
3. `CalendarPanel.kt` clears pending drafts when opening existing event details
4. `CalendarPanel.kt` cancels pending drafts on long-press over blank timeline space
5. `app/build.gradle.kts` is bumped to `1.6.31 / 103`
6. `./gradlew assembleDebug` has succeeded for this version

## What Not To Do Immediately

- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`
- do not assume device behavior is fixed until the user actually tests it

## Current External Dependency

No external file is needed for the current 1.6.31 verification task.

If future work touches icon generation or adaptation, verify the intended in-repo resource chain before reprocessing any external image.
