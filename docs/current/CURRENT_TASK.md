# Current Task

## Active Development Focus

The current round has produced a `1.6.50` baseline. The focus was closing a work-mode reliability gap: quiet mode now forces strong vibration even if the individual item had vibration disabled.

Completed in this round:

1. Restored work mode behavior: when enabled, PaykiTodo suppresses outward reminder sound by default and keeps calendar reminders on the full-screen / accessibility fallback chain.
2. Fixed work-mode vibration reliability: work mode now forces the stronger vibration pattern even if an individual todo / event had vibration disabled.
3. Restored new-reminder defaults so work mode disables default ringing while keeping stronger vibration.
4. Preserved the compact Settings UI from `1.6.48`: dropdown rows for enum settings and slider + numeric input for percentage values.
5. Updated the in-app Wiki and current docs so work mode is documented as quiet + forced strong vibration.
6. Updated `docs/current/UI_DESIGN_RULES.md` to record both the no-large-button-group UI rule and the expected detailed Chinese commit-message style.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.50-debug.apk`
2. open Settings -> 提醒声音策略
3. verify playback channel is still a dropdown, not a wrapped button group
4. verify PaykiTodo internal volume and temporary boost target still use slider + editable percentage box
5. verify 工作模式 suppresses outward sound for ringing reminders, forces stronger vibration even if the item-level vibration switch is off, and keeps calendar full-screen / accessibility fallback
6. open Settings -> 日历与提醒 and verify week start / default calendar reminder mode remain compact dropdowns

## Repository-Verified Notes

The current code baseline includes these specific `1.6.50` changes:

1. `app/build.gradle.kts` is bumped to `1.6.50 / 122`.
2. `ReminderAlertController.kt` skips sound playback when work mode is enabled and forces vibration in work mode.
3. `DashboardScreen.kt` again makes new reminder ringing default to off when work mode is enabled.
4. `SettingsPanel.kt` keeps the compact Settings controls from `1.6.48`, but its work-mode copy now describes silent strong-vibration behavior.
5. `docs/current/UI_DESIGN_RULES.md` documents work mode as quiet strong-reminder mode and records detailed commit-message expectations.

## What Not To Do Immediately

- do not reintroduce large button groups for enum-like Settings choices
- do not describe the internal volume as a true Android system-level PaykiTodo volume channel
- do not change 工作模式 into a normal ringing mode unless explicitly requested
- do not enable temporary system volume boost by default
- do not move desktop Web resources back into Kotlin raw strings
- do not scan the whole workspace outside this repo
- do not change JDK setup; use Android Studio bundled `jbr`

## Current External Dependency

No external file is needed for the current `1.6.50` verification task.
