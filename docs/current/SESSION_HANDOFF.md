# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.16` / `versionCode 210`.
- Main user request: review the recent `1.9.11`-`1.9.15` experience fixes, identify remaining user-facing issues, and implement practical performance improvements where safe.
- This continuation keeps the no-DDL, widget, desktop lightweight snapshot, AI-provider save-state, calendar-index, phone screen-scoped subscription, Planning Desk parser threading, AI-report paging, and Planning Desk announcement split fixes, then persists Planning Desk announcement hint state so board/widget/desktop announcement queries use an indexed boolean field instead of Markdown-body scans.
- Latest debug APK target after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.16-debug.apk`.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.16 Review / Performance Pass

1. Desktop Web first connection still calls `/api/snapshot?scope=board` so the browser can show the daily board without transferring all todos/events/planning data first.
2. Desktop Web can still load full data on demand when the user opens `日程时间轴` or clicks `加载完整待办 / 日程数据`; the topbar distinguishes `看板轻量数据` and `完整数据`.
3. `TodoViewModel.uiState` now uses today's focus aggregate Flow and no longer observes the full `focus_sessions` list for ordinary board/task screens.
4. `TodoViewModel.uiState` now observes active todos through a dedicated active-todo Flow instead of observing the full `todo_items` table.
5. No-DDL active todos still stay in `今日待办` every day; the active-todo split preserves the `!hasDueDate || dueDate == today` logic.
6. Daily board observes only today/tomorrow-range active events; the Calendar page collects the full active-event list only while the Calendar section is open.
7. History todos, schedule templates, and reminder-chain diagnostics are collected only by their owning pages (`历史记录`, `日历`, `设置`).
8. `AI 报告` no longer observes the full report archive; it loads reports by filter + limit, defaults to 30 rows, exposes `加载更多`, and can fetch a deep-linked report by ID.
9. Planning Desk local-rule recognition runs on `Dispatchers.Default`; AI failure fallback local parsing also avoids occupying the Compose main thread for long documents.
10. Calendar month view, agenda/list view, and visible all-day rows reuse one top-level event-by-date index instead of rebuilding date buckets independently.
11. `DailyBoardSnapshotBuilderTest.noDdlTodosStayInTodayTodosOnLaterDates` confirms a no-DDL active todo remains in `今日待办` across later days.
12. Ordinary board/task `TodoUiState` no longer carries the full Planning Desk note list; complete planning notes are collected only while the `规划台` page is open.
13. Phone board announcements, Android widget board rows, and desktop `/api/snapshot` now read only planning notes with announcement hints before strict announcement parsing.
14. `PlanningAnnouncementParserTest.announcementHintHelperMatchesSupportedEntryForms` covers common announcement hint forms and ordinary non-announcement planning text.
15. `planning_notes` now persists `hasAnnouncementHint`, computed from Markdown content when notes are created / edited.
16. `MIGRATION_14_15` adds and backfills `hasAnnouncementHint` for existing planning notes, then creates `index_planning_notes_announcement_lookup` on `archived`, `hasAnnouncementHint`, `updatedAtMillis`, and `createdAtMillis`.
17. Phone board announcements, Android widget board rows, and desktop `/api/snapshot` now use the indexed hint field instead of running `LIKE` scans over complete planning Markdown.
18. Backup export includes the hint for visibility, backup import recomputes the hint from `contentMarkdown`, and desktop planning-note JSON exposes the hint consistently.
19. Version metadata moved to `1.9.16` / `versionCode 210`.

## Verification Status

Completed locally in this continuation:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed.
5. `git diff --check` passed.
6. `output-metadata.json` reports `versionCode=210`, `versionName=1.9.16`, and `outputFile=PaykiTodo-1.9.16-debug.apk`.
7. Emulator smoke on `emulator-5554` installed `app/build/outputs/apk/debug/PaykiTodo-1.9.16-debug.apk`, launched `com.paykitodo.app`, confirmed the Daily Board UI tree contained `每日看板` / `今日待办（0）` / `今日日程（0）` / `明天暂无日程`, and found no `FATAL EXCEPTION` in the checked logcat window.

Remaining after local completion:

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.16-debug.apk` on the user's physical phone.
2. Verify a normal no-DDL todo appears under `今日待办`, does not enable reminders, and does not enable recurrence.
3. Verify Planning Desk plain bullets import as no-DDL todos and appear under `今日待办`; preview should show the no-DDL explanation.
4. Verify Android widget, phone daily board, desktop daily board, and desktop todo list all show no-DDL todos in today's todo block.
5. Verify desktop sync auto-disables after 5 minutes if no authorized desktop connection occurs.
6. Verify desktop sync stays enabled if the desktop browser connects with the correct token within 5 minutes.
7. Verify Settings -> AI 调用配置 provider changes persist after leaving/reopening the page.
8. Verify desktop browser lightweight snapshot flow: initial `看板轻量数据`, full-data transition, and create/edit/delete refresh behavior.
9. Verify `AI 报告` paging and notification deep-link detail opening on a real device with enough reports.
10. Verify active Planning Desk announcements still appear on phone board, Android widget, and desktop daily board after upgrading through `MIGRATION_14_15`; this proves the indexed `hasAnnouncementHint` backfill is correct on a real user database.
11. Do not push unless the user explicitly asks.

## Performance Notes

Fixed locally across `1.9.12`-`1.9.16`:

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
11. Changed phone board, widget, and desktop board announcements to query only announcement-candidate planning notes before parsing.
12. Persisted Planning Desk announcement hints and indexed them, so board/widget/desktop announcement candidate queries no longer scan full Markdown bodies with `LIKE`.

Worth doing later:

1. Split desktop management APIs further for very large histories: separate todos/events/planning endpoints or pagination.
2. Add AI-report search/date-range filters if the archive becomes too large for simple paging.
3. Profile calendar drag/add/edit/delete on a real device with a large event set; this round reduced redundant observation/grouping but did not run full runtime profiling.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/assets/wiki/index.html`
- `app/src/main/java/com/example/todoalarm/data/PlanningAnnouncementParser.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningNote.kt`
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningRecognitionService.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/AiReportPanel.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
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

Latest recorded emulator use:

- Device id: `emulator-5554`
- Installed APK: `app/build/outputs/apk/debug/PaykiTodo-1.9.16-debug.apk`
- Checked flows: app launch, Daily Board UI tree, and logcat fatal-crash scan
- Verified result: MainActivity displayed, UI tree contained `每日看板` / `今日待办（0）` / `今日日程（0）` / `明天暂无日程`, and no `FATAL EXCEPTION` was found in the checked logcat window
- Boundary: this emulator pass does not replace real-phone verification for OEM notification, vibration, lock-screen, widget, alarm, reboot, battery-management behavior, Android launcher widget rendering, Planning Desk announcement migration on the user's real database, or live desktop-browser verification of `1.9.16`
