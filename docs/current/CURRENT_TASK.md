# Current Task

## Active Development Focus

The current round has produced a `1.6.80` build. It continues the user-requested desktop/web, daily-board, calendar-header, and lunar-calendar repair pass while keeping the repository buildable.

Completed in this round:

1. Desktop web existing event cards now open the event preview sheet first, matching the phone-side preview interaction model.
2. The event preview sheet still exposes `编辑` and `删除`; choosing `编辑` opens the event editor and saving continues to use `PUT /api/events/{id}`.
3. Direct access to `http://192.168.0.100:18765/` is currently reachable. Live HTML serves `/app.css?v=1.6.80` and `/app.js?v=1.6.80`, and live `/app.js` SHA-256 matches the local desktop-web asset.
4. Calendar event editing now has compact `农历开始` / `农历结束` date picking for both all-day and timed events. Timed events preserve the original clock time while replacing the date.
5. Calendar header month title no longer uses hard clipping; it uses a slightly smaller title style and can wrap/ellipsis under tight width or large font scale.
6. Daily-board schedule rows keep normal events borderless and move the left color strip closer to the block edge; only currently running rows keep the gold highlight.
7. Version was bumped to `1.6.80` / `versionCode 152`.
8. Settings-side desktop-sync status now has an explicit ViewModel refresh tick after enabling/disabling sync or rotating the key, reducing stale access-address display after state changes.
9. Calendar event editor removed redundant all-day explanatory copy while preserving the switch and lunar start/end actions.
10. Timed event start/end rows now also expose lunar date picking and preserve the original time of day.
11. Desktop timed event cards now expose button semantics, keyboard focus, and an aria label so keyboard open-preview behavior is reachable.
12. In-app Wiki desktop-sync instructions now match the current address-expansion and preview/edit desktop web behavior.
13. A Node DOM simulation using the live phone snapshot verified event card click -> preview -> edit -> save path, including observed `PUT /api/events/15`.
14. `./gradlew.bat assembleDebug` succeeded with Android Studio bundled `jbr`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.80-debug.apk`
2. refresh the desktop browser page after installing the APK
3. verify clicking an existing desktop event opens the event preview, then `编辑` opens the editor and save updates the existing event
4. verify Settings -> 电脑同步 shows no address when desktop sync is disabled and only shows addresses after explicitly expanding while running
5. verify event `农历开始` / `农历结束` can select a lunar date and write the corresponding Gregorian start/end date; timed events should preserve their original clock time
6. verify calendar header month title remains readable on the problematic narrow/large-font device state

## Deferred From The User's Larger Request

- Full desktop web UI parity with the phone UI is still not complete.
- Todo lunar wheel/date-picker UX remains pending; event editing now has compact lunar start/end picking for both all-day and timed events.
- A full calendar performance pass remains pending; current work only includes incremental layout/composition hardening.
- Android Emulator visual QA was attempted again with SDK emulator `Pixel_8`; `emulator.exe` launched but no device appeared in `adb devices` within 90 seconds, so no install/screenshot UI loop can be claimed yet.
- Broad UI-copy cleanup is partially improved, but future screens should still be reviewed for unnecessary helper text.

## Current External Dependency

No external file is needed for the current `1.6.80` verification task.