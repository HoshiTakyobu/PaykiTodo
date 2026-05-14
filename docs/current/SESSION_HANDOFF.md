# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.7.2` / `versionCode 159`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.7.2-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- This round implemented phone-side Planning Desk Markdown rendering.
- Planning Desk is available on the phone drawer and desktop web console, with editable preview, automatic `#imported` write-back, and phone-side rendered Markdown reading mode.

## Latest Fixes In 1.7.2

1. Added phone-side Planning Desk Markdown reading mode.
2. Rendered headings, task checkboxes, subtask indentation, tag pills, and imported-state pills.
3. Added `编辑全文 / 预览` switching so raw Markdown remains available for editing.
4. Tapping a rendered checkbox toggles the raw Markdown line between `- [ ]` and `- [x]` without directly completing imported todos.
5. Added AI planning assistant guidance to Planning Desk design docs without implementing AI calls in this version.
6. README, CHANGELOG, TODO, docs/current, and Wiki were updated for the Markdown rendering workflow.

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

1. Install `PaykiTodo-1.7.2-debug.apk`.
2. Verify drawer -> `规划台` opens and creates/loads `我的规划`.
3. Verify `编辑全文 / 预览` switching.
4. Verify rendered headings, checkboxes, tag pills, subtask indentation, and imported-state pills.
5. Verify tapping a rendered checkbox changes the raw Markdown checkbox state.
6. Verify parsing examples:
   - `- [ ] 整理材料 #ddl 5.28`
   - `10:00-12:30 作业1`
   - `明天 19:30-21:00 整理保研材料`
7. Verify preview editing before import and linked todo creation for events.
8. Verify successful import appends `#imported` to source lines and appears as an imported-state pill.
9. Verify desktop web `规划台` with the same phone database, including editable preview and updated Markdown after import.

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
