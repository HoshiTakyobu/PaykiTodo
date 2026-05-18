# Current Task

## Active Development Focus

Active goal: implement `docs/goals/2026-05-18-paykitodo-1.11.0-revised-goal.md` from the current `1.10.3 / versionCode 221` baseline, plus the user's additional Android widget requirements.

Do not push to GitHub unless the user explicitly asks.

## Completed Goal Slices

### Android widget slice

1. `今日看板` widget removed the top menu/title/date header.
2. `今日看板` widget uses provider-owned minute refresh instead of relying on the system `updatePeriodMillis` floor.
3. Independent `倒数日` widget was converted to a scrollable `RemoteViewsService` / `ListView` widget instead of three fixed rows.
4. Independent `倒数日` widget rows use daily-board-style soft cards, dynamic accent strips, larger readable countdown text, multi-line title/meta text, and direct todo/event deep links.
5. `倒数日` widget picker preview matches the new card structure.

### D1-D10 focus / pomodoro removal slice

1. Removed `FocusActivity`, `FocusSession`, `FocusWidgetProvider`, focus widget layouts, and focus widget XML.
2. Removed `FocusActivity` and `FocusWidgetProvider` manifest declarations.
3. Removed focus entities, DAO methods, repository APIs, settings fields, and backup export/import fields.
4. Removed focus entry points from the drawer, dashboard body, settings, todo long-press menu, and board UI.
5. Removed focus stats from desktop sync and AI report generation.
6. Database version is now `18` in the working tree.
7. `MIGRATION_17_18` drops `focus_sessions`, creates `event_check_ins`, creates `todo_group_tags`, adds `checkInEnabled` and `totalCheckInMinutes` to `todo_items`, backfills todo group tags, and merges the old default `专注` group into `例行`.
8. Old backup JSON that still contains `focusSessions` is ignored instead of being restored.

### P4/P5/P11/P12/P13 performance / robustness slice

1. Room schema export is enabled and `app/schemas/com.example.todoalarm.data.AppDatabase/18.json` records the database-18 schema.
2. Compose `ui-tooling-preview` is now debug-only instead of being included in release dependencies.
3. Desktop sync keeps the existing 4 MB request-body limit and returns HTTP 413 for oversized requests.
4. Application startup initialization now uses an application-level `SupervisorJob` scope with non-fatal exception logging through `CrashLogger.recordNonFatal`.
5. Settings -> `AI 调用配置` -> `AI 日报 / 周报` adds a report-retention dropdown for 30 days / 90 days / 365 days / forever.
6. Generating a daily or weekly report purges older archived AI reports according to the selected retention policy.
7. Backup / restore preserves the AI report retention policy while still excluding AI API Keys.

### M1/M5 drawer / todo group navigation slice

1. Drawer navigation renames `我的任务` to the shorter single-line `待办`.
2. The drawer no longer expands task groups and no longer exposes a standalone `分组管理` section.
3. Group filtering and group management moved into the todo page through a top horizontal chip bar.
4. The todo page chip bar exposes `全部`, sorted group chips, and `新建`; tapping a group filters todos, tapping the selected group clears the filter, and long-pressing a group chip opens edit/delete actions.
5. Planning Desk tutorial text, README, TODO, Wiki, and current-state docs now describe the new `待办` entry and in-page group management path.

### M2/M3/M4/M6/M7 multi-group todo slice

1. `todo_group_tags` is now used as the persistent multi-group relationship for todos, with old single `groupId` data backfilled into the join table.
2. Phone-side todo filtering now supports multi-select group chips with intersection semantics: selecting multiple groups shows only todos that belong to every selected group.
3. Phone-side todo editing now uses compact multi-select group chips while still preserving a primary `groupId` for compatibility.
4. Backup export/import includes `todoGroupTags`; old backups without explicit tags are restored by backfilling from each todo's original `groupId`.
5. Desktop sync `/api/todos` and `/api/snapshot` now expose todo `groupIds`, accept `groupIds` during create/update, and keep the primary `groupId` aligned with the first selected group.
6. Desktop Web todo management now has compact multi-select group filter chips, multi-select group chips in the todo editor, multi-group labels in cards/previews/board rows, and saves `groupIds` without collapsing phone-side multi-group relationships.

### C1/C5/C6 event check-in data / API foundation slice

1. `CalendarEventDraft` and calendar-event persistence now carry `checkInEnabled`, while existing event edits preserve `totalCheckInMinutes`.
2. Repository APIs can create an event check-in, check out the active record, recompute total invested minutes, query event records, and query today's invested event minutes.
3. Deleting events clears their `event_check_ins` rows so orphaned check-in records do not remain.
4. Backup export/import now includes `eventCheckIns`, plus `checkInEnabled` and `totalCheckInMinutes` on event/todo rows.
5. Desktop sync event payloads expose `checkInEnabled` and `totalCheckInMinutes`.
6. Desktop sync now has initial check-in endpoints: `GET /api/events/{id}/check-ins`, `POST /api/events/{id}/check-in`, and `POST /api/events/{id}/check-out`.

### C2 phone event editor check-in switch slice

1. Phone calendar-event editor now shows a compact `打卡追踪` switch in the same `日程标记` block as `倒数日`.
2. Creating or editing an event persists `checkInEnabled` through `CalendarEventDraft`.
3. Dragging / moving an event preserves both `countdownEnabled` and `checkInEnabled` instead of silently resetting these optional markers.

### C3 phone event details check-in operation slice

1. Phone calendar-event details bottom sheet now shows a `打卡追踪` card for events with `checkInEnabled = true`.
2. The details card loads event check-in records, shows total invested time, highlights an active check-in as `签到中`, and lists closed / active segments.
3. The details card can execute `签到` and `签退`, then refresh both the displayed event statistics and check-in records.

### C3 full-screen reminder check-in slice

1. Full-screen event reminders now show a `签到` action when the event has `打卡追踪` enabled.
2. Tapping `签到` starts or reuses the active event check-in, acknowledges the current event reminder, and closes the reminder surface.
3. The accessibility fallback reminder overlay exposes the same `签到` action for check-in-enabled event reminders.

### C5 phone daily-board check-in status slice

1. Phone daily board in-progress schedule rows now show `未签到` for check-in-enabled events with no active record.
2. Phone daily board in-progress schedule rows now show `签到中 · 已 Xm` while a check-in is active.
3. The in-progress row exposes a compact `签到` / `签退` action and refreshes the row state after a successful operation.

### C5 Android widget check-in status slice

1. `今日看板` widget now batch-loads active check-in records for visible in-progress calendar events.
2. In-progress widget schedule rows with an active check-in show `⏱ 签到中 Xm` in the event card using the event accent color.
3. Widget schedule rows remain display-only for check-in state; no sign-in / sign-out buttons are exposed on the launcher widget.

## Verification Completed

### Widget slice

1. `./gradlew.bat :app:compileDebugKotlin` passed.
2. Static search confirmed removed widget header IDs and old fixed countdown row IDs are no longer referenced in `app/src/main/java` or `app/src/main/res`.
3. `git diff --check` passed.
4. `./gradlew.bat :app:assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk`.
5. Debug APK metadata confirms `versionName = 1.10.3`, `versionCode = 221`.

### Focus removal slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after the focus-removal code cleanup.
2. Fresh `./gradlew.bat :app:compileDebugKotlin` passed again after docs were updated.
3. Fresh `git diff --check` passed after docs were updated.
4. No new APK has been built after the focus-removal slice yet.

### P4/P5/P11/P12/P13 performance / robustness slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after the schema / report-retention / startup-scope changes.
2. `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

### M1/M5 drawer / todo group navigation slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after moving group filtering / management into the todo page.
2. `git diff --check` passed after docs sync.
3. No new APK has been built for this slice yet.

### M2/M3/M4/M6/M7 multi-group todo slice

1. `node --check app/src/main/assets/desktop-web/app.js` passed after the desktop Web multi-group UI changes.
2. `./gradlew.bat :app:compileDebugKotlin` passed after the repository / sync / phone UI / desktop Web multi-group changes.
3. `git diff --check` passed after the multi-group slice.
4. No new APK has been built for this slice yet.

### C1/C5/C6 event check-in data / API foundation slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after adding event check-in repository, backup, and desktop-sync API support.
2. `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

### C2 phone event editor check-in switch slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after adding the phone event-editor check-in switch and preserving marker fields during event moves.
2. `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

### C3 phone event details check-in operation slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after wiring phone event details to check-in record loading, sign-in, and sign-out operations.
2. `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

### C3 full-screen reminder check-in slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after adding `签到` to full-screen event reminders and the accessibility fallback overlay.
2. `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

### C5 phone daily-board check-in status slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after adding daily-board in-progress event check-in status and compact sign-in / sign-out actions.
2. `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

### C5 Android widget check-in status slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after adding widget active check-in status loading and rendering.
2. `git diff --check` passed after the slice.
3. No new APK has been built for this slice yet.

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk` on the physical phone if validating the latest built widget APK.
2. Add / resize the `今日看板` widget and confirm the removed top header, minute refresh, and cross-day date/list update behavior on the launcher.
3. Add / resize the `倒数日` widget and confirm scroll behavior, readable multi-line rows, and row deep links on the launcher.
4. After a later focus-removal APK build, confirm the app opens without any focus / pomodoro entry in the drawer, settings, todo long-press menu, desktop web, AI report, or widgets.
5. In a real desktop browser, verify todo multi-group behavior: selecting multiple filter chips uses intersection filtering, editing a todo preserves all selected groups, and the card / preview labels show all groups.

## Remaining 1.11.0 Work

The full goal remains active. Major remaining slices:

1. C3-C7 remaining: event check-in / time tracking still needs completion statistics, settings switches, desktop web UI, and AI report integration.
2. V1-V6: Planning Desk image recognition through vision-capable AI providers.
3. T1-T3: Planning Desk shortcut bar simplification and help update.
4. P6/P7/P9/P10/P8: narrow database queries, countdown widget update metadata, and desktop-sync suspend handler cleanup.
5. P1/P2/P3: R8/resource shrinking, WebP conversion, icon dependency audit, release launch verification, and final APK-size check.
6. Final version bump to `1.11.0 / versionCode 222`.
