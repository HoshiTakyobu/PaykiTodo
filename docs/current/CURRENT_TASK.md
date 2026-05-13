# Current Task

## Active Development Focus

The current round has produced a `1.6.61` build. It continues desktop-phone parity by aligning desktop todo/event reminder editing with the phone-side mixed reminder syntax.

Completed in this round:

1. Desktop todo reminder editing now uses one mixed-syntax input instead of a single concrete reminder date-time field.
2. Desktop event reminder editing uses the same mixed-syntax parser instead of accepting only comma-separated minute values.
3. Supported examples include `5,15,16:30,05-10 15:00,2026-05-10 14:30`.
4. Number entries are treated as minutes before the anchor time.
5. `HH:mm` entries are treated as same-day concrete reminder times.
6. `MM-DD HH:mm` entries are treated as current-year concrete reminder times.
7. `YYYY-MM-DD HH:mm` entries are treated as full concrete reminder times.
8. Concrete reminder times later than the todo DDL or event start time are rejected and the desktop input is marked invalid.
9. Desktop sync API now accepts todo `reminderOffsetsMinutes`, so desktop-created / edited todos can persist multiple reminders rather than only `reminderAt`.
10. Node parser simulation passed for mixed reminder syntax and late-reminder rejection.
11. Version was bumped to `1.6.61` / `versionCode 133`.
12. `./gradlew.bat assembleDebug` succeeded with Android Studio bundled `jbr`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.61-debug.apk`
2. enable desktop sync on the phone and connect from the desktop browser
3. verify the desktop page left brand block shows `v1.6.61`
4. edit a todo and enter `5,15,16:30` when its DDL is later today; verify multiple reminders are saved
5. edit an event and enter `5,15,05-10 15:00`; verify reminders are converted and saved
6. enter a reminder later than the DDL / event start and verify the field becomes invalid and save does not proceed

## Deferred From The User's Larger Request

- Full desktop web UI parity with the phone UI is still not complete; this round only aligns reminder input semantics.
- Lunar-calendar date picking and yearly lunar recurrence are not implemented yet.
- A full calendar performance pass remains pending; the latest performance work only scoped the 30-second current-time recomposition.
- Android Emulator visual QA was not completed in this round; only build-level and local desktop-JS simulation checks were completed.
- Broad UI-copy cleanup is partially improved, but future screens should still be reviewed for unnecessary helper text.

## Current External Dependency

No external file is needed for the current `1.6.61` verification task.
