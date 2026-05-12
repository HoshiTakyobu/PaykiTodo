# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.6.45"`
  - `versionCode = 117`

## Current Build Facts

- Latest debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.6.45-debug.apk`
- Minimal verification completed in the latest code round:
  - `./gradlew assembleDebug` succeeded with Android Studio bundled `jbr`
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now at `1.6.45` after splitting desktop Web UI assets out of Kotlin raw strings and documenting the architecture.

Most important current baseline facts:

- version metadata is `1.6.45 / 117`
- launcher adaptive icon foreground now directly uses picture resource `@drawable/ic_launcher_art`
- old vector mark launcher resources have been deleted so the launcher cannot fall back to them again
- picture launcher art has been reprocessed to an opaque pure-white background with smaller centered content
- todo and calendar editors share a comma-separated reminder input syntax
- reminder input accepts examples such as `5,15,16:30,05-10 15:00,2026-05-10 14:30`
- invalid reminder entries turn the field/error text red and disable the save button
- normal todos now use `reminderOffsetsCsv` for multi-reminder storage and scheduling, not only the single `reminderAtMillis` field
- todo batch import uses the lightweight comma syntax `DDL时间,任务名称,提醒时间` and defaults group / ring / vibrate settings
- todo batch-import DDL accepts `HH:mm` as today's time and also accepts Chinese colon input such as `16：30`
- calendar batch import `Remind=` fields use the same reminder-time parser as the editors
- full-screen and accessibility reminder custom snooze inputs accept minutes or a concrete future time
- custom snooze no longer has a 180-minute cap; the target only has to be in the future
- todo snooze moves the todo DDL when the snooze target is later than the current DDL, and pins the next reminder to that target
- reminder / batch / custom snooze input surfaces have nearby question-mark syntax help buttons
- in-app Wiki has been updated to describe current reminder syntax, todo batch syntax, calendar batch `Remind=`, and custom snooze behavior
- daily board no longer exposes add / batch-add controls; it is a read-only board surface
- active task surface exposes only todo batch import beside the bottom-right new-todo button
- calendar header exposes a standalone `批量` button; calendar batch import remains on the calendar surface
- daily-board schedule event color strips now match the right-side text block height
- daily-board in-progress and normal schedule rows share the same left color-bar column
- normal daily-board schedule rows have a thin same-color border; in-progress rows use a gold border plus very subtle inner highlight rather than a large yellow overlay
- launch screen uses transparent `ic_launcher_art_transparent`; launcher / install icons still use the white-background adaptive icon art
- desktop web can edit existing todos, including DDL, reminder time, group, recurrence, ring, and vibration
- desktop web timed and all-day event cards expose explicit edit buttons; event editing keeps the existing event editor flow
- desktop web todo / event editors now use a bottom-sheet-like header with cancel / centered title / save actions
- desktop web editor fields are grouped into card-like surfaces, and timeline / event card actions use lighter pill buttons
- desktop web create-mode destructive buttons are hidden by a shared `.hidden` rule rather than only the modal backdrop rule
- desktop Web HTML / CSS / JS now live under `app/src/main/assets/desktop-web/` instead of inside large Kotlin raw strings
- `DesktopSyncWebAssets.kt` is now a small asset loader with a minimal fallback page
- `docs/current/DESKTOP_WEB_ARCHITECTURE.md` documents why the APK contains desktop UI assets and how to evolve that structure later

## Recent Checked Areas

Recent code inspection and build verification cover:

- `ReminderInputParser.kt`: shared parser for minutes, same-day times, current-year date-times, and full date-times
- `TodoRecurrence.kt`: `TodoDraft` carries normalized reminder offsets while keeping backward compatibility with `reminderAt`
- `ReminderOffsetCodec.kt`: todo reminder trigger calculation reads configured offsets
- `TodoRepository.kt`: todo items and recurring templates persist multiple reminder offsets
- `TodoEditorDialog.kt` and `CalendarEventEditorDialog.kt`: shared input syntax, validation message, save disabling
- `TodoBatchImport.kt`: lightweight comma-based todo batch parser/dialog, including same-day DDL time parsing
- `ReminderActivity.kt`, `ReminderAccessibilityOverlay.kt`, `ReminderInputParser.kt`, `TodoRepository.kt`: custom snooze parsing and DDL update behavior
- `InputSyntaxHelp.kt`: shared question-mark help button and syntax help dialog
- `app/src/main/assets/wiki/index.html`: current input syntax documentation
- `DesktopSyncCoordinator.kt`, `DesktopSyncWebAssets.kt`, `app/src/main/assets/desktop-web/*`: desktop web routing, asset loading, and desktop browser UI resources

## Documentation Health

Current docs have been synchronized for `1.6.45`:

- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/DESKTOP_WEB_ARCHITECTURE.md`

Older versioned docs under `docs/` remain historical references and should not be treated as the live baseline unless a current doc explicitly points to them.

## Current Risk Areas

1. Device-side verification is required for the new reminder input UX, especially invalid-value red state and disabled save behavior
2. Todo multi-reminder scheduling should be tested with at least two future reminders on one todo
3. Todo batch import should be tested with valid comma rows, no-DDL rows, and illegal reminder rows
4. Calendar event reminder input and calendar batch `Remind=` should be tested for all-day and timed events
5. Custom snooze should be tested with both minutes and a future clock time
6. Long-running chat sessions can become unreliable, so repository docs must carry state

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and git status
4. Only then decide the next edit
