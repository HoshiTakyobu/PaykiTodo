# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.23` / `versionCode 217`.
- Main user request: review the recent `1.9.11`+ experience / performance work, identify remaining user-facing issues, and implement practical performance improvements where safe.
- This continuation keeps the no-DDL, widget, desktop lightweight snapshot, AI-provider save-state, calendar-index, phone screen-scoped subscription, Planning Desk parser threading, AI-report paging, Planning Desk announcement indexing, desktop-sync hardening, AI-report filtering, calendar date-window, visible-range phone Calendar, and Calendar top-bar localization baseline, then fixes Android launcher widget date/list refresh and light-mode readability.
- Latest signed release APK after packaging: `app/build/outputs/apk/release/PaykiTodo-1.9.23-release.apk`.
- Do not push to GitHub unless the user explicitly asks.
- Current continuation is preparing a safe first-release workflow: historical docs are being archived, signing templates are kept safe to commit, and real signing values stay only in ignored root-level files.

## Latest 1.9.23 Widget Location / Desktop Sync Release Pass

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
22. Phone Calendar top bar now shows `日历` instead of the leftover English `Schedule`.
23. Android widget `onUpdate` now also calls `notifyAppWidgetViewDataChanged`, so the header and `ListView` rows refresh together rather than only updating the static header.
24. Android widget receiver listens for `DATE_CHANGED`, `TIME_SET`, `TIMEZONE_CHANGED`, and `MY_PACKAGE_REPLACED`, then refreshes all widget instances.
25. Android widget RemoteAdapter data URI includes the current date, reducing launcher-side reuse of yesterday's collection factory/cache.
26. Android widget light-mode surfaces were made more opaque and text/accent colors were darkened for better contrast over the light daily-board background.
27. Android widget event locations now render saved text directly; the widget no longer prepends `@` to locations, so user-entered `@地点` remains exactly one `@`.
28. Desktop sync enabling now immediately ensures the LAN server starts, and Settings shows a "正在启动" hint instead of a dead "服务未运行" state while addresses are being prepared.
29. Release lint cleanup removed the stale night-only `widget_focus_action_background` resource left after the widget focus-card removal, allowing `lintVitalRelease` to pass.

## Verification Status

Completed locally in this continuation:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat :app:testDebugUnitTest` passed.
4. `./gradlew.bat assembleRelease` passed.
5. `G:\Android\SDK\build-tools\37.0.0\apksigner.bat verify --verbose --print-certs app\build\outputs\apk\release\PaykiTodo-1.9.23-release.apk` passed.
6. `git diff --check` passed.
7. Unit tests include `TodoItemSectionsTest`, covering no-DDL active todos staying in today across dates.
8. release `output-metadata.json` reports `versionCode=217`, `versionName=1.9.23`, and `outputFile=PaykiTodo-1.9.23-release.apk`.
9. `git check-ignore -v` confirms `keystore.properties`, `release/PaykiTodo-release.jks`, and built APK outputs are ignored.
10. Debug packaging note: `:app:assembleDebug` was blocked by a Windows file lock on the previous `PaykiTodo-1.9.22-debug.apk`; use the signed release APK for distribution.

Latest emulator smoke installed `app/build/outputs/apk/debug/PaykiTodo-1.9.21-debug.apk` on `emulator-5554`, checked app launch, Daily Board UI tree, drawer UI tree, Calendar UI tree, screenshot capture, and a PaykiTodo fatal-crash logcat scan. MainActivity displayed, Daily Board showed `今日待办（0）` / `今日日程（0）`, drawer showed primary entries, Calendar displayed `2026年5月` with timeline content, Calendar top bar showed `日历` rather than `Schedule`, and no PaykiTodo `FATAL EXCEPTION` was found in the checked logcat window. The `1.9.23` widget change has compile/build verification, but final launcher rendering still needs physical-device testing.

## Release Signing / Docs Archive Continuation

This continuation moves old versioned root docs to `docs/archive/historical/`,
adds `docs/README.md`, moves the release-signing explanation template to
`docs/templates/PaykiTodo-Release-Signing-Template.md`, and keeps actual signing
values out of Git through ignored root-level `keystore.properties` plus ignored
`release/` output.

The Gradle release build reads `keystore.properties` only when release tasks are
requested. If any required field is blank / still a placeholder, or if the
configured keystore file is missing, release tasks fail with a clear message
instead of producing an unsigned or wrong-signed release APK.

For this round, the user filled local `keystore.properties`, `release/PaykiTodo-release.jks`
was generated locally, `assembleRelease` passed, and `PaykiTodo-1.9.23-release.apk`
was verified with `apksigner`. These files remain ignored and must not be committed.

Important: do not commit `keystore.properties`, `release/`, or built APK/AAB
artifacts unless the user explicitly changes that policy.

## Remaining Device / Browser Verification

1. Install `app/build/outputs/apk/release/PaykiTodo-1.9.23-release.apk` on the physical phone.
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
   - after midnight, manual date/time change, timezone change, or app replacement, the widget header date and list-card date/greeting should update together;
   - light-mode cards/text should remain readable over the board background; if the launcher keeps old focus-card rows after app upgrade, remove and re-add the widget once to clear stale launcher state.
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
12. Keep `keystore.properties`, `release/`, and generated APK/AAB artifacts out of Git unless the user explicitly changes that policy.

## Performance Notes

Fixed locally across `1.9.12`-`1.9.23`:

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
17. Localized the phone Calendar top-bar title from `Schedule` to `日历`.
18. Fixed Android widget stale list refresh across date/time/timezone/app-replacement events and made light-mode widget cards/text more readable.

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
- `keystore.properties`, `release/`, APK/AAB outputs, and private signing material are ignored and must stay out of commits.

## Android Emulator Verification Rule

If an Android Emulator is started or reused, record:

- device id / AVD name
- installed APK path
- checked screens or flows
- what was actually verified

This avoids confusion when an emulator window remains on the user's desktop.

Do not overclaim emulator results. A real phone is still required for OEM notification, vibration, lock-screen, widget, alarm, reboot, battery-management behavior, Android launcher widget rendering, Planning Desk announcement migration on the user's real database, and live desktop-browser verification.
