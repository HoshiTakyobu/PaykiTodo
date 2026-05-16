# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.9.0.1"`
  - `versionCode = 194`

## Current Build Facts

- Latest debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.9.0.1-debug.apk`
- Minimal verification completed in the latest code round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
  - `git diff --check`
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now on `1.9.0.1`. It carries forward the `1.9.0` focus-mode baseline and applies a launcher-widget visual hotfix so the Android `今日看板` widget reads closer to the in-app daily board.

Most important current baseline facts:

- version metadata is `1.9.0.1 / 194`
- Database version is now `12`; `MIGRATION_11_12` creates `focus_sessions`.
- Settings -> `专注模式` controls default focus duration, extension duration, screen-on behavior, and a documented-only notification-suppression preference.
- Active todo long-press menus include `开始专注 · X 分钟`; the daily-board focus card can start free focus.
- `FocusActivity` provides a full-screen circular countdown with pause / continue, completion confirmation, abandon confirmation, zero-time vibration, extension, save-before-exit behavior, and a completion feedback page.
- Daily board shows completed focus minutes, total focus sessions, and completed session count for today.
- Planning Desk still keeps AI-first / local-fallback recognition, editable preview, selected import, and automatic `#imported` write-back.
- Planning Desk AI recognition is now explicit-only: phone recognition still starts from the `识别` button, desktop recognition starts from the `识别` button or `Ctrl+Enter`, and desktop import no longer silently calls parse when no preview exists.
- Settings -> `AI 调用配置` can fetch a single provider's model list with only Base URL / API Key before saving. Model fetch handles service roots, `/v1`, full `/chat/completions`, and full `/models` URLs, shows a compact dropdown on success, keeps a manual model-name fallback, and reports API-key / endpoint / non-JSON failures in user-readable language.
- Settings -> `AI 调用配置` can still test a single provider with the currently edited Base URL / API Key / model before saving; root Base URLs try `/v1/chat/completions` first, `/v1` Base URLs append `/chat/completions`, full `/models` URLs convert back to sibling `/chat/completions`, and HTML/non-JSON responses produce a readable Base URL hint.
- Daily board can show multiple active announcements parsed from unarchived Planning Desk notes. Announcement parsing now accepts explicit lines, checkbox announcement lines, quote-prefixed announcement lines, and inline `#公告` hints; tail `#imported` / hashtag metadata is stripped from display text.
- Settings no longer exposes or stores a separate announcement editor. The old `AppSettings` announcement fields and backup serialization were removed; old backup JSON fields are ignored and legacy SharedPreferences keys are cleaned once.
- Android launcher widgets now expose a `今日看板` widget backed by Room data: active announcements, today todos, today schedule state, and tomorrow schedule summary share one RemoteViews `ListView`; widget colors support system dark mode and widget refresh uses a board-range query rather than pulling all todos.
- Android launcher widget root now layers the daily-board background art with light/dark scrims and uses a circular menu-button + `每日看板` title area, so the widget reads closer to the in-app daily board rather than a generic list surface.
- Android launcher widget rows now use distinct RemoteViews layouts for the greeting card, focus summary card, orange announcement banner, section headers, empty cards, todo cards, and an aggregated schedule card. The schedule card mirrors the daily-board structure: one left date block, right-side today rows, tomorrow label, tomorrow rows, and vertical color strips.
- Android launcher widget now includes a `今日已专注` card with today's completed focus minutes, total sessions, and completed sessions, matching the in-app daily-board ordering more closely.
- Tapping a todo row opens that todo, tapping an event row opens Calendar with that event detail, tapping an announcement row opens the source Planning Desk note, and section/empty rows return to the default daily board.
- Desktop web `/api/snapshot` includes active Planning Desk announcements and the browser console renders them as a top announcement banner. Long announcement text now scrolls only when the combined text exceeds 60 characters, and hover pauses the marquee.
- Desktop web now follows system dark mode through CSS variables for timeline cards, event cards, modal sheets, summary cards, sidebar cards, tab buttons, Planning Desk, and announcements.
- Launch screen now hides as soon as `TodoUiState.dataReady` is true, with an 800 ms fallback cap.
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
- Planning notes, planning mappings, and focus sessions are included in backup / restore snapshots.

## Recent Checked Areas

Recent code inspection and build verification cover:

- `PlanningLineMapping.kt`, `PlanningLineMatcher.kt`, `PlanningSyncModels.kt`: mapping schema, matching strategy, and operation result models.
- `TodoRepository.kt`: mapping status sync, refresh, postpone, undo, conflict resolution, and backup inclusion.
- `TodoDao.kt`, `AppDatabase.kt`, `DatabaseMigrations.kt`, `TodoApplication.kt`: Room entity registration, migration path, and DB version `11`.
- `PlanningDeskPanel.kt`: phone-side refresh/postpone/undo entry points, completed-to-markdown sync, preview state pills, and conflict actions.
- `TodoViewModel.kt`: phone-side orchestration for mapping operations and reminder rescheduling after planning operations.
- `DesktopSyncCoordinator.kt`, `DesktopSyncServer.kt`, `app/src/main/assets/desktop-web/*`: desktop Planning Desk mapping APIs, preview/status rendering, desktop refresh/postpone/undo/conflict actions, announcement marquee, and system-follow dark theme.
- `BackupManager.kt`, `BackupModels.kt`: planning mapping export/import.
- `PlanningAiCaller.kt`, `SettingsPanel.kt`: AI provider model-list fetch path, test-connection path, endpoint fallback, and Settings UI.
- `PlanningAnnouncementParser.kt`, `DashboardChrome.kt`, `PlanningDeskPanel.kt`: Planning Desk multi-announcement parsing, help text, and board banner visibility.
- `DailyBoardSnapshot.kt`, `TodoWidgetProvider.kt`, `TodoWidgetService.kt`, widget XML resources, `AndroidManifest.xml`, `MainActivity.kt`, `DashboardScreen.kt`, `CalendarPanel.kt`: Android launcher widget registration, board-style RemoteViews data path, card-style row layouts, row-level deep links, and in-app launch routing.
- `FocusSession.kt`, `FocusActivity.kt`, `SettingsPanel.kt`, `TodoCards.kt`, `DashboardChrome.kt`, `TodoViewModel.kt`: focus-session schema, full-screen countdown, focus preferences, todo long-press entry, daily-board focus stats, and settings state propagation.

## Documentation Health

Current docs are being synchronized for `1.9.0.1`:

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

1. Device-side verification is still required for the full Planning Desk mapping loop: import -> status sync -> completed markdown sync -> refresh -> postpone -> undo -> conflict resolution.
2. Settings -> AI 调用配置 model discovery still needs device-side verification with real providers: valid `/models`, root and `/v1` Base URLs, full `/chat/completions` conversion, invalid keys, unsupported `/models`, HTML responses, dropdown selection, and manual fallback.
3. Desktop-browser verification is still required for the mapping loop, system dark mode, and the top announcement marquee after installing the latest APK on the phone and reconnecting from a real browser.
4. Widget behavior and the new card-style visual hierarchy must be verified on a real launcher because Android widget picker / RemoteViews refresh, resize, dark-mode behavior, and row-level PendingIntent deep links cannot be fully covered by JVM tests.
5. Planning Desk announcement syntax, multi-announcement visibility, date-range filtering, long-text marquee, preview highlighting, desktop-web propagation, and widget propagation need real UI verification.
6. Unit tests currently cover parser / AI / line-matching behavior, but there are still no dedicated repository-level automated tests for refresh/postpone/undo/conflict flows; those are presently covered by code inspection plus build/test success.
7. Long-running chat sessions can become unreliable, so repository docs must remain the source of truth for future continuation.
8. 专注模式 requires real-device verification for haptics, screen-on behavior, countdown extension, save-before-exit persistence, and daily-board stat refresh.
9. The AI 日报 / 周报 goal remains pending after the `1.9.0` focus-session commit.

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and `git status`
4. Only then decide the next edit
