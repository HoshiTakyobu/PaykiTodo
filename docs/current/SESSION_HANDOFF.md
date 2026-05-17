# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.20` / `versionCode 214`.
- Main user request: review the recent `1.9.11`+ experience / performance work, identify remaining user-facing issues, and implement practical performance improvements where safe.
- This continuation keeps the no-DDL, widget, desktop lightweight snapshot, AI-provider save-state, calendar-index, phone screen-scoped subscription, Planning Desk parser threading, AI-report paging, Planning Desk announcement indexing, desktop-sync hardening, AI-report filtering, and calendar date-window baseline, then hardens no-DDL todo sectioning and changes the phone Calendar page to observe only the current visible event range.
- Latest expected debug APK after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.20-debug.apk`.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.20 Review / Performance Pass

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
11. `planning_notes.hasAnnouncementHint` remains persisted and indexed; database version is now 16.
12. Desktop sync server now uses one accept thread plus a bounded client pool instead of an unbounded cached thread pool.
13. Desktop sync HTTP requests are read by byte `Content-Length`, fixing UTF-8 Chinese long-body truncation risks in desktop Planning Desk save/import flows.
14. Desktop sync handles `OPTIONS` preflight before authorization and returns explicit 400/413 JSON errors for malformed or oversized requests.
15. `AI 报告` archive now supports keyword search, type dropdown, and all-time / recent-7 / recent-30 / recent-90-day range filters.
16. AI report filtering is pushed into Room queries, with separate typed/untyped paths to preserve better index use.
17. Calendar timeline uses a lightweight date window rather than allocating the whole long date list and date-index map.
18. `MIGRATION_15_16` adds `index_todo_items_desktop_todo_paging`, `index_ai_reports_type_generated_id`, and `index_ai_reports_generated_id`.
19. Active no-DDL todo sectioning is centralized in `classifyActiveTodoItems`, so phone board/task sections use the same rule: active no-DDL items stay under `今日待办` every day and never become upcoming.
20. Phone Calendar now reports its visible date window to the ViewModel and observes only overlapping active events in that padded range instead of subscribing to every active event when the Calendar page is open.
21. Notification / deep-link navigation to a far calendar event first expands the event query around the target event date, then focuses the Calendar page on that date.

## Verification Status

Completed locally in this continuation:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin testDebugUnitTest assembleDebug` passed.
3. `git diff --check` passed.
4. Unit tests include `TodoItemSectionsTest`, covering no-DDL active todos staying in today across dates.
5. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=214`, `versionName=1.9.20`, and `outputFile=PaykiTodo-1.9.20-debug.apk`.

Latest emulator smoke installed `app/build/outputs/apk/debug/PaykiTodo-1.9.20-debug.apk` on `emulator-5554`, checked app launch, Daily Board UI tree, drawer UI tree, Calendar UI tree, screenshot capture, and a PaykiTodo fatal-crash logcat scan. MainActivity displayed, Daily Board showed `今日待办（0）` / `今日日程（0）`, drawer showed primary entries, Calendar displayed `2026年5月` with timeline content, and no PaykiTodo `FATAL EXCEPTION` was found in the checked logcat window.

## Remaining Device / Browser Verification

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.20-debug.apk` on the physical phone.
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
9. Verify desktop Planning Desk can save a long Chinese document without body truncation.
10. Verify `AI 报告` search/type/range filters with enough real reports.
11. Do not push unless the user explicitly asks.

## Performance Notes

Fixed locally across `1.9.12`-`1.9.20`:

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
14. Bounded desktop sync client handling and fixed UTF-8 body reading by byte length.
15. Added AI-report keyword/type/range query pushdown and matching Room indices.
16. Replaced calendar timeline date-list allocation with a lightweight date window.

Worth doing later:

1. Profile calendar drag/add/edit/delete on a real device with a large event set.
2. Browser-profile desktop todo pagination/search with a large real database to tune page size and indexing if needed.
3. Consider FTS for AI-report content only if keyword search becomes slow with real large archives.
4. Consider moving desktop Web source to a separate source/build folder only if the static asset grows too large to maintain directly under `app/src/main/assets/desktop-web/`.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/wiki/index.html`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
- `app/src/main/java/com/example/todoalarm/data/AiReport.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoItem.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncServer.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/ui/AiReportPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `app/src/test/java/com/example/todoalarm/sync/DesktopSyncServerTest.kt`
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
