# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.10.0` / `versionCode 218`.
- Main user request: add countdown-day mode for important DDL / schedule targets, expose it on the phone board, desktop board, the existing 今日看板 widget, and an independent 倒数日 widget; keep the existing widget visually aligned with the in-app daily board and do not show the focus card there.
- Latest debug APK after this round: `app/build/outputs/apk/debug/PaykiTodo-1.10.0-debug.apk`.
- Latest signed release APK remains the previous `1.9.23` release artifact unless the user asks for a new release build.
- Do not push to GitHub unless the user explicitly asks.
- Keep `keystore.properties`, `release/`, APK/AAB outputs, API keys, tokens, and private Base URLs out of Git.

## Latest 1.10.0 Countdown / Widget Pass

1. Database version is now `17`.
2. `MIGRATION_16_17` adds `countdownEnabled` to `todo_items` and `recurring_task_templates`.
3. `MIGRATION_16_17` creates `index_todo_items_countdown`.
4. Todo countdown targets use DDL dates; no-DDL todos are not valid countdown targets.
5. Event countdown targets use event start dates.
6. Expired countdown targets are filtered out from phone board, desktop board, and widgets.
7. Todo editor exposes a 倒数日 block when DDL is enabled; turning off DDL clears countdown state.
8. Calendar event editor exposes a 倒数日 block and persists it through normal event saves and recurring event draft previews.
9. Daily board renders a `CountdownBoardCard` above ordinary todo / schedule content.
10. Desktop `/api/snapshot` includes `todayBoard.countdownItems`.
11. Desktop Web daily board renders countdown targets and todo/event edit forms save `countdownEnabled`.
12. Android 今日看板 widget inserts a countdown section after greeting and before today todos.
13. Android 今日看板 widget still omits the focus card / 专注一下 content.
14. New `CountdownWidgetProvider` registers an independent PaykiTodo 倒数日 widget.
15. The independent 倒数日 widget shows the nearest 3 countdown targets and routes todo targets to My Tasks, event targets to Calendar.
16. Backup / restore JSON and desktop sync JSON preserve `countdownEnabled`.
17. `DailyBoardSnapshotBuilderTest` covers active future/today countdown targets, expired target exclusion, and no-DDL exclusion.

## Verification Status

Completed locally in this continuation:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat :app:testDebugUnitTest` passed.
4. `./gradlew.bat :app:assembleDebug` passed.
5. Debug `output-metadata.json` reports `versionCode=218`, `versionName=1.10.0`, and `outputFile=PaykiTodo-1.10.0-debug.apk`.

Also checked:

1. `git diff --check` passed.
2. `git status --short` was reviewed.
3. `git check-ignore -v` confirmed `keystore.properties`, `release/PaykiTodo-release.jks`, and `app/build/outputs/apk/debug/PaykiTodo-1.10.0-debug.apk` are ignored.

Latest emulator smoke remains historical from `1.9.21`; this `1.10.0` widget pass has local build/unit verification but no emulator or physical launcher verification yet.

## Remaining Device / Browser Verification

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.0-debug.apk` on the physical phone.
2. Phone-test countdown:
   - enabling 倒数日 on a DDL todo shows it on 每日看板.
   - disabling DDL clears countdown.
   - enabling 倒数日 on an event shows it on 每日看板.
   - expired targets disappear.
3. Device-test widgets:
   - 今日看板 widget shows announcements, greeting, countdown, today todos, and today/tomorrow schedule content.
   - 今日看板 widget does not show focus statistics or 专注一下.
   - 倒数日 widget shows the nearest 3 targets.
   - light/dark mode text and row layout remain readable after resizing.
4. Browser-test desktop Web:
   - first lightweight snapshot renders countdown targets when present.
   - desktop todo/event editors preserve countdown state.
   - desktop board countdown rows open the right item type.

## Performance Notes

- Countdown data is stored as a boolean marker on existing todos/events rather than a separate table.
- Board / widget snapshots include countdown-enabled items through indexed query paths, then filter expired targets before rendering.
- The independent 倒数日 widget takes only the nearest 3 targets.
- Existing desktop split loading remains in place: lightweight board snapshot first, paged/searchable todos on demand, visible-range events on demand.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/TodoItem.kt`
- `app/src/main/java/com/example/todoalarm/data/RecurringTaskTemplate.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRecurrence.kt`
- `app/src/main/java/com/example/todoalarm/data/CalendarEventDraft.kt`
- `app/src/main/java/com/example/todoalarm/data/DailyBoardSnapshot.kt`
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoEditorDialog.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarEventEditorDialog.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/java/com/example/todoalarm/widget/CountdownWidgetProvider.kt`
- `app/src/main/res/layout/widget_countdown.xml`
- `app/src/main/res/xml/widget_countdown_info.xml`
- `app/src/test/java/com/example/todoalarm/data/DailyBoardSnapshotBuilderTest.kt`
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
