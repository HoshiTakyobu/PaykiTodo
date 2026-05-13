# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.54` / `versionCode 126`
- Latest debug APK path after build should be `app/build/outputs/apk/debug/PaykiTodo-1.6.54-debug.apk`
- Latest fix round addressed real desktop-web editing failures and small phone UI regressions:
  1. Desktop web assets are versioned and served with no-cache headers.
  2. Event card clicks use delegated handling.`r`n  3. Event/todo editors tolerate `recurrenceWeekdays` as string or array, fixing the hidden-modal failure after the form was partially filled.
  3. All-day events are shown as compact per-day pills and can be clicked to edit.
  4. Desktop sync settings hide LAN access addresses unless sync is enabled and running.
  5. Daily-board normal schedule rows no longer have outer borders; in-progress rows keep gold emphasis.
  6. Calendar header separates month title from action buttons.
  7. `今日看板` / `每日看板` naming was unified to `每日看板`.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncServer.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`

## Current Verification Focus

1. Install `PaykiTodo-1.6.54-debug.apk`
2. Enable desktop sync and connect from a desktop browser
3. Hard-refresh the desktop page and verify it loads `/app.js?v=1.6.54`
4. Click timed and all-day event entries and verify both open the event editor
5. Verify saving an event edit still works
6. Disable desktop sync and verify Settings does not show stale access addresses
7. Check daily board schedule row borders and the calendar header month title on device

## Deferred Larger Work

- Full desktop UI parity with phone UI
- Lunar calendar and lunar yearly recurrence
- Full calendar rendering/performance optimization
- Emulator-driven visual QA loop
- Broad UI-copy/comment cleanup beyond the concrete strings changed in this round

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
