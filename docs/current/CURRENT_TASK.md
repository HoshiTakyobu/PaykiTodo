# Current Task

## Active Development Focus

The current round has produced a local `1.6.81` build. It continues the user-requested desktop/web, daily-board, calendar-header, and desktop-sync address repair pass while keeping the repository buildable.

Completed in this round:

1. Desktop web existing timed event cards now open the event editor directly on card click instead of stopping at the preview sheet.
2. The existing event editor still saves through `PUT /api/events/{id}`; delete actions still require the PaykiTodo dangerous-action confirmation modal.
3. Direct access to `http://192.168.0.100:18765/` with token `6CF4` was reachable during this round. The phone was still serving the previously installed APK assets; local `1.6.81` assets were verified by Node DOM simulation with the live phone snapshot.
4. Node DOM simulation using live snapshot event `15` verified: render event card -> click card -> open `event-modal` -> fill title -> set `editingEventId` -> do not open `event-preview-modal`.
5. Settings -> 电脑同步 no longer renders the `连接地址` section at all while desktop sync is disabled; addresses only appear when sync is enabled, service is running, and the user expands `显示连接地址`.
6. Daily-board schedule rows keep normal events borderless and move the left color strip closer to the block edge; only currently running rows keep the gold highlight.
7. Calendar header now uses a left flexible year/month pill plus compact right-side action buttons to reduce the chance that `2026年5月` loses the month text on narrow screens.
8. Version was bumped to `1.6.81` / `versionCode 153`.
9. `node --check app/src/main/assets/desktop-web/app.js` succeeded.
10. `./gradlew.bat assembleDebug` succeeded with Android Studio bundled `jbr` and produced `app/build/outputs/apk/debug/PaykiTodo-1.6.81-debug.apk`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.81-debug.apk`
2. refresh the desktop browser page after installing the APK
3. verify clicking an existing desktop event opens the editor directly and saving updates the existing event
4. verify Settings -> 电脑同步 shows no `连接地址` section while desktop sync is disabled, and only shows addresses after explicitly expanding while running
5. verify calendar header month title remains readable on the problematic narrow/large-font device state
6. verify daily-board normal schedule rows remain borderless and the left color strip is close enough to the block edge

## Deferred From The User's Larger Request

- Full desktop web UI parity with the phone UI is still not complete.
- Todo lunar wheel/date-picker UX remains pending; event editing has compact lunar start/end picking for both all-day and timed events.
- A full calendar performance pass remains pending; current work only includes incremental layout/composition hardening.
- Android Emulator visual QA was attempted previously with SDK emulator `Pixel_8`; `emulator.exe` launched but no device appeared in `adb devices` within 90 seconds, so no install/screenshot UI loop can be claimed yet.
- Broad UI-copy cleanup is partially improved, but future screens should still be reviewed for unnecessary helper text.

## Current External Dependency

No external file is needed for the current `1.6.81` verification task.