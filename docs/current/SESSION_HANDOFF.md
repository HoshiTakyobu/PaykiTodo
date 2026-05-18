# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project has been advanced to `1.10.2` / `versionCode 220`.
- Main user request for this continuation:
  1. Countdown rows must stop showing total hours/seconds and use days/hours/minutes correctly.
  2. App and widgets must not count ended today events as visible `今日日程`.
  3. The independent `倒数日` widget should not show a redundant header/date/count block and should deep-link to exact items.
  4. Daily board should not carry free-focus UI; focus should become its own surface.
  5. Widget picker entries need clear labels, descriptions, suggested sizes, and static previews.
- Latest debug APK built:
  - `app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk`
- Latest signed release APK:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk`
- Do not push to GitHub unless the user explicitly asks.
- Keep `keystore.properties`, `release/`, APK/AAB outputs, API keys, tokens, and private Base URLs out of Git.

## Latest 1.10.2 Fix Pass

1. `DailyBoardSnapshotBuilder` now filters countdown targets by exact target millis and exposes shared remaining-time text without seconds.
2. Phone App countdown rows use `Nd` + `xh ym`, `Nh` + `xm`, or `Nm`; event rows show only the schedule time.
3. Desktop daily-board countdown rows use the same remaining-time format and no longer show the focus stat block.
4. Phone daily board / desktop daily board / Android widget counts for `今日日程` use unfinished visible events rather than all today-overlapping events.
5. Android `倒数日` widget is now a compact target list with no title/date/count header and with item-level deep links.
6. Android `倒数日` widget schedules minute ticks for more accurate minute-level text.
7. Android 今日看板 widget section titles use primary text color rather than the orange header color, fixing the light-mode brown-title mismatch.
8. Countdown widget event rows deep-link to the exact event editor, not only the Calendar section.
9. Daily board no longer displays the free-focus card, and board todo long-press no longer offers start-focus.
10. Drawer now includes a dedicated `专注` page with focus stats and free-focus entry.
11. Android includes a new `专注` widget for focus stats and free-focus launch.
12. Widget picker metadata and preview layouts were added or refreshed for 今日看板, 倒数日, and 专注.

## Verification Status

Completed locally:

1. `./gradlew.bat :app:compileDebugKotlin` passed.
2. `node --check app/src/main/assets/desktop-web/app.js` passed.
3. `./gradlew.bat :app:testDebugUnitTest` passed.
4. `./gradlew.bat :app:assembleDebug` passed.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` confirms `1.10.2 / 220`.
7. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk` confirmed local signing material and APK outputs stay ignored.
8. `./gradlew.bat :app:assembleRelease` passed.
9. `app/build/outputs/apk/release/output-metadata.json` confirms release `1.10.2 / 220`.
10. `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk` passed with one v2 signer.
11. Git history scan over 181 commits found no committed signing config, keystore/JKS/env secret files, private-key blocks, common live token prefixes, or non-placeholder password/API-key literals. The remaining keyword matches were code identifiers, settings keys, templates, editor insertion tokens, or test example URLs.

Latest emulator smoke remains historical from `1.9.21`; this `1.10.2` continuation has not started or reused an emulator.

## Remaining Device / Browser Verification

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk`.
2. Verify daily-board countdown text and ended-event count on the phone.
3. Verify widget picker labels/previews and launcher rendering for 今日看板, 倒数日, and 专注.
4. Verify countdown widget minute refresh and item deep links on the physical launcher.
5. Verify desktop daily-board countdown/count/focus removal from a real browser.

## Performance Notes

- Countdown data still uses the existing `countdownEnabled` marker and indexed board/widget lookup paths.
- The independent `倒数日` widget still renders only the nearest 3 targets.
- Minute-level countdown widget refresh is scheduled by `AlarmManager`; OEM launchers and battery policies may still delay widget refreshes under aggressive power management.
- Removing focus from the daily-board surface reduces ordinary board UI density; focus stats now live in the dedicated Focus page/widget.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/DailyBoardSnapshot.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/java/com/example/todoalarm/widget/CountdownWidgetProvider.kt`
- `app/src/main/java/com/example/todoalarm/widget/FocusWidgetProvider.kt`
- `app/src/main/res/layout/widget_countdown.xml`
- `app/src/main/res/layout/widget_countdown_preview.xml`
- `app/src/main/res/layout/widget_focus.xml`
- `app/src/main/res/layout/widget_focus_preview.xml`
- `app/src/main/res/xml/widget_todo_info.xml`
- `app/src/main/res/xml/widget_countdown_info.xml`
- `app/src/main/res/xml/widget_focus_info.xml`
- `app/src/main/assets/desktop-web/app.js`
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
