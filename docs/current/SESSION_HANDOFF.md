# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.8.7` / `versionCode 190`.
- Latest debug APK output after final build: `app/build/outputs/apk/debug/PaykiTodo-1.8.7-debug.apk`.
- Latest verification in this round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
  - `git diff --check`
- This round refines the Android `今日看板` launcher widget after visual review so it follows the in-app daily board structure more closely.
- Do not push to GitHub unless the user explicitly asks.

## Latest Changes In 1.8.7

1. Upgraded app version metadata to `1.8.7` / `versionCode 190`.
2. Widget root background changed to a light/dark gradient board surface.
3. Widget top content now starts with a greeting card instead of a plain `今日看板` title row.
4. Todo cards now include a left vertical color strip based on the todo's task group color.
5. Schedule content is now one aggregated board card with:
   - a left date block for today
   - today schedule rows or today's empty/all-finished message
   - a `明天` subsection
   - tomorrow schedule rows or `明天暂无日程` guidance
6. Schedule rows inside the aggregated card still deep-link to the corresponding calendar event.
7. Todo and announcement row deep links remain in place.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/res/layout/widget_todo.xml`
- `app/src/main/res/layout/widget_todo_greeting_card.xml`
- `app/src/main/res/layout/widget_todo_schedule_card.xml`
- `app/src/main/res/layout/widget_todo_task_card.xml`
- `app/src/main/res/drawable*/widget_board_background.xml`
- `app/src/main/res/drawable*/widget_greeting_background.xml`
- `app/src/main/res/drawable*/widget_schedule_inner_background.xml`
- `app/src/main/res/drawable/widget_vertical_pill.xml`
- `app/src/main/res/values*/colors.xml`
- `CHANGELOG.md`
- `README.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

## Current Verification Focus

1. Verify on a real Android launcher after installing `PaykiTodo-1.8.7-debug.apk`:
   - widget resembles the in-app daily board rather than a generic list;
   - greeting card, todo group color strips, and one aggregated schedule card render correctly;
   - resizing the widget reveals more content cleanly;
   - todo/event/announcement taps still open the correct in-app target;
   - dark-mode widget colors remain readable.
2. Re-check Settings -> `AI 调用配置` model discovery from `1.8.5` if installing this APK for broader QA.

## Deferred Larger Work

- The widget is still implemented with Android RemoteViews, not Compose, so it cannot perfectly reuse the in-app daily board component.
- Launcher-specific widget padding, scaling, nested row click behavior, and list scroll behavior still require real-device visual checks.

## Required Reading For A New Session

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/UI_DESIGN_RULES.md`
7. `docs/current/PLANNING_DESK_DESIGN.md`
8. `docs/current/DESKTOP_WEB_ARCHITECTURE.md`
9. `docs/current/PAYKITODO_SESSION_LEDGER.md`
10. `docs/current/AI_RECOGNITION_VERIFICATION.md`

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
