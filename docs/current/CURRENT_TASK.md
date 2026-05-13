# Current Task

## Active Development Focus

The current round has produced a `1.6.70` build. It fixes the desktop event edit path, tightens desktop-sync/offline status behavior, polishes daily-board and calendar header UI, and extends lunar support from display labels to yearly same-lunar-date recurrence.

Completed in this round:

1. Desktop web existing event cards now open the event editor directly again, instead of stopping at the preview sheet.
2. A Node-based DOM simulation verified that clicking an existing desktop event opens the editor, fills the title, sets `editingEventId`, and preserves `YEARLY_LUNAR_DATE` recurrence selection.
3. Desktop-sync status now reports `running=false` and an empty address list whenever Settings says desktop sync is disabled, even if a background service stop is still settling.
4. Daily-board schedule rows keep normal events borderless and move the left color strip closer to the block edge; in-progress rows keep only a lighter gold outline / inner glow.
5. Calendar header title width is protected and header buttons are more compact, reducing month-title clipping such as missing “5月”.
6. `LunarCalendar` now converts Gregorian dates to lunar labels and can also resolve the same lunar month/day in a target Gregorian year.
7. Todo and event recurrence support now includes `YEARLY_LUNAR_DATE` / 每年同农历月日, with generation and preview support.
8. Phone-side todo/event editors and desktop web recurrence selects expose the yearly lunar recurrence option.
9. Phone-side todo/event editor date rows now append the corresponding lunar date in parentheses after the Gregorian date.
10. Desktop web todo/event editors now show card-style date/time previews below segmented date inputs.
11. Calendar timeline headers, month cells, and agenda/list date surfaces continue to show lunar labels.
12. Version was bumped to `1.6.70` / `versionCode 142`.
13. `./gradlew.bat assembleDebug` succeeded with Android Studio bundled `jbr`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.70-debug.apk`
2. verify desktop web existing event cards open the editor directly after installing the new APK and refreshing the browser page
3. verify Settings -> 电脑同步 shows no address when desktop sync is disabled
4. open the calendar timeline/month/agenda views and verify lunar labels remain readable
5. create a test todo/event with “每年同农历月日” and verify the recurrence preview/generation dates match the same lunar birthday pattern

## Deferred From The User's Larger Request

- Lunar date picking is not implemented yet.
- The current lunar recurrence support is the minimal yearly same-lunar-date path; a dedicated lunar date picker / lunar wheel remains pending.
- Full desktop web UI parity with the phone UI is still not complete.
- A full calendar performance pass remains pending; the latest performance work only scoped the 30-second current-time recomposition.
- Android Emulator visual QA was attempted against the installed `Pixel_8` AVD; the emulator process did not become visible to `adb devices`, so no reliable UI screenshot/interaction loop was completed in this round.
- Broad UI-copy cleanup is partially improved, but future screens should still be reviewed for unnecessary helper text.

## Current External Dependency

No external file is needed for the current `1.6.70` verification task.
