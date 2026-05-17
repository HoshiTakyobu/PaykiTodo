# Current Task

## Active Development Focus

The current round is PaykiTodo `1.9.17` / `versionCode 211`.

Primary goal: review the recent `1.9.11`-`1.9.16` experience and performance work, close obvious implementation gaps, split the remaining desktop Web full-snapshot hot path, and leave the repo in a verified state for user-side phone / browser testing.

## Completed In This Round

1. Kept the `1.9.11` / `1.9.12` behavior fixes as the base:
   - active no-DDL todos remain part of `今日待办` across phone board, widget board query, desktop board, and desktop todo management.
   - Planning Desk plain bullets can become no-DDL todo candidates and preview explains that behavior.
   - Desktop sync explains and enforces the 5-minute no-authorized-client auto-close behavior.
   - Settings -> `AI 调用配置` provider rows use compact summary cards and persist valid provider edits more safely.
2. Kept the `1.9.13` performance baseline:
   - Desktop Web first connection uses `/api/snapshot?scope=board`.
   - full desktop todos/events are deferred until timeline/manual full-data load.
   - daily-board focus stats use a Room aggregate Flow.
   - calendar month/list/all-day surfaces reuse one event-date index.
   - a unit test confirms no-DDL active todos stay in `今日待办` across later dates.
3. Added the `1.9.14` phone-side state split:
   - ordinary `TodoUiState` observes active todos through a dedicated active-todo Flow instead of observing all `todo_items`.
   - daily board observes only today/tomorrow-range active events.
   - full active calendar events and schedule templates are collected only when the Calendar page is open.
   - history todos are collected only when `历史记录` is open.
   - reminder-chain diagnostics are collected only when Settings is open.
4. Moved Planning Desk local-rule recognition to `Dispatchers.Default`; AI failure fallback to local parsing also avoids occupying the Compose main thread for long documents.
5. Changed `AI 报告` archive loading from full-list observation to paged queries:
   - default page size is 30 reports.
   - filter tabs query Room by type + limit instead of filtering a full in-memory list.
   - notification deep links can fetch a target report by ID even if it is not in the current page.
6. Removed the now-unused full `observeAiReports()` DAO/repository path to avoid future accidental full-history observation.
7. Added the `1.9.15` Planning Desk announcement subscription split:
   - ordinary board/task `TodoUiState` no longer carries the full planning-note list.
   - the full planning-note Flow is collected only while the `规划台` page is open.
   - phone board announcements use an announcement-hint planning-note Flow before strict announcement parsing.
   - Android widget and desktop `/api/snapshot` use the same announcement-hint query instead of reading all planning notes.
   - announcement-hint coverage is now unit-tested for common announcement forms and ordinary non-announcement planning text.
8. Added the `1.9.16` indexed Planning Desk announcement lookup:
   - `planning_notes` now stores `hasAnnouncementHint`, computed from Markdown content.
   - `MIGRATION_14_15` adds and backfills the hint field for existing planning notes.
   - the announcement lookup index covers `archived`, `hasAnnouncementHint`, `updatedAtMillis`, and `createdAtMillis`.
   - phone board, Android widget, and desktop lightweight snapshot announcement candidate queries now use the boolean hint instead of `LIKE` scanning full Markdown bodies.
   - backup / restore and desktop planning-note JSON include the hint, while restore recomputes it from Markdown content.
9. Added the `1.9.17` desktop Web data split:
   - desktop Web still starts from `/api/snapshot?scope=board` for the daily board.
   - complete desktop todo management now loads through `/api/todos`.
   - calendar timeline data now loads through `/api/events?start=...&end=...` for the visible date range.
   - todo / event mutations and Planning Desk import / refresh / undo paths refresh only the currently needed page data instead of reloading one complete snapshot.
   - event range loading uses a request serial so fast date switching cannot let an older response overwrite the latest range.
10. Version metadata moved to `1.9.17` / `versionCode 211`.
11. README / CHANGELOG / TODO / Wiki header / current docs are being synchronized for this `1.9.17` performance review pass.

## Verification Completed This Round

Completed locally:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=211`, `versionName=1.9.17`, and `outputFile=PaykiTodo-1.9.17-debug.apk`.
7. Emulator smoke on `emulator-5554` installed `app/build/outputs/apk/debug/PaykiTodo-1.9.17-debug.apk`, launched `com.paykitodo.app`, confirmed the Daily Board UI tree contained `每日看板` / `今日已专注` / `今日待办（0）` / `今日日程（0）` / `明天暂无日程 · 去规划台安排一下？`, and found no `FATAL EXCEPTION` in the checked logcat window.

## Verification Still Needed On Device / Browser

1. Device-test no-DDL todos:
   - create a normal todo with DDL disabled.
   - confirm it appears under `今日待办`.
   - confirm it has no reminder / recurrence behavior until DDL is added.
   - confirm it appears in Android widget 今日待办 after widget refresh.
2. Device-test Planning Desk plain bullets:
   - write `- 想办的事`.
   - run recognition.
   - confirm preview explains it was recognized as a no-DDL todo.
   - confirm import creates a no-DDL todo and it appears in 今日待办.
3. Device-test desktop sync:
   - enable sync and connect with the correct token within 5 minutes; it should stay running.
   - enable sync and do not connect with the correct token; it should auto-disable after 5 minutes.
4. Browser-test desktop lightweight snapshot:
   - first connection should show `看板轻量数据`.
   - daily board should render without full timeline data.
   - clicking `加载完整待办列表` should load only `/api/todos` and switch the status to `待办按需数据`.
   - entering `日程时间轴` should load only `/api/events?start=...&end=...` for the visible range and switch the status to `日程范围数据`.
   - fast event-date switching should not leave stale events from an older range on screen.
   - todo/event create/edit/delete should refresh without losing the current tab.
5. Device-test Settings -> AI 调用配置:
   - add/edit/toggle/reorder/delete a complete provider and leave/reopen the page; changes should persist.
   - enabled incomplete providers should show the required-field warning.
6. Device-test `AI 报告` archive:
   - list opens with recent reports.
   - filter tabs work.
   - `加载更多` loads another page when at least 30 reports exist.
   - notification deep link opens the target report detail even if it is older than the first page.
7. Device/browser-test Planning Desk announcements:
   - active `#公告` / `> [!公告]` / `[!announcement]` lines still appear on phone board, widget, and desktop daily board.
   - ordinary Planning Desk notes without announcement syntax should not show as announcements.
   - the Planning Desk page should still open the full document list normally after the ordinary UI state split.

## Performance Findings

Completed locally:

1. Added `todo_items` indices to reduce scans in daily board, widget board query, reminder scan, group filtering, and recurrence-series lookup.
2. Removed duplicate group reads from desktop `/api/snapshot`.
3. Split the desktop first-load path so `/api/snapshot?scope=board` can serve the board without transferring all todos/events/planning content.
4. Replaced ordinary board/task-page full focus-session observation with a today aggregate Flow.
5. Replaced ordinary board/task-page full todo observation with active-todo-only observation.
6. Deferred history todos, full active events, schedule templates, and reminder diagnostics to their owning sections.
7. Reused a single calendar event-by-date index across month/list/visible all-day surfaces.
8. Moved Planning Desk local parsing off the Compose main thread.
9. Added paged Room queries for `AI 报告` instead of full archive observation.
10. Removed full Planning Desk note observation from ordinary board/task state; complete planning docs now load only in the Planning Desk section.
11. Changed phone board, Android widget, and desktop lightweight snapshot announcements to use a planning-note announcement-hint query before strict parsing.
12. Persisted Planning Desk announcement hints in `planning_notes.hasAnnouncementHint`, added a Room index and `MIGRATION_14_15`, and changed announcement candidate queries to use the indexed boolean instead of Markdown-body `LIKE` scans.
13. Added desktop `/api/todos` and visible-range `/api/events?start=...&end=...`, and changed desktop Web current-page mutation refresh to avoid reloading complete snapshots after ordinary operations.

Still recommended later:

1. Add desktop pagination/search if the full todo list itself becomes too large for `/api/todos`; `1.9.17` removes the biggest snapshot coupling but does not yet paginate todos.
2. Profile calendar drag/add/edit/delete on a real device with a large event set; this round reduced data subscription and redundant grouping but did not run a full runtime profiling专项.
3. Consider search/date-range filters for `AI 报告` if the archive becomes long enough that paging alone is not enough.

## Immediate Practical Next Steps

1. Install and test `app/build/outputs/apk/debug/PaykiTodo-1.9.17-debug.apk` on the physical phone.
2. Focus physical-device verification on no-DDL todos, Android widget refresh, desktop-sync auto-close, real-provider AI source editing, AI report paging, and OEM reminder behavior.
3. Browser-test desktop Web with the new split endpoints: first board load, loading complete todos, switching event ranges, and todo/event mutation refresh.
4. If another performance pass is started, prioritize desktop todo pagination/search and profiling calendar drag/add/edit/delete on a real device with large datasets.
5. Do not push unless the user explicitly asks.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.
