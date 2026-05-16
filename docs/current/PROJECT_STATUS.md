# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.8.1"`
  - `versionCode = 184`

## Current Build Facts

- Latest debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.8.1-debug.apk`
- Minimal verification completed in the latest code round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now at `1.8.1`. It carries forward the `1.8.0` Planning Desk mapping loop and focuses on repairs and small user-facing additions: explicit-only AI recognition triggers, AI provider connection testing, daily-board announcements, an Android today-todo widget, and a shorter launch screen.

Most important current baseline facts:

- version metadata is `1.8.1 / 184`
- Planning Desk still keeps AI-first / local-fallback recognition, editable preview, selected import, and automatic `#imported` write-back.
- Planning Desk AI recognition is now explicit-only: phone recognition still starts from the `识别` button, desktop recognition starts from the `识别` button or `Ctrl+Enter`, and desktop import no longer silently calls parse when no preview exists.
- Settings -> `AI 调用配置` can test a single provider with the currently edited Base URL / API Key / model before saving.
- Daily board can show a date-ranged announcement banner configured in Settings; long text uses marquee and expired/future announcements stay hidden.
- Android launcher widgets now expose a first `今日待办` widget backed by Room data and refresh notifications after todo mutations.
- Launch screen delay is now 600 ms instead of 1600 ms.
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
- `PlanningAiCaller.kt`, `SettingsPanel.kt`: AI provider test-connection path and Settings UI.
- `DashboardChrome.kt`, `AppSettingsStore.kt`: announcement settings storage and board banner visibility.
- `TodoWidgetProvider.kt`, `TodoWidgetService.kt`, widget XML resources, `AndroidManifest.xml`: Android launcher widget registration and RemoteViews data path.

## Documentation Health

Current docs have been synchronized for `1.8.1`:

- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/PLANNING_DESK_DESIGN.md`
- `docs/current/PAYKITODO_SESSION_LEDGER.md`
- `docs/current/AI_RECOGNITION_VERIFICATION.md`

Older versioned docs under `docs/` remain historical references and should not be treated as the live baseline unless a current doc explicitly points to them.

## Current Risk Areas

1. Device-side verification is still required for the full `1.8.1` Planning Desk mapping loop: import -> status sync -> completed markdown sync -> refresh -> postpone -> undo -> conflict resolution.
2. Desktop-browser verification is still required for the same mapping loop after installing `PaykiTodo-1.8.1-debug.apk` on the phone and reconnecting from a real browser.
3. Widget behavior must be verified on a real launcher because Android widget picker / RemoteViews refresh behavior cannot be fully covered by JVM tests.
4. Announcement date-range visibility and long-text marquee need real UI verification.
5. Unit tests currently cover parser / AI / line-matching behavior, but there are still no dedicated repository-level automated tests for refresh/postpone/undo/conflict flows; those are presently covered by code inspection plus build/test success.
6. Long-running chat sessions can become unreliable, so repository docs must remain the source of truth for future continuation.

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and `git status`
4. Only then decide the next edit
