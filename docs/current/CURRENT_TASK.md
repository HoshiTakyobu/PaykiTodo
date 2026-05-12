# Current Task

## Active Development Focus

The current round has produced a `1.6.41` baseline. The implemented focus was refining Daily Board schedule-row visuals:

1. In-progress and normal schedule rows now use the same card structure and padding.
2. The left vertical color bars align to the same column across gold and blue/normal events.
3. Normal schedule rows now have a thin border using the same color as the left bar.
4. In-progress rows no longer use a large yellow overlay; they use a gold border plus a very subtle inner highlight so the text stays readable.

## Immediate Practical Next Steps

When testing on a device, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.41-debug.apk`
2. open 每日看板 with at least one in-progress and one upcoming schedule item
3. verify the gold left bar and the blue/normal left bar align in the same vertical column
4. verify normal schedule rows show a full thin border, not a clipped side arc
5. verify the in-progress row text remains readable and is not covered by a broad yellow glow
6. verify the previous todo batch DDL fix still works with `16:30,写报告,5`

## Repository-Verified Notes

The current code baseline includes these specific `1.6.41` changes:

1. `BoardScheduleEventRow` now uses one `Surface` card structure for both in-progress and normal schedule rows.
2. The left strip fills the measured row content height and shares the same horizontal position for all row states.
3. Normal rows use a subtle same-color border.
4. In-progress rows use a gold border and very low-alpha inner surface highlight instead of an external glow overlay.
5. `app/build.gradle.kts` is bumped to `1.6.41 / 113`.
6. `./gradlew assembleDebug` has succeeded for this version using Android Studio bundled `jbr`.

## What Not To Do Immediately

- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`
- do not assume device UI polish is fully verified until the user tests on device

## Current External Dependency

No external file is needed for the current `1.6.41` verification task.
