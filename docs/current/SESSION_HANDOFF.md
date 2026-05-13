# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.60` / `versionCode 132`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.6.60-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- Desktop web still comes from the phone APK assets under `app/src/main/assets/desktop-web/`.
- New desktop web pages should show the installed version in the left brand block as `v1.6.60`.

## Latest Fixes In 1.6.60

1. Desktop todo cards no longer show inline edit / complete / cancel / delete buttons.
2. Clicking a desktop todo card opens a todo preview sheet first.
3. Todo preview shows color block, title, state, group, DDL, recurrence, reminder, and notes.
4. Todo preview actions handle edit, complete, cancel, and delete.
5. Desktop event cards and compact all-day pills open an event preview sheet first instead of jumping straight to the editor.
6. Event preview shows color block, title, time, recurrence, location, reminder, and notes.
7. Event preview actions handle edit and delete.
8. Delete actions continue to use the in-app dangerous confirmation modal.
9. Node simulated-browser checks passed for event preview -> edit and todo preview -> edit.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/app.css`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/*`

## Current Verification Focus

1. Install `PaykiTodo-1.6.60-debug.apk`.
2. Enable desktop sync and connect from a desktop browser.
3. Verify the desktop page shows `v1.6.60`; if not, the phone is still serving an old APK.
4. Click todo cards and verify they open preview sheets.
5. From todo preview, test edit / complete / cancel / delete confirmation.
6. Click timed and all-day event entries and verify they open event preview sheets.
7. From event preview, test edit and delete confirmation.

## Deferred Larger Work

- Full desktop UI parity with phone UI.
- Lunar calendar and lunar yearly recurrence.
- Full calendar rendering/performance optimization beyond the current-time tick scoping.
- Emulator-driven visual QA loop.
- Broader UI-copy cleanup beyond the concrete strings changed so far.

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
