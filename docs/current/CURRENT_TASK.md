# Current Task

## Active Development Focus

The current round is PaykiTodo `1.9.23` / `versionCode 217`.

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
8. Hardened the no-DDL todo classification invariant:
   - active no-DDL todos are classified through a shared `classifyActiveTodoItems` helper.
   - My Tasks and the ordinary board/task UI now use the same missed / today / upcoming split.
   - unit tests cover that no-DDL active todos stay in `今日待办` on later dates and never become upcoming.
9. Reduced phone Calendar subscription scope:
   - the Calendar page reports its visible date range to the ViewModel.
   - the ViewModel observes only active events overlapping that padded visible window instead of every active event.
   - deep links to a far event expand the queried range around the target date before focusing the calendar.
10. Version metadata moved to `1.9.21` / `versionCode 215`.
11. Emulator UI review found the phone Calendar top bar still using the English label `Schedule`; the title is now localized to `日历`.
12. User real-device screenshot found the Android launcher widget header date could show today while list rows still showed yesterday's greeting/date and old focus content. The widget provider now refreshes the collection list on widget update, date/time/timezone changes, and app replacement, and its RemoteAdapter cache key includes the current date.
13. Widget light-mode cards now use more opaque warm surfaces and darker text/accent colors to avoid unreadable white-on-white cards over the board background.
14. Widget event locations now display the saved location exactly as stored instead of prepending `@`, so `@主楼B1-412` no longer becomes `@@主楼B1-412`.
15. Desktop sync enabling now immediately starts / self-starts the LAN server and the Settings copy explains short "正在启动" states instead of only saying the service is not running.
16. README / CHANGELOG / TODO / Wiki / current docs are being synchronized for this `1.9.23` pass.
17. Release-readiness cleanup started:
   - old versioned docs were moved to `docs/archive/historical/`.
   - safe templates now live under `docs/templates/`.
   - release signing uses ignored root-level `keystore.properties` plus ignored `release/` keystore output.
   - Gradle release builds now fail clearly if signing is incomplete.
18. Release packaging completed after the user filled local `keystore.properties`:
   - generated ignored `release/PaykiTodo-release.jks`.
   - removed stale `drawable-night/widget_focus_action_background.xml` left from the deleted widget focus-card path.
   - built and verified `app/build/outputs/apk/release/PaykiTodo-1.9.23-release.apk`.

## Verification Completed This Round

Completed locally:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat :app:testDebugUnitTest` passed.
4. `./gradlew.bat assembleRelease` passed.
5. `G:\Android\SDK\build-tools\37.0.0\apksigner.bat verify --verbose --print-certs app\build\outputs\apk\release\PaykiTodo-1.9.23-release.apk` passed.
6. `git diff --check` passed.
7. release `output-metadata.json` reports `versionCode=217`, `versionName=1.9.23`, and `outputFile=PaykiTodo-1.9.23-release.apk`.
8. `TodoItemSectionsTest` covers no-DDL active todos staying under today across dates and never entering upcoming.
9. Debug packaging note: `:app:assembleDebug` was blocked by a Windows file lock on the previous `PaykiTodo-1.9.22-debug.apk`; use the signed release APK from this round for distribution.
10. `git check-ignore -v` confirms `keystore.properties`, `release/PaykiTodo-release.jks`, and built APK outputs are ignored.
11. Emulator smoke on `emulator-5554` installed `app/build/outputs/apk/debug/PaykiTodo-1.9.21-debug.apk`, launched MainActivity, dumped Daily Board / drawer / Calendar UI trees, captured screenshots, confirmed the Calendar top bar shows `日历` rather than `Schedule`, and found no PaykiTodo `FATAL EXCEPTION` in the checked logcat window.

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/release/PaykiTodo-1.9.23-release.apk` on the physical phone.
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
   - after midnight, manual date/time change, timezone change, or app replacement, the header date and list-card dates should update together rather than showing different days.
   - light-mode text should remain readable over the widget background; if the existing launcher instance still shows old focus content after upgrading, remove and re-add the widget once to clear launcher-side stale RemoteViews.
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
5. Android widget collection refresh now handles date/time/timezone/app-replacement events and uses a date-specific adapter cache key.
6. Android widget light-mode cards and text colors have stronger contrast.
7. Existing phone-side performance work remains in place: active-todo-only main observation, today focus aggregate, range-scoped board events, paged/searchable AI reports, indexed Planning Desk announcement lookup, and off-main-thread Planning Desk parsing.
8. Desktop sync now avoids unbounded client thread growth and reads request bodies by UTF-8 byte length.
9. Calendar timeline date handling avoids creating a 1461-item date list and lookup map on entry.

Still recommended later:

1. Profile calendar drag/add/edit/delete on a real device with a large event set.
2. Browser-profile desktop todo pagination/search with a large real database to tune page size and indexing if needed.
3. If AI-report keyword search becomes a bottleneck, consider a real FTS table rather than adding more `LIKE '%q%'` scans.
4. Continue physical-device verification for widget, OEM notification, vibration, lock-screen reminder, exact-alarm, and battery-policy behavior.

## Immediate Practical Next Steps

1. Install `app/build/outputs/apk/release/PaykiTodo-1.9.23-release.apk` on the physical phone.
2. Browser-test desktop Web with the new paged/searchable todo management list.
3. Verify the Android widget picker preview and live resized widget on the user's launcher.
4. Do not push unless the user explicitly asks.
5. Keep `keystore.properties`, `release/`, and built APK artifacts out of Git unless the user explicitly changes that policy.
