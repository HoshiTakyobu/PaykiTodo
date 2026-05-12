# Current Task

## Active Development Focus

The current round has produced a `1.6.47` baseline. The implemented focus was reminder audio strategy and Settings restructuring.

Completed in this round:

1. Added `ReminderAudioChannel` settings for alarm, accessibility, notification, and media playback channels.
2. Added PaykiTodo internal reminder volume percentage for self-played alert audio.
3. Added an advanced temporary system-channel volume boost option, default off, which attempts to restore the previous stream volume after playback / reminder stop.
4. Added work / class quiet mode: suppress reminder sound by default, strengthen vibration, and route calendar reminders through full-screen / accessibility fallback.
5. Reorganized Settings into `常用设置` and `高级设置`, moving diagnostics, backup, desktop sync, and crash logs into advanced settings.
6. Updated the in-app Wiki to explain the new audio strategy and the limitation that Android does not allow a real system-level PaykiTodo-only volume stream.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.47-debug.apk`
2. open Settings -> 提醒声音策略
3. test channel choices: 闹钟 / 无障碍辅助 / 通知 / 媒体
4. test PaykiTodo internal volume at low and high values
5. test work / class quiet mode: sound should be suppressed, vibration should be stronger, and calendar reminders should use full-screen fallback
6. test temporary system-channel volume boost carefully, because it changes the selected global stream volume and then restores it
7. open Wiki and verify the reminder-audio explanation is present

## Repository-Verified Notes

The current code baseline includes these specific `1.6.47` changes:

1. `app/build.gradle.kts` is bumped to `1.6.47 / 119`.
2. `AppSettingsStore.kt` stores audio channel, internal volume, boost setting / target, and work quiet mode.
3. `BackupManager.kt` exports and imports the new audio strategy settings.
4. `ReminderAlertController.kt` applies the chosen audio channel, internal player volume, optional system volume boost, and stronger quiet-mode vibration.
5. `ReminderForegroundService.kt` uses work quiet mode to force full-screen / accessibility fallback even for calendar reminders.
6. `SettingsPanel.kt` contains the new reminder audio strategy panel and common / advanced settings split.

## What Not To Do Immediately

- do not describe the internal volume as a true Android system-level PaykiTodo volume channel
- do not enable temporary system volume boost by default
- do not remove the existing alarm-channel default without device testing
- do not regress the 1.6.45 desktop Web asset split back into Kotlin raw strings
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`

## Current External Dependency

No external file is needed for the current `1.6.47` verification task.
