# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.8.2` / `versionCode 185`.
- Expected debug APK path after the final build: `app/build/outputs/apk/debug/PaykiTodo-1.8.2-debug.apk`.
- Latest verification in this round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
  - `git diff --check`
- This round repairs AI provider endpoint compatibility, moves announcements into Planning Desk as multiple Markdown lines, and upgrades the Android widget from today-todo list to board-style content.
- Do not push to GitHub unless the user explicitly asks.

## Latest Changes In 1.8.2

1. Upgraded app version metadata to `1.8.2` / `versionCode 185`.
2. `PlanningAiCaller` now supports common OpenAI-compatible Base URL forms:
   - full `/chat/completions` URL: use directly
   - `/v1` Base URL: append `/chat/completions`
   - root Base URL: try `/v1/chat/completions` before `/chat/completions`
3. AI provider `测试连接` timeout changed to 10s connect / 20s read.
4. Non-JSON / HTML provider responses now report a readable Base URL / endpoint hint instead of raw `<!doctype` JSON conversion failures.
5. Added `PlanningAnnouncementParser`.
6. Announcements now come from all unarchived Planning Desk notes:
   - `#公告 5.16-7.1 内容`
   - `#公告 2026-05-16 2026-05-20 内容`
   - `> [!公告] 内容`
   - no date range means long-running announcement
7. `TodoUiState` exposes `activeAnnouncements`; `DashboardChrome` renders each active announcement above the greeting card.
8. Settings no longer exposes `公告设置`; old app-settings fields are retained only for backward-compatible storage / backup data.
9. Added `DailyBoardSnapshotBuilder` to share board-style todo/event filtering outside Compose.
10. Android widget is now titled `今日看板` and displays:
    - active Planning Desk announcements
    - today todo count and rows
    - today schedule count, visible rows, or “all events ended” / empty message
    - tomorrow schedule rows or empty message
11. Widget rows are adaptive-height and the hard `take(5)` cap was removed; the list is sanity-capped at 40 rows.
12. Widget provider XML declares min resize dimensions.
13. Planning Desk phone tutorial, desktop-web Planning Desk help, in-app Wiki, README, current docs, and changelog now mention Planning Desk announcements and AI endpoint behavior.
14. Added JVM tests:
    - `PlanningAiCallerTest`
    - `PlanningAnnouncementParserTest`

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiCaller.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAnnouncementParser.kt`
- `app/src/main/java/com/example/todoalarm/data/DailyBoardSnapshot.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetProvider.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/res/layout/widget_todo.xml`
- `app/src/main/res/layout/widget_todo_item.xml`
- `app/src/main/res/xml/widget_todo_info.xml`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/wiki/index.html`
- `app/src/test/java/com/example/todoalarm/data/PlanningAiCallerTest.kt`
- `app/src/test/java/com/example/todoalarm/data/PlanningAnnouncementParserTest.kt`

## Current Verification Focus

1. Install `PaykiTodo-1.8.2-debug.apk`.
2. Verify AI provider `测试连接` with:
   - root Base URL that needs `/v1/chat/completions`
   - Base URL ending in `/v1`
   - full `/v1/chat/completions`
   - HTML/non-JSON response
   - timeout/unreachable host
3. Verify Planning Desk announcements:
   - multiple active announcements
   - date-range filtering
   - no-date long-running announcements
   - deleted / archived / edited source notes update board and widget
4. Verify launcher widget add/list/empty/tap/refresh/resize behavior on a real Android launcher.

## Deferred Larger Work

- Planning Desk remains an import + tracked refresh/sync model, not a fully live bidirectional rich editor.
- Drag-and-drop planning, Gantt chart, AI auto-planning, complex project tree, and deeper desktop parity remain deferred.
- Widget deep links to a specific todo/event are not implemented; current widget opens the app.

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
