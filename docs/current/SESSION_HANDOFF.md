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
  11. Full `1.11.0 / versionCode 222` version bump is still pending.
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

Secret / release safety checks already performed:

1. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk` confirmed local signing material and APK outputs are ignored.
2. `git ls-files` only reports `keystore.properties.example` for keystore-like tracked files.
3. Git history search found no committed real keystore/JKS/env/private-key files or common live token prefixes; keyword-like matches are code identifiers, templates, placeholder examples, or test URLs.

## Remaining Device / Browser Verification

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk` on the user's phone if validating the latest built widget APK.
2. Add / resize the `今日看板` widget and confirm the top header is gone, content remains readable, and cross-day / minute refresh updates the list.
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
13. After the phone / desktop UI is added, verify event check-in end to end: enable check-in for an event, sign in / sign out multiple times, confirm total minutes, backup / restore `eventCheckIns`, and test the desktop check-in endpoints from a browser.

## Performance Notes

- Countdown data still uses the existing `countdownEnabled` marker and indexed board/widget lookup paths.
- The independent `倒数日` widget is no longer limited to three fixed rows; it is now a scrollable RemoteViews ListView capped at 60 active countdown targets.
- Minute-level widget refresh is scheduled by `AlarmManager`; OEM launchers and battery policies may still delay widget refreshes under aggressive power management.
- Removing focus mode reduces board/settings/widget/report data surface before adding event check-in.
- Schema export gives future migrations a concrete Room reference file for database version 18.
- AI report retention prevents the `ai_reports` archive from growing without limit once reports are generated regularly.

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
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncModels.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoCards.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
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
