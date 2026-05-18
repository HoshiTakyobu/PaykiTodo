# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- Active goal: implement `docs/goals/2026-05-18-paykitodo-1.11.0-revised-goal.md` from the current `1.10.3` / `versionCode 221` baseline, plus the user's extra Android widget requirements.
- Already completed baseline widget slice:
  1. `今日看板` widget removes the top menu/title/date header.
  2. `今日看板` widget and `倒数日` widget refresh through provider-owned minute ticks instead of relying on `updatePeriodMillis`.
  3. Independent `倒数日` widget is a scrollable RemoteViews `ListView` with larger daily-board-style rows, dynamic accent strips, multi-line text, and direct row deep links.
- Current completed `1.11.0` slice:
  1. Former focus / pomodoro mode has been removed from app code, manifest, UI routes, settings, widgets, backup, desktop sync, and AI reports.
  2. Database version is now `18` in the working tree.
  3. `MIGRATION_17_18` drops `focus_sessions`, creates `event_check_ins`, creates `todo_group_tags`, adds `checkInEnabled` and `totalCheckInMinutes`, backfills group tags, and merges the old default `专注` group into `例行`.
  4. Room schema export is enabled and schema `18.json` is generated.
  5. AI report retention, debug-only Compose preview tooling, and application-scope non-fatal startup logging are implemented.
  6. Drawer navigation now shows a single-line `待办`, with group filtering and group maintenance moved into the todo page chip bar.
  7. Todo multi-group relationships are implemented across phone UI, repository queries, backup / restore, desktop sync, and desktop Web todo management.
  8. Event check-in data and desktop-sync API foundations are implemented: events can persist `checkInEnabled`, check-in records can be created / checked out / totaled, backup includes `eventCheckIns`, and desktop sync exposes check-in endpoints.
  9. Phone calendar-event editor now exposes the `打卡追踪` switch and preserves `checkInEnabled` when events are moved.
  10. Phone calendar-event details sheet now has a `打卡追踪` card for enabled events, with record loading, total invested time, active check-in status, `签到`, and `签退`.
  11. Full-screen event reminders and the accessibility fallback reminder overlay show `签到` for check-in-enabled events; signing in acknowledges and closes the current reminder.
  12. Phone daily-board in-progress schedule rows show check-in status for enabled events and expose compact `签到` / `签退` actions.
  13. Android `今日看板` widget in-progress schedule rows show active check-in status without launcher-side sign-in / sign-out buttons.
  14. Settings -> `日历与提醒` stores the event check-in behavior preferences `日程结束时自动签退` and `完成日程时显示投入统计`; both default on and are preserved in backup / restore.
  15. Calendar event details can complete check-in-enabled events, auto-checkout active records according to settings, and show planned/actual/check-in-count/investment-rate completion statistics when enabled.
  16. Desktop sync item completion now uses the same event-completion path and can return `eventCheckInSummary`.
  17. AI daily reports now include today's event check-in investment minutes in both the AI prompt and local fallback report.
  18. Desktop Web event editor exposes `打卡追踪`, and the event preview sheet can load check-in records plus perform `签到` / `签退`.
  19. Phone Planning Desk shortcut toolbar is now reduced to `子任务` and `公告`; task, DDL, reminder, group, date, and schedule input remain natural-text / tag parser workflows instead of visible shortcut buttons.
  20. AI daily/weekly reports, schedule-template saving, and desktop Planning Desk note operations now use narrower DAO queries instead of full-table or full-note-list scans.
  21. Desktop sync business request handling now uses suspend route handlers; the only remaining `runBlocking` is the intentional socket-thread response boundary in `DesktopSyncServer`.
  22. Phone Planning Desk now supports `更多 -> 从图片识别日程`, using only vision-capable AI providers, appending recognized Markdown to the active note, and preserving the existing preview/import gate before database writes.
  23. Full `1.11.0 / versionCode 222` version bump is still pending.
- Latest published signed release APK:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk`
  - GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.10.2`
- Latest locally built APKs:
  - Debug: `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk`
  - Release: `app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk`
- Do not push to GitHub unless the user explicitly asks.
- Keep `keystore.properties`, `release/`, APK/AAB outputs, API keys, tokens, and private Base URLs out of Git.

## Latest Widget Fix Pass

1. `widget_todo.xml` and `widget_todo_preview.xml` no longer contain the menu icon / `每日看板` / date header.
2. `TodoWidgetProvider` schedules minute ticks through `AlarmManager`, refreshes the `widget_list` collection, and `widget_todo_info.xml` declares `updatePeriodMillis=0`.
3. `CountdownWidgetProvider` binds a `ListView` to `CountdownWidgetService` and refreshes `widget_countdown_list` on minute ticks.
4. `CountdownWidgetService` loads active countdown targets through the existing board-range repository path and renders scrollable rows with dynamic accent color, larger countdown text, multi-line title/meta text, and exact todo/event deep links.
5. `widget_countdown_preview.xml` shows the new daily-board-style row layout.

## Latest Focus Removal Pass

1. Deleted former focus implementation files and XML resources.
2. Removed focus manifest components and widget metadata.
3. Removed focus settings, settings-store fields, repository methods, DAO methods, backup models, and backup serialization.
4. Removed focus drawer route, dashboard section, todo long-press action, and dashboard cards.
5. Removed focus fields from desktop sync board payloads.
6. Removed focus stats from AI daily/weekly report generation.
7. The old default group label `专注` is no longer seeded; old rows are migrated to `例行`.

## Latest Performance / Robustness Pass

1. Room schema export is enabled through KSP and `app/schemas/com.example.todoalarm.data.AppDatabase/18.json` is present as the database-18 reference.
2. `androidx.compose.ui:ui-tooling-preview` is scoped to `debugImplementation`, so release builds do not carry that preview dependency.
3. Settings -> `AI 调用配置` -> `AI 日报 / 周报` has a compact `报告保留时长` dropdown: 30 天 / 90 天 / 365 天 / 永久.
4. Generating a daily or weekly AI report purges archived reports older than the selected retention period; `永久` skips automatic deletion.
5. Backup / restore preserves the report-retention preference but still does not export AI API Keys.
6. App startup initialization uses an application-level `SupervisorJob` scope and records non-fatal initialization failures through `CrashLogger.recordNonFatal`.
7. Desktop sync already has the 4 MB request-body limit from the previous sync hardening; oversized requests return HTTP 413 and this should be treated as satisfied for P11.

## Latest Drawer / Todo Navigation Pass

1. Drawer entry `我的任务` was renamed to single-line `待办`.
2. The drawer no longer expands a group list and no longer exposes a standalone `分组管理` section.
3. The todo page now renders `TodoFilterBar` above the existing `已错过 / 今日待办 / 计划中` sections.
4. `TodoFilterBar` supports `全部`, sorted group chips, `新建`, tap-to-filter / tap-selected-to-clear, and long-press group edit / delete actions.
5. Planning Desk tutorial text, README, TODO, Wiki, and current-state docs were updated to describe `待办` and in-page group management.

## Latest Multi-Group Todo Pass

1. `todo_group_tags` is now the persistent multi-group relation for active todos; existing `todo_items.groupId` values are backfilled into the join table during migration and old backup restore.
2. Phone-side todo filters now support multi-select intersection semantics: selecting multiple group chips shows only todos that belong to every selected group.
3. Phone-side todo editing uses compact multi-select group chips and still stores the first selected group as the primary `groupId` for color / compatibility.
4. Backup export / import includes `todoGroupTags`; old backups without explicit tags are restored by rebuilding tags from each todo's legacy `groupId`.
5. Desktop sync `/api/todos` and `/api/snapshot` expose todo `groupIds`; create / update APIs accept `groupIds` and keep primary `groupId` aligned with the first selected group.
6. Desktop Web todo management has compact multi-select group filters, multi-select group chips in the todo editor, and multi-group labels in cards, previews, and daily-board todo rows.

## Latest Event Check-In Data / API Pass

1. Calendar-event drafts and saved events now carry `checkInEnabled`; editing an existing event preserves its accumulated `totalCheckInMinutes`.
2. Repository support exists for event check-in, check-out, active-record lookup, per-event records, per-event total recomputation, and today's total event-check-in minutes.
3. Deleting events clears related `event_check_ins` rows.
4. Backup JSON now exports / imports `eventCheckIns`, plus `checkInEnabled` and `totalCheckInMinutes` on todo/event rows.
5. Desktop sync event JSON exposes `checkInEnabled` and `totalCheckInMinutes`.
6. Desktop sync exposes initial event check-in endpoints: `GET /api/events/{id}/check-ins`, `POST /api/events/{id}/check-in`, and `POST /api/events/{id}/check-out`.

## Latest Phone Event Editor Check-In Pass

1. Phone calendar-event editor groups `倒数日` and `打卡追踪` under `日程标记`.
2. The `打卡追踪` switch persists into `CalendarEventDraft` when creating or editing an event.
3. Moving an event preserves `countdownEnabled` and `checkInEnabled` instead of resetting optional event markers.

## Latest Phone Event Details Check-In Pass

1. Calendar event details bottom sheet shows a `打卡追踪` card only for events with `checkInEnabled = true`.
2. The card loads event check-in records, shows total invested time, marks active records as `签到中`, and lists closed / active segments.
3. The card can perform `签到` and `签退`, then refresh both the displayed event statistics and record list.

## Latest Full-Screen Reminder Check-In Pass

1. Full-screen event reminders show a `签到` action when the event has `打卡追踪` enabled.
2. Tapping `签到` starts or reuses the active event check-in, acknowledges the current event reminder, clears reminder artifacts, and closes the reminder screen.
3. The accessibility fallback reminder overlay exposes the same `签到` action and closes itself after successful sign-in.

## Latest Phone Daily-Board Check-In Pass

1. In-progress schedule rows on the phone daily board show `未签到` for check-in-enabled events with no active record.
2. In-progress schedule rows show `签到中 · 已 Xm` while a check-in is active.
3. The in-progress row exposes a compact `签到` / `签退` action and refreshes the row state after a successful operation.

## Latest Android Widget Check-In Status Pass

1. `今日看板` widget now batch-loads active check-in records for visible in-progress calendar events.
2. In-progress widget schedule rows with an active check-in show `⏱ 签到中 Xm` in the event accent color.
3. Widget check-in state is display-only; the launcher widget still does not expose sign-in / sign-out buttons.

## Latest Event Check-In Settings Pass

1. Settings -> `日历与提醒` now includes compact switches for `日程结束时自动签退` and `完成日程时显示投入统计`.
2. Both switches default to on and persist through `AppSettingsStore`.
3. Backup JSON export / import preserves both event check-in behavior preferences.

## Latest Event Completion Statistics Pass

1. Check-in-enabled calendar events now expose a `完成日程` action in the event details sheet.
2. Completing a tracked event marks it complete through the repository, clears reminder artifacts, and triggers auto-backup.
3. If `日程结束时自动签退` is enabled, completion automatically checks out any active event check-in before totals are calculated.
4. If `完成日程时显示投入统计` is enabled, the phone details sheet shows planned minutes, actual invested minutes, check-in count, investment rate, and automatic-checkout status.
5. Desktop sync `/api/items/{id}/complete` uses the same completion logic and returns `eventCheckInSummary` when statistics display is enabled.

## Latest AI Daily Report Event-Investment Pass

1. Daily-report context now reads today's event check-in total from `event_check_ins`.
2. The AI daily-report prompt includes `今日日程投入：Y 分钟`.
3. The local daily-report fallback also writes today's event investment minutes, so the value remains visible when AI is disabled or fails.

## Latest Desktop Web Event Check-In UI Pass

1. Desktop Web event editor now includes a `打卡追踪` checkbox and sends `checkInEnabled` during event create / update.
2. Desktop Web event preview now renders a `打卡追踪` card for enabled events, including total invested time, active segment state, and closed / active record rows.
3. The preview card can call existing desktop sync endpoints to `签到` / `签退`, then reload the record card.
4. Events without check-in tracking show `未开启` and do not expose sign-in controls.

## Latest Planning Desk Shortcut Simplification Pass

1. Phone Planning Desk shortcut toolbar now exposes only `子任务` and `公告`.
2. The toolbar no longer shows task, indent/outdent, DDL, schedule, reminder, group, today, or tomorrow shortcut buttons.
3. Planning Desk tutorial, README, in-app Wiki, feature ledger, design doc, and examples now describe natural-text / tag input as the main path for tasks, DDL, reminders, groups, dates, and schedules.
4. The parser and preview workflows are unchanged; this pass reduces visible UI density and prevents the shortcut bar from becoming a button grid again.

## Latest Narrow Query Performance Pass

1. `TodoDao` and `TodoRepository` expose one-shot range queries for completed todos, missed todos, active DDL-backed todos, and overlapping active events.
2. AI daily-report context collection reads only today / tomorrow windows instead of loading every todo and filtering in memory.
3. AI weekly-report context collection reads the current week and next DDL window through range queries instead of scanning the full todo table.
4. Saving a week as a schedule template now queries active events overlapping the selected week before applying the existing week-overlap semantics.
5. Desktop sync Planning Desk note update and mapping refresh read a single planning note by ID instead of loading the full planning-note list and filtering it.
6. `widget_countdown_info.xml` already declares `android:updatePeriodMillis="0"`, so P10 is satisfied by the existing provider-owned minute-refresh design rather than a new XML change.

## Latest Desktop Sync Suspend Handler Pass

1. `DesktopSyncServer` accepts a suspend request handler and wraps it exactly once at the per-client socket thread where a response must be written synchronously.
2. `DesktopSyncCoordinator.handleRequest` and repository-backed desktop routes are suspend functions.
3. Snapshot, todo/event CRUD, event check-in, item completion/cancel/delete, Planning Desk notes/import/refresh/postpone/undo/conflict, group resolution, reminder cleanup, and group-tag lookup call repository suspend APIs directly.
4. Static search should show no `runBlocking` in `DesktopSyncCoordinator.kt`; `DesktopSyncServer.kt` keeps the single socket-boundary `runBlocking`.

## Latest Planning Desk Image Recognition Pass

1. `PlanningAiProvider` now persists `supportsVision`; Settings -> `AI 调用配置` provider editing exposes a compact `此服务支持图片识别` switch and provider cards show when image recognition is enabled.
2. `PlanningAiCaller.callVisionWithFallback` sends OpenAI-compatible vision chat-completion messages only through enabled, complete, vision-capable providers while preserving endpoint fallback behavior.
3. Phone Planning Desk overflow menu now includes `从图片识别日程`; it opens the Android image picker, compresses the selected image to a long side no greater than 1600px as JPEG quality 80, and rejects oversized/failing images with `图片过大，请裁剪后重试`.
4. Image recognition uses the fixed timetable/schedule prompt, shows the non-cancel `AI 识别中…可能需要 10-30 秒` progress dialog, appends non-empty returned Markdown to the current note, moves the cursor to the end, and tells the user to use the existing `识别` button for preview import.
5. Missing vision-capable providers, empty AI output, compression failure, and network/API failure all surface as user-facing toasts without creating official todos/events or writing partial database items.

## Previous 1.10.3 Planning Desk Fix Pass

1. Local Planning Markdown parsing recognizes inline `@地点`, quoted `"@地点"`, and `地点：...` event locations.
2. Natural event candidates default `createLinkedTodo=false`.
3. AI recognition defaults event `createLinkedTodo=false` and the prompt only permits linked todos when the user explicitly asks.
4. AI result cleanup moves `@地点` out of event titles when the model forgot the `location` field.
5. Phone-side and desktop-sync Planning Desk linked todo creation no longer adds a fixed generated note.
6. Desktop-web Planning Desk preview uses the explicit label `同步创建以日程结束时间为 DDL 的待办任务`.
7. Desktop-web event editor renders the same eight compact color swatches as the phone editor while preserving custom color input.
8. Tests were added for location extraction, default-off linked todos, AI cleanup, and group sanitization.

## Verification Status

Completed locally for `1.10.3`:

Widget continuation:

1. `./gradlew.bat :app:compileDebugKotlin` passed.
2. Static search confirmed removed widget header IDs and old fixed countdown row IDs are no longer referenced in `app/src/main/java` or `app/src/main/res`.
3. `git diff --check` passed.
4. `./gradlew.bat :app:assembleDebug` passed.
5. `app/build/outputs/apk/debug/output-metadata.json` confirms `1.10.3 / 221`.

Previous Planning Desk continuation:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:testDebugUnitTest` passed.
3. `git diff --check` passed.
4. `./gradlew.bat :app:assembleRelease` passed.
5. `app/build/outputs/apk/release/output-metadata.json` confirms `1.10.3 / 221`.
6. `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk` passed with one v2 signer.
7. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk` confirmed local signing material and APK outputs stay ignored.
8. After QQ released the old debug APK handle, `./gradlew.bat :app:assembleDebug` passed.
9. `app/build/outputs/apk/debug/output-metadata.json` confirms `1.10.3 / 221`.

Focus removal slice:

1. `./gradlew.bat :app:compileDebugKotlin` passed after code cleanup.
2. Fresh `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` passed after docs were updated.
3. No new APK has been built after focus removal yet.

Performance / robustness slice:

1. `./gradlew.bat :app:compileDebugKotlin` passed after schema export, report retention, and startup-scope changes.
2. `git diff --check` passed.
3. No new APK has been built for this slice yet.

Drawer / todo navigation slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after moving group filtering / management into the todo page.
2. Fresh `git diff --check` passed after docs sync.
3. No new APK has been built for this slice yet.

Multi-group todo slice:

1. Fresh `node --check app/src/main/assets/desktop-web/app.js` passed after desktop Web multi-group UI changes.
2. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after repository / sync / phone UI / desktop Web multi-group changes.
3. Fresh `git diff --check` passed after docs and Wiki synchronization.
4. No new APK has been built for this slice yet.

Event check-in data / API foundation slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after adding event check-in repository, backup, and desktop-sync API support.
2. Fresh `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

Phone event editor check-in switch slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after adding the phone event-editor check-in switch and preserving marker fields during event moves.
2. Fresh `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

Phone event details check-in operation slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after wiring phone event details to check-in record loading, sign-in, and sign-out operations.
2. Fresh `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

Full-screen reminder check-in slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after adding event check-in to the full-screen reminder activity and accessibility fallback overlay.
2. Fresh `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

Phone daily-board check-in status slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after adding daily-board in-progress event check-in status and compact sign-in / sign-out actions.
2. Fresh `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

Android widget check-in status slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after adding widget active check-in status loading and rendering.
2. Fresh `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

Event check-in settings slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after adding event check-in preference switches and persistence.
2. Fresh `git diff --check` passed after code and docs synchronization.
3. No new APK has been built for this slice yet.

Event completion statistics slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after wiring tracked-event completion, automatic checkout, and completion summary display.
2. Fresh `git diff --check` passed after code and docs synchronization.
3. No new APK has been built for this slice yet.

AI daily report event-investment slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after adding `今日日程投入` to daily-report context and fallback text.
2. Fresh `git diff --check` passed after code and docs synchronization.
3. No new APK has been built for this slice yet.

Desktop Web event check-in UI slice:

1. Fresh `node --check app/src/main/assets/desktop-web/app.js` passed after adding the Web check-in card and operations.
2. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after the desktop Web asset update.
3. Fresh `git diff --check` passed after code and docs synchronization.
4. No new APK has been built for this slice yet.

Planning Desk shortcut simplification slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after removing extra phone shortcut entries.
2. Static search found no remaining phone `PlanningShortcutSpec` entries for task, DDL, schedule, group, today, tomorrow, or indent shortcuts.
3. Fresh `git diff --check` passed after code and docs synchronization.
4. No new APK has been built for this slice yet.

Narrow query performance slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after adding the range-limited DAO / repository queries and replacing the full-table call sites.
2. Fresh `git diff --check` passed after code and docs synchronization.
3. No new APK has been built for this slice yet.

Desktop sync suspend handler slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after converting desktop sync route handling to suspend.
2. Static `runBlocking` search confirms no `runBlocking` remains in `DesktopSyncCoordinator.kt`; only `DesktopSyncServer.kt` keeps the intended socket response boundary.
3. Fresh `git diff --check` passed after code and docs synchronization.
4. No new APK has been built for this slice yet.

Planning Desk image recognition slice:

1. Fresh `./gradlew.bat :app:compileDebugKotlin` passed after adding Provider vision support, the vision caller, Settings switch, image picker, compression, progress dialog, and append-to-note flow.
2. Static search found the expected `supportsVision`, `callVisionWithFallback`, `从图片识别日程`, image picker, compression helper, and `AI 识别中…可能需要 10-30 秒` surfaces.
3. Fresh `git diff --check` passed after code and docs synchronization.
4. No new APK has been built for this slice yet.

Secret / release safety checks already performed:

1. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk` confirmed local signing material and APK outputs are ignored.
2. `git ls-files` only reports `keystore.properties.example` for keystore-like tracked files.
3. Git history search found no committed real keystore/JKS/env/private-key files or common live token prefixes; keyword-like matches are code identifiers, templates, placeholder examples, or test URLs.

## Remaining Device / Browser Verification

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk` on the user's phone if validating the latest built widget APK.
2. Add / resize the `今日看板` widget and confirm the top header is gone, content remains readable, cross-day / minute refresh updates the list, and an active checked-in in-progress event shows `⏱ 签到中 Xm`.
3. Add / resize the `倒数日` widget and confirm scroll behavior, multi-line row readability, and exact todo/event row deep links.
4. Verify Planning Desk imports for `@地点`, quoted `@地点`, and `地点：...`.
5. Verify ordinary event import creates only an event.
6. Verify manually enabled linked todo uses event end time as DDL and has no fixed generated note.
7. Verify desktop-web event color swatches save correctly.
8. After a focus-removal APK is built, confirm no user-facing focus / pomodoro entry remains in app UI, widgets, desktop web, backup, or AI reports.
9. Verify drawer / todo navigation on device: drawer has single-line `待办`, no expanded group list, no standalone `分组管理`, and the todo page chip bar can filter, create, edit, and delete groups.
10. Verify multi-group todos on device: creating / editing a todo with multiple groups preserves all selected chips after reopening, selecting multiple group filters uses intersection semantics, and group edit / delete does not silently drop unrelated memberships.
11. Verify multi-group todos in desktop browser: `/api/todos` exposes `groupIds`, the editor preserves all selected groups, cards / previews / board rows show all group labels, and selecting multiple filter chips uses intersection filtering.
12. Verify backup / restore: fresh backups include `todoGroupTags`, restored data keeps all selected groups, and old backups without `todoGroupTags` backfill from each todo's `groupId`.
13. Verify event-reminder check-in on device: create a check-in-enabled event reminder, trigger the full-screen reminder, tap `签到`, and confirm the reminder closes while the event details / board show an active check-in.
14. Verify tracked-event completion on device: complete a check-in-enabled event with an active check-in, confirm automatic checkout, completion summary values, and final total invested minutes.
15. In desktop browser, verify event check-in end to end: enable check-in for an event, sign in / sign out multiple times from the preview card, confirm total minutes, backup / restore `eventCheckIns`, and test the desktop check-in endpoints.
16. Verify Planning Desk image recognition on device: mark a real vision model Provider as `此服务支持图片识别`, pick a course/schedule image through `更多 -> 从图片识别日程`, confirm Markdown is appended to the current note, then run the normal `识别` preview flow.

## Performance Notes

- Countdown data still uses the existing `countdownEnabled` marker and indexed board/widget lookup paths.
- The independent `倒数日` widget is no longer limited to three fixed rows; it is now a scrollable RemoteViews ListView capped at 60 active countdown targets.
- Minute-level widget refresh is scheduled by `AlarmManager`; OEM launchers and battery policies may still delay widget refreshes under aggressive power management.
- Removing focus mode reduces board/settings/widget/report data surface before adding event check-in.
- Schema export gives future migrations a concrete Room reference file for database version 18.
- AI report retention prevents the `ai_reports` archive from growing without limit once reports are generated regularly.
- AI daily/weekly report generation now uses range-limited todo/event reads; schedule-template saving uses a week-overlap event query; desktop Planning Desk note operations use single-note lookup by ID.
- Desktop sync business routes now call suspend repository APIs directly; only the socket client thread boundary uses `runBlocking` while waiting to write an HTTP response.

## Files Most Relevant To The Current Goal

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/TodoApplication.kt`
- `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
- `app/src/main/java/com/example/todoalarm/data/EventCheckIn.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoGroupTag.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoItem.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupModels.kt`
- `app/src/main/java/com/example/todoalarm/data/DailyReportGenerator.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiCaller.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiProvider.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncModels.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoCards.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetProvider.kt`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/wiki/index.html`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/*`

## Known Worktree Notes

- Branch is `main`; do not push without user authorization.
- Goal prompt files under `docs/goals/` should remain separate archive commits after secret scanning.
- `keystore.properties`, `release/`, APK/AAB outputs, and private signing material are ignored and must stay out of commits.
- Version numbers must be written exactly as `v1.10.3`, `1.10.3`, or `1.11.0 / versionCode 222`; never write Chinese-numeral forms.

## Android Emulator Verification Rule

If an Android Emulator is started or reused, record:

- device id / AVD name
- installed APK path
- checked screens or flows
- what was actually verified

Do not overclaim emulator results. A real phone is still required for OEM notification, vibration, lock-screen, widget, alarm, reboot, battery-management behavior, Android launcher widget rendering, Planning Desk announcement migration on the user's real database, and live desktop-browser verification.
