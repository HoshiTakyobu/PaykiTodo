# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.6.33"`
  - `versionCode = 105`

## Current Build Facts

- Latest debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.6.33-debug.apk`
- Minimal verification completed in the latest code round:
  - `./gradlew assembleDebug` succeeded
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now at `1.6.33` baseline after the Wiki / settings direct-jump / desktop all-day / launcher icon repair round.

Most important current baseline facts:

- version metadata is `1.6.33 / 105`
- in-app Wiki WebView enables JavaScript for the local asset page, so sidebar chapter buttons can switch content
- Settings -> 使用说明 opens the in-app Wiki directly
- Settings -> 提示音 opens the system notification-tone picker directly
- desktop web console renders multi-day all-day events as one horizontal continuous bar across visible days
- adaptive launcher icon foreground is reconnected to the safe-zone PaykiTodo vector mark, not the full raster launcher art
- daily-board todo block includes missed active todos as well as today's normal todos
- shared phone-side delete confirmation uses a refined dangerous-action bottom sheet with red icon, irreversible-action text, message card, red confirm button, and cancel button
- daily-board timed events that have already ended are filtered out of the today's schedule block
- currently running daily-board events are highlighted with a gold outline and subtle glow
- calendar timeline pending event drafts can be canceled by long-pressing blank timeline space
- opening an existing calendar event clears any pending event draft behind it

## Recent Checked Areas

Recent code inspection and build verification cover:

- `WikiActivity.kt`: local Wiki JavaScript navigation enabled with file-cross-origin access disabled
- `SettingsPanel.kt`: help and tone rows changed from subpanels to direct actions
- `DesktopSyncWebAssets.kt`: multi-day all-day event rendering as spanning horizontal cards
- `ic_launcher_foreground.xml`: adaptive launcher foreground points to `ic_payki_mark`
- `DashboardChrome.kt`: daily-board missed todo inclusion, schedule filtering, and running-event gold highlight
- `EditorBottomSheet.kt`: refined shared dangerous-action delete confirmation UI
- `CalendarPanel.kt`: timeline pending-draft cancel / clear behavior

## Documentation Health

Current docs have been synchronized for `1.6.33`:

- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

Older versioned docs under `docs/` remain historical references and should not be treated as the live baseline unless a current doc explicitly points to them.

## Current Risk Areas

1. Device-side verification is required for the launcher icon surface because Android launchers can cache icons aggressively
2. Wiki sidebar navigation should be tested inside the app WebView, not only in a desktop browser
3. Settings direct actions should be tested for 使用说明 and 提示音 rows
4. Desktop web all-day spanning behavior should be tested with a real multi-day all-day event
5. Long-running chat sessions can become unreliable, so repository docs must carry state

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and git status
4. Only then decide the next edit
