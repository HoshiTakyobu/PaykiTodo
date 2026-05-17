# Current Task

## Active Development Focus

The current round is PaykiTodo `1.9.18` / `versionCode 212`.

Primary goal: review the recent `1.9.11`+ experience and performance work, close the remaining safe performance / UX gaps found during inspection, and leave the repo in a verified state for user-side phone / browser testing.

## Completed In This Round

1. Kept the `1.9.11`-`1.9.17` behavior and performance baseline:
   - active no-DDL todos remain part of `今日待办` across phone board, widget board query, desktop board, and desktop todo management.
   - desktop first connection still uses `/api/snapshot?scope=board`.
   - desktop event timeline still loads only the visible `/api/events?start=...&end=...` range.
   - phone board/task state remains screen-scoped instead of carrying full history, full calendar, full Planning Desk notes, or full AI report history.
   - Planning Desk announcement candidates use indexed `planning_notes.hasAnnouncementHint`.
2. Finished the desktop todo-management pagination/search pass:
   - `GET /api/todos` now accepts `offset`, `limit`, and `q`.
   - desktop todo query searches title, notes, and location.
   - desktop todo ordering keeps business priority: missed/overdue, today/no-DDL, upcoming, then history.
   - no-DDL todos are explicitly ordered near today's active todos instead of being pushed behind future rows by their sentinel DDL value.
   - the browser UI now has a compact search row, loaded/total count, and `加载更多`.
   - failed todo loads reset the loading flag through `try/finally` so the page does not get stuck.
3. Cleaned the Android widget focus-card remnants:
   - removed the unused `WidgetRowType.FOCUS` branch and `focusViews`.
   - removed the unused focus-card layout and focus-action background resource.
   - removed the static widget picker preview's `今日已专注` / `自由专注` block.
   - removed the fixed `take(6)` todo cap; the widget now relies on the wider overall row cap.
4. Version metadata moved to `1.9.18` / `versionCode 212`.
5. README / CHANGELOG / TODO / Wiki / current docs were synchronized for this `1.9.18` pass.

## Verification Completed This Round

Completed locally:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin testDebugUnitTest assembleDebug` passed.
3. `git diff --check` passed.
4. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=212`, `versionName=1.9.18`, and `outputFile=PaykiTodo-1.9.18-debug.apk`.

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.18-debug.apk` on the physical phone.
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

## Performance Findings

Completed locally:

1. Desktop first-load remains lightweight via `/api/snapshot?scope=board`.
2. Desktop todo management is now paged/searchable instead of pulling every todo row at once.
3. Desktop event timeline remains range-scoped.
4. Android widget no longer keeps a dead focus-card view type or preview block.
5. Existing phone-side performance work remains in place: active-todo-only main observation, today focus aggregate, range-scoped board events, paged AI reports, indexed Planning Desk announcement lookup, and off-main-thread Planning Desk parsing.

Still recommended later:

1. Profile calendar drag/add/edit/delete on a real device with a large event set.
2. Add AI-report search/date-range filters if the archive becomes large.
3. Browser-profile desktop todo pagination/search with a large real database to tune page size and indexing if needed.
4. Continue physical-device verification for widget, OEM notification, vibration, lock-screen reminder, exact-alarm, and battery-policy behavior.

## Immediate Practical Next Steps

1. Install and test `app/build/outputs/apk/debug/PaykiTodo-1.9.18-debug.apk` on the physical phone.
2. Browser-test desktop Web with the new paged/searchable todo management list.
3. Verify the Android widget picker preview and live resized widget on the user's launcher.
4. Do not push unless the user explicitly asks.
