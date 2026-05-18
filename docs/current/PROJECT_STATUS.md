# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.10.0"`
  - `versionCode = 218`

## Current Build Facts

- Latest debug APK output after this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.10.0-debug.apk`
- Latest signed release APK remains the previous release artifact unless a new release build is requested:
  - `app/build/outputs/apk/release/PaykiTodo-1.9.23-release.apk`
- Verification completed for the current `1.10.0` development round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `./gradlew.bat :app:assembleDebug`
  - `git diff --check`
  - `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/debug/PaykiTodo-1.10.0-debug.apk`
  - debug `output-metadata.json` reports `versionCode=218`, `versionName=1.10.0`, `outputFile=PaykiTodo-1.10.0-debug.apk`
- Release-signing privacy:
  - local `keystore.properties`, `release/PaykiTodo-release.jks`, and built package artifacts are ignored by Git
- Latest emulator smoke recorded for `1.9.21` remains historical:
  - device id: `emulator-5554`
  - installed APK: `app/build/outputs/apk/debug/PaykiTodo-1.9.21-debug.apk`
  - checked: app launch, Daily Board UI tree, drawer UI tree, Calendar page UI tree, screenshot capture, and logcat fatal-crash scan
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo
- Release-signing baseline:
  - safe template: `keystore.properties.example`
  - real local config: ignored root-level `keystore.properties`
  - generated keystore location: ignored root-level `release/`
  - Gradle release tasks fail early if required signing fields or the keystore file are missing

## Current Worktree Reality

The repository is now being advanced to `1.10.0`. It carries forward the `1.9.23` release baseline, then adds countdown-day support for important DDL / schedule targets and expands the widget surfaces.

Most important current baseline facts:

- version metadata is `1.10.0 / 218`
- Database version is `17`.
- `MIGRATION_12_13` creates `ai_reports`.
- `MIGRATION_13_14` creates `todo_items` indices for board, reminder, group, and recurrence lookup paths.
- `MIGRATION_14_15` adds / backfills `planning_notes.hasAnnouncementHint` plus the announcement lookup index.
- `MIGRATION_15_16` adds desktop todo paging and AI-report generated-time/type indices.
- `MIGRATION_16_17` adds `countdownEnabled` to `todo_items` and `recurring_task_templates`, plus `index_todo_items_countdown`.
- Active no-DDL todos are treated as today todos across phone daily board, Android widget board query, desktop daily board, and desktop todo management. They remain reminder-disabled, recurrence-disabled, and countdown-disabled until the user adds a DDL.
- Countdown-enabled todos use their DDL date as the target; countdown-enabled events use their start date.
- Expired countdown targets are filtered out before board / widget / desktop rendering.
- Phone daily board now has a countdown card for active countdown targets.
- Desktop daily board now renders countdown targets and desktop todo / event editors can preserve countdown state.
- Android 今日看板 widget now includes a countdown section while still omitting the focus card.
- Android includes a new independent PaykiTodo 倒数日 widget for the nearest 3 countdown targets.
- Android launcher widget event locations render saved text directly and no longer auto-prepend `@`; user-entered `@主楼B1-412` stays exactly one `@`.
- Desktop web first connection still requests `/api/snapshot?scope=board`, returning lightweight daily-board data first.
- Desktop web loads todo management through paged/searchable `/api/todos?offset=...&limit=...&q=...` and calendar timeline through visible-range `/api/events?start=...&end=...`.
- Phone ordinary board/task state remains screen-scoped and avoids carrying full history, full calendar, full Planning Desk notes, or full AI report history.
- Daily-board announcements in phone UI, Android widget, and desktop `/api/snapshot` use indexed `planning_notes.hasAnnouncementHint` candidates instead of scanning every planning document body.
- Desktop sync enable immediately ensures the phone-side LAN server is started; if no authorized client connects within 5 minutes, desktop sync disables itself and stops the service.
- Historical versioned docs live under `docs/archive/historical/`; current docs and code remain the source of truth.
- Safe release-signing explanations live under `docs/templates/`; real signing values must stay only in ignored root-level `keystore.properties`.

## Recent Checked Areas

Recent code inspection and build verification cover:

- `TodoItem.kt`, `RecurringTaskTemplate.kt`, `TodoRecurrence.kt`, `CalendarEventDraft.kt`, `TodoRepository.kt`, `BackupManager.kt`: countdown persistence for todos, events, recurring templates, drafts, backup and restore.
- `AppDatabase.kt`, `DatabaseMigrations.kt`, `TodoApplication.kt`, `TodoDao.kt`: database version 17, countdown migration, countdown query index, active countdown lookup, and board-range inclusion for countdown targets.
- `DailyBoardSnapshot.kt`, `DailyBoardSnapshotBuilderTest.kt`: countdown target/date/day calculations and exclusion of expired/no-DDL targets.
- `TodoEditorDialog.kt`, `CalendarEventEditorDialog.kt`, `DashboardChrome.kt`, `TodoViewModel.kt`: phone editor switches and daily-board countdown rendering.
- `DesktopSyncCoordinator.kt`, `DesktopSyncModels.kt`, `app/src/main/assets/desktop-web/app.js`, `index.html`, `app.css`: desktop countdown JSON, daily-board display, editor persistence, and preview status.
- `TodoWidgetService.kt`, `TodoWidgetProvider.kt`, `CountdownWidgetProvider.kt`, `widget_countdown.xml`, `widget_countdown_info.xml`: existing 今日看板 widget countdown section and independent 倒数日 widget.

## Documentation Health

Current docs synchronized or updated for `1.10.0`:

- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `app/src/main/assets/wiki/index.html`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/PAYKITODO_SESSION_LEDGER.md`

Older versioned docs under `docs/archive/historical/` remain historical references and should not be treated as the live baseline unless a current doc explicitly points to them.

## Current Risk Areas

1. Device-side verification is still required for Android widget countdown rendering, launcher widget resizing, stale launcher cache behavior, light/dark readability, and date-change refresh because RemoteViews behavior varies by launcher.
2. Browser verification is required for the desktop daily-board countdown card, countdown state preservation in todo/event editors, and split-endpoint refresh behavior after mutations.
3. Settings -> AI 调用配置 model discovery still needs device-side verification with real providers.
4. Planning Desk announcement syntax, multi-announcement visibility, date-range filtering, desktop-web propagation, and widget propagation need real UI verification.
5. 专注模式 still needs broader real-device verification for haptics, screen-on behavior, countdown extension, save-before-exit persistence, and daily-board stat refresh.
6. AI 日报 / 周报 physical-device verification is still recommended for OEM alarm policies and reboot/time-change recovery.
7. Very large datasets may still require full-text AI-report search, deeper desktop browser profiling, and real-device calendar profiling.
8. Long-running chat sessions can become unreliable, so repository docs must remain the source of truth for future continuation.

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and `git status`
4. Only then decide the next edit
