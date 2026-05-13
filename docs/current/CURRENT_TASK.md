# Current Task

## Active Development Focus

The current round has produced a `1.6.60` build. It continues the larger desktop-parity request by changing the desktop web console from card-inline actions / direct editor jumps toward the phone-side preview-first interaction model.

Completed in this round:

1. Desktop todo cards no longer render inline `编辑 / 完成 / 取消 / 删除` buttons.
2. Clicking a desktop todo card opens a todo preview sheet first.
3. The todo preview sheet shows a color block, title, state, group, DDL, recurrence, reminder, and notes.
4. Todo preview actions now provide edit, complete, cancel, and delete from the preview surface.
5. Desktop event cards no longer open the editor directly when clicked.
6. Clicking a desktop timed event or compact all-day event pill opens an event preview sheet first.
7. The event preview sheet shows a color block, title, time, recurrence, location, reminder, and notes.
8. Event preview actions provide edit and delete; delete still uses the in-app dangerous confirmation modal.
9. Node simulated-browser checks passed for both paths:
   - event card -> event preview -> edit -> event editor filled
   - todo card -> todo preview -> edit -> todo editor filled
10. Version was bumped to `1.6.60` / `versionCode 132`.
11. `./gradlew.bat assembleDebug` succeeded with Android Studio bundled `jbr`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.60-debug.apk`
2. enable desktop sync on the phone and connect from the desktop browser
3. verify the desktop page left brand block shows `v1.6.60`
4. click a desktop todo card and verify it opens the todo preview rather than exposing inline buttons
5. from the todo preview, test edit, complete, cancel, and delete confirmation
6. click a timed desktop event card and verify it opens the event preview before editing
7. from the event preview, test edit and delete confirmation
8. navigate to a day with an all-day event and verify its compact pill also opens the event preview

## Deferred From The User's Larger Request

- Full desktop web UI parity with the phone UI is still not complete; this round only converts the main card interactions to preview-first behavior.
- Lunar-calendar date picking and yearly lunar recurrence are not implemented yet.
- A full calendar performance pass remains pending; the latest performance work only scoped the 30-second current-time recomposition.
- Android Emulator visual QA was not completed in this round; only build-level and local desktop-JS simulation checks were completed.
- Broad UI-copy cleanup is partially improved, but future screens should still be reviewed for unnecessary helper text.

## Current External Dependency

No external file is needed for the current `1.6.60` verification task.
