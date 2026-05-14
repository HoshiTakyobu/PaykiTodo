# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.7.3` / `versionCode 160`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.7.3-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- This round hotfixed the phone-side Planning Desk Markdown rendering startup crash.
- Planning Desk is available on the phone drawer and desktop web console, with editable preview, automatic `#imported` write-back, and phone-side rendered Markdown reading mode.

## Latest Fixes In 1.7.3

1. Planning Desk now always starts in raw `编辑全文` mode instead of auto-rendering old Markdown during app launch.
2. Markdown preview remains available only when the user taps `预览`.
3. Markdown preview has parse/render failure protection and a fallback back to `编辑全文`.
4. Experimental `FlowRow` preview layout was replaced with simpler `Column` / `Row` layout to reduce device compatibility risk.
5. README, CHANGELOG, TODO, docs/current, and Wiki were updated for `1.7.3`.

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

1. Install `PaykiTodo-1.7.3-debug.apk`.
2. Verify the app no longer crashes immediately after launch.
3. Verify drawer -> `规划台` opens in `编辑全文` mode.
4. Verify `编辑全文 / 预览` switching.
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
