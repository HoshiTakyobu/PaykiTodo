# Current Task

## Active Development Focus

The current round has been consolidated into a committed and pushed `1.6.30` baseline. The next work should start from repository facts rather than old chat memory.

Primary active focus areas:

1. Device-test the daily-board todo preview fix: tapping a todo body should open preview and must not complete it
2. Device-test destructive action confirmations across phone UI and desktop web console
3. Device-test calendar reminder preview preservation after all reminders for an event have fired
4. Device-test the desktop-sync foreground notification click route into Settings -> Desktop Sync
5. Continue board / dashboard and calendar polish without regressing existing flows
6. Keep version metadata and docs aligned with the actual code state

## Immediate Practical Next Steps

When a new session takes over, it should usually do these in order:

1. run `git status --short --branch`
2. verify current version number and APK naming
3. if testing on device, install `app/build/outputs/apk/debug/PaykiTodo-1.6.30-debug.apk`
4. test tapping an active todo in the daily board and pressing back from preview
5. test delete confirmations for todo, calendar event, group, schedule template, and desktop web item deletion
6. test desktop-sync notification click routing while desktop sync is enabled
7. test event reminder acknowledgement and then reopen event preview to verify configured reminder offsets remain visible

## Repository-Verified Notes

The current code baseline includes these specific 1.6.30 changes:

1. `TodoCards.kt` active todo card body and checkbox are separate interaction targets
2. active todo preview uses `PaykiBottomSheet`
3. `CalendarPanel.kt` has delete confirmation for calendar events and schedule templates
4. `GroupManagementPanel.kt` has delete confirmation for groups
5. `DesktopSyncWebAssets.kt` confirms web delete actions before sending DELETE
6. `TodoRepository.acknowledgeCalendarEvent()` preserves configured reminder offsets
7. `DesktopSyncService` notification includes a `PendingIntent` into `MainActivity`
8. `MainActivity` / `DashboardScreen` / `SettingsPanel` route that intent to the desktop sync settings panel

## What Not To Do Immediately

- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`
- do not assume device behavior is fixed until the user actually tests it

## Current External Dependency

No external file is needed for the current 1.6.30 verification task.

If future work touches icon generation or adaptation, verify the intended in-repo resource chain before reprocessing any external image.
