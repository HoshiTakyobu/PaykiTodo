# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.13` / `versionCode 207`.
- Main user request: review the current `1.9.11`/`1.9.12` state, identify remaining user-facing issues, and implement practical performance improvements where safe.
- This continuation keeps the `1.9.12` no-DDL/widget/index/AI-provider fixes and adds desktop lightweight first load, phone main-state subscription splitting, calendar date-index reuse, and a no-DDL cross-day unit test.
- Latest debug APK target after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.13-debug.apk`.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.13 Review / Performance Pass

1. Desktop Web first connection now calls `/api/snapshot?scope=board` so the browser can show the daily board without transferring all todos/events/planning data first.
2. Desktop Web can still load full data on demand when the user opens `日程时间轴` or clicks `加载完整待办 / 日程数据`; the topbar distinguishes `看板轻量数据` and `完整数据`.
3. Desktop Web copy now says the full management timeline is loaded on demand instead of implying it is always loaded under the board.
4. `DesktopSyncSnapshot` includes a `partial` flag so the browser can render light vs full states correctly.
5. `DesktopSyncCoordinator.buildSnapshot(boardOnly = true)` now loads board-range items, active planning notes, and today focus aggregate stats instead of all todos/events/planning/focus data.
6. `FocusSessionStats` and DAO aggregate queries were added for focus-session counts/minutes in a time range.
7. `TodoViewModel.uiState` now uses today's focus aggregate Flow and no longer observes the full `focus_sessions` list for ordinary board/task screens.
8. `TodoViewModel.aiReports` is exposed separately; `DashboardChrome` collects it only in the `AI 报告` section, so ordinary board/task screens no longer carry the full AI report list.
9. `todayDateFlow` refreshes date-sensitive today/no-DDL classification across midnight.
10. Calendar month view, agenda/list view, and visible all-day rows reuse one top-level event-by-date index instead of rebuilding date buckets independently.
11. `DailyBoardSnapshotBuilderTest.noDdlTodosStayInTodayTodosOnLaterDates` confirms a no-DDL active todo remains in `今日待办` across later days.
12. Version metadata moved to `1.9.13` / `versionCode 207`.

## Verification Status

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed.
5. `git diff --check` passed.
6. `output-metadata.json` reports `versionCode=207`, `versionName=1.9.13`, and `outputFile=PaykiTodo-1.9.13-debug.apk`.

Remaining after local completion:

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.13-debug.apk` on the user's physical phone.
2. Verify a normal no-DDL todo appears under `今日待办`, does not enable reminders, and does not enable recurrence.
3. Verify Planning Desk plain bullets import as no-DDL todos and appear under `今日待办`; preview should show the no-DDL explanation.
4. Verify Android widget, phone daily board, desktop daily board, and desktop todo list all show no-DDL todos in today's todo block.
5. Verify desktop sync auto-disables after 5 minutes if no authorized desktop connection occurs.
6. Verify desktop sync stays enabled if the desktop browser connects with the correct token within 5 minutes.
7. Verify Settings -> AI 调用配置 provider changes persist after leaving/reopening the page.
8. Verify desktop browser lightweight snapshot flow: initial `看板轻量数据`, full-data transition, and create/edit/delete refresh behavior.
9. Do not push unless the user explicitly asks.

## Performance Notes

Fixed locally in `1.9.12` / `1.9.13`:

1. Added `todo_items` indices for high-frequency board/reminder/group/recurrence queries.
2. Removed duplicate group reads from desktop `/api/snapshot`.
3. Split desktop first-load data so daily board can load without all todos/events/planning/focus history.
4. Replaced ordinary main UI full focus-session observation with today's aggregate Flow.
5. Moved full AI report observation out of ordinary main UI state and into the `AI 报告` section.
6. Reduced calendar recomputation by reusing a single event-by-date index across month/list/visible all-day surfaces.

Worth doing later:

1. Split desktop management APIs further for very large histories: separate todos/events/planning endpoints or pagination.
2. Add paging/search for the AI report archive if report history grows large.
3. Profile calendar drag/add/edit/delete on a real device with a large event set; this round reduced redundant grouping but did not run full runtime profiling.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/wiki/index.html`
- `app/src/main/java/com/example/todoalarm/data/FocusSession.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncModels.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `app/src/test/java/com/example/todoalarm/data/DailyBoardSnapshotBuilderTest.kt`
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
- Boundary: this emulator pass does not replace real-phone verification for OEM notification, vibration, lock-screen, widget, alarm, reboot, battery-management behavior, or live desktop-browser verification of `1.9.13`
