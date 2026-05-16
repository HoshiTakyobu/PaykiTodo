# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.8.4` / `versionCode 187`.
- Latest debug APK output: `app/build/outputs/apk/debug/PaykiTodo-1.8.4-debug.apk`.
- Latest verification in this round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
  - `git diff --check`
- This round implements the `docs/goals/2026-05-16-paykitodo-1.8.4-goal.md` scope: desktop-web announcement marquee, desktop-web system dark theme, Android widget row deep links, widget single empty state, and data-ready launch hiding.
- Do not push to GitHub unless the user explicitly asks.

## Latest Changes In 1.8.4

1. Upgraded app version metadata to `1.8.4` / `versionCode 187`.
2. Desktop-web announcement banner now adds `marquee-needed` only when the combined active announcement text exceeds 60 characters.
3. Desktop-web long announcements scroll horizontally; hovering the banner pauses the marquee.
4. Desktop-web CSS now has light/dark theme variables with `@media (prefers-color-scheme: dark)`.
5. Desktop-web timeline cards, event cards, modal sheets, summary cards, tab buttons, sidebar cards, Planning Desk surfaces, inputs, and announcement banner now use the theme variables for dark-mode readability.
6. Widget rows now use explicit row types: `HEADER`, `EMPTY`, `TODO`, `EVENT`, `ANNOUNCEMENT`.
7. Widget row fill-in intents now carry `EXTRA_OPEN_TODO_ID`, `EXTRA_OPEN_EVENT_ID`, or `EXTRA_OPEN_PLANNING_NOTE_ID` as appropriate.
8. `TodoWidgetProvider` uses a mutable `PendingIntentTemplate` on Android 12+ so list-row fill-in extras work.
9. `MainActivity` and `DashboardLaunchRoute` parse widget deep-link targets in addition to Settings section targets.
10. Dashboard launch routing opens targeted todo rows in the todo editor, targeted event rows in Calendar event details, and targeted announcement rows in the source Planning Desk note.
11. Widget completely empty board state now shows only `今天没有安排，去 App 创建吧`.
12. `TodoUiState.dataReady` marks the first real combined state; launch screen hides when ready with an 800ms fallback.
13. The goal prompt for this round is archived under `docs/goals/2026-05-16-paykitodo-1.8.4-goal.md`.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetProvider.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `CHANGELOG.md`
- `README.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

## Current Verification Focus

1. Build `PaykiTodo-1.8.4-debug.apk`.
2. Verify desktop web announcement behavior:
   - short text stays static;
   - 60+ character text scrolls;
   - hover pauses scrolling.
3. Verify desktop web system dark mode readability for:
   - `.timeline-card`
   - `.event-card`
   - `.modal-sheet`
   - `.summary-card`
   - `.tab-btn`
   - `.sidebar-card`
   - `.announcements-banner`
4. Verify launcher widget behavior on a real device:
   - todo row opens the matching todo;
   - event row opens Calendar event details;
   - announcement row opens the source Planning Desk note;
   - header and empty rows return to the default daily board;
   - completely empty board shows a single empty row.
5. Verify launch screen disappears when data is ready and never waits longer than roughly 800ms.

## Deferred Larger Work

- Planning Desk remains an import + tracked refresh/sync model, not a fully live bidirectional rich editor.
- Drag-and-drop planning, Gantt chart, AI auto-planning, complex project tree, and deeper desktop parity remain deferred.
- Widget row deep links are implemented, but launcher-specific PendingIntent behavior still needs real-device confirmation.

## Required Reading For A New Session

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/UI_DESIGN_RULES.md`
7. `docs/current/PLANNING_DESK_DESIGN.md`
8. `docs/current/DESKTOP_WEB_ARCHITECTURE.md`
9. `docs/current/PAYKITODO_SESSION_LEDGER.md`
10. `docs/current/AI_RECOGNITION_VERIFICATION.md`

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
