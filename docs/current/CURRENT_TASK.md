# Current Task

## Active Development Focus

The current round is PaykiTodo `1.9.19` / `versionCode 213`.

Primary goal: review the recent `1.9.11`+ experience and performance work, close the remaining safe performance / UX gaps found during inspection, and leave the repo in a verified state for user-side phone / browser testing.

## Completed In This Round

1. Kept the `1.9.11`-`1.9.17` behavior and performance baseline:
   - active no-DDL todos remain part of `今日待办` across phone board, widget board query, desktop board, and desktop todo management.
   - desktop first connection still uses `/api/snapshot?scope=board`.
   - desktop event timeline still loads only the visible `/api/events?start=...&end=...` range.
   - phone board/task state remains screen-scoped instead of carrying full history, full calendar, full Planning Desk notes, or full AI report history.
   - Planning Desk announcement candidates use indexed `planning_notes.hasAnnouncementHint`.
2. Kept the desktop todo-management pagination/search pass:
   - `GET /api/todos` now accepts `offset`, `limit`, and `q`.
   - desktop todo query searches title, notes, and location.
   - desktop todo ordering keeps business priority: missed/overdue, today/no-DDL, upcoming, then history.
   - no-DDL todos are explicitly ordered near today's active todos instead of being pushed behind future rows by their sentinel DDL value.
   - the browser UI now has a compact search row, loaded/total count, and `加载更多`.
   - failed todo loads reset the loading flag through `try/finally` so the page does not get stuck.
3. Kept the Android widget focus-card cleanup:
   - removed the unused `WidgetRowType.FOCUS` branch and `focusViews`.
   - removed the unused focus-card layout and focus-action background resource.
   - removed the static widget picker preview's `今日已专注` / `自由专注` block.
   - removed the fixed `take(6)` todo cap; the widget now relies on the wider overall row cap.
4. Hardened the desktop sync server:
   - connection handling now uses a bounded client thread pool rather than an unbounded cached pool.
   - HTTP requests are read by byte length, so Chinese UTF-8 Planning Desk bodies are not truncated by character-count reads.
   - oversized headers / bodies and invalid `Content-Length` return explicit errors.
   - `OPTIONS` preflight is handled without calling the authorized API handler.
5. Added AI report archive search / range filtering:
   - `AI 报告` can search report content / provider name.
   - type and time-range filters are compact dropdowns.
   - filtering is pushed into Room instead of filtering only the currently loaded page in memory.
6. Reduced calendar date-window allocation:
   - the long date range is represented by a lightweight date window instead of allocating a full date list and date-index map.
7. Added database version `16`:
   - `MIGRATION_15_16` creates an index for desktop todo paging / sorting.
   - `MIGRATION_15_16` creates AI report generated-time and type+generated-time indexes.
8. Version metadata moved to `1.9.19` / `versionCode 213`.
9. README / CHANGELOG / TODO / Wiki / current docs were synchronized for this `1.9.19` pass.

## Verification Completed This Round

Completed locally:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin testDebugUnitTest assembleDebug` passed.
3. `git diff --check` passed.
4. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=213`, `versionName=1.9.19`, and `outputFile=PaykiTodo-1.9.19-debug.apk`.

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.19-debug.apk` on the physical phone.
2. Browser-test desktop todos:
   - first connection should show `看板轻量数据`.
   - click `加载待办管理列表`; it should request `/api/todos?offset=0&limit=80`.
   - search should pass `q=...`, show matching rows, and keep no-DDL active todos in 今日待办 when they match.
   - `加载更多` should append the next page without duplicating rows.
3. Browser-test desktop events:
   - entering 日程时间轴 should load only the visible `/api/events?start=...&end=...` range.
   - fast date switching should not leave stale event data.
4. Device-test Android widget:
   - widget picker preview should not show `今日已专注` or `自由专注`.
   - the live widget should show announcements, greeting, today todos, and today/tomorrow schedule content only.
   - a resized large widget should reveal more than six today todos when data exists.
5. Device-test no-DDL todos:
   - normal no-DDL todo and Planning Desk plain-bullet no-DDL import both appear under 今日待办.
   - no-DDL todos remain reminder-disabled and recurrence-disabled until a DDL is added.
6. Device-test Settings -> AI 调用配置 provider persistence and real provider model discovery.
7. Device/browser-test active Planning Desk announcements after upgrading through `MIGRATION_14_15`.
8. Browser-test desktop Planning Desk long Chinese document save after the byte-length HTTP body reader change.
9. Device-test `AI 报告` keyword search, type dropdown, and time-range dropdown with enough generated reports.

## Performance Findings

Completed locally:

1. Desktop first-load remains lightweight via `/api/snapshot?scope=board`.
2. Desktop todo management is now paged/searchable instead of pulling every todo row at once, with an added paging-path index in database version 16.
3. Desktop event timeline remains range-scoped.
4. Android widget no longer keeps a dead focus-card view type or preview block.
5. Existing phone-side performance work remains in place: active-todo-only main observation, today focus aggregate, range-scoped board events, paged/searchable AI reports, indexed Planning Desk announcement lookup, and off-main-thread Planning Desk parsing.
6. Desktop sync now avoids unbounded client thread growth and reads request bodies by UTF-8 byte length.
7. Calendar timeline date handling avoids creating a 1461-item date list and lookup map on entry.

Still recommended later:

1. Profile calendar drag/add/edit/delete on a real device with a large event set.
2. Browser-profile desktop todo pagination/search with a large real database to tune page size and indexing if needed.
3. If AI-report keyword search becomes a bottleneck, consider a real FTS table rather than adding more `LIKE '%q%'` scans.
4. Continue physical-device verification for widget, OEM notification, vibration, lock-screen reminder, exact-alarm, and battery-policy behavior.

## Immediate Practical Next Steps

1. Install and test `app/build/outputs/apk/debug/PaykiTodo-1.9.19-debug.apk` on the physical phone.
2. Browser-test desktop Web with the new paged/searchable todo management list.
3. Verify the Android widget picker preview and live resized widget on the user's launcher.
4. Do not push unless the user explicitly asks.
