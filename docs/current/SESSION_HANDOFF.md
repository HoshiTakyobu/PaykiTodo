# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.12` / `versionCode 206`.
- Main user request: review the current `1.9.11` state, identify remaining user-facing issues, and point out practical performance optimizations.
- Core fix in this continuation: Android widget board queries now include active no-DDL todos; `todo_items` gained Room indices; desktop `/api/snapshot` no longer rereads groups; Planning Desk plain bullets and AI-provider save state now give clearer feedback.
- Latest debug APK target after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.12-debug.apk`.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.12 Review / Performance Pass

1. `TodoDao.getActiveItemsForBoardRange()` now includes `NO_DUE_DATE_MILLIS`, fixing the gap where the Android launcher widget could miss no-DDL todos even though phone/desktop boards showed them.
2. `TodoRepository.getActiveItemsForBoardRange()` passes `NO_DUE_DATE_MILLIS` into the DAO query.
3. `TodoItem` now declares indices for board todo queries, board event range queries, active reminders, group+due sorting, and recurring-series lookup.
4. Database version moved from `13` to `14`; `DatabaseMigrations.MIGRATION_13_14` creates the new `todo_items` indices; `TodoApplication` registers the migration.
5. `DesktopSyncCoordinator` now reuses `snapshot.groups.associateBy { it.id }` when rendering `/api/snapshot`, removing one duplicate Room group read per snapshot request.
6. `PlanningMarkdownParser` now marks plain bullet no-DDL candidates with the preview message `普通项目符号已识别为无 DDL 待办。`
7. `PlanningMarkdownParserTest.parsesPlainBulletsAsNoDdlTodos` now asserts that explanatory message.
8. Settings -> `电脑同步` now explains the 5-minute no-authorized-client auto-close rule in the actual settings panel.
9. Settings -> `AI 调用配置` now auto-saves valid provider changes after add/edit/toggle/reorder/delete where possible, and shows an in-page warning for unsaved/incomplete configuration.
10. Version metadata moved to `1.9.12` / `versionCode 206`.

## Verification Status

Completed so far in this round:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=206`, `versionName=1.9.12`, and `outputFile=PaykiTodo-1.9.12-debug.apk`.

Remaining after local completion:

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.12-debug.apk` on the user's physical phone.
2. Verify a normal no-DDL todo appears under `今日待办`, does not enable reminders, and does not enable recurrence.
3. Verify Planning Desk plain bullets import as no-DDL todos and appear under `今日待办`; preview should show the new plain-bullet explanation.
4. Verify Android widget, phone daily board, desktop daily board, and desktop todo list all show no-DDL todos in today's todo block.
5. Verify desktop sync auto-disables after 5 minutes if no authorized desktop connection occurs.
6. Verify desktop sync stays enabled if the desktop browser connects with the correct token within 5 minutes.
7. Verify Settings -> AI 调用配置 provider changes persist after leaving/reopening the page.
8. Do not push unless the user explicitly asks.

## Performance Notes

Fixed locally in `1.9.12`:

1. Added `todo_items` indices for high-frequency board/reminder/group/recurrence queries.
2. Removed duplicate group reads from desktop `/api/snapshot`.

Worth doing later:

1. Split desktop Web data refresh so `/api/snapshot` does not always send all todos/events/planning notes/focus data.
2. Replace main UI full focus-session / AI-report observation with aggregated or paged flows.
3. Run a dedicated Calendar timeline performance pass for large event sets.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/TodoItem.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
- `app/src/main/java/com/example/todoalarm/TodoApplication.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningMarkdownParser.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/test/java/com/example/todoalarm/data/PlanningMarkdownParserTest.kt`
- `README.md`
- `TODO.md`
- `CHANGELOG.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/PLANNING_AI_ASSISTANT_DESIGN.md`

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
- Boundary: this emulator pass does not replace real-phone verification for OEM notification, vibration, lock-screen, widget, alarm, reboot, battery-management behavior, or live desktop-browser verification of `1.9.12`
