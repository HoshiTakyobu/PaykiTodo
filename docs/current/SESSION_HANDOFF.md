# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.51` / `versionCode 123`
- Latest debug APK path after build should be `app/build/outputs/apk/debug/PaykiTodo-1.6.51-debug.apk`
- Latest feature round fixed board and desktop event-timeline UI details:
  1. Daily board schedule card always includes a tomorrow section.
  2. If tomorrow has no events, it says `明天暂无日程`.
  3. Desktop web event cards no longer show inline edit/delete buttons; card click opens the editor.
  4. Desktop web event-card display color prefers group color, matching todo color behavior.
  5. Desktop web event timeline no longer shows the separate all-day strip above the grid.

Previous feature round (`1.6.48`) introduced the compact Settings controls and moved desktop sync back into common Settings.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/alarm/ReminderAlertController.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/assets/wiki/index.html`
- `docs/current/UI_DESIGN_RULES.md`

## Current Verification Focus

1. Install `PaykiTodo-1.6.51-debug.apk`
2. Open daily board and verify the schedule card shows `明天暂无日程` when tomorrow has no events
3. Open desktop web event timeline and verify the separate all-day strip is gone
4. Click a desktop web event card and verify it opens the existing event editor
5. Verify event cards no longer show inline edit/delete buttons and use group colors first

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
