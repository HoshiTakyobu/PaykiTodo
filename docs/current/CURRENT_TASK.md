# Current Task

## Active Development Focus

The current round has produced a `1.6.42` baseline. The implemented focus was launch-screen icon cleanup and reminder snooze behavior:

1. The launch screen now uses a transparent logo asset so the central white square no longer covers the background sun circle.
2. Launcher / install icons still use the existing white-background adaptive icon art, avoiding launcher regression.
3. Custom snooze input no longer has a 180-minute cap; the target only needs to be in the future.
4. When a todo snooze target is later than the current DDL, the todo DDL is moved to that target time.
5. Snoozed todos pin the next reminder to the target time by storing a zero-minute reminder offset.

## Immediate Practical Next Steps

When testing on a device, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.42-debug.apk`
2. start the app and verify the launch-screen logo has no white square background
3. trigger a todo reminder and enter a custom snooze more than 180 minutes away, for example `18:00`
4. confirm the custom snooze input accepts the value if it is in the future
5. confirm the todo DDL moves to the snooze target when the target is later than the old DDL
6. confirm the next reminder is scheduled at the target time

## Repository-Verified Notes

The current code baseline includes these specific `1.6.42` changes:

1. `ic_launcher_art_transparent.png` was added under `drawable-nodpi` for launch-screen use.
2. `LaunchScreen` in `DashboardChrome.kt` uses `R.drawable.ic_launcher_art_transparent`.
3. `ReminderInputParser.parseSnoozeInput` no longer accepts a `maxMinutes` cap and no longer rejects values over 180 minutes.
4. `TodoRepository.snoozeTodo` updates DDL when the snooze target is later than the current DDL and sets reminder offset to zero for todo snoozes.
5. `ReminderActivity`, `InputSyntaxHelp`, and the Wiki now explain the new snooze / DDL behavior.
6. `app/build.gradle.kts` is bumped to `1.6.42 / 114`.
7. `./gradlew assembleDebug` has succeeded for this version using Android Studio bundled `jbr`.

## What Not To Do Immediately

- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`
- do not assume device UI polish is fully verified until the user tests on device

## Current External Dependency

No external file is needed for the current `1.6.42` verification task.
