# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.18` / `versionCode 212`.
- Main user request: review the recent `1.9.11`+ experience / performance work, identify remaining user-facing issues, and implement practical performance improvements where safe.
- This continuation keeps the no-DDL, widget, desktop lightweight snapshot, AI-provider save-state, calendar-index, phone screen-scoped subscription, Planning Desk parser threading, AI-report paging, and Planning Desk announcement indexing baseline, then finishes desktop todo pagination/search and removes leftover launcher-widget focus-card artifacts.
- Latest debug APK after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.18-debug.apk`.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.18 Review / Performance Pass

1. Desktop Web first connection still calls `/api/snapshot?scope=board` so the browser can show the daily board without transferring all todos/events/planning data first.
2. Desktop Web todo management now loads through `GET /api/todos?offset=...&limit=...&q=...` when the user clicks `加载待办管理列表`, searches, or loads more.
3. The desktop todo endpoint searches title, notes, and location, caps page size, and reports `offset`, `limit`, `total`, `hasMore`, and `query`.
4. Desktop todo ordering keeps missed/overdue, today/no-DDL, upcoming, then history priority; no-DDL todos are not pushed behind future rows by the sentinel DDL value.
5. Desktop Web event timeline still loads through `GET /api/events?start=...&end=...`, limited to the currently visible date range.
6. Desktop Web ignores stale event-range responses if the user switches dates quickly while an older request is still in flight.
7. Todo / event create, update, delete, complete, cancel and Planning Desk import / refresh / undo paths still refresh only the current page's needed data instead of reloading one complete snapshot after every operation.
8. Android widget runtime no longer has a `FOCUS` row type or `focusViews` branch.
9. Android widget picker preview no longer shows `今日已专注` / `自由专注`.
10. Android widget no longer hard-caps visible today todos at six rows; it relies on the broader overall row cap.
11. `planning_notes.hasAnnouncementHint` remains persisted and indexed through database version 15; `1.9.18` does not change the database schema.

## Verification Status

Completed locally in this continuation:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin testDebugUnitTest assembleDebug` passed.
3. `git diff --check` passed.
4. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=212`, `versionName=1.9.18`, and `outputFile=PaykiTodo-1.9.18-debug.apk`.

No `1.9.18` emulator smoke was run in this continuation. The latest recorded emulator smoke remains the previous `1.9.17` app-launch check on `emulator-5554`.

## Remaining Device / Browser Verification

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.18-debug.apk` on the physical phone.
2. Browser-test desktop todo pagination/search:
   - initial connect shows `看板轻量数据`;
   - `加载待办管理列表` requests `/api/todos?offset=0&limit=80`;
   - search sends `q=...` and resets to the first page;
   - `加载更多` appends the next page without duplicate rows;
   - no-DDL active todos remain in 今日待办 when they match the page/query.
3. Browser-test desktop event range loading:
   - entering `日程时间轴` requests only visible-range `/api/events?start=...&end=...`;
   - fast date switching does not leave stale events from an older range.
4. Device-test Android widget:
   - widget picker preview should not show `今日已专注` or `自由专注`;
   - live widget should show announcements, greeting, today todos, and today/tomorrow schedule content only;
   - resizing larger should reveal more than six today todos when enough data exists.
5. Device-test no-DDL todos:
   - normal no-DDL todo appears under `今日待办`;
   - Planning Desk plain bullets import as no-DDL todos and appear under `今日待办`;
   - no-DDL todos remain reminder-disabled and recurrence-disabled until a DDL is added.
6. Verify desktop sync auto-disables after 5 minutes if no authorized desktop connection occurs, and stays enabled if the browser connects with the correct token within 5 minutes.
7. Verify Settings -> AI 调用配置 provider changes persist after leaving/reopening the page and real `/models` discovery still behaves correctly.
8. Verify active Planning Desk announcements still appear on phone board, Android widget, and desktop daily board after upgrading through `MIGRATION_14_15`.
9. Do not push unless the user explicitly asks.

## Performance Notes

Fixed locally across `1.9.12`-`1.9.18`:

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
12. Split desktop Web management reads into lightweight board snapshot, paged/searchable todo endpoint, and visible-range event endpoint.
13. Removed remaining widget focus-card runtime and picker-preview artifacts.

Worth doing later:

1. Add AI-report search/date-range filters if the archive becomes too large for simple paging.
2. Profile calendar drag/add/edit/delete on a real device with a large event set.
3. Browser-profile desktop todo pagination/search with a large real database to tune page size and indexing if needed.
4. Consider moving desktop Web source to a separate source/build folder only if the static asset grows too large to maintain directly under `app/src/main/assets/desktop-web/`.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/wiki/index.html`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/res/layout/widget_todo_preview.xml`
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
