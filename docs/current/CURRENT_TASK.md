# Current Task

## Active Development Focus

The current round has produced a `1.6.48` baseline. The focus was correcting the Settings reminder-audio UI and aligning work-mode behavior with the strong-reminder product goal.

Completed in this round:

1. Replaced large button groups in Settings with compact dropdown rows for reminder playback channel, calendar week-start mode, and default calendar reminder delivery mode.
2. Replaced `±10%` percent controls with a 0-100 slider plus a numeric input box for PaykiTodo internal volume and temporary system-channel boost target.
3. Renamed the previous work / class quiet mode surface to `工作模式`.
4. Corrected work-mode behavior: it now keeps ringing enabled when an item has ringing enabled, strengthens vibration, and keeps calendar reminders on the full-screen / accessibility fallback chain.
5. Moved desktop sync back into common Settings. Advanced Settings now focuses on diagnostics, data/backup, and crash logs.
6. Added `docs/current/UI_DESIGN_RULES.md` and linked it from `AGENTS.md` so future sessions avoid button-group option UIs.
7. Updated the in-app Wiki and current docs for the new Settings behavior.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.48-debug.apk`
2. open Settings -> 提醒声音策略
3. verify playback channel is a dropdown, not a wrapped button group
4. verify PaykiTodo internal volume uses a slider and editable percentage box
5. verify 工作模式 keeps ring enabled for ringing reminders while strengthening vibration and forcing the full-screen / accessibility fallback for calendar reminders
6. verify the temporary system-channel boost target also uses slider + percentage input
7. open Settings -> 日历与提醒 and verify week start / default calendar reminder mode are compact dropdowns
8. verify Settings common / advanced split: desktop sync is common; diagnostics / backup / crash logs are advanced

## Repository-Verified Notes

The current code baseline includes these specific `1.6.48` changes:

1. `app/build.gradle.kts` is bumped to `1.6.48 / 120`.
2. `SettingsPanel.kt` contains compact dropdown rows and percent slider/input controls for the affected settings.
3. `ReminderAlertController.kt` no longer suppresses sound just because work mode is enabled.
4. `DashboardScreen.kt` no longer turns new reminder ring defaults off just because work mode is enabled.
5. `ReminderForegroundService.kt` still uses work mode to force calendar reminders into the full-screen / accessibility fallback chain.
6. `docs/current/UI_DESIGN_RULES.md` documents the project rule against large button-group option UIs.

## What Not To Do Immediately

- do not reintroduce large button groups for enum-like Settings choices
- do not describe the internal volume as a true Android system-level PaykiTodo volume channel
- do not reimplement 工作模式 as silent mode unless explicitly requested
- do not enable temporary system volume boost by default
- do not move desktop Web resources back into Kotlin raw strings
- do not scan the whole workspace outside this repo
- do not change JDK setup; use Android Studio bundled `jbr`

## Current External Dependency

No external file is needed for the current `1.6.48` verification task.
