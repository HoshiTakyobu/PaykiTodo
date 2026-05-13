# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.52` / `versionCode 124`
- Latest debug APK path after build should be `app/build/outputs/apk/debug/PaykiTodo-1.6.52-debug.apk`
- Latest fix round addressed a desktop web regression:
  1. Desktop web event cards still have no inline edit/delete buttons.
  2. Clicking an event card now opens the existing event editor by using string-compatible ID lookup.
  3. Card clicks stop propagation so they do not fall through into blank timeline create-event handling.

Previous feature round (`1.6.51`) fixed daily-board tomorrow copy and simplified the desktop event-timeline UI.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/desktop-web/index.html`
- `docs/current/CURRENT_TASK.md`
- `docs/current/PROJECT_STATUS.md`

## Current Verification Focus

1. Install `PaykiTodo-1.6.52-debug.apk`
2. Enable desktop sync and connect from a desktop browser
3. Open the desktop web event timeline
4. Click an existing event card and verify it opens the existing event editor
5. Verify blank timeline clicks still create a new event draft
6. Verify event cards still have no inline edit/delete buttons

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
