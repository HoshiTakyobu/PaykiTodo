# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.10` / `versionCode 204`.
- Main user request: widget click targets should open the correct app sections, the widget should not show focus content, todos need full-screen / notification reminder-mode selection, and the reminder UI needs explicit DDL postponement separate from snoozing.
- Core behavior change: widget todo rows open `我的任务`, widget schedule/event rows open `日历`, widget focus rows are not generated, todo reminder mode is persisted, custom snooze no longer changes DDL, and `DDL 推迟` is a separate validated action.
- Latest debug APK target after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.10-debug.apk`.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.10 Widget / Todo Reminder Pass

1. `MainActivity` accepts new `EXTRA_OPEN_TASKS` and `EXTRA_OPEN_CALENDAR` section-level launch extras.
2. `DashboardScreen` routes those extras to `DashboardSection.ACTIVE` and `DashboardSection.CALENDAR`.
3. `TodoWidgetService` no longer queries focus sessions for widget rows and no longer inserts the focus card.
4. Widget todo rows fill in `EXTRA_OPEN_TASKS`; widget event rows and the aggregated schedule card fill in `EXTRA_OPEN_CALENDAR`.
5. `TodoDraft` includes `ReminderDeliveryMode`; `TodoRepository` stores it for todos and recurring todo templates.
6. Phone `TodoEditorDialog` exposes reminder delivery mode through a selection row and single-choice dialog, avoiding a button-group UI.
7. Desktop Web todo editor includes a `todo-reminder-mode` select and sends `reminderDeliveryMode` through create/update payloads.
8. Desktop sync todo create/update sanitization now preserves `reminderDeliveryMode`.
9. `snoozeTodo` no longer mutates todo DDL; it stores a one-off reminder target instead.
10. `postponeTodoDueAt` explicitly changes a todo DDL only when the new target is later than the existing DDL.
11. `parseDdlPostponeInput` accepts positive minute increments such as `30分钟` / `往后推45分钟`, current-DDL-date clocks such as `16:30`, and full date-time values such as `2026-05-22 16:30`.
12. Full-screen `ReminderActivity` now shows `延后 5 分钟`, `延后 10 分钟`, `自定义延后提醒`, and `DDL 推迟`.
13. `ReminderAccessibilityOverlay` mirrors the 10-minute quick snooze and DDL-postpone path.
14. Wiki / README / TODO / CHANGELOG / current docs were synchronized for `1.9.10`.

## Verification Status

Completed:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.9.10-debug.apk`.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=204`, `versionName=1.9.10`, and `outputFile=PaykiTodo-1.9.10-debug.apk`.

Remaining after local completion:

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.10-debug.apk` on the user's physical phone.
2. Verify the launcher widget on a real launcher: no focus card, todo click -> `我的任务`, schedule click -> `日历`, announcement click -> source Planning Desk note.
3. Verify todo editor reminder mode persists and affects dispatch path.
4. Trigger a real todo reminder and verify custom snooze does not change DDL while explicit `DDL 推迟` does.
5. Verify desktop Web todo editor exposes and persists `提醒方式` after refreshing the browser against the new APK.
6. Do not push unless the user explicitly asks.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRecurrence.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/data/ReminderTextParser.kt`
- `app/src/main/java/com/example/todoalarm/ui/ReminderInputParser.kt`
- `app/src/main/java/com/example/todoalarm/ui/ReminderActivity.kt`
- `app/src/main/java/com/example/todoalarm/accessibility/ReminderAccessibilityOverlay.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoEditorDialog.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/wiki/index.html`
- `app/src/test/java/com/example/todoalarm/ui/ReminderInputParserTest.kt`
- `README.md`
- `TODO.md`
- `CHANGELOG.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`

## Known Worktree Notes

- Branch is `main`; local branch is far ahead of origin. Do not push without user authorization.
- Existing untracked temp UI dumps such as `.tmp-*.xml` were present before this round and should not be committed unless intentionally needed.
- An untracked user note named `当前使用中存在的问题.md` may exist in the repo root; do not commit or modify it unless the user asks.
- Goal prompt files under `docs/goals/` should remain separate archive commits after secret scanning.

## Android Emulator Verification Rule

If an Android Emulator is started or reused, record:

- device id / AVD name
- installed APK path
- checked screens or flows
- what was actually verified

This avoids confusion when an emulator window remains on the user's desktop.

Latest recorded emulator use remains the `1.9.8` smoke check:

- Device id: `emulator-5554`
- Installed APK: `app/build/outputs/apk/debug/PaykiTodo-1.9.8-debug.apk`
- Checked flows: app launch, drawer navigation to `AI 报告`, report detail opening, Settings -> AI 调用配置, and `了解 AI 日报`
- Verified result: `AI 报告` archive is reachable and populated from legacy migration data; the `了解 AI 日报` help surface is centered/readable on the emulator
- Boundary: this emulator pass does not replace real-phone verification for OEM notification, vibration, lock-screen, widget, alarm, reboot, battery-management behavior, or live desktop-browser verification of `1.9.10`
