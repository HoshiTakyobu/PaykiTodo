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

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk` on the physical phone if validating the latest built widget APK.
2. Add / resize the `今日看板` widget and confirm the removed top header, minute refresh, and cross-day date/list update behavior on the launcher.
3. Add / resize the `倒数日` widget and confirm scroll behavior, readable multi-line rows, and row deep links on the launcher.
4. After a later focus-removal APK build, confirm the app opens without any focus / pomodoro entry in the drawer, settings, todo long-press menu, desktop web, AI report, or widgets.

## Remaining 1.11.0 Work

The full goal remains active. Major remaining slices:

1. P5/P4/P11/P12/P13: Room schema export, debug-only tooling preview, desktop-sync request size limit, structured application coroutine scope, and AI-report retention.
2. M1/M5: drawer simplification and moving group management into the todo page.
3. M3/M2/M4/M6/M7: multi-group todo data model, intersection filter chips, multi-select editor, desktop sync support, and backup support.
4. C1-C7: event check-in / time tracking across phone UI, widgets, desktop web, backup, and AI reports.
5. V1-V6: Planning Desk image recognition through vision-capable AI providers.
6. T1-T3: Planning Desk shortcut bar simplification and help update.
7. P6/P7/P9/P10/P8: narrow database queries, countdown widget update metadata, and desktop-sync suspend handler cleanup.
8. P1/P2/P3: R8/resource shrinking, WebP conversion, icon dependency audit, release launch verification, and final APK-size check.
9. Final version bump to `1.11.0 / versionCode 222`.
