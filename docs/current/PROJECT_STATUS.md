# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.12.2"`
  - `versionCode = 229`

## Current Build Facts

- Latest `1.10.2` signed release APK:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk`
  - Published as GitHub Release tag `v1.10.2`
- Latest signed release APK built locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.11.0-release.apk`
- Latest fully built debug APK:
  - `app/build/outputs/apk/debug/PaykiTodo-1.12.2-debug.apk`
- Current `1.12.2 / versionCode 229` verification completed:
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `git diff --check`
  - `./gradlew.bat :app:assembleDebug`
  - Debug APK metadata inspected: `versionName = 1.12.2`, `versionCode = 229`, output `PaykiTodo-1.12.2-debug.apk`
- Current `1.12.1 / versionCode 228` verification completed:
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `git diff --check`
  - `./gradlew.bat :app:assembleDebug`
  - Debug APK metadata inspected: `versionName = 1.12.1`, `versionCode = 228`, output `PaykiTodo-1.12.1-debug.apk`
- Current `1.12.0 / versionCode 227` verification completed:
  - `./gradlew.bat :app:compileDebugKotlin`
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `git diff --check`
  - `./gradlew.bat :app:assembleDebug`
  - Debug APK metadata inspected: `versionName = 1.12.0`, `versionCode = 227`, output `PaykiTodo-1.12.0-debug.apk`
- Current `1.11.4 / versionCode 226` verification completed:
  - `./gradlew.bat :app:compileDebugKotlin`
  - `git diff --check`
  - `./gradlew.bat :app:assembleDebug`
  - Debug APK metadata inspected: `versionName = 1.11.4`, `versionCode = 226`, output `PaykiTodo-1.11.4-debug.apk`
- Current `1.11.2 / versionCode 224` verification completed:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:compileDebugKotlin`
  - `git diff --check`
  - `./gradlew.bat :app:assembleDebug`
  - Debug APK metadata inspected: `versionName = 1.11.2`, `versionCode = 224`, output `PaykiTodo-1.11.2-debug.apk`
- Verification completed for the `1.11.0 / versionCode 222` build:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `git diff --check`
  - `./gradlew.bat :app:assembleRelease`
  - Release APK metadata inspected: `versionName = 1.11.0`, `versionCode = 222`, output `PaykiTodo-1.11.0-release.apk`
  - `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.11.0-release.apk` passed with one v2 signer
  - `./gradlew.bat :app:assembleDebug`
  - Debug APK metadata inspected: `versionName = 1.11.0`, `versionCode = 222`, output `PaykiTodo-1.11.0-debug.apk`
  - APK size inspection: release `4.83 MB`, debug `22.12 MB`
  - `Pixel_8` AVD was started as `emulator-5554`; after removing an older differently signed emulator install, `PaykiTodo-1.11.0-release.apk` installed and launched successfully
  - Release smoke test opened `MainActivity`, read the daily-board UI tree, opened the drawer, and loaded `待办` / `日历` / `规划台` / `AI 报告` / `设置` without an app `FATAL EXCEPTION` in logcat
  - `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/release/PaykiTodo-1.11.0-release.apk app/build/outputs/apk/debug/PaykiTodo-1.11.0-debug.apk` confirmed local signing material and APK outputs are ignored
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

The repository now targets the `1.12.2 / versionCode 229` workline. Phase 1 of the capture + outliner goal is implemented. The current Outliner slice now has database `planning_nodes`, migration / backup support, phone-side tree editing, desktop `/api/planning/nodes` routes, a desktop Web node editor, Markdown import/export compatibility, and capture/share/photo/voice recognition that can directly create planning nodes and linked official todos/events. This is still not the complete Outliner goal: phone keyboard interactions and long-press time/location editing remain incomplete, and desktop reorder is still a minimal API foundation rather than a polished drag/reorder UI. The latest 1.12.2 patch fixes parent-node completion reminder cleanup and capture-created false reminder state. The earlier Android widget, multi-group todo, event check-in, Planning Desk image recognition, release-size, and `1.11.4` check-in regression fixes remain in the working tree. Release builds enable R8/resource shrinking and use WebP dashboard backgrounds; the latest signed `PaykiTodo-1.11.0-release.apk` size check is `4.83 MB`, and release startup / main-surface smoke testing passed on `Pixel_8 / emulator-5554`.

Most important current baseline facts:

- Database version is now `20` in the working tree. `MIGRATION_17_18` drops the old `focus_sessions` table, creates `event_check_ins`, creates `todo_group_tags`, adds `checkInEnabled` and `totalCheckInMinutes` to `todo_items`, backfills todo group tags, and merges the old default `专注` group into `例行`; `MIGRATION_18_19` adds `planning_notes.documentDateEpochDay`; `MIGRATION_19_20` creates `planning_nodes` and migrates existing planning Markdown lines into node records.
- Quick capture is implemented: `ShareReceiverActivity`, `CaptureActivity`, `VoiceCaptureActivity`, `BackgroundCaptureProcessor`, shortcuts XML, FileProvider paths, and direct node insertion exist. Legacy preview UI code still exists for reusable/old paths but is no longer the default share/capture write path.
- Phase 2 Outliner is partially user-facing: `PlanningNode`, DAO methods, repository CRUD / Markdown import-export helpers, backup / restore, schema `20.json`, phone outline editing, desktop sync node routes, desktop Web node editing, and capture-to-node insertion exist. Remaining gaps include phone keyboard editing shortcuts, long-press time/location editing, polished desktop reorder, and stricter migration/runtime QA.
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
- Room schema export is enabled and `app/schemas/com.example.todoalarm.data.AppDatabase/20.json` records the database-20 structure.
- Compose `ui-tooling-preview` is now scoped to `debugImplementation`, so release builds do not carry the preview tooling dependency.
- AI 日报 / 周报 settings include a report-retention dropdown; generating a report purges older archived reports according to 30-day / 90-day / 365-day / forever retention.
- Backup / restore preserves the AI report retention policy while still excluding AI API Keys.
- Application startup initialization uses an application-level `SupervisorJob` scope and records non-fatal initialization failures through `CrashLogger.recordNonFatal`.
- Desktop sync keeps the 4 MB request-body limit and returns HTTP 413 for oversized requests.
- Drawer navigation now shows `待办` as a simple module entry instead of expandable `我的任务`.
- Group filtering and group management now live in the todo page top chip bar: `全部`, each sorted group, and `新建`; long-pressing a group chip opens edit / delete behavior.
- Todos now support multiple group tags through `todo_group_tags`; phone todo filters can switch between multi-select intersection and union semantics, while todo editors keep a primary `groupId` plus the full `groupIds` tag set.
- Backup / restore now includes `todoGroupTags`, and old backups without explicit tags are restored by backfilling from each todo's original `groupId`.
- Desktop sync todo payloads now expose and accept `groupIds`; desktop Web todo cards, previews, board rows, filter chips, and editor chips preserve multi-group relationships instead of collapsing them to one group.
- Calendar-event persistence now carries `checkInEnabled` and `totalCheckInMinutes`; repository APIs can check in, check out, list event check-ins, recompute total invested minutes, and query today's invested event minutes.
- Backup / restore now includes `eventCheckIns`, and deleting events clears their related check-in rows.
- Desktop sync event payloads now expose `checkInEnabled` / `totalCheckInMinutes`, and initial check-in endpoints exist at `GET /api/events/{id}/check-ins`, `POST /api/events/{id}/check-in`, and `POST /api/events/{id}/check-out`.
- Desktop Web event editor exposes `打卡追踪`; event preview loads check-in records, shows total invested time / active segment state, and can perform `签到` / `签退`.
- Phone calendar-event editor now shows `打卡追踪` under `日程标记`; event move operations preserve both `countdownEnabled` and `checkInEnabled`.
- Phone calendar-event details now shows a `打卡追踪` card for enabled events, including total invested time, active `签到中` status, closed / active segment rows, and direct `签到` / `签退` actions.
- Full-screen event reminders and the accessibility fallback reminder overlay now show a `签到` action for check-in-enabled events; signing in acknowledges the current event reminder and closes the reminder surface.
- Phone daily-board in-progress schedule rows now show `未签到` / `签到中 · 已 Xm` for check-in-enabled events and route `去签到` / `查看` to the independent full-screen check-in surface.
- Android `今日看板` widget in-progress schedule rows now batch-load active check-ins and show `⏱ 签到中 Xm` in the event accent color while keeping widget check-in state display-only.
- Settings -> `日历与提醒` includes `日程结束时自动签退`, `完成日程时显示投入统计`, and `闲置自动签退阈值`; defaults persist locally and are preserved in backup / restore snapshots.
- Phone calendar-event details exposes `完成日程` only for check-in-enabled events; completion can auto-checkout the active record and, when enabled, show planned minutes, actual invested minutes, check-in count, investment rate, and auto-checkout status.
- Desktop sync `/api/items/{id}/complete` uses the same event-completion path and includes `eventCheckInSummary` in its JSON response when completion statistics are enabled.
- AI daily reports now collect today's event check-in minutes and render `今日日程投入：Y 分钟` in the AI prompt; the local fallback report also writes the same investment total.
- Phone Planning Desk shortcut toolbar now defaults to a minimal `子任务` / `公告` pair; task, DDL, reminder, group, date, and schedule entry are intentionally handled through natural text, explicit tags, and the preview parser instead of a crowded button grid.
- Phone Planning Desk overflow now supports `从图片识别日程`; image recognition uses only AI providers marked as supporting vision, compresses selected images before upload, appends Markdown recognition output to the current note, auto-opens the preview sheet when candidates are parsed, and still avoids direct database writes until the user confirms preview/import.
- Settings -> `AI 调用配置` provider editing includes a compact `此服务支持图片识别` switch, persisted as `supportsVision` and excluded from any need to discover provider capabilities automatically.
- Release builds now enable R8 minification and resource shrinking; `assembleRelease` succeeded with this configuration and produced `app/build/outputs/apk/release/PaykiTodo-1.11.0-release.apk` at `4.83 MB`.
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
