# Current Task

## Active Development Focus

The current round has produced a `1.6.53` baseline. The focus was fixing the desktop web event-edit path that still failed in real use, then landing small phone-side UI corrections from the same report.

Completed in this round:

1. Desktop web assets now use versioned `app.css?v=1.6.53` and `app.js?v=1.6.53` URLs.
2. `DesktopSyncServer` now sends no-cache headers and allows PUT in CORS methods.
3. Desktop event clicks now use delegated handling on the timeline container rather than per-card rebinding only.
4. All-day events are visible again through compact per-day pills, so they have an edit entry without restoring the large separate all-day strip.
5. `PUT /api/events/{id}` was verified against the live phone sync server at `192.168.0.100:18765` with token `6CF4`.
6. Desktop sync status no longer returns/shows LAN addresses when the service is not running.
7. Daily-board normal schedule rows no longer show an outer border; only currently running rows keep the gold highlight.
8. Calendar header title/actions are split to avoid the month title being squeezed out.
9. Drawer and top-bar naming are unified to `每日看板`.
10. Version and current docs were synchronized for `1.6.53`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.53-debug.apk`
2. enable desktop sync on the phone and connect from the desktop browser
3. force-refresh the desktop page once, then verify the HTML loads `/app.js?v=1.6.53`
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
- A broad cleanup for unnecessary UI helper text/comments remains pending beyond the concrete UI copy touched here.

## Current External Dependency

No external file is needed for the current `1.6.53` verification task.
