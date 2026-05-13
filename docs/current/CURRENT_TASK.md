# Current Task

## Active Development Focus

The current round has produced a `1.6.56` baseline. It keeps the 1.6.54 desktop event-edit fix and adds a focused UI-copy cleanup plus lightweight desktop-web motion polish.

Completed in this round:

1. Desktop web assets now use versioned `app.css?v=1.6.56` and `app.js?v=1.6.56` URLs.
2. `DesktopSyncServer` sends no-cache headers and allows PUT in CORS methods.
3. Desktop event clicks use delegated handling on the timeline container rather than per-card rebinding only.
4. Desktop event/todo editors tolerate `recurrenceWeekdays` from snapshots as either an array or a comma-separated string; this prevents `.join()` from throwing before `openModal()`.
5. All-day events are visible through compact per-day pills, so they have an edit entry without restoring the large separate all-day strip.
6. `PUT /api/events/{id}` was verified against the live phone sync server at `192.168.0.100:18765` with token `6CF4`.
7. Desktop sync status no longer returns/shows LAN addresses when the service is not running.
8. Daily-board normal schedule rows no longer show an outer border; only currently running rows keep the gold highlight.
9. Calendar header title/actions are split to avoid the month title being squeezed out.
10. Drawer and top-bar naming are unified to `每日看板`.
11. Phone-side todo/calendar editor bottom sheets no longer show generic explanatory subtitles.
12. Settings removes redundant helper copy from the reminder-chain test, default snooze picker, and About usage-guide entry.
13. Desktop web adds lightweight tab/card/modal/button motion with reduced-motion fallback.
14. Version and current docs were synchronized for `1.6.56`.
## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.56-debug.apk`
2. enable desktop sync on the phone and connect from the desktop browser
3. force-refresh the desktop page once, then verify the HTML loads `/app.js?v=1.6.56`
4. click a timed desktop event card and verify the event editor opens
5. navigate to a day with an all-day event and verify the compact all-day pill opens the event editor
6. save a small event edit, refresh, and verify the edit persists
7. disable desktop sync and verify Settings no longer shows stale access addresses
8. verify daily-board normal schedule rows have no outer border and in-progress rows still have gold emphasis
9. verify the calendar month title is readable beside the header controls

## Deferred From The User's Larger Request

- Full desktop web UI parity with the phone UI is not complete; this round only fixed the broken edit path and the all-day edit entry.
- Lunar-calendar date picking and yearly lunar recurrence are not implemented yet.
- A full calendar performance pass remains pending.
- Android Emulator visual QA was not completed in this round; only build/static/API checks were completed.
- Broad UI-copy cleanup is partially improved in 1.6.56, but future screens should still be reviewed for unnecessary helper text.

## Current External Dependency

No external file is needed for the current `1.6.56` verification task.
