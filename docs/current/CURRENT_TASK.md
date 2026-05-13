# Current Task

## Active Development Focus

The current round has produced a `1.6.59` build. It focuses on the desktop event-click failure report, stale desktop-sync address risk, daily-board row polish, calendar header readability, and a small calendar recomposition optimization.

Completed in this round:

1. Confirmed the live phone desktop page at `192.168.0.100:18765` was still serving old desktop assets: its `/app.js` lacked `csvValue`, lacked `eventTimeline.addEventListener`, and still contained the old `recurrenceWeekdays || []).join` code.
2. Kept the local desktop event-click edit path intact and verified it with a Node-based simulated browser click: a timed event card opens the event editor and string `recurrenceWeekdays` does not block `openModal()`.
3. Desktop web `index.html` now uses `__PAYKI_VERSION__` placeholders, and `DesktopSyncWebAssets.indexHtml()` replaces them with the installed APK version at runtime.
4. Desktop web left brand block shows `v<installed-version>`, making it obvious whether the computer is still talking to an older installed APK.
5. `DesktopSyncService` now self-stops if Android restarts it while Settings has desktop sync disabled; it also stops the coordinator before returning `START_NOT_STICKY`.
6. Calendar current-time ticking no longer recomposes the whole `CalendarPanel` every 30 seconds; current-time state is scoped to the time axis and current red line, while top-level current date updates across midnight.
7. Daily-board normal schedule rows no longer receive non-progress tint/fill, and their left color bar starts closer to the row edge. Only currently running events keep the gold border.
8. Calendar header now keeps the month title on the left and actions on the right in one compact row, with the title using remaining width and ellipsis rather than being completely squeezed out.
9. Settings desktop-sync summary now says `未开启` when disabled; the usage-guide enum / intermediate panel was removed because the menu item already opens Wiki directly.
10. Removed a generic phone-side calendar editor hint under the start/end time row.
11. Version was bumped to `1.6.59` / `versionCode 131`.
12. `./gradlew.bat assembleDebug` succeeded with Android Studio bundled `jbr`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.59-debug.apk`
2. enable desktop sync on the phone and connect from the desktop browser
3. verify the desktop page left brand block shows `v1.6.59`
4. verify the HTML loads `/app.js?v=1.6.59` and `/app.css?v=1.6.59`
5. click a timed desktop event card and verify the event editor opens
6. navigate to a day with an all-day event and verify the compact all-day pill opens the event editor
7. disable desktop sync and verify the foreground service / access address does not remain visible
8. verify daily-board normal schedule rows have no outer fill or border, and in-progress rows still have gold emphasis
9. verify the calendar month title remains readable beside the header controls

## Deferred From The User's Larger Request

- Full desktop web UI parity with the phone UI is still not complete; the desktop editor remains a web adaptation, not a one-to-one clone of the Compose bottom sheets.
- Lunar-calendar date picking and yearly lunar recurrence are not implemented yet.
- A full calendar performance pass remains pending; this round only scoped the 30-second current-time recomposition.
- Android Emulator visual QA was not completed in this round; only build-level and local desktop-JS simulation checks were completed.
- Broad UI-copy cleanup is partially improved, but future screens should still be reviewed for unnecessary helper text.

## Current External Dependency

No external file is needed for the current `1.6.59` verification task.
