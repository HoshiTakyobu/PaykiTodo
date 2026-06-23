# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.14.13`
  - `versionCode = 333`
  - database version = `28`
- Latest debug APK target in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.14.13-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`

## Active Goal

Finish and verify the 1.14.13 fix round for Planning Desk recognition/import, Daily Board countdown alignment, and full-screen reminder retry reliability.

## What Changed In The 1.14.13 Patch

1. **Planning Desk recognition**
   - Local natural schedule parsing recognizes leading date ranges such as `06-22~06-24 19:00-21:00 ...`.
   - A date-range schedule becomes one daily recurring event candidate with the first date as `startAt` and the last date as recurrence `endDate`.
   - Natural event parsing extracts `备注：...` / `备注:...` into notes and preserves the original Chinese punctuation.
   - When AI is enabled and at least one provider has Base URL / API Key / model, recognition tries AI first and uses local rules only as fallback.

2. **Planning Desk historical event import**
   - Event candidates can be imported even if their default reminder trigger has already passed.
   - The scheduler already filters trigger times to future timestamps, so already-past reminders are not backfilled; future recurring occurrences still receive reminders.
   - Past todo candidates still require reminders to be disabled before import.

3. **Daily Board countdown row**
   - Countdown rows now use intrinsic-height layout.
   - The left countdown block is centered vertically and the accent strip fills the row height when the title wraps to multiple lines.

4. **Full-screen reminder chain**
   - `ReminderForegroundService` no longer marks the reminder surface as shown immediately after `startActivity`.
   - `ReminderActivity.onResume` now marks the surface as actually shown, preventing silent Android background-launch blocks from suppressing scheduled retries.
   - Android 14+ full-screen-intent permission state is logged during full-screen attempts.
   - Missing full-screen permission triggers the accessibility fallback broadcast.
   - If the phone is unlocked and Android leaves the Activity handoff pending, the retry path now forces the accessibility full-screen overlay instead of repeatedly relying on a background Activity launch.
   - Reminder notification channels were moved to v10 to avoid old downgraded channel settings suppressing high-priority delivery.

5. **Ongoing event notification readability**
   - The in-progress schedule notification uses its own light content background and dark text colors, instead of reusing widget white text that was unreadable on light notification shades.

6. **Version**
   - Version metadata is `1.14.13 / versionCode 333`; database version remains `28`.

## Validation

- Passed:
  - `./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.example.todoalarm.data.PlanningMarkdownParserTest --tests com.example.todoalarm.data.PlanningImportCandidateTest --tests com.example.todoalarm.data.PlanningAiRecognizerTest --tests com.example.todoalarm.data.PlanningRecognitionServiceTest --rerun-tasks`
  - `./gradlew.bat --no-daemon :app:compileDebugKotlin`
  - `./gradlew.bat --no-daemon :app:testDebugUnitTest`
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `git diff --check`
  - `./gradlew.bat --no-daemon :app:assembleDebug`
  - APK metadata inspection for `versionName = 1.14.13`, `versionCode = 333`

## Next Session Notes

1. If the user says full-screen still does not pop on the physical phone, ask them to open Settings -> Advanced -> 提醒链路诊断 and check whether the latest log reaches `全屏界面已启动` but not `全屏界面可见`; that distinction now matters.
2. If unlocked reminders still only show a notification, confirm whether PaykiTodo's accessibility service is enabled; the unlocked-over-other-app fallback now depends on that service when Android blocks background Activity launches.
3. Android/OEM full-screen behavior still needs physical-device validation. Passing emulator/build tests cannot prove lock-screen or vendor background-launch behavior.
4. If a configured AI provider returns malformed JSON or bad recurrence fields, local parsing still remains the fallback; do not reintroduce the old “local covers all lines, skip AI” shortcut.
