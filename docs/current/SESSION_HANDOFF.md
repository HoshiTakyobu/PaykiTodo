# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is being advanced to `1.10.3` / `versionCode 221`.
- Main user request for this continuation:
  1. Planning Desk and AI recognition must extract `@图书馆3楼` / `"@主楼B1-412"` as event locations, not title text.
  2. Event import must not auto-create linked todos unless the user explicitly enables that preview option.
  3. Linked todos must not receive the old fixed note `由规划台日程自动生成，DDL 为日程结束时间。`.
  4. Desktop-web event color editing should match the phone editor's preset color choices.
  5. Previously published `1.10.2` and earlier commits were pushed and tagged.
- Latest published signed release APK:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk`
  - GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.10.2`
- Expected next debug APK after verification:
  - `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk`
- Do not push to GitHub unless the user explicitly asks.
- Keep `keystore.properties`, `release/`, APK/AAB outputs, API keys, tokens, and private Base URLs out of Git.

## Latest 1.10.3 Fix Pass

1. Local Planning Markdown parsing recognizes inline `@地点`, quoted `"@地点"`, and `地点：...` event locations.
2. Natural event candidates now default `createLinkedTodo=false`.
3. AI recognition now defaults event `createLinkedTodo=false` and the prompt only permits linked todos when the user explicitly asks.
4. AI result cleanup moves `@地点` out of event titles when the model forgot the `location` field.
5. Phone-side and desktop-sync Planning Desk linked todo creation no longer adds a fixed generated note.
6. Desktop-web Planning Desk preview uses the explicit label `同步创建以日程结束时间为 DDL 的待办任务`.
7. Desktop-web event editor renders the same eight compact color swatches as the phone editor while preserving custom color input.
8. Tests were added for location extraction, default-off linked todos, AI cleanup, and group sanitization.

## Verification Status

Completed locally for `1.10.3`:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:testDebugUnitTest` passed.
3. `git diff --check` passed.
4. `./gradlew.bat :app:assembleRelease` passed.
5. `app/build/outputs/apk/release/output-metadata.json` confirms `1.10.3 / 221`.
6. `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk` passed with one v2 signer.
7. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk` confirmed local signing material and APK outputs stay ignored.
8. After QQ released the old debug APK handle, `./gradlew.bat :app:assembleDebug` passed.
9. `app/build/outputs/apk/debug/output-metadata.json` confirms `1.10.3 / 221`.

Already completed for the published `1.10.2` release:

1. `./gradlew.bat :app:assembleRelease` passed.
2. `app/build/outputs/apk/release/output-metadata.json` confirmed `1.10.2 / 220`.
3. `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk` passed with one v2 signer.
4. Git history scan over 181 commits found no committed signing config, keystore/JKS/env secret files, private-key blocks, common live token prefixes, or non-placeholder password/API-key literals. The remaining keyword matches were code identifiers, settings keys, templates, editor insertion tokens, or test example URLs.

## Remaining Device / Browser Verification

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk` on the user's phone; share `app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk` with external testers if needed.
2. Verify Planning Desk imports for `@地点`, quoted `@地点`, and `地点：...`.
3. Verify ordinary event import creates only an event.
4. Verify manually enabled linked todo uses event end time as DDL and has no fixed generated note.
5. Verify desktop-web event color swatches save correctly.

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
