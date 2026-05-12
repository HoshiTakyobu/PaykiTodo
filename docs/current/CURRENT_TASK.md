# Current Task

## Active Development Focus

The current round has produced a `1.6.49` baseline. The focus was restoring work mode to the intended quiet strong-reminder behavior while keeping the compact Settings UI introduced in `1.6.48`.

Completed in this round:

1. Restored work mode behavior: when enabled, PaykiTodo suppresses outward reminder sound by default, strengthens vibration, and keeps calendar reminders on the full-screen / accessibility fallback chain.
2. Restored new-reminder defaults so work mode disables default ringing while keeping stronger vibration.
3. Preserved the compact Settings UI from `1.6.48`: dropdown rows for enum settings and slider + numeric input for percentage values.
4. Updated the in-app Wiki and current docs so work mode is documented as a quiet strong-reminder mode.
5. Updated `docs/current/UI_DESIGN_RULES.md` to record both the no-large-button-group UI rule and the expected detailed Chinese commit-message style.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.49-debug.apk`
2. open Settings -> 提醒声音策略
3. verify playback channel is still a dropdown, not a wrapped button group
4. verify PaykiTodo internal volume and temporary boost target still use slider + editable percentage box
5. verify 工作模式 suppresses outward sound for ringing reminders, uses stronger vibration, and keeps calendar full-screen / accessibility fallback
6. open Settings -> 日历与提醒 and verify week start / default calendar reminder mode remain compact dropdowns

## Repository-Verified Notes

The current code baseline includes these specific `1.6.49` changes:

1. `app/build.gradle.kts` is bumped to `1.6.49 / 121`.
2. `ReminderAlertController.kt` again skips sound playback when work mode is enabled.
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

No external file is needed for the current `1.6.49` verification task.
