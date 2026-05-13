# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.84` / `versionCode 156`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.6.84-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- This round implemented Planning Desk Phase 1.
- Planning Desk is available on the phone drawer and desktop web console.

## Latest Fixes In 1.6.84

1. Added `planning_notes` Room entity/table and database migration `8 -> 9`.
2. Added multi-document Planning Desk: create, open, rename, archive, delete, and last-opened-note restoration.
3. Added local `PlanningMarkdownParser`; no AI or paid model is involved.
4. Parser recognizes markdown checkbox todos, completed-task skipping, subtask parent metadata, date headings, common DDL formats, reminder/group/schedule tags, and natural schedule ranges.
5. Phone-side `规划台` page provides a plain Markdown editor, shortcut bar, parse preview, and selected import.
6. Natural schedule lines can import as calendar events and optionally create linked todos whose DDL equals the event end time.
7. Planning imports default to 5 minutes before, full-screen, ring + vibration.
8. Planning notes are included in JSON backup / restore.
9. Desktop web console has a `规划台` tab backed by `/api/planning/*` routes.
10. README, CHANGELOG, TODO, docs/current, and Wiki were updated for the new workflow.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/PlanningNote.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningMarkdownParser.kt`
- `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/wiki/index.html`
- `docs/current/PLANNING_DESK_DESIGN.md`

## Current Verification Focus

1. Install `PaykiTodo-1.6.84-debug.apk`.
2. Verify drawer -> `规划台` opens and creates/loads `我的规划`.
3. Verify create/open/rename/archive/delete planning documents.
4. Verify phone shortcut bar can insert tasks/subtasks/tags and indent/outdent current line.
5. Verify parsing examples:
   - `- [ ] 整理材料 #ddl 5.28`
   - `10:00-12:30 作业1`
   - `明天 19:30-21:00 整理保研材料`
6. Verify preview-first import and linked todo creation for events.
7. Verify desktop web `规划台` with the same phone database.

## Deferred Larger Work

- Drag-and-drop planning.
- Gantt chart.
- AI auto-planning.
- Complex project tree.
- Markdown highlighting / rich editor.
- Dedicated parser unit-test suite.
- Full desktop UI parity with phone UI.
- Full calendar rendering/performance optimization.

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

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
