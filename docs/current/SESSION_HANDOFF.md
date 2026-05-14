# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.7.5` / `versionCode 162`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.7.5-debug.apk`.
- Latest build commands used Android Studio bundled JBR and succeeded:
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
- `node --check app/src/main/assets/desktop-web/app.js` also succeeded.
- This round is an emergency startup-crash stabilization after `1.7.4` still crashed on device launch.
- The current suspected root cause is Room migration schema mismatch for `planning_notes`, not the already-reverted Planning Desk Markdown renderer.
- Phone-side Planning Desk Markdown rendering remains removed from the phone UI path.
- The phone Planning Desk stays on the stable `1.7.1` raw Markdown / natural-text editor while keeping Phase 2 import/edit workflow.
- Desktop web Planning Desk remains available with textarea editing, editable parse preview, selected import, and `#imported` write-back.
- Do not push `1.7.x` or the last `1.6.x` line to GitHub unless the user explicitly asks again.

## Latest Fixes In 1.7.5

1. Upgraded app version metadata to `1.7.5` / `versionCode 162`.
2. Upgraded Room database version from `9` to `10`.
3. Corrected `MIGRATION_8_9` so direct `1.6.x -> 1.7.5` upgrades create a `planning_notes` table matching the `PlanningNote` entity.
4. Added `MIGRATION_9_10` to rebuild existing `planning_notes` tables created by `1.7.0`-`1.7.4` into the Room-expected schema.
5. Made the repair path tolerate incomplete leftover `planning_notes` tables by recreating the table if required columns are missing.
6. Kept the `1.7.4` rollback of phone-side rendered Markdown preview.
7. README, CHANGELOG, TODO, docs/current, and Wiki were updated for `1.7.5`.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/TodoApplication.kt`
- `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
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

1. Install `PaykiTodo-1.7.5-debug.apk` over the crashing `1.7.4` installation.
2. Verify the app no longer crashes immediately after launch.
3. Verify existing todos/events survived the database migration.
4. Verify drawer -> `规划台` opens the raw Markdown editor.
5. Verify document create/open/search works.
6. Verify `识别` still opens editable import preview cards.
7. Verify importing selected todos/events still writes data and appends `#imported` to source Markdown lines.
8. Verify desktop web `规划台` still works with the same phone database.
9. Verify basic navigation to 每日看板 / 我的任务 / 日历 / 设置 still works.

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
