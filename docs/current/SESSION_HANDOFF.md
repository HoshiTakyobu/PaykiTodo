# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.59` / `versionCode 131`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.6.59-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- Live desktop URL `http://192.168.0.100:18765` was checked during this round and was still serving old desktop assets. Its live `/app.js` lacked the current `csvValue` and event-timeline delegated click listener. Do not keep debugging that live page until the phone has installed the latest APK.
- New desktop web pages should show the installed version in the left brand block as `v1.6.59` and should load `/app.js?v=1.6.59` / `/app.css?v=1.6.59`.

## Latest Fixes In 1.6.59

1. Desktop web resource versioning is now runtime-driven by the installed APK version via `DesktopSyncWebAssets.indexHtml()`.
2. Desktop web click-edit was locally verified with a Node simulated browser click: clicking a timed event card opens the event editor and handles string `recurrenceWeekdays` safely.
3. `DesktopSyncService` self-stops if relaunched while desktop sync is disabled, preventing stale foreground-service / access-address behavior.
4. Calendar current-time updates were scoped to `CalendarTimeAxis` and `CurrentTimeLine`, reducing unrelated whole-panel recomposition every 30 seconds.
5. Daily-board normal schedule rows no longer have non-progress fill / border, and their left color bar is closer to the row edge.
6. Calendar header layout was tightened so the month title and action buttons share one row without the title disappearing.
7. Settings desktop-sync summary now says `未开启` when disabled; the redundant usage-guide intermediate section was removed.
8. One generic calendar-editor hint under start/end time was removed.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncService.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncWebAssets.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarEventEditorDialog.kt`

## Current Verification Focus

1. Install `PaykiTodo-1.6.59-debug.apk`.
2. Enable desktop sync and connect from a desktop browser.
3. Verify the desktop page shows `v1.6.59`; if not, the phone is still serving an old APK.
4. Click timed and all-day event entries and verify both open the event editor.
5. Verify saving an event edit still works.
6. Disable desktop sync and verify no stale access address / foreground service remains.
7. Check daily board schedule row borders and the calendar header month title on device.

## Deferred Larger Work

- Full desktop UI parity with phone UI.
- Lunar calendar and lunar yearly recurrence.
- Full calendar rendering/performance optimization beyond the 30-second tick scoping.
- Emulator-driven visual QA loop.
- Broader UI-copy cleanup beyond the concrete strings changed in 1.6.59.

## Required Reading For A New Session

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/UI_DESIGN_RULES.md`
7. `docs/current/DESKTOP_WEB_ARCHITECTURE.md`
8. `docs/current/PAYKITODO_SESSION_LEDGER.md`

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
