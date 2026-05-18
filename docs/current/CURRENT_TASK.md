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

### C6 event check-in settings slice

1. Settings -> `日历与提醒` now includes `日程结束时自动签退` and `完成日程时显示投入统计` switches.
2. Both switches default to on and persist through `AppSettingsStore`.
3. Backup export / import preserves both event check-in behavior preferences.

### C4 event completion statistics slice

1. Calendar-event details now expose `完成日程` only for events with `打卡追踪` enabled.
2. Completing a check-in-enabled event uses the shared repository completion path, marks the event completed, clears reminder artifacts, and triggers auto-backup.
3. If `日程结束时自动签退` is enabled, completing the event automatically checks out any active check-in before calculating totals.
4. If `完成日程时显示投入统计` is enabled, the phone details sheet shows a completion summary card with planned time, actual invested time, check-in count, investment rate, and automatic-checkout status.
5. Desktop sync `/api/items/{id}/complete` now uses the same completion path and returns `eventCheckInSummary` when completion statistics are enabled.

### C7 AI daily report event investment slice

1. AI daily-report context now includes today's event check-in total from `event_check_ins`.
2. The AI daily-report prompt includes `今日日程投入：Y 分钟`.
3. The local daily-report fallback also writes today's event investment minutes, so the field is visible even when AI is disabled or fails.

### C5 desktop web event check-in UI slice

1. Desktop Web event editor now exposes a `打卡追踪` checkbox and persists `checkInEnabled` through the existing event create / update API.
2. Desktop Web event preview now shows a check-in card for check-in-enabled events, including total invested time, active segment state, and closed / active record rows.
3. Desktop Web event preview can call the existing `POST /api/events/{id}/check-in` and `POST /api/events/{id}/check-out` endpoints, then reload the record card.
4. Events without `打卡追踪` enabled show a simple `未开启` row instead of exposing sign-in controls.

### T1-T3 Planning Desk shortcut simplification slice

1. Phone Planning Desk shortcut toolbar now exposes only `子任务` and `公告`.
2. Removed the crowded shortcut entries for task, indent/outdent, DDL, schedule, reminder, group, today, and tomorrow from the phone toolbar.
3. Planning Desk tutorial, README, in-app Wiki, and current design docs now say top-level tasks, DDL, reminders, groups, dates, and schedules should be written naturally or with explicit tags instead of through a button grid.
4. Bottom parser and preview behavior are preserved; this slice only reduces the visible shortcut UI and synchronizes documentation.

### P6/P7/P9/P10 narrow query / widget metadata slice

1. `TodoDao` and `TodoRepository` now expose narrow one-shot queries for completed todos in a time range, missed todos due in a time range, active DDL-backed todos in a time range, and active events overlapping a time range.
2. AI daily-report and weekly-report context collection no longer loads the complete todo table before in-memory filtering; it reads only the date windows needed for today, tomorrow, this week, and the next DDL window.
3. Saving a week as a schedule template now queries active events overlapping the target week instead of scanning all todo items.
4. Desktop sync Planning Desk note update and mapping refresh now read a single planning note by ID instead of loading every planning note and filtering in memory.
5. `widget_countdown_info.xml` was rechecked and already declares `android:updatePeriodMillis="0"`, so the independent countdown widget remains on provider-owned minute ticks rather than the system widget update floor.

### P8 desktop-sync suspend handler slice

1. `DesktopSyncServer` now accepts a suspend request handler and keeps the single `runBlocking` boundary inside the per-client socket thread where the HTTP response must wait.
2. `DesktopSyncCoordinator.handleRequest` and repository-backed desktop sync route handlers are now suspend functions.
3. Desktop sync snapshot, todo/event CRUD, event check-in, item completion/cancel/delete, Planning Desk notes/import/refresh/postpone/undo/conflict, group resolution, reminder scheduling cleanup, and group-tag lookup now call repository suspend APIs directly instead of wrapping each operation in `runBlocking`.
4. Static search confirms `DesktopSyncCoordinator.kt` no longer contains `runBlocking`; the only remaining sync-server `runBlocking` is the intended socket-thread boundary in `DesktopSyncServer.kt`.

### V1-V6 Planning Desk image recognition slice

1. Phone Planning Desk overflow menu now includes `从图片识别日程` and launches the Android image picker instead of adding another shortcut-bar button.
2. Selected images are decoded off the main thread, resized to a maximum 1600px long side, JPEG-compressed at quality 80, and rejected with `图片过大，请裁剪后重试` if still too large.
3. AI Provider settings now persist `supportsVision` and expose a compact switch labeled `此服务支持图片识别`; provider summary cards show when image recognition is enabled.
4. `PlanningAiCaller.callVisionWithFallback` sends OpenAI-compatible vision chat-completion messages only through enabled, complete, vision-capable providers, preserving the existing endpoint fallback behavior.
5. Image recognition uses the fixed timetable/schedule OCR prompt, shows the non-cancel progress dialog `AI 识别中…可能需要 10-30 秒`, appends returned Markdown to the current planning note, moves the cursor to the end, and asks the user to use the existing `识别` button for preview import.
6. Empty AI output, missing vision-capable providers, compression failure, and network/API failures now produce the required user-facing toasts without writing partial text into the planning document.

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

### C6 event check-in settings slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after adding the event check-in preference switches and persistence.
2. `git diff --check` passed after the code and docs sync.
3. No new APK has been built for this slice yet.

### C4 event completion statistics slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after adding check-in-enabled event completion, automatic checkout, and completion statistics.
2. `git diff --check` passed after the code and docs sync.
3. No new APK has been built for this slice yet.

### C7 AI daily report event investment slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after adding `今日日程投入` to AI daily-report context and fallback text.
2. `git diff --check` passed after the code and docs sync.
3. No new APK has been built for this slice yet.

### C5 desktop web event check-in UI slice

1. `node --check app/src/main/assets/desktop-web/app.js` passed after adding the Web check-in card and operations.
2. `./gradlew.bat :app:compileDebugKotlin` passed after the desktop Web asset update.
3. `git diff --check` passed after the code and docs sync.
4. No new APK has been built for this slice yet.

### T1-T3 Planning Desk shortcut simplification slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after removing the extra shortcut entries.
2. Static search found no remaining phone `PlanningShortcutSpec` entries for task, DDL, schedule, group, today, tomorrow, or indent shortcuts.
3. `git diff --check` passed after the code and docs sync.
4. No new APK has been built for this slice yet.

### P6/P7/P9/P10 narrow query / widget metadata slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after adding the narrow DAO / repository queries and updating call sites.
2. `git diff --check` passed after code and docs synchronization.
3. No new APK has been built for this slice yet.

### P8 desktop-sync suspend handler slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after converting desktop sync handlers to suspend.
2. Static search confirmed `runBlocking` appears only in `DesktopSyncServer.kt` at the socket response boundary and no longer appears in `DesktopSyncCoordinator.kt`.
3. `git diff --check` passed after code and docs synchronization.
4. No new APK has been built for this slice yet.

### V1-V6 Planning Desk image recognition slice

1. `./gradlew.bat :app:compileDebugKotlin` passed after adding Provider vision support, the vision caller, Settings switch, image picker, compression, progress dialog, and append-to-note flow.
2. Static search confirmed the new `supportsVision`, `callVisionWithFallback`, `从图片识别日程`, `ActivityResultContracts.GetContent`, image compression helper, and `AI 识别中…可能需要 10-30 秒` surfaces exist in the expected files.
3. `git diff --check` passed after code synchronization.
4. No new APK has been built for this slice yet.

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk` on the physical phone if validating the latest built widget APK.
2. Add / resize the `今日看板` widget and confirm the removed top header, minute refresh, and cross-day date/list update behavior on the launcher.
3. Add / resize the `倒数日` widget and confirm scroll behavior, readable multi-line rows, and row deep links on the launcher.
4. After a later focus-removal APK build, confirm the app opens without any focus / pomodoro entry in the drawer, settings, todo long-press menu, desktop web, AI report, or widgets.
5. In a real desktop browser, verify todo multi-group behavior: selecting multiple filter chips uses intersection filtering, editing a todo preserves all selected groups, and the card / preview labels show all groups.

## Remaining 1.11.0 Work

The full goal remains active. Major remaining slices:

1. P1/P2/P3: R8/resource shrinking, WebP conversion, icon dependency audit, release launch verification, and final APK-size check.
2. Final version bump to `1.11.0 / versionCode 222`.
