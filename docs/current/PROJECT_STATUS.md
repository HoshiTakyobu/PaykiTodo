# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.6.30"`
  - `versionCode = 102`

## Current Build Facts

- Latest debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.6.30-debug.apk`
- Minimal verification completed in the latest code round:
  - `./gradlew assembleDebug` succeeded
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now at committed and pushed `1.6.30` baseline after the todo preview / delete confirmation / desktop sync notification repair round.

Most important current baseline facts:

- version metadata is `1.6.30 / 102`
- active todo preview uses the shared bottom-sheet visual language instead of the old alert-style dialog
- active todo completion is tied to the checkbox area; tapping the card body opens preview and should not complete the item
- delete actions that are user-visible destructive operations now require confirmation in the phone UI and desktop web console paths touched in this round
- calendar event reminder acknowledgement preserves configured reminder offsets so the event preview can still show the reminder setup after all reminders have fired
- the desktop sync foreground notification has a content intent that opens the app directly into Settings -> Desktop Sync
- the daily board remains the default landing section
- calendar and board visual polish from the 1.6.x line remains included
- launcher / notification icon resource work from the 1.6.x line remains included

## Recent Checked Areas

Recent code inspection and build verification cover:

- `TodoCards.kt`: active todo preview and completion-click split
- `CalendarPanel.kt`: calendar event delete confirmation and schedule-template delete confirmation
- `GroupManagementPanel.kt`: group deletion confirmation
- `TodoRepository.kt`, `TodoViewModel.kt`, `DesktopSyncCoordinator.kt`: reminder configuration preservation after acknowledgement / scheduling changes
- `DesktopSyncService.kt`, `MainActivity.kt`, `DashboardScreen.kt`, `DashboardChrome.kt`, `SettingsPanel.kt`: foreground notification route into desktop sync settings
- `DesktopSyncWebAssets.kt`: desktop web delete confirmation and event delete semantics

## Documentation Health

Current docs have been synchronized for `1.6.30`:

- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

Older versioned docs under `docs/` remain historical references and should not be treated as the live baseline unless a current doc explicitly points to them.

## Current Risk Areas

1. Device-side verification is still required for the todo-preview no-miscompletion fix on the daily board
2. Destructive action confirmation should be rechecked on both phone and desktop web paths
3. Desktop-sync notification click routing should be checked while the foreground service is running
4. Calendar reminder preview preservation should be checked after real reminder acknowledgement
5. Long-running chat sessions can become unreliable, so repository docs must carry state

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and git status
4. Only then decide the next edit
