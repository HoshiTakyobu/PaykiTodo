# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.17` / `versionCode 211`.
- Main user request: review the recent `1.9.11`+ experience / performance work, identify remaining user-facing issues, and implement practical performance improvements where safe.
- This continuation keeps the no-DDL, widget, desktop lightweight snapshot, AI-provider save-state, calendar-index, phone screen-scoped subscription, Planning Desk parser threading, AI-report paging, Planning Desk announcement indexing, and then splits the desktop Web full-snapshot management path into narrower todo/event endpoints.
- Latest debug APK target after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.17-debug.apk`.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.17 Review / Performance Pass

1. Desktop Web first connection still calls `/api/snapshot?scope=board` so the browser can show the daily board without transferring all todos/events/planning data first.
2. Desktop Web complete todo management now loads through `GET /api/todos` only when the user clicks `加载完整待办列表`.
3. Desktop Web event timeline now loads through `GET /api/events?start=...&end=...`, limited to the currently visible date range.
4. Desktop Web ignores stale event-range responses if the user switches dates quickly while an older request is still in flight.
5. Desktop Web topbar now distinguishes `看板轻量数据`, `待办按需数据`, and `日程范围数据`.
6. Todo / event create, update, delete, complete, cancel and Planning Desk import / refresh / undo paths now refresh only the current page's needed data instead of reloading one complete snapshot after every operation.
7. Active no-DDL todos still stay in `今日待办` every day; the desktop full todo list keeps `!hasDueDate || dueDate == today` semantics.
8. Phone board/task `TodoUiState` remains screen-scoped: active todos, today/tomorrow events, today focus aggregate, settings, groups, and announcement candidates only.
9. Full Planning Desk notes remain collected only while the Planning Desk page is open; ordinary board/task state still uses indexed announcement hints.
10. `planning_notes.hasAnnouncementHint` remains persisted and indexed through database version 15; `1.9.17` does not change the database schema.

## Verification Status

Completed locally in this continuation:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=211`, `versionName=1.9.17`, and `outputFile=PaykiTodo-1.9.17-debug.apk`.
7. Emulator smoke on `emulator-5554` installed `app/build/outputs/apk/debug/PaykiTodo-1.9.17-debug.apk`, launched `com.paykitodo.app`, confirmed the Daily Board UI tree contained `每日看板` / `今日已专注` / `今日待办（0）` / `今日日程（0）` / `明天暂无日程 · 去规划台安排一下？`, and found no `FATAL EXCEPTION` in the checked logcat window.

## Remaining Device / Browser Verification

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.17-debug.apk` on the physical phone.
2. Verify a normal no-DDL todo appears under `今日待办`, does not enable reminders, and does not enable recurrence.
3. Verify Planning Desk plain bullets import as no-DDL todos and appear under `今日待办`; preview should show the no-DDL explanation.
4. Verify Android widget, phone daily board, desktop daily board, and desktop todo list all show no-DDL todos in today's todo block.
5. Verify desktop sync auto-disables after 5 minutes if no authorized desktop connection occurs, and stays enabled if the browser connects with the correct token within 5 minutes.
6. Verify Settings -> AI 调用配置 provider changes persist after leaving/reopening the page.
7. Browser-test desktop Web split loading:
   - initial connect shows `看板轻量数据`;
   - daily board renders immediately;
   - `加载完整待办列表` requests only `/api/todos` and shows `待办按需数据`;
   - entering `日程时间轴` requests only visible-range `/api/events?start=...&end=...` and shows `日程范围数据`;
   - fast event-date switching does not leave stale events from an older range;
   - todo/event create/edit/delete refresh without losing the current tab.
8. Verify `AI 报告` paging and notification deep-link detail opening on a real device with enough reports.
9. Verify active Planning Desk announcements still appear on phone board, Android widget, and desktop daily board after upgrading through `MIGRATION_14_15`; this proves the indexed `hasAnnouncementHint` backfill is correct on a real user database.
10. Do not push unless the user explicitly asks.

## Performance Notes

Fixed locally across `1.9.12`-`1.9.17`:

1. Added `todo_items` indices for high-frequency board/reminder/group/recurrence queries.
2. Removed duplicate group reads from desktop `/api/snapshot`.
3. Split desktop first-load data so daily board can load without all todos/events/planning/focus history.
4. Replaced ordinary main UI full focus-session observation with today's aggregate Flow.
5. Replaced ordinary main UI full todo observation with active-todo-only observation.
6. Deferred history todos, full active events, schedule templates, and reminder diagnostics to the pages that actually need them.
7. Paged the `AI 报告` archive instead of observing all reports.
8. Reduced calendar recomputation by reusing a single event-by-date index across month/list/visible all-day surfaces.
9. Moved Planning Desk local parsing off the Compose main thread.
10. Removed full Planning Desk note observation from ordinary board/task state; full notes now load only in the Planning Desk section.
11. Changed phone board, widget, and desktop board announcements to query only indexed announcement-candidate planning notes before parsing.
12. Split desktop Web management reads into lightweight board snapshot, full todo list endpoint, and visible-range event endpoint.

Worth doing later:

1. Add desktop todo pagination/search if the full `/api/todos` response becomes too large.
2. Add AI-report search/date-range filters if the archive becomes too large for simple paging.
3. Profile calendar drag/add/edit/delete on a real device with a large event set; this round reduced redundant observation/grouping but did not run a full runtime profiling专项.
4. Consider moving desktop Web source to a separate source/build folder only if the static asset grows too large to maintain directly under `app/src/main/assets/desktop-web/`.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/wiki/index.html`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncModels.kt`
- `README.md`
- `TODO.md`
- `CHANGELOG.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/PAYKITODO_SESSION_LEDGER.md`
- `docs/current/PLANNING_AI_ASSISTANT_DESIGN.md`

## Known Worktree Notes

- Branch is `main`; local branch is far ahead of origin. Do not push without user authorization.
- Existing untracked temp UI dumps such as `.tmp-*.xml` and `.tmp-paykitodo-1913-*` were present before this round and should not be committed unless intentionally needed.
- An untracked user note named `当前使用中存在的问题.md` may exist in the repo root; do not commit or modify it unless the user asks.
- Goal prompt files under `docs/goals/` should remain separate archive commits after secret scanning.

## Android Emulator Verification Rule

If an Android Emulator is started or reused, record:

- device id / AVD name
- installed APK path
- checked screens or flows
- what was actually verified

This avoids confusion when an emulator window remains on the user's desktop.

Do not overclaim emulator results. A real phone is still required for OEM notification, vibration, lock-screen, widget, alarm, reboot, battery-management behavior, Android launcher widget rendering, Planning Desk announcement migration on the user's real database, and live desktop-browser verification.
