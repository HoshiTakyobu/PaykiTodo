# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.7.4` / `versionCode 161`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.7.4-debug.apk`.
- Latest build command used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat assembleDebug`
- This round is an emergency stabilization rollback after `1.7.2` and `1.7.3` still crashed on device launch.
- Phone-side Planning Desk Markdown rendering has been removed from the phone UI path for now.
- The phone Planning Desk has been restored to the stable `1.7.1` raw Markdown / natural-text editor while keeping Phase 2 import/edit workflow.
- Desktop web Planning Desk remains available with textarea editing, editable parse preview, selected import, and `#imported` write-back.

## Latest Fixes In 1.7.4

1. Restored `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt` from the stable `1.7.1` implementation.
2. Removed the unstable phone-side rendered Markdown preview path introduced in `1.7.2` / `1.7.3`.
3. Preserved core Planning Desk functionality: multiple documents, search, raw editing, local parser, editable preview, import, and automatic `#imported` marker write-back.
4. Upgraded version metadata to `1.7.4` / `versionCode 161`.
5. README, CHANGELOG, TODO, docs/current, and Wiki were updated to document the rollback.
6. `./gradlew.bat assembleDebug` succeeded with Android Studio bundled JBR.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
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

1. Install `PaykiTodo-1.7.4-debug.apk`.
2. Verify the app no longer crashes immediately after launch.
3. Verify drawer -> `规划台` opens the raw Markdown editor.
4. Verify document create/open/search works.
5. Verify `识别` still opens editable import preview cards.
6. Verify importing selected todos/events still writes data and appends `#imported` to source Markdown lines.
7. Verify desktop web `规划台` still works with the same phone database.
8. Verify basic navigation to 每日看板 / 我的任务 / 日历 / 设置 still works.

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
