# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.8.9` / `versionCode 192`.
- Latest debug APK output after final build: `app/build/outputs/apk/debug/PaykiTodo-1.8.9-debug.apk`.
- Latest verification in this round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
  - `git diff --check`
- This round refines the Android `今日看板` launcher widget after visual review so it follows the in-app daily board background/topbar/card structure more closely.
- Do not push to GitHub unless the user explicitly asks.

## Latest Changes In 1.8.9

1. Upgraded app version metadata to `1.8.9` / `versionCode 192`.
2. Widget root now layers the daily-board background art with light/dark scrims, making the widget resemble the in-app daily board rather than only a gradient list panel.
3. Widget header now uses a circular menu-button + `每日看板` title + current-date subtitle layout, closer to the app's daily-board top bar.
4. Announcement rows now use the orange rounded banner style instead of a generic card.
5. Greeting, empty, todo, and schedule card surfaces use stronger light/dark opacity and wider color strips for better launcher readability.
6. Widget keeps the daily-board structure: greeting card, section titles, todo cards with group-color strips, and one aggregated schedule card for today/tomorrow.
7. RemoteViews child bitmap clipping still needs real launcher verification; if square-corner background bleed appears, the next step is a pre-composited/cropped bitmap background.
8. Todo, event, and announcement row deep links remain in place.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetProvider.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/res/layout/widget_todo.xml`
- `app/src/main/res/layout/widget_todo_greeting_card.xml`
- `app/src/main/res/layout/widget_todo_schedule_card.xml`
- `app/src/main/res/layout/widget_todo_task_card.xml`
- `app/src/main/res/layout/widget_todo_empty_card.xml`
- `app/src/main/res/layout/widget_todo_announcement_card.xml`
- `app/src/main/res/layout/widget_todo_section.xml`
- `app/src/main/res/drawable*/widget_board_background.xml`
- `app/src/main/res/drawable*/widget_board_scrim.xml`
- `app/src/main/res/drawable*/widget_dashboard_bg_image.xml`
- `app/src/main/res/drawable*/widget_greeting_background.xml`
- `app/src/main/res/drawable*/widget_schedule_inner_background.xml`
- `app/src/main/res/drawable*/widget_todo_item_background.xml`
- `app/src/main/res/drawable*/widget_header_dot_background.xml`
- `app/src/main/res/drawable*/widget_topbar_button_background.xml`
- `app/src/main/res/drawable*/widget_announcement_background.xml`
- `app/src/main/res/values*/colors.xml`
- `CHANGELOG.md`
- `README.md`
- `TODO.md`
- `app/src/main/assets/wiki/index.html`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

## Verification Focus

Completed before closing the round:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat testDebugUnitTest`
3. `./gradlew.bat assembleDebug`
4. `git diff --check`

Then verify on a real Android launcher after installing `PaykiTodo-1.8.9-debug.apk`:

1. widget resembles the in-app daily board background/topbar/card hierarchy rather than a generic list;
2. circular menu-button header, greeting card, orange announcement banner, todo group color strips, and one aggregated schedule card render correctly;
3. resizing the widget reveals more content cleanly;
4. todo/event/announcement taps still open the correct in-app target;
5. dark-mode widget colors remain readable.

## Active Goal Version Note

Two untracked goal docs currently exist:

- `docs/goals/2026-05-17-paykitodo-focus-session-goal.md`
- `docs/goals/2026-05-17-paykitodo-ai-daily-report-goal.md`

They were written before the `1.8.8 / 191` and `1.8.9 / 192` widget fixes and still mention the old `1.9.0` versionCode baseline. If that goal resumes, continue from the current code version and do not reuse old versionCodes.

## Deferred Larger Work

- The widget is still implemented with Android RemoteViews, not Compose, so it cannot perfectly reuse the in-app daily board component or guarantee child bitmap clipping on every launcher.
- Launcher-specific widget padding, scaling, nested row click behavior, and list scroll behavior still require real-device visual checks.
- Focus-session / AI-report goal work remains separate and has not been implemented in this widget-focused round.

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
