# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.8.0` / `versionCode 183`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.8.0-debug.apk`.
- Latest completed verification before final handoff:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
- This round implements the Planning Desk upstream-sync loop rather than another parser expansion:
  - imported planning lines now create persistent mappings
  - mapping status is synchronized to item reality
  - completed imported tasks can be manually written back to Markdown as `- [x]`
  - unfinished imported items can be refreshed or batch-postponed
  - the last import / refresh / postpone batch can be undone
  - conflicts can be resolved either from document to item or from item back to document
  - desktop Planning Desk exposes the same mapping-aware operations
- Do not push to GitHub unless the user explicitly asks.

## Latest Changes In 1.8.0

1. Upgraded app version metadata to `1.8.0` / `versionCode 183`.
2. Added `PlanningLineMapping` plus `planning_line_mappings` Room storage and migration `10 -> 11`.
3. Importing from Planning Desk now writes mapping records alongside the existing `#imported` source-line marker.
4. `PlanningLineMatcher` now relocates mappings by normalized fingerprint first and fuzzy text distance second, so edits or inserted lines do not rely purely on stored line numbers.
5. Repository mapping sync now classifies each imported line as `ACTIVE`, `COMPLETED`, `CANCELED`, `ORPHANED`, or `CONFLICT`.
6. `refreshPlanningImportedItems` updates only unfinished active mappings, supports whole-document or current-section scope, and marks missing/diverged mappings instead of blindly overwriting.
7. `postponePlanningImportedItems` batch-shifts unfinished active mappings and rewrites the corresponding Markdown time text in the planning document.
8. `undoLastPlanningOperation` now rolls back the latest import / refresh / postpone batch, including Markdown marker/time restoration where applicable.
9. Conflict resolution now has two explicit paths:
   - document overwrites item
   - item rewrites source line
10. Phone-side Planning Desk preview now shows mapping status pills, conflict actions, and a `同步完成状态到原文` action that rewrites completed imported task lines to `- [x]`.
11. Desktop-web Planning Desk now shows current note title, empty-state guidance, mapping preview, refresh/postpone/undo controls, and conflict actions backed by `/api/planning/*`.
12. Planning mappings are now included in backup export/import snapshots.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/PlanningLineMapping.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningLineMatcher.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningSyncModels.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupModels.kt`
- `app/src/main/java/com/example/todoalarm/TodoApplication.kt`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/test/java/com/example/todoalarm/data/PlanningLineMatcherTest.kt`
- `CHANGELOG.md`
- `README.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/PLANNING_DESK_DESIGN.md`

## Current Verification Focus

1. Install `PaykiTodo-1.8.0-debug.apk`.
2. In Planning Desk, import several todo/event lines and confirm mapping pills appear in Markdown preview.
3. Complete one imported todo and cancel another; confirm status changes to `✓ 已完成` / `已取消`.
4. Trigger `同步完成状态到原文` and confirm only completed imported task lines become `- [x]`.
5. Run `刷新已导入项` and confirm completed/canceled items are skipped while unfinished items refresh from current Markdown.
6. Run `批量顺延` and confirm both the formal item time and Markdown time text shift together.
7. Run `撤销上次操作` after import, refresh, and postpone in separate passes; confirm each latest batch rolls back correctly.
8. Edit an imported item outside Planning Desk so it diverges from the source line, then confirm preview marks it as `已手动修改` and both conflict-resolution actions work.
9. Open the desktop Planning Desk from a real browser, confirm mapping preview loads, and verify desktop refresh/postpone/undo/conflict actions against the same phone data.

## Deferred Larger Work

- Planning Desk now has mapping-aware refresh/postpone/undo/conflict handling, but it is still not a fully live bidirectional editor. The current model is import + tracked refresh/sync, not arbitrary rich-text collaborative sync.
- Drag-and-drop planning, Gantt chart, AI auto-planning, complex project tree, and deeper desktop parity remain deferred.
- Device-side UI verification is still required for `1.8.0`.

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
