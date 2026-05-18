# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now at `1.10.1` / `versionCode 219`.
- Main user request for this continuation:
  1. Desktop Planning Desk AI preview/import must expose event location and phone-side key fields instead of putting location into notes.
  2. The existing Android `今日看板` widget must not include countdown-day rows.
  3. Countdown-day targets should be shown in the independent `倒数日` widget with App daily-board-like wording and layout.
  4. Event countdown rows should not show a completion circle and should show full event time.
- Latest debug APK after build:
  - `app/build/outputs/apk/debug/PaykiTodo-1.10.1-debug.apk`
- Latest signed release APK remains the previous `1.9.23` release artifact unless the user asks for a new release build.
- Do not push to GitHub unless the user explicitly asks.
- Keep `keystore.properties`, `release/`, APK/AAB outputs, API keys, tokens, and private Base URLs out of Git.

## Latest 1.10.1 Fix Pass

1. `PlanningParsedCandidate` and `PlanningImportCandidate` now include `location`, `allDay`, `countdownEnabled`, and `recurrence`.
2. `PlanningAiRecognizer` prompt/schema now asks for `location` and `recurrence`, keeps `@地点` as literal user text, and keeps conservative group assignment.
3. Local Planning Desk parser extracts `@地点` / `#location` / `#地点` into event location without adding another `@`.
4. Phone Planning Desk preview cards can edit event location, all-day, countdown, and recurrence before import.
5. Desktop Web Planning Desk preview can edit event location, all-day, countdown, recurrence type, recurrence end date, and weekly days before import.
6. Phone and desktop Planning Desk import paths persist these fields into `TodoDraft` / `CalendarEventDraft`.
7. Android `今日看板` widget no longer builds or routes countdown rows.
8. Android independent `倒数日` widget:
   - keeps a checkbox-like circle for todo targets;
   - hides the circle for event targets;
   - uses `10d` style day text plus remaining `xh ym zs`;
   - shows full event time metadata;
   - removes redundant item-level `倒数日` text.
9. App daily-board and desktop daily-board countdown rows now use `Nd` wording and fuller event-time metadata.
10. Version metadata is `1.10.1 / 219`.

## Verification Status

Completed locally:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat :app:testDebugUnitTest` passed.
4. `./gradlew.bat :app:assembleDebug` passed.
5. `git diff --check` passed.
6. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/debug/PaykiTodo-1.10.1-debug.apk` confirmed signing material and APK output are ignored.
7. Debug `output-metadata.json` reports `versionCode=219`, `versionName=1.10.1`, and `outputFile=PaykiTodo-1.10.1-debug.apk`.

Latest emulator smoke remains historical from `1.9.21`; this `1.10.1` continuation has not started or reused an emulator.

## Remaining Device / Browser Verification

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.1-debug.apk` on the physical phone.
2. Phone-test countdown:
   - App daily-board countdown row shows `Nd` and full event time.
   - Todo countdown opens todo editing; event countdown opens event editing.
3. Device-test widgets:
   - `今日看板` widget has no countdown section.
   - Independent `倒数日` widget shows nearest 3 targets.
   - Todo rows show a circle; event rows do not.
   - Light/dark mode text remains readable after resizing.
4. Browser-test desktop Web:
   - AI/local Planning Desk preview exposes location, all-day, countdown, and recurrence.
   - Import persists location into the event location field, not notes.
   - `16:05-18:00 入党表格填写` remains title `入党表格填写` with empty group unless a group marker is present.

## Performance Notes

- Countdown data still uses the existing `countdownEnabled` marker and indexed board/widget lookup paths.
- Removing countdown rows from `TodoWidgetService` reduces the existing `今日看板` widget row count and keeps that widget focused on board content.
- The independent `倒数日` widget still renders only the nearest 3 targets.
- Widget seconds are snapshot text from the last widget refresh; Android launcher widgets are not guaranteed to tick once per second.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/PlanningMarkdownParser.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningImportCandidate.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiRecognizer.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/java/com/example/todoalarm/widget/CountdownWidgetProvider.kt`
- `app/src/main/res/layout/widget_countdown.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/wiki/index.html`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/*`

## Known Worktree Notes

- Branch is `main`; do not push without user authorization.
- Goal prompt files under `docs/goals/` should remain separate archive commits after secret scanning.
- `keystore.properties`, `release/`, APK/AAB outputs, and private signing material are ignored and must stay out of commits.

## Android Emulator Verification Rule

If an Android Emulator is started or reused, record:

- device id / AVD name
- installed APK path
- checked screens or flows
- what was actually verified

Do not overclaim emulator results. A real phone is still required for OEM notification, vibration, lock-screen, widget, alarm, reboot, battery-management behavior, Android launcher widget rendering, Planning Desk announcement migration on the user's real database, and live desktop-browser verification.
