# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.7.1` / `versionCode 158`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.7.1-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- This round implemented Planning Desk Phase 2 usability improvements.
- Planning Desk is available on the phone drawer and desktop web console, with editable preview and automatic `#imported` write-back.

## Latest Fixes In 1.7.1

1. Phone Planning Desk document sheet now supports title/content search.
2. Phone recognition preview cards are editable before import for title, group, notes, DDL, event start/end, reminder minutes, and linked-todo creation.
3. Added shared Planning Desk import models for edited candidates and import results.
4. Successful Planning Desk imports now append `#imported` to imported source lines and save the active planning note.
5. Desktop web Planning Desk preview cards now support inline editing before import.
6. `/api/planning/import` accepts edited candidates, validates them, imports selected entries, writes back `#imported`, and returns `updatedMarkdown`.
7. Planning imports still default to 5 minutes before, full-screen, ring + vibration.
8. README, CHANGELOG, TODO, docs/current, and Wiki were updated for the Phase 2 workflow.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/PlanningNote.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningMarkdownParser.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningImportCandidate.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningImportResult.kt`
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

1. Install `PaykiTodo-1.7.1-debug.apk`.
2. Verify drawer -> `规划台` opens and creates/loads `我的规划`.
3. Verify create/open/rename/archive/delete planning documents and document search.
4. Verify phone shortcut bar can insert tasks/subtasks/tags and indent/outdent current line.
5. Verify parsing examples:
   - `- [ ] 整理材料 #ddl 5.28`
   - `10:00-12:30 作业1`
   - `明天 19:30-21:00 整理保研材料`
6. Verify preview editing before import and linked todo creation for events.
7. Verify successful import appends `#imported` to source lines.
8. Verify desktop web `规划台` with the same phone database, including editable preview and updated Markdown after import.

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
