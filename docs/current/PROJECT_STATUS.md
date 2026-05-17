# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.9.10"`
  - `versionCode = 204`

## Current Build Facts

- Latest debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.9.10-debug.apk`
- Minimal verification completed in the latest code round:
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `git diff --check`
  - `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=204`, `versionName=1.9.10`, `outputFile=PaykiTodo-1.9.10-debug.apk`
- Previous `1.9.5` emulator verification:
  - installed `PaykiTodo-1.9.5-debug.apk` on `emulator-5554`
  - verified AI daily-report fallback scheduling while exact-alarm permission is denied, automatic report generation, notification posting, notification deep link to `AI 日报`, disabled-switch cancellation, and weekly-report alarm registration
- Latest `1.9.7` emulator smoke verification:
  - installed `PaykiTodo-1.9.7-debug.apk` on `emulator-5554`
  - launched the app to Daily Board without startup crash
  - confirmed Planning Desk shortcut toolbar exposes `公告`
  - confirmed Settings -> AI 调用配置 -> AI 日报 / 周报 exposes `了解 AI 日报`, and the guide sheet contains the five expected sections
- Latest `1.9.8` emulator smoke verification:
  - installed `PaykiTodo-1.9.8-debug.apk` on `emulator-5554`
  - launched the app to Daily Board without startup crash
  - confirmed drawer navigation shows `AI 报告` between `规划台` and `历史记录`
  - opened `AI 报告` and confirmed migrated report cards show `来源：历史归档`
  - opened a report detail and confirmed type, coverage period, provider/source, and delete entry are visible
  - opened Settings -> AI 调用配置 -> `了解 AI 日报` and confirmed the help surface is centered/readable rather than left-biased
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now being advanced to `1.9.10`. It carries forward the `1.9.0` focus-mode baseline, the `1.9.1` AI daily / weekly report generation, the Android launcher-widget visual hotfixes, the AI report scheduling reliability fix for Android devices without exact-alarm permission, the independent AI report archive, the desktop Web daily-board work, and now fixes widget navigation plus todo reminder handling.

Most important current baseline facts:

- version metadata is `1.9.10 / 204`
- Database version is now `13`; `MIGRATION_12_13` creates `ai_reports`.
- Settings -> `专注模式` controls default focus duration, extension duration, screen-on behavior, and a documented-only notification-suppression preference.
- Active todo long-press menus include `开始专注 · X 分钟`; the daily-board focus card can start free focus.
- `FocusActivity` provides a full-screen circular countdown with pause / continue, completion confirmation, abandon confirmation, zero-time vibration, extension, save-before-exit behavior, and a completion feedback page.
- Daily board shows completed focus minutes, total focus sessions, and completed session count for today.
- Planning Desk still keeps AI-first / local-fallback recognition, editable preview, selected import, and automatic `#imported` write-back.
- Planning Desk AI recognition is now explicit-only: phone recognition still starts from the `识别` button, desktop recognition starts from the `识别` button or `Ctrl+Enter`, and desktop import no longer silently calls parse when no preview exists.
- Settings -> `AI 调用配置` can fetch a single provider's model list with only Base URL / API Key before saving. Model fetch handles service roots, `/v1`, full `/chat/completions`, and full `/models` URLs, shows a compact dropdown on success, keeps a manual model-name fallback, and reports API-key / endpoint / non-JSON failures in user-readable language.
- Settings -> `AI 调用配置` can still test a single provider with the currently edited Base URL / API Key / model before saving; root Base URLs try `/v1/chat/completions` first, `/v1` Base URLs append `/chat/completions`, full `/models` URLs convert back to sibling `/chat/completions`, and HTML/non-JSON responses produce a readable Base URL hint.
- Settings -> `AI 调用配置` includes `AI 日报 / 周报` controls: daily and weekly switches, compact HH:mm time fields, save/re-schedule behavior, centered `了解 AI 日报` guidance, and an immediate daily-report generation button for debugging.
- AI daily / weekly reports write into the independent `AI 报告` archive (`ai_reports`) instead of Planning Desk. Reports collect completed todos, missed todos, events, upcoming DDLs, and focus minutes, call enabled Planning AI providers first, and fall back to a local template if AI is unavailable.
- AI report notifications use a low-priority `ai_report_channel`; tapping a report notification opens the matching AI report detail.
- `DailyReportScheduler` schedules daily and Sunday weekly report alarms, cancels disabled schedules, and is invoked from app startup plus boot/time/timezone recovery; it uses exact alarms when allowed and falls back to system-allowed idle scheduling when exact-alarm permission is missing.
- Legacy Planning Desk report notes named `AI 日报` / `AI 周报` are migrated once into `ai_reports`, deleted from Planning Desk, and removed from `lastOpenedPlanningNoteId` if needed.
- Planning Desk shortcut toolbar includes `公告`, which inserts `#公告 ` on a new line so users do not need to manually type the announcement token.
- Daily board can show multiple active announcements parsed from unarchived Planning Desk notes. Announcement parsing now accepts explicit lines, checkbox announcement lines, quote-prefixed announcement lines, and inline `#公告` hints; tail `#imported` / hashtag metadata is stripped from display text.
- Settings no longer exposes or stores a separate announcement editor. The old `AppSettings` announcement fields and backup serialization were removed; old backup JSON fields are ignored and legacy SharedPreferences keys are cleaned once.
- Android launcher widgets now expose a `今日看板` widget backed by Room data: active announcements, today todos, today schedule state, and tomorrow schedule summary share one RemoteViews `ListView`; widget colors support system dark mode and widget refresh uses a board-range query rather than pulling all todos.
- Android launcher widget root now layers the daily-board background art with light/dark scrims and uses a circular menu-button + `每日看板` title area, so the widget reads closer to the in-app daily board rather than a generic list surface.
- Android launcher widget rows now use distinct RemoteViews layouts for the greeting card, orange announcement banner, section headers, empty cards, todo cards, and an aggregated schedule card. The schedule card mirrors the daily-board structure: one left date block, right-side today rows, tomorrow label, tomorrow rows, and vertical color strips.
- Android launcher widget no longer includes the `今日已专注` / focus card, so the launcher surface stays focused on announcements, greeting, todos, and today/tomorrow schedules.
- Android launcher widget card surfaces now use stronger light/dark opacity, lightweight elevation, retuned scrims, tighter title/card spacing, and daily-board ordering with announcements before greeting so the widget resembles the in-app daily board card stack more than a generic RemoteViews list.
- Android launcher widget default provider size is now closer to a square / vertical daily-board card instead of a shallow flat list; its header is lighter and the greeting, focus, todo, empty, and schedule cards use more solid rounded daily-board-style surfaces with wider text/strip breathing room.
- Android launcher widget `1.9.4` visual pass adds a static daily-board-style widget picker preview, raises the default target to a 4x5 vertical board, changes todo rows to include a checkbox-like marker plus `DDL HH:mm` chip, and removes ordinary schedule-row fill so schedule rows read as transparent daily-board rows with vertical color strips rather than small system-list blocks.
- Android launcher widget `1.9.6` visual pass compresses the topbar to a daily-board-like menu/title/date row, removes the extra `轻触打开` subtitle, makes todo cards show group tag, notes, and `⏰ DDL HH:mm`, removes heavy card strokes, and gives in-progress schedule rows a gold border with a faint gold fill.
- Tapping a widget todo area opens the in-app My Tasks section, tapping a widget event / schedule area opens Calendar without forcing a specific editor detail, tapping an announcement row opens the source Planning Desk note, and section/empty rows return to the default daily board.
- Todo editor now exposes reminder delivery mode for todos, so a todo can be set to full-screen reminder or notification reminder instead of always being stored as full-screen.
- Desktop Web todo editing also includes the todo reminder delivery mode field and sends it through the sync API.
- Reminder screens now separate snooze from DDL changes: custom snooze only moves the next reminder, while a distinct `DDL 推迟` input validates positive minute increments, same-date clock targets, and full date-time targets against the current DDL.
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
- Desktop-web `/api/snapshot` now includes `todayBoard`, built from the same `DailyBoardSnapshotBuilder` used by the phone/widget board. The browser first tab is `每日看板`, with a current/next item card, today focus statistics, today todos, today schedule, tomorrow schedule, phone-like ended-event filtering, and in-progress gold highlighting above the full todo management timeline.
- Desktop Planning Desk import now accepts edited AI candidates directly when their IDs do not match local-rule `line-*` fallback IDs. This fixes the “AI recognized 1 item but imported 0” path for AI candidates such as `ai-0`.
- Planning Desk AI group names are explicit-only: `groupName` is preserved only when the source line contains explicit group markers such as `#group`, `分组：`, `项目：`, or `课程：`; ordinary titles such as `16:05-18:00 入党表格填写` keep an empty group.
- Planning notes, planning mappings, focus sessions, and AI reports are included in backup / restore snapshots.

## Recent Checked Areas

Recent code inspection and build verification cover:

- `PlanningLineMapping.kt`, `PlanningLineMatcher.kt`, `PlanningSyncModels.kt`: mapping schema, matching strategy, and operation result models.
- `TodoRepository.kt`: mapping status sync, refresh, postpone, undo, conflict resolution, and backup inclusion.
- `TodoDao.kt`, `AppDatabase.kt`, `DatabaseMigrations.kt`, `TodoApplication.kt`: Room entity registration, migration path, DB version `12`, and `MIGRATION_11_12`.
- `PlanningDeskPanel.kt`: phone-side refresh/postpone/undo entry points, completed-to-markdown sync, preview state pills, and conflict actions.
- `TodoViewModel.kt`: phone-side orchestration for mapping operations and reminder rescheduling after planning operations.
- `DesktopSyncCoordinator.kt`, `DesktopSyncServer.kt`, `app/src/main/assets/desktop-web/*`: desktop Planning Desk mapping APIs, preview/status rendering, desktop refresh/postpone/undo/conflict actions, announcement marquee, and system-follow dark theme.
- `BackupManager.kt`, `BackupModels.kt`: planning mapping export/import.
- `PlanningAiCaller.kt`, `SettingsPanel.kt`: AI provider model-list fetch path, test-connection path, endpoint fallback, and Settings UI.
- `AiReport.kt`, `DailyReportGenerator.kt`, `DailyReportNotifier.kt`, `AiReportPanel.kt`: AI report archive schema, generation path, notification deep link, list/detail UI, and centered help dialog.
- `PlanningAnnouncementParser.kt`, `DashboardChrome.kt`, `PlanningDeskPanel.kt`: Planning Desk multi-announcement parsing, help text, and board banner visibility.
- `DailyBoardSnapshot.kt`, `TodoWidgetProvider.kt`, `TodoWidgetService.kt`, widget XML resources, `AndroidManifest.xml`, `MainActivity.kt`, `DashboardScreen.kt`, `CalendarPanel.kt`: Android launcher widget registration, board-style RemoteViews data path, card-style row layouts, row-level deep links, and in-app launch routing.
- `FocusSession.kt`, `FocusActivity.kt`, `SettingsPanel.kt`, `TodoCards.kt`, `DashboardChrome.kt`, `TodoViewModel.kt`: focus-session schema, full-screen countdown, focus preferences, todo long-press entry, daily-board focus stats, and settings state propagation.

## Documentation Health

Current docs are being synchronized for `1.9.10`:

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
8. 专注模式 still needs broader real-device verification for haptics, screen-on behavior, countdown extension, save-before-exit persistence, and daily-board stat refresh beyond the emulator free-focus path already exercised.
9. AI 日报 / 周报 `1.9.5` emulator verification covers exact-alarm-denied fallback scheduling, automatic daily generation, local-template fallback, report notification, notification deep link, disabled-switch cancellation, and weekly alarm registration; physical-device verification is still recommended for OEM alarm policies and reboot/time-change recovery.
10. Android launcher widget rendering still needs real launcher verification because RemoteViews sizing, widget picker previews, resize behavior, dark mode, and row-level PendingIntent handling vary by launcher.

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and `git status`
4. Only then decide the next edit
