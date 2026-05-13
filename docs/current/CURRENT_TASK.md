# Current Task

## Active Development Focus

The current round has produced a local `1.6.83` build. It continues the user-requested settings tone, daily-board spacing, calendar-header, and lunar-date repair pass while keeping the repository buildable.

Completed in this round:

1. Settings -> 提示音 now opens a compact tone panel instead of jumping directly to the system picker.
2. The tone panel exposes both `内置提醒音` and `系统通知提示音`, and shows a checkmark for the current choice.
3. Todo editing now has a wheel-style `农历 DDL` entry. Picking a lunar date converts it to the corresponding Gregorian date while preserving the existing DDL time of day.
4. The lunar date picker was extracted into shared `LunarDatePickerDialog.kt`, so todo and event editors reuse the same validation and conversion behavior.
5. Calendar header puts the year/month title on its own row and moves actions to a separate row, avoiding `2026年5月` truncation caused by buttons sharing the same line.
6. Daily-board schedule rows add more space between the left color strip and event text, increase vertical padding, and reduce the time row font size to avoid the cramped red-box layout shown by the user.
7. Version was bumped to `1.6.83` / `versionCode 155`.
8. `./gradlew.bat assembleDebug` succeeded with Android Studio bundled `jbr` and produced `app/build/outputs/apk/debug/PaykiTodo-1.6.83-debug.apk`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.83-debug.apk`
2. verify Settings -> 提示音 can switch back to `内置提醒音` and can still open the system notification tone picker
3. verify Todo edit/create -> 截止时间 -> `农历 DDL` opens a wheel-style lunar picker, converts a lunar date, and preserves the time
4. verify calendar header shows `2026年5月` on the separate title row without being squeezed by action buttons
5. verify daily-board schedule row text no longer sits too close to the blue strip and has more comfortable vertical spacing

## Deferred From The User's Larger Request

- Full desktop web UI parity with the phone UI is still not complete.
- A full calendar performance pass remains pending; current work only includes incremental layout/composition hardening.
- Android Emulator visual QA was attempted previously with SDK emulator `Pixel_8`; `emulator.exe` launched but no device appeared in `adb devices` within 90 seconds, so no install/screenshot UI loop can be claimed yet.
- Broad UI-copy cleanup is partially improved, but future screens should still be reviewed for unnecessary helper text.

## Current External Dependency

No external file is needed for the current `1.6.83` verification task.
