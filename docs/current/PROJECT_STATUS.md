# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.6.70"`
  - `versionCode = 142`

## Current Build Facts

- Latest debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.6.70-debug.apk`
- Minimal verification completed in the latest code round:
  - `./gradlew assembleDebug` succeeded with Android Studio bundled `jbr`
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now at `1.6.70`. It includes the desktop web editor crash fix, UI-copy cleanup, desktop web no-DDL todo editing support, in-app desktop delete confirmations, desktop-sync service self-stop protection, dynamic desktop-web resource versioning, a smaller calendar recomposition pass, desktop todo preview sheets plus direct desktop event editing, desktop mixed reminder syntax for todos/events, and lunar-label display in calendar views, minimal yearly same-lunar-date recurrence, and the latest desktop event direct-edit/status/UI polish.

Most important current baseline facts:

- version metadata is `1.6.70 / 142`
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
- normal daily-board schedule rows no longer show an outer border; in-progress rows use a gold border plus very subtle inner highlight rather than a large yellow overlay
- launch screen uses transparent `ic_launcher_art_transparent`; launcher / install icons still use the white-background adaptive icon art
- desktop web can edit existing todos, including DDL, reminder time, group, recurrence, ring, and vibration
- desktop web event cards open editing by card click; delete remains available inside the event editor, and inline event-card buttons are intentionally removed
- desktop web todo / event editors now use a bottom-sheet-like header with cancel / centered title / save actions
- desktop web editor fields are grouped into card-like surfaces, and timeline / event card actions use lighter pill buttons
- desktop web create-mode destructive buttons are hidden by a shared `.hidden` rule rather than only the modal backdrop rule
- desktop Web HTML / CSS / JS now live under `app/src/main/assets/desktop-web/` instead of inside large Kotlin raw strings
- `DesktopSyncWebAssets.kt` is now a small asset loader with a minimal fallback page
- `docs/current/DESKTOP_WEB_ARCHITECTURE.md` documents why the APK contains desktop UI assets and how to evolve that structure later
- in-app Wiki now preserves a left navigation / right article layout on phone-sized screens instead of stacking the navigation above the article
- daily board distinguishes between no schedule today and all of today's schedule already finished
- drawer header app icon is clipped to the circular header surface and enlarged to avoid showing the white rounded-rectangle launcher background
- reminder playback can now use alarm, accessibility, notification, or media audio channels
- PaykiTodo has an internal reminder-volume percentage for self-played alert audio
- an advanced temporary system-channel volume boost can raise the selected global stream during reminder playback and then restore it; it is off by default
- work mode suppresses outward reminder sound by default, forces stronger vibration even when an individual item disabled vibration, and routes calendar reminders into the full-screen / accessibility fallback chain
- daily board always shows a tomorrow schedule section; when tomorrow has no events it says `明天暂无日程`
- desktop web event cards open editing through delegated card click handling, no longer show inline edit/delete buttons, prefer group colors for display, use string-compatible ID lookup, tolerate recurrence weekdays as strings or arrays, and expose all-day events through compact per-day pills
- desktop web no longer shows the large separate all-day strip above the event timeline; compact all-day pills remain available inside day columns
- Settings is split into common settings and advanced settings; desktop sync is common, while diagnostics / backup / crash logs are advanced maintenance surfaces
- enum-like Settings choices use compact dropdown rows instead of large button groups; percentage values use a slider plus numeric input
- phone-side todo and calendar editor bottom sheets no longer show generic explanatory subtitles
- Settings removes redundant helper copy from reminder-chain test, default snooze picker, and About usage-guide entry
- desktop web has lightweight tab/card/modal/button motion with prefers-reduced-motion fallback
- desktop web todo editor supports creating and editing no-DDL todos; disabling DDL also disables reminder and recurrence fields
- desktop web delete flows use an in-app dangerous-action confirmation modal instead of browser confirm
- desktop web HTML now replaces `__PAYKI_VERSION__` with the installed APK version at runtime and shows that version in the left brand block
- desktop web todo cards open a detail preview first; desktop web event cards now open the editor directly again for the primary card-click path
- desktop web todo and event editors accept the same mixed reminder syntax as the phone UI and convert concrete reminder times into normalized offset minutes
- desktop sync API accepts todo `reminderOffsetsMinutes`, so desktop-created / edited todos can persist multiple reminder points
- calendar timeline headers, month cells, and agenda/list date surfaces show lunar labels using Android ICU `ChineseCalendar`
- lunar date picking / lunar wheel is not implemented yet; current lunar support includes display labels plus minimal yearly same-lunar-date recurrence
- if a live phone desktop page still loads `/app.js` without a `?v=` marker, that phone is serving an older installed APK and must be updated before further desktop click-edit debugging
- desktop sync foreground service stops itself when relaunched while Settings says desktop sync is disabled, preventing stale access addresses / service state from persisting after sync is turned off
- calendar current-time ticks are scoped to the time axis and current-time line instead of recomposing the whole calendar panel every 30 seconds
- calendar header now keeps the month title and action buttons in one compact row with the title owning the remaining left-side width

- desktop web existing event cards now open the editor directly again; a Node DOM simulation verifies editor opening, title fill, edit ID state, and yearly-lunar recurrence selection
- recurrence type `YEARLY_LUNAR_DATE` is available for todo/event recurrence generation, preview, phone editors, and desktop web selects
- phone-side todo/event editor date rows append a lunar parenthesized label after the Gregorian date, giving immediate lunar feedback after picking a date
- desktop web todo/event editors show card-style date/time previews below segmented date inputs, closer to the phone-side time-card presentation

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

Current docs have been synchronized for `1.6.70`:

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
