# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.0.1` / `versionCode 194`.
- This round is a focused Android `今日看板` launcher-widget visual hotfix.
- Latest intended debug APK after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.0.1-debug.apk`.
- The separate AI 日报 / 周报 goal is still pending and should remain the planned `1.9.1` work after this hotfix is committed.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.0.1 Widget Changes

1. Widget version metadata moved to `1.9.0.1` / `versionCode 194` for installable hotfix testing.
2. Widget root padding, title header size, list spacing, and light/dark scrims were adjusted so the launcher surface feels less like a generic Android list.
3. A new `今日已专注` RemoteViews card was added to the widget, showing today's completed focus minutes, total focus sessions, and completed session count.
4. Greeting, section, todo, empty, announcement, and schedule-card XML layouts were retuned for stronger rounded-card hierarchy, lightweight card elevation, higher card-surface opacity, larger readable title text, and daily-board-like spacing.
5. Widget docs were updated in README, CHANGELOG, in-app Wiki, `PROJECT_STATUS`, `FEATURE_LEDGER`, `CURRENT_TASK`, and TODO.

## Files Most Relevant To This Hotfix

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/res/layout/widget_todo.xml`
- `app/src/main/res/layout/widget_todo_focus_card.xml`
- `app/src/main/res/layout/widget_todo_greeting_card.xml`
- `app/src/main/res/layout/widget_todo_task_card.xml`
- `app/src/main/res/layout/widget_todo_schedule_card.xml`
- `app/src/main/res/layout/widget_todo_section.xml`
- `app/src/main/res/layout/widget_todo_empty_card.xml`
- `app/src/main/res/layout/widget_todo_announcement_card.xml`
- `app/src/main/res/drawable/widget_todo_item_background.xml`
- `app/src/main/res/drawable-night/widget_todo_item_background.xml`
- `app/src/main/res/drawable/widget_schedule_inner_background.xml`
- `app/src/main/res/drawable-night/widget_schedule_inner_background.xml`
- `app/src/main/res/drawable/widget_board_scrim.xml`
- `app/src/main/res/drawable-night/widget_board_scrim.xml`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `app/src/main/assets/wiki/index.html`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

## Verification Status

Completed so far:

1. `./gradlew.bat :app:compileDebugKotlin`
2. `git diff --check`

Still recommended before commit:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat assembleDebug`
3. `git diff --check`

Then verify on a real Android launcher after installing `PaykiTodo-1.9.0.1-debug.apk`:

1. add / refresh the PaykiTodo `今日看板` widget;
2. confirm the widget includes the new `今日已专注` card;
3. confirm the overall surface resembles the in-app daily board rather than a system list;
4. resize the widget and check text readability in light / dark mode;
5. verify row-level deep links still open todo, event, or source planning note correctly.

## Pending Next Goal

After this widget hotfix is committed, implement `docs/goals/2026-05-17-paykitodo-ai-daily-report-goal.md` as `1.9.1`.

The two goal docs are intentionally untracked local task specs unless the user explicitly asks to keep them in git.

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
