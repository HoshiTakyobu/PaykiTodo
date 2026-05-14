# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.7.12` / `versionCode 169`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.7.12-debug.apk`.
- Latest build commands used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
- `node --check app/src/main/assets/desktop-web/app.js` also succeeded.
- This round documents the `1.7.11` syntax consistency work across all user-facing help surfaces: phone Planning Desk help, todo batch help, calendar batch help, built-in Wiki, desktop Planning Desk help, and desktop reminder placeholders.
- The previous crash root cause was likely the Room migration schema mismatch for `planning_notes`; `1.7.6` keeps that repair and improves the Planning Desk UI.
- Phone-side Planning Desk Markdown rendering remains opt-in manual preview, not the startup default.
- The phone Planning Desk keeps the raw Markdown / natural-text editor plus Phase 2 import/edit workflow.
- Desktop web Planning Desk remains available with textarea editing, editable parse preview, selected import, document deletion, unified reminder input handoff, and `#imported` write-back.
- Do not push `1.7.x` or the last `1.6.x` line to GitHub unless the user explicitly asks again.

## Latest Fixes In 1.7.12

1. Upgraded app version metadata to `1.7.12` / `versionCode 169`.
2. Phone Planning Desk help now explains common DDL forms, natural reminder forms, and preview-edit time fields.
3. Todo batch import help now states that English commas split fields, while date/time inside one field should use a space or Chinese comma.
4. Calendar batch import help now documents `Remind=` support for the shared reminder syntax, including natural dates and Chinese date-time commas.
5. Built-in Wiki now covers the same syntax boundaries across Planning Desk, todo batch import, calendar batch import, and desktop Planning Desk.
6. Desktop Web Planning Desk help and reminder placeholders now show the same accepted reminder examples.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/ReminderTextParser.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningMarkdownParser.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningImportCandidate.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoBatchImport.kt`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/ReminderInputParser.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/wiki/index.html`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/PLANNING_DESK_DESIGN.md`
- `docs/current/PAYKITODO_SESSION_LEDGER.md`

## Current Verification Focus

1. Install `PaykiTodo-1.7.12-debug.apk`.
2. Verify the app no longer crashes immediately after launch.
3. Verify existing todos/events are still present.
4. Verify todo batch rows such as `5.28,整理材料,5`, `5月28日,整理材料,5`, `明天,整理材料,5`, and `周五,整理材料,5`.
5. Verify desktop web todo/event reminder input accepts `5,15,2:30 pm,明天 16:30,周五 16:30,5.10 15:00,5月10日，14:30`.
6. Verify Planning Desk help, todo batch help, calendar batch help, built-in Wiki, and desktop Planning Desk help all explain the same syntax rules.
7. Verify invalid desktop reminder inputs still fail visibly instead of saving bad offsets.
8. Verify drawer -> `规划台` still opens an empty new document with grey placeholder examples.
9. Verify `任务` repeated taps do not duplicate `- [ ]` on one line, and `子任务` inserts a new indented child line.
10. Verify importing selected todos/events still writes data and appends `#imported` to source Markdown lines.
11. Verify basic navigation to 每日看板 / 我的任务 / 日历 / 设置 still works.

## If 1.7.5 Still Crashes

Do not keep guessing in UI code first. Next likely steps:

1. Capture actual Android crash output if possible.
2. Build a startup-safe diagnostic variant that delays repository/database access until after a minimal screen renders.
3. Inspect other startup-time initializers: `TodoApplication.onCreate`, `TodoViewModel.init`, Room open path, settings store reads, startup services, and resource loading.
4. Consider adding a guarded startup recovery screen if Room migration fails, but only with explicit user approval because destructive recovery risks data loss.

## Deferred Larger Work

- Reintroduce phone-side Markdown rendering only after isolating it behind a safer, smaller path and adding parser/render tests.
- Drag-and-drop planning.
- Gantt chart.
- AI auto-planning.
- Complex project tree.
- Markdown highlighting / rich editor.
- Dedicated parser unit-test suite.
- Full desktop UI parity with phone UI.
- Full calendar rendering/performance optimization.
- Complete Todo lunar wheel / lunar DDL work.

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

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
