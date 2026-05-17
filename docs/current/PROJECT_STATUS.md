# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.9.12"`
  - `versionCode = 206`

## Current Build Facts

- Latest debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.9.12-debug.apk`
- Minimal verification completed in the latest code round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
  - `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=206`, `versionName=1.9.12`, `outputFile=PaykiTodo-1.9.12-debug.apk`
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

The repository is now being advanced to `1.9.12`. It carries forward the `1.9.0` focus-mode baseline, the `1.9.1` AI daily / weekly report generation, Android launcher-widget visual work, AI report archive, desktop Web daily-board work, widget navigation plus todo reminder handling, no-DDL today semantics, desktop-sync auto-shutdown, and now a `1.9.12` review/performance hotfix.

Most important current baseline facts:

- version metadata is `1.9.12 / 206`
- Database version is now `14`; `MIGRATION_13_14` creates `todo_items` indices for board, reminder, group, and recurrence lookup paths.
- `MIGRATION_12_13` remains the migration that creates `ai_reports`.
- Settings -> `专注模式` controls default focus duration, extension duration, screen-on behavior, and a documented-only notification-suppression preference.
- Active todo long-press menus include `开始专注 · X 分钟`; the daily-board focus card can start free focus.
- `FocusActivity` provides a full-screen circular countdown with pause / continue, completion confirmation, abandon confirmation, zero-time vibration, extension, save-before-exit behavior, and a completion feedback page.
- Daily board shows completed focus minutes, total focus sessions, and completed session count for today.
- Planning Desk keeps AI-first / local-fallback recognition, editable preview, selected import, automatic `#imported` write-back, stable mappings, refresh/postpone/undo, and conflict resolution.
- Planning Desk AI recognition is explicit-only: phone recognition starts from the `识别` button, desktop recognition starts from the `识别` button or `Ctrl+Enter`, and desktop import no longer silently calls parse when no preview exists.
- Settings -> `AI 调用配置` can fetch a single provider's model list with only Base URL / API Key before saving. Model fetch handles service roots, `/v1`, full `/chat/completions`, and full `/models` URLs, shows a compact dropdown on success, keeps a manual model-name fallback, and reports API-key / endpoint / non-JSON failures in user-readable language.
- Settings -> `AI 调用配置` can test a single provider with the currently edited Base URL / API Key / model before saving; root Base URLs try `/v1/chat/completions` first, `/v1` Base URLs append `/chat/completions`, full `/models` URLs convert back to sibling `/chat/completions`, and HTML/non-JSON responses produce a readable Base URL hint.
- Settings -> `AI 调用配置` provider rows use summary cards with visible enable switches and a compact more menu for edit / reorder / delete.
- Settings -> `AI 调用配置` now auto-saves valid provider changes after add/edit/toggle/reorder/delete where possible and shows an in-page warning when enabled providers are incomplete or current edits are not saved.
- Settings -> `AI 调用配置` includes `AI 日报 / 周报` controls: daily and weekly switches, compact HH:mm time fields, save/re-schedule behavior, centered `了解 AI 日报` guidance, and an immediate daily-report generation button for debugging.
- AI daily / weekly reports write into the independent `AI 报告` archive (`ai_reports`) instead of Planning Desk. Reports collect completed todos, missed todos, events, upcoming DDLs, and focus minutes, call enabled Planning AI providers first, and fall back to a local template if AI is unavailable.
- AI report notifications use a low-priority `ai_report_channel`; tapping a report notification opens the matching AI report detail.
- `DailyReportScheduler` schedules daily and Sunday weekly report alarms, cancels disabled schedules, and is invoked from app startup plus boot/time/timezone recovery; it uses exact alarms when allowed and falls back to system-allowed idle scheduling when exact-alarm permission is missing.
- Daily board can show multiple active announcements parsed from unarchived Planning Desk notes.
- Android launcher widgets expose a PaykiTodo `今日看板` widget backed by Room data: active announcements, today todos, today schedule state, and tomorrow schedule summary share one RemoteViews `ListView`.
- Android launcher widget no longer includes the `今日已专注` / focus card, so the launcher surface stays focused on announcements, greeting, todos, and today/tomorrow schedules.
- Tapping a widget todo area opens the in-app My Tasks section, tapping a widget event / schedule area opens Calendar without forcing a specific editor detail, tapping an announcement row opens the source Planning Desk note, and section/empty rows return to the default daily board.
- Active no-DDL todos are treated as today todos across phone daily board, Android widget board query, desktop daily board, and desktop todo management. They remain reminder-disabled and recurrence-disabled until the user adds a DDL.
- Planning Desk local parsing recognizes plain bullets (`- item`, `* item`, `• item`) as no-DDL todo candidates and now shows an explicit preview message when doing so.
- Desktop sync records the first authorized API request as a real desktop connection. If no authorized client connects within 5 minutes after enabling sync, `DesktopSyncService` disables the setting and stops the local server / foreground service.
- Settings -> `电脑同步` explains both the multi-IP address meaning and the 5-minute no-authorized-client auto-close behavior.
- Desktop web `/api/snapshot` includes active Planning Desk announcements and a phone-derived `todayBoard`; the browser first tab is `每日看板`.
- Desktop web `/api/snapshot` now reuses the groups already loaded into the snapshot instead of reading groups again just to build JSON group mappings.
- Planning notes, planning mappings, focus sessions, and AI reports are included in backup / restore snapshots.

## Recent Checked Areas

Recent code inspection and build verification cover:

- `TodoItem.kt`, `TodoDao.kt`, `TodoRepository.kt`, `AppDatabase.kt`, `DatabaseMigrations.kt`, `TodoApplication.kt`: no-DDL widget query path and DB version `14` / `MIGRATION_13_14` index migration.
- `PlanningMarkdownParser.kt`, `PlanningMarkdownParserTest.kt`: plain-bullet no-DDL preview messaging.
- `DesktopSyncCoordinator.kt`: `/api/snapshot` group-map reuse.
- `SettingsPanel.kt`: desktop-sync auto-close explanation and AI-provider save-state behavior.
- `README.md`, `CHANGELOG.md`, `TODO.md`, `docs/current/*`: `1.9.12` status synchronization.

## Documentation Health

Current docs are being synchronized for `1.9.12`:

- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/PLANNING_AI_ASSISTANT_DESIGN.md`

Older versioned docs under `docs/` remain historical references and should not be treated as the live baseline unless a current doc explicitly points to them.

## Current Risk Areas

1. Device-side verification is still required for Android widget no-DDL display because RemoteViews / launcher refresh behavior varies by launcher.
2. Settings -> AI 调用配置 model discovery still needs device-side verification with real providers: valid `/models`, root and `/v1` Base URLs, full `/chat/completions` conversion, invalid keys, unsupported `/models`, HTML responses, dropdown selection, and manual fallback.
3. Desktop-browser verification is still required for the mapping loop, system dark mode, and the top announcement marquee after installing the latest APK on the phone and reconnecting from a real browser.
4. Planning Desk announcement syntax, multi-announcement visibility, date-range filtering, long-text marquee, preview highlighting, desktop-web propagation, and widget propagation need real UI verification.
5. Unit tests currently cover parser / AI / line-matching behavior, but there are still no dedicated repository-level automated tests for refresh/postpone/undo/conflict flows; those are presently covered by code inspection plus build/test success.
6. Long-running chat sessions can become unreliable, so repository docs must remain the source of truth for future continuation.
7. 专注模式 still needs broader real-device verification for haptics, screen-on behavior, countdown extension, save-before-exit persistence, and daily-board stat refresh beyond the emulator free-focus path already exercised.
8. AI 日报 / 周报 physical-device verification is still recommended for OEM alarm policies and reboot/time-change recovery.
9. Desktop Web still needs a deeper performance pass: `/api/snapshot` no longer duplicates group reads, but it still transfers full datasets.
10. Main phone `TodoViewModel.uiState` still observes full focus-session and AI-report lists; later work should add aggregate/paged flows.

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and `git status`
4. Only then decide the next edit
