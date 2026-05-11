# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.6.32"`
  - `versionCode = 104`

## Current Build Facts

- Latest debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.6.32-debug.apk`
- Minimal verification completed in the latest code round:
  - `./gradlew assembleDebug` succeeded
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now at `1.6.32` baseline after the daily-board missed-todo and delete-confirmation repair round.

Most important current baseline facts:

- version metadata is `1.6.32 / 104`
- daily-board todo block now includes missed active todos as well as today's normal todos
- shared phone-side delete confirmation uses a refined dangerous-action bottom sheet with red icon, irreversible-action text, message card, red confirm button, and cancel button
- daily-board timed events that have already ended are filtered out of the today's schedule block
- currently running daily-board events are highlighted with a gold outline and subtle glow
- calendar timeline pending event drafts can be canceled by long-pressing blank timeline space
- opening an existing calendar event clears any pending event draft behind it
- active todo preview uses the shared bottom-sheet visual language instead of the old alert-style dialog
- active todo completion is tied to the checkbox area; tapping the card body opens preview and should not complete the item
- calendar event reminder acknowledgement preserves configured reminder offsets so the event preview can still show the reminder setup after all reminders have fired
- the desktop sync foreground notification has a content intent that opens the app directly into Settings -> Desktop Sync
- the daily board remains the default landing section

## Recent Checked Areas

Recent code inspection and build verification cover:

- `DashboardChrome.kt`: daily-board missed todo inclusion, schedule filtering, and running-event gold highlight
- `EditorBottomSheet.kt`: refined shared dangerous-action delete confirmation UI
- `CalendarPanel.kt`: timeline pending-draft cancel / clear behavior
- `TodoCards.kt`: active todo preview and completion-click split
- `GroupManagementPanel.kt`: group deletion confirmation through shared component
- `TodoRepository.kt`, `TodoViewModel.kt`, `DesktopSyncCoordinator.kt`: reminder configuration preservation after acknowledgement / scheduling changes
- `DesktopSyncService.kt`, `MainActivity.kt`, `DashboardScreen.kt`, `DashboardChrome.kt`, `SettingsPanel.kt`: foreground notification route into desktop sync settings
- `DesktopSyncWebAssets.kt`: desktop web delete confirmation and event delete semantics

## Documentation Health

Current docs have been synchronized for `1.6.32`:

- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

Older versioned docs under `docs/` remain historical references and should not be treated as the live baseline unless a current doc explicitly points to them.

## Current Risk Areas

1. Device-side verification is still required for daily-board missed todo inclusion
2. The refined delete confirmation sheet should be checked on todo, event, group, and template deletion paths
3. Device-side verification is still required for daily-board finished-event filtering and running-event highlight
4. Calendar pending draft cancel behavior should be checked in the real time-axis UI
5. Long-running chat sessions can become unreliable, so repository docs must carry state

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and git status
4. Only then decide the next edit
