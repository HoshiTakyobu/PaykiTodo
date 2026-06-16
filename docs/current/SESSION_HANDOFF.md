# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.14.12`
  - `versionCode = 332`
  - database version = `28`
- Latest debug APK target in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.14.12-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.13.11-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.13.11`

## Active Goal

Finish the 1.14.12 Daily Board visual polish and Planning Desk historical-import follow-up.

## What Changed In The Latest 1.14.12 Patch

1. **Phone Daily Board schedule/countdown polish**
   - Countdown target titles now wrap fully instead of being truncated to one line.
   - `今日日程` no longer shows a parenthesized count.
   - The tomorrow schedule block no longer has its own collapse button and no longer depends on stale saved tomorrow-collapse state.
   - The today-schedule empty state is now a compact `暂无日程` instead of the oversized completion sentence.

2. **Planning Desk historical import follow-up**
   - Past todo candidates can be imported when reminders are disabled, matching the past-event behavior from 1.14.11.
   - Enabled reminders whose trigger time is already past still block import.
   - Phone-side Planning Desk import and Desktop Web Planning Desk import both use the relaxed no-reminder historical-todo path.
   - Ordinary manual todo creation still rejects past DDLs.

3. **Desktop Web preview state**
   - Desktop Web Planning Desk preview keeps local edits while re-rendering.
   - Toggling `启用提醒` immediately refreshes the visible candidate message, so stale `提醒时间已经过去` copy disappears when reminders are disabled.
   - Reminder warning text is filtered out from preview messages when the candidate has no active reminder.

4. **Version**
   - Version metadata is `1.14.12 / versionCode 332`; database version remains `28`.

## Validation

- Passed:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat :app:testDebugUnitTest --tests com.example.todoalarm.data.PlanningImportCandidateTest`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `git diff --check`
  - `./gradlew.bat :app:assembleDebug`
  - APK metadata inspection for `versionName = 1.14.12`, `versionCode = 332`, `outputFile = PaykiTodo-1.14.12-debug.apk`

## Next Session Notes

1. If importing a past event/todo still fails, check whether the candidate still has enabled reminders or an invalid recurrence range.
2. If the user wants richer parity with the phone editor later, Planning Desk preview can add alarm mode / ring / vibration controls, but 1.14.11-1.14.12 only covers reminder enable/disable and delivery mode.
3. The Kotlin/KSP incremental cache corrupted once after parallel Gradle runs; it was fixed by stopping Gradle daemons and deleting generated `app/build` Kotlin/KSP cache directories. Avoid parallel Gradle invocations in this repo.
