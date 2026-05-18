# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.10.3"`
  - `versionCode = 221`

## Current Build Facts

- Latest `1.10.2` signed release APK:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk`
  - Published as GitHub Release tag `v1.10.2`
- Latest signed release APK built in this round:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk`
- Latest debug APK built in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk`
- Verification completed for this `1.10.3` continuation:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `git diff --check`
  - `./gradlew.bat :app:assembleRelease`
  - APK metadata inspected: `versionName = 1.10.3`, `versionCode = 221`, output `PaykiTodo-1.10.3-release.apk`
  - `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk` passed with one v2 signer
  - `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk` confirmed local signing material and APK outputs are ignored
  - `./gradlew.bat :app:assembleDebug`
  - Debug APK metadata inspected: `versionName = 1.10.3`, `versionCode = 221`, output `PaykiTodo-1.10.3-debug.apk`
  - Widget continuation: `./gradlew.bat :app:compileDebugKotlin`
  - Widget continuation: static search confirmed removed widget header IDs and old fixed countdown-row IDs are no longer referenced
  - Widget continuation: `git diff --check`
  - Widget continuation: `./gradlew.bat :app:assembleDebug`
- Release-signing privacy:
  - local `keystore.properties`, `release/PaykiTodo-release.jks`, APK/AAB outputs, API keys, tokens, and private Base URLs must stay out of Git
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is implementing the larger `1.11.0 / versionCode 222` goal from the current `1.10.3 / 221` baseline. The Android widget requirements have already landed in the current baseline. The first `1.11.0` goal slice removes the former focus / pomodoro mode and prepares the database for later check-in and multi-group todo work. The second small `1.11.0` slice adds database schema export, debug-only Compose tooling, structured startup initialization, and AI-report retention cleanup. The navigation slice renames the drawer task entry to single-line `待办`, removes the drawer group expansion and standalone `分组管理` entry, and moves group filtering / group maintenance into the todo page chip bar. The multi-group slice implements multi-group todo relationships across phone UI, repository queries, backup/restore, desktop sync, and desktop Web todo management. The event check-in foundation slice adds event check-in data, backup, repository, and initial desktop-sync API foundations. Phone check-in work now includes the event-editor `打卡追踪` switch, preserving optional event markers when moving events, a calendar-event details card that can load records, show total invested time, and perform `签到` / `签退`, full-screen / accessibility fallback event reminder surfaces that can start a check-in, plus daily-board in-progress event rows that show check-in state and expose compact `签到` / `签退` actions. Desktop Web now also exposes the event-editor `打卡追踪` checkbox and a preview-side check-in card that lists records and performs `签到` / `签退` through the desktop sync endpoints. The Android `今日看板` widget now also shows active in-progress event check-in status without exposing launcher-side sign-in / sign-out buttons. Settings -> `日历与提醒` now stores the two event check-in behavior preferences for automatic checkout on event completion and showing investment statistics on completion. Completing a check-in-enabled event now uses those settings to auto-checkout any active record and show the phone-side investment summary, while desktop sync item-completion responses can include the same summary JSON. AI daily reports now include today's event check-in investment minutes in both the AI prompt and local fallback report. Phone Planning Desk shortcut UI has also been simplified to `子任务` and `公告`, with normal task / DDL / reminder / group / schedule input staying in natural text and parser tags. The latest performance slices replace several full-table reads with narrow range / single-row queries and move desktop sync business handlers to suspend calls, leaving only one `runBlocking` boundary in the socket client thread. Phone Planning Desk can now call vision-capable AI providers from `更多 -> 从图片识别日程`, append recognized schedule Markdown to the current note, and still require the normal preview/import flow before writing database items. Release builds now enable R8/resource shrinking and use WebP dashboard backgrounds; the latest signed release APK size check is `4.83 MB`. The full `1.11.0` version bump is still pending.

Most important current baseline facts:

- Database version is now `18` in the working tree. `MIGRATION_17_18` drops the old `focus_sessions` table, creates `event_check_ins`, creates `todo_group_tags`, adds `checkInEnabled` and `totalCheckInMinutes` to `todo_items`, backfills todo group tags, and merges the old default `专注` group into `例行`.
- Active no-DDL todos are still treated as today todos across phone daily board, Android widget board query, desktop board, and desktop todo management.
- Countdown-enabled todos use their DDL time as the target; countdown-enabled events use their start time.
- Countdown targets whose exact target time has passed are filtered out before board / widget / desktop rendering.
- Planning Desk event import defaults to event-only. A linked todo is created only if the user explicitly enables the preview option.
- Planning Desk linked todos no longer receive a fixed auto-generated note.
- Planning Desk local parser and AI cleanup both treat `@地点` as event location text, not a required prefix or title content.
- Desktop-web event editor now exposes compact preset color swatches matching the phone editor.
- `今日看板` Android widget no longer renders its fixed top menu/title/date header; the RemoteViews list fills the widget body.
- `今日看板` and independent `倒数日` widgets both set `updatePeriodMillis=0` and use provider-owned `AlarmManager` minute ticks to refresh RemoteViews collections.
- Independent `倒数日` widget is now a scrollable `RemoteViewsService` / `ListView` widget rather than three fixed rows, with multi-line daily-board-style rows and direct todo/event detail deep links.
- Former focus / pomodoro mode is removed from the working tree: no drawer entry, settings block, FocusActivity, focus widget, focus DAO/repository API, desktop sync focus stats, AI-report focus fields, or backup focus-session export remains.
- Existing old backups that contain `focusSessions` are ignored rather than failing import.
- Room schema export is enabled and `app/schemas/com.example.todoalarm.data.AppDatabase/18.json` records the database-18 structure.
- Compose `ui-tooling-preview` is now scoped to `debugImplementation`, so release builds do not carry the preview tooling dependency.
- AI 日报 / 周报 settings include a report-retention dropdown; generating a report purges older archived reports according to 30-day / 90-day / 365-day / forever retention.
- Backup / restore preserves the AI report retention policy while still excluding AI API Keys.
- Application startup initialization uses an application-level `SupervisorJob` scope and records non-fatal initialization failures through `CrashLogger.recordNonFatal`.
- Desktop sync keeps the 4 MB request-body limit and returns HTTP 413 for oversized requests.
- Drawer navigation now shows `待办` as a simple module entry instead of expandable `我的任务`.
- Group filtering and group management now live in the todo page top chip bar: `全部`, each sorted group, and `新建`; long-pressing a group chip opens edit / delete behavior.
- Todos now support multiple group tags through `todo_group_tags`; phone and desktop todo filters use multi-select intersection semantics, while todo editors keep a primary `groupId` plus the full `groupIds` tag set.
- Backup / restore now includes `todoGroupTags`, and old backups without explicit tags are restored by backfilling from each todo's original `groupId`.
- Desktop sync todo payloads now expose and accept `groupIds`; desktop Web todo cards, previews, board rows, filter chips, and editor chips preserve multi-group relationships instead of collapsing them to one group.
- Calendar-event persistence now carries `checkInEnabled` and `totalCheckInMinutes`; repository APIs can check in, check out, list event check-ins, recompute total invested minutes, and query today's invested event minutes.
- Backup / restore now includes `eventCheckIns`, and deleting events clears their related check-in rows.
- Desktop sync event payloads now expose `checkInEnabled` / `totalCheckInMinutes`, and initial check-in endpoints exist at `GET /api/events/{id}/check-ins`, `POST /api/events/{id}/check-in`, and `POST /api/events/{id}/check-out`.
- Desktop Web event editor exposes `打卡追踪`; event preview loads check-in records, shows total invested time / active segment state, and can perform `签到` / `签退`.
- Phone calendar-event editor now shows `打卡追踪` under `日程标记`; event move operations preserve both `countdownEnabled` and `checkInEnabled`.
- Phone calendar-event details now shows a `打卡追踪` card for enabled events, including total invested time, active `签到中` status, closed / active segment rows, and direct `签到` / `签退` actions.
- Full-screen event reminders and the accessibility fallback reminder overlay now show a `签到` action for check-in-enabled events; signing in acknowledges the current event reminder and closes the reminder surface.
- Phone daily-board in-progress schedule rows now show `未签到` / `签到中 · 已 Xm` for check-in-enabled events and expose compact `签到` / `签退` actions.
- Android `今日看板` widget in-progress schedule rows now batch-load active check-ins and show `⏱ 签到中 Xm` in the event accent color while keeping widget check-in state display-only.
- Settings -> `日历与提醒` includes `日程结束时自动签退` and `完成日程时显示投入统计`; both default on, persist locally, and are preserved in backup / restore snapshots.
- Phone calendar-event details exposes `完成日程` only for check-in-enabled events; completion can auto-checkout the active record and, when enabled, show planned minutes, actual invested minutes, check-in count, investment rate, and auto-checkout status.
- Desktop sync `/api/items/{id}/complete` uses the same event-completion path and includes `eventCheckInSummary` in its JSON response when completion statistics are enabled.
- AI daily reports now collect today's event check-in minutes and render `今日日程投入：Y 分钟` in the AI prompt; the local fallback report also writes the same investment total.
- Phone Planning Desk shortcut toolbar now defaults to a minimal `子任务` / `公告` pair; task, DDL, reminder, group, date, and schedule entry are intentionally handled through natural text, explicit tags, and the preview parser instead of a crowded button grid.
- Phone Planning Desk overflow now supports `从图片识别日程`; image recognition uses only AI providers marked as supporting vision, compresses selected images before upload, appends Markdown recognition output to the current note, and avoids direct database writes until the user runs the normal `识别` preview/import flow.
- Settings -> `AI 调用配置` provider editing includes a compact `此服务支持图片识别` switch, persisted as `supportsVision` and excluded from any need to discover provider capabilities automatically.
- Release builds now enable R8 minification and resource shrinking; `assembleRelease` succeeded with this configuration and produced a `4.83 MB` APK for the current `1.10.3 / 221` baseline.
- The dashboard background art under `drawable-nodpi` is now stored as WebP resources (`dashboard_bg.webp`, `dashboard_bg_light.webp`, `dashboard_bg_dark.webp`) while existing resource names remain stable for app and widget references.
- ZIP-level inspection found no `androidx/compose/material/icons` entries in the release APK after R8, so `material-icons-extended` is currently shrunk out sufficiently and does not need replacement.
- AI daily-report and weekly-report context collection now uses range-limited DAO queries for completed todos, missed todos, active DDL-backed todos, and overlapping active events instead of loading the full todo table.
- Saving a week as a schedule template now queries only active events overlapping the selected week.
- Desktop sync Planning Desk note updates and mapping refreshes now read the requested planning note by ID instead of loading all planning notes and filtering in memory.
- Desktop sync request handling now uses a suspend request handler from `DesktopSyncServer` into `DesktopSyncCoordinator`; repository-backed desktop routes call suspend APIs directly, with the only remaining `runBlocking` kept at the per-client socket response boundary.
- The independent countdown widget metadata remains `updatePeriodMillis=0`; minute refresh is still controlled by the widget provider's own scheduling path.
- `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, and `git diff --check` have passed for the multi-group todo slice.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the event check-in data / API foundation slice.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the phone event-editor check-in switch slice.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the phone event-details check-in operation slice.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the full-screen reminder check-in slice.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the phone daily-board check-in status slice.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the Android widget check-in status slice.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the event check-in settings slice.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the event completion statistics slice.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the AI daily report event-investment slice.
- `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, and `git diff --check` have passed for the Desktop Web event check-in UI slice.
- `./gradlew.bat :app:compileDebugKotlin`, static shortcut-entry search, and `git diff --check` have passed for the Planning Desk shortcut simplification slice.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the latest narrow-query performance slice.
- `./gradlew.bat :app:compileDebugKotlin`, static `runBlocking` search, and `git diff --check` have passed for the latest desktop-sync suspend handler slice.
- `./gradlew.bat :app:compileDebugKotlin`, static Planning Desk vision-surface search, and `git diff --check` have passed for the latest Planning Desk image-recognition slice.
- `./gradlew.bat :app:assembleRelease`, release APK size inspection, ZIP-level material-icons inspection, and `git diff --check` have passed for the latest release-size optimization slice.

## Immediate Manual Verification Targets

1. Android launcher widgets:
   - Add / resize `今日看板` and confirm there is no top header row.
   - Confirm `今日看板` content updates after minute ticks and across date changes.
   - Add / resize `倒数日` and confirm rows scroll, long titles wrap, and row taps open the matching todo / event detail.
2. Planning Desk local rule import:
   - `10:00-12:00 自习 @图书馆3楼`
   - `10:00-12:00, 【课程】习思想，"@主楼B1-412"`
   - `10:00-12:00 自习 地点：图书馆3楼`
3. Planning Desk AI import:
   - verify title, location, group, and linked-todo checkbox defaults.
   - mark a test AI source as vision-capable, choose a schedule screenshot through `更多 -> 从图片识别日程`, confirm recognized Markdown is appended, then run the normal `识别` preview flow.
4. Desktop web:
   - event color preset swatches update the color input and persist after saving.
   - todo multi-group filter chips use intersection filtering; editing a todo preserves all selected groups and cards / previews show all group names.
5. Regression:
   - normal event import does not create an unwanted todo;
   - manually linked todo has DDL equal to event end time and no fixed generated note.
6. Drawer / todo navigation:
   - drawer shows a single-line `待办`;
   - drawer has no expanded group list and no standalone `分组管理`;
   - todo page chip bar filters by group, `新建` creates a group, and long-press edits / deletes a group.
