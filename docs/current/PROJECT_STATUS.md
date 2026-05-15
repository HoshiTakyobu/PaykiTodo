# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.8.0"`
  - `versionCode = 183`

## Current Build Facts

- Latest debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.8.0-debug.apk`
- Minimal verification completed in the latest code round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now at `1.8.0`. It carries forward the `1.7.25` Planning Desk AI-recognition integration and adds the next missing product loop: imported planning lines are no longer treated as fire-and-forget text markers, but as tracked upstream planning entries with mapping status, refresh/postpone/undo operations, and explicit conflict resolution on both phone and desktop.

Most important current baseline facts:

- version metadata is `1.8.0 / 183`
- Planning Desk still keeps AI-first / local-fallback recognition, editable preview, selected import, and automatic `#imported` write-back.
- Importing from Planning Desk now also creates persistent `planning_line_mappings` records, connecting the planning note line to the created todo or event item.
- Mapping relocation is no longer line-number-only: `PlanningLineMatcher` normalizes text, ignores `#imported`, hashes the line, and falls back to fuzzy edit-distance matching when the original line wording changes.
- Mapping status sync now distinguishes `ACTIVE`, `COMPLETED`, `CANCELED`, `ORPHANED`, and `CONFLICT`.
- Phone-side Markdown preview now renders imported-line state pills directly; completed or canceled mapped items are visually marked, and `同步完成状态到原文` can rewrite completed imported task lines to `- [x]`.
- `刷新已导入项` only refreshes unfinished mapped items. Completed/canceled items are skipped; missing items become `ORPHANED`; manually diverged items become `CONFLICT`.
- `批量顺延` only targets unfinished active mappings and updates both the formal item timestamps and the Markdown time text.
- The latest import / refresh / postpone batch can be undone. Undo import removes created items and `#imported`; undo refresh restores prior item content; undo postpone restores both item times and Markdown time text.
- Conflicts can now be resolved in both directions:
  - `以文档为准覆盖`: current planning line overwrites the item
  - `以事项为准更新文档`: current item rewrites the source planning line
- Desktop-web Planning Desk now exposes mapping preview, refresh, postpone, undo, and conflict resolution through `/api/planning/*`, and the desktop UI shows the current note title plus an empty-state hint before parsing.
- Planning notes and planning mappings are both included in backup / restore snapshots.
- Database version is now `11`; `MIGRATION_10_11` creates the `planning_line_mappings` table and its indices.

## Recent Checked Areas

Recent code inspection and build verification cover:

- `PlanningLineMapping.kt`, `PlanningLineMatcher.kt`, `PlanningSyncModels.kt`: mapping schema, matching strategy, and operation result models.
- `TodoRepository.kt`: mapping status sync, refresh, postpone, undo, conflict resolution, and backup inclusion.
- `TodoDao.kt`, `AppDatabase.kt`, `DatabaseMigrations.kt`, `TodoApplication.kt`: Room entity registration, migration path, and DB version `11`.
- `PlanningDeskPanel.kt`: phone-side refresh/postpone/undo entry points, completed-to-markdown sync, preview state pills, and conflict actions.
- `TodoViewModel.kt`: phone-side orchestration for mapping operations and reminder rescheduling after planning operations.
- `DesktopSyncCoordinator.kt`, `DesktopSyncServer.kt`, `app/src/main/assets/desktop-web/*`: desktop Planning Desk mapping APIs, preview/status rendering, and desktop refresh/postpone/undo/conflict actions.
- `BackupManager.kt`, `BackupModels.kt`: planning mapping export/import.
- `DashboardChrome.kt`: prior dark-board title readability fix remains intact alongside the new Planning Desk work.

## Documentation Health

Current docs have been synchronized for `1.8.0`:

- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/PLANNING_DESK_DESIGN.md`
- `docs/current/PAYKITODO_SESSION_LEDGER.md`

Older versioned docs under `docs/` remain historical references and should not be treated as the live baseline unless a current doc explicitly points to them.

## Current Risk Areas

1. Device-side verification is still required for the full `1.8.0` Planning Desk mapping loop: import -> status sync -> completed markdown sync -> refresh -> postpone -> undo -> conflict resolution.
2. Desktop-browser verification is still required for the same mapping loop after installing `PaykiTodo-1.8.0-debug.apk` on the phone and reconnecting from a real browser.
3. Unit tests currently cover parser / AI / line-matching behavior, but there are still no dedicated repository-level automated tests for refresh/postpone/undo/conflict flows; those are presently covered by code inspection plus build/test success.
4. Long-running chat sessions can become unreliable, so repository docs must remain the source of truth for future continuation.

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and `git status`
4. Only then decide the next edit
