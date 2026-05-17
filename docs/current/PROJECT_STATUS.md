# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.9.13"`
  - `versionCode = 207`

## Current Build Facts

- Latest debug APK output after this round should be:
  - `app/build/outputs/apk/debug/PaykiTodo-1.9.13-debug.apk`
- Verification completed before the final `1.9.13` rebuild:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
- Final verification after the `1.9.13` version bump:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
  - `git diff --check`
  - `output-metadata.json` reports `versionCode=207`, `versionName=1.9.13`, `outputFile=PaykiTodo-1.9.13-debug.apk`
- Previous emulator verification remains historical:
  - `1.9.5` checked AI report scheduling fallback on `emulator-5554`
  - `1.9.7` checked Planning Desk announcement shortcut and AI report guide on `emulator-5554`
  - `1.9.8` checked `AI 报告` archive reachability and guide readability on `emulator-5554`
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now being advanced to `1.9.13`. It carries forward the `1.9.12` widget no-DDL / database-index / AI-provider save-state fixes and adds a practical performance pass for desktop first load, phone main state, and calendar event grouping.

Most important current baseline facts:

- version metadata is `1.9.13 / 207`
- Database version remains `14`; `MIGRATION_13_14` creates `todo_items` indices for board, reminder, group, and recurrence lookup paths.
- `MIGRATION_12_13` remains the migration that creates `ai_reports`.
- Active no-DDL todos are treated as today todos across phone daily board, Android widget board query, desktop daily board, and desktop todo management. They remain reminder-disabled and recurrence-disabled until the user adds a DDL.
- A unit test now confirms no-DDL active todos remain in `今日待办` across later dates, not only on the creation day.
- Android launcher widget board queries include no-DDL todos and continue to use board-range data rather than scanning all history.
- Desktop web first connection requests `/api/snapshot?scope=board`, returning lightweight daily-board data first.
- Desktop web loads full todos/events only when the user enters the event timeline or explicitly clicks `加载完整待办 / 日程数据`.
- Desktop web status text distinguishes `看板轻量数据` from `完整数据`.
- Phone ordinary board/task `TodoUiState` uses a Room aggregate Flow for today's focus stats instead of observing the full `focus_sessions` table.
- Full AI report history is no longer merged into ordinary `TodoUiState`; `AI 报告` collects a separate `aiReports` StateFlow only when that section is shown.
- Calendar month view, agenda/list view, and visible all-day rows reuse one top-level event-by-date index instead of rebuilding date buckets independently.
- Planning Desk local parsing recognizes plain bullets (`- item`, `* item`, `• item`) as no-DDL todo candidates and shows an explicit preview message when doing so.
- Desktop sync records the first authorized API request as a real desktop connection. If no authorized client connects within 5 minutes after enabling sync, `DesktopSyncService` disables the setting and stops the local server / foreground service.
- Settings -> `电脑同步` explains both the multi-IP address meaning and the 5-minute no-authorized-client auto-close behavior.
- Settings -> `AI 调用配置` provider rows use summary cards with visible enable switches and a compact more menu for edit / reorder / delete.
- Settings -> `AI 调用配置` auto-saves valid provider changes after add/edit/toggle/reorder/delete where possible and shows an in-page warning when enabled providers are incomplete or current edits are not saved.
- Planning notes, planning mappings, focus sessions, and AI reports are included in backup / restore snapshots.

## Recent Checked Areas

Recent code inspection and build verification cover:

- `TodoDao.kt`, `TodoRepository.kt`, `FocusSession.kt`, `TodoViewModel.kt`: today focus aggregate, active planning-note query, no-DDL board range, midnight-sensitive today classification.
- `DesktopSyncCoordinator.kt`, `DesktopSyncModels.kt`, `app/src/main/assets/desktop-web/app.js`, `index.html`: lightweight board snapshot and full-data on-demand loading.
- `DashboardChrome.kt`, `DashboardScreen.kt`, `MainActivity.kt`: AI report list collection moved out of ordinary UI state.
- `CalendarPanel.kt`: shared event-by-date index across calendar surfaces.
- `DailyBoardSnapshotBuilderTest.kt`: no-DDL todo stays in today's todo list across later dates.
- `README.md`, `CHANGELOG.md`, `TODO.md`, `docs/current/*`, Wiki header: `1.9.13` status synchronization.

## Documentation Health

Current docs are being synchronized for `1.9.13`:

- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `app/src/main/assets/wiki/index.html`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/PAYKITODO_SESSION_LEDGER.md`
- `docs/current/PLANNING_AI_ASSISTANT_DESIGN.md`

Older versioned docs under `docs/` remain historical references and should not be treated as the live baseline unless a current doc explicitly points to them.

## Current Risk Areas

1. Device-side verification is still required for Android widget no-DDL display because RemoteViews / launcher refresh behavior varies by launcher.
2. Desktop browser verification is required for `1.9.13` lightweight snapshot flow: first load, full-data transition, event/todo editing from partial data, and tab retention after mutations.
3. Settings -> AI 调用配置 model discovery still needs device-side verification with real providers: valid `/models`, root and `/v1` Base URLs, full `/chat/completions` conversion, invalid keys, unsupported `/models`, HTML responses, dropdown selection, and manual fallback.
4. Planning Desk announcement syntax, multi-announcement visibility, date-range filtering, long-text marquee, preview highlighting, desktop-web propagation, and widget propagation need real UI verification.
5. Unit tests cover parser / AI / line-matching / no-DDL board behavior, but there are still no dedicated repository-level automated tests for Planning Desk refresh/postpone/undo/conflict flows.
6. 专注模式 still needs broader real-device verification for haptics, screen-on behavior, countdown extension, save-before-exit persistence, and daily-board stat refresh beyond the emulator free-focus path already exercised.
7. AI 日报 / 周报 physical-device verification is still recommended for OEM alarm policies and reboot/time-change recovery.
8. Very large datasets may still require deeper endpoint-level desktop splitting, AI report paging, and real-device calendar profiling.
9. Long-running chat sessions can become unreliable, so repository docs must remain the source of truth for future continuation.

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and `git status`
4. Only then decide the next edit
