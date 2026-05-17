# Current Task

## Active Development Focus

The current round is PaykiTodo `1.9.13` / `versionCode 207`.

Primary goal: review the `1.9.11` / `1.9.12` experience fixes, close high-confidence behavior gaps, and land practical performance improvements rather than leaving them only as future notes.

## Completed In This Round

1. Kept the `1.9.12` fixes as the base:
   - Android widget board-range queries include active no-DDL todos.
   - `todo_items` Room indices cover common board/reminder/group/recurrence access paths.
   - Database version remains `14` with `MIGRATION_13_14`.
   - Planning Desk plain-bullet candidates show the no-DDL explanation.
   - Settings -> `电脑同步` explains 5-minute no-authorized-client auto-close.
   - Settings -> `AI 调用配置` persists valid provider edits more safely.
2. Desktop Web first connection now loads a lightweight board snapshot via `/api/snapshot?scope=board`.
3. Desktop Web loads full todos/events only when the user opens the event timeline or explicitly clicks `加载完整待办 / 日程数据`.
4. Desktop Web copy now explains that the full management timeline is loaded on demand.
5. Phone main `TodoViewModel.uiState` no longer observes full `focus_sessions`; daily-board focus stats use a Room aggregate Flow for the current day.
6. Phone main `TodoViewModel.uiState` no longer carries full AI report history; `AI 报告` collects `viewModel.aiReports` only inside that section.
7. `todayDateFlow` refreshes date-sensitive today/no-DDL classification across midnight.
8. Calendar month view, agenda/list view, and visible all-day rows reuse one top-level event-by-date index instead of each rebuilding date buckets.
9. Added a unit test confirming an active no-DDL todo stays in `今日待办` across later dates, not only on the creation day.
10. Version metadata moved to `1.9.13` / `versionCode 207`.
11. README / CHANGELOG / TODO / Wiki / current docs are being synchronized for the `1.9.13` performance review pass.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=207`, `versionName=1.9.13`, and `outputFile=PaykiTodo-1.9.13-debug.apk`.

## Verification Still Needed On Device / Browser

1. Device-test no-DDL todos:
   - create a normal todo with DDL disabled
   - confirm it appears under `今日待办`
   - confirm it has no reminder / recurrence behavior until DDL is added
   - confirm it appears in Android widget 今日待办 after widget refresh
2. Device-test Planning Desk plain bullets:
   - write `- 想办的事`
   - run recognition
   - confirm preview explains it was recognized as a no-DDL todo
   - confirm import creates a no-DDL todo and it appears in 今日待办
3. Device-test desktop sync:
   - enable sync and connect with the correct token within 5 minutes; it should stay running
   - enable sync and do not connect with the correct token; it should auto-disable after 5 minutes
4. Browser-test desktop lightweight snapshot:
   - first connection should show `看板轻量数据`
   - daily board should render without full timeline data
   - entering `日程时间轴` or clicking `加载完整待办 / 日程数据` should load full data and switch the status to `完整数据`
   - todo/event create/edit/delete should refresh without losing the current tab
5. Device-test Settings -> AI 调用配置:
   - add/edit/toggle/reorder/delete a complete provider and leave/reopen the page; changes should persist
   - enabled incomplete providers should show the required-field warning

## Performance Findings

Completed locally:

1. Added `todo_items` indices to reduce scans in daily board, widget board query, reminder scan, group filtering, and recurrence-series lookup.
2. Removed duplicate group reads from desktop `/api/snapshot`.
3. Split the desktop first-load path so `/api/snapshot?scope=board` can serve the board without transferring all todos/events/planning content.
4. Replaced ordinary board/task-page full focus-session observation with a today aggregate Flow.
5. Moved AI report list observation out of ordinary board/task UI state and into the AI report section.
6. Reused a single calendar event-by-date index across month/list/visible all-day surfaces.

Still recommended later:

1. Add deeper desktop endpoint splitting for very large datasets: separate todo/event/planning management endpoints rather than using one full snapshot for all management tabs.
2. Add paging/search for `AI 报告` when report history becomes large.
3. Profile calendar drag/add/edit/delete on a real device with a large event set; this round reduced redundant grouping but did not run a full runtime profiling专项.

## Immediate Practical Next Steps

1. Finish verification after the `1.9.13` version bump.
2. Commit the verified fix round locally.
3. Do not push unless the user explicitly asks.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.
