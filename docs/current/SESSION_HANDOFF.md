# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.8.3` / `versionCode 186`.
- Expected debug APK path after the final build: `app/build/outputs/apk/debug/PaykiTodo-1.8.3-debug.apk`.
- Latest verification in this round so far:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
  - `git diff --check`
- Latest debug APK output: `app/build/outputs/apk/debug/PaykiTodo-1.8.3-debug.apk`.
- This round cleans up legacy announcement settings, improves Planning Desk announcement parsing / preview / ordering, adds desktop-web announcement visibility, and hardens the Android `今日看板` widget for dark mode and refresh performance.
- Do not push to GitHub unless the user explicitly asks.

## Latest Changes In 1.8.3

1. Upgraded app version metadata to `1.8.3` / `versionCode 186`.
2. Removed Settings-backed announcement fields from `AppSettings`.
3. Removed old announcement JSON backup export/import fields; old backup files that still contain those unknown fields remain tolerated.
4. Added one-time cleanup for legacy SharedPreferences announcement keys.
5. Planning Desk announcement parser now recognizes:
   - `#公告 ...`
   - `- [ ] #公告 ...`
   - `> #公告 ...`
   - inline `今日提醒：#公告 ...`
   - `> [!公告] ...`
   - `公告: ...`
6. Announcement display text strips trailing `#imported`, `#group ...`, and ordinary tail hashtags.
7. Active announcements sort date-scoped items before long-running announcements, with newer start dates first.
8. Planning Desk Markdown preview renders announcement rows with orange styling, campaign icon, `全局公告` pill, and range label.
9. Desktop web `/api/snapshot` includes `announcements`; `index.html` / `app.js` / `app.css` render a top announcement banner.
10. Android widget color resources now live in day/night color resources, and widget drawables have night variants.
11. `TodoWidgetService` reloads colors on each `onDataSetChanged` and uses `ContextCompat.getColor`.
12. Widget refresh uses `TodoDao.getActiveItemsForBoardRange(...)` via `TodoRepository.getActiveItemsForBoardRange()` instead of `getAllTodos()`.
13. `TodoWidgetProvider.onReceive` duplicate refresh override was removed.
14. `AI_RECOGNITION_VERIFICATION.md` now documents `1.8.1` trigger tightening and `1.8.2` Base URL endpoint fallback / non-JSON handling.
15. The goal prompt for this round is archived under `docs/goals/2026-05-16-paykitodo-1.8.3-goal.md`.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/AppSettingsStore.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAnnouncementParser.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetProvider.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncModels.kt`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values-night/colors.xml`
- `app/src/main/res/drawable/widget_todo_background.xml`
- `app/src/main/res/drawable-night/widget_todo_background.xml`
- `app/src/main/res/drawable/widget_todo_item_background.xml`
- `app/src/main/res/drawable-night/widget_todo_item_background.xml`
- `app/src/main/res/layout/widget_todo.xml`
- `app/src/main/res/layout/widget_todo_item.xml`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/test/java/com/example/todoalarm/data/PlanningAnnouncementParserTest.kt`
- `docs/current/AI_RECOGNITION_VERIFICATION.md`

## Current Verification Focus

1. Build `PaykiTodo-1.8.3-debug.apk`.
2. Verify Planning Desk announcement parser cases on device:
   - checkbox announcement
   - quote announcement
   - inline announcement
   - `#imported` / hashtag cleanup
   - recent-date sorting before long-running announcements
3. Verify Planning Desk preview announcement card and click-to-edit behavior.
4. Verify desktop web announcement banner from a real browser connected to the phone.
5. Verify launcher widget dark-mode readability, resize behavior, tap behavior, and refresh after todo/event/planning-note edits.

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
