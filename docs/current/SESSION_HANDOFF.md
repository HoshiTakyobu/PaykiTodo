# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.6` / `versionCode 200`.
- This round carries forward AI 日报 / 周报 after the `1.9.0` focus-mode baseline, keeps the `1.9.5` exact-alarm fallback, and applies another launcher-widget visual pass for the Android `今日看板` widget.
- Latest debug APK after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.6-debug.apk`.
- Local commits before this hotfix: `c20d18a` for focus mode and `104c9aa` for AI reports plus the previous widget visual pass.
- Latest local commit implements the `1.9.6` launcher-widget daily-board visual pass on top of the `1.9.5` AI report scheduling fallback.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.6 Widget Visual Pass

1. Android `今日看板` launcher widget topbar is now tighter and closer to the in-app daily-board TopBar: circular menu button, `每日看板` title, compact date, and no extra `轻触打开` copy.
2. Todo widget cards now show the task group tag, notes summary, and `⏰ DDL HH:mm`, while keeping the group-color strip and checkbox-like marker.
3. Widget card drawables removed heavy strokes and slightly reduced padding / font sizes, so the launcher surface reads more like the app's card stack instead of a system list.
4. The aggregated schedule card now uses tighter date/event spacing; currently running schedule rows get a gold border with faint gold fill, while ordinary schedule rows remain transparent with vertical color strips.
5. Version metadata moved to `1.9.6` / `versionCode 200`.

## Latest 1.9.4 Widget Hotfix Changes

1. Android `今日看板` launcher widget now declares a static daily-board-style `previewLayout`, so the widget picker no longer falls back to a blank / empty-list initial layout.
2. The widget provider default target is now a 4x5 vertical board with larger minimum height, better matching the in-app daily-board screenshot the user referenced.
3. Todo widget rows now look closer to active todo cards: group color strip, checkbox-like marker, title, and `DDL HH:mm` chip instead of a separate narrow time column.
4. Schedule rows inside the aggregated schedule card no longer use ordinary row fill; they return to transparent rows with vertical color strips like the in-app daily-board schedule block.
5. Version metadata moved to `1.9.4` / `versionCode 198`.

## Latest 1.9.5 AI Report Scheduling Fix

1. `DailyReportScheduler` still uses exact daily / weekly alarms when Android allows exact alarms.
2. If Android 12+ denies `SCHEDULE_EXACT_ALARM`, AI 日报 / 周报 scheduling now falls back to `setAndAllowWhileIdle` / `set` instead of failing silently or throwing a startup `SecurityException`.
3. Settings -> `AI 调用配置` -> `AI 日报 / 周报` and the in-app Wiki now explain that missing exact-alarm permission may delay automatic report generation.
4. Version metadata moved to `1.9.5` / `versionCode 199`.

## Latest 1.9.1 AI Report Changes

1. `DailyReportScheduler` schedules daily and Sunday weekly report alarms, cancels disabled alarms, and is called on app startup plus boot/time/timezone recovery.
2. `DailyReportReceiver` runs report generation with `goAsync()` and reschedules after each run.
3. `DailyReportGenerator` collects completed todos, missed todos, events, upcoming DDLs, and focus minutes; it tries Planning Desk AI providers first and falls back to local templates.
4. Generated reports are written to Planning Desk `AI 日报` / `AI 周报` notes, with new entries prepended above old reports.
5. `DailyReportNotifier` posts low-priority report notifications and deep-links to the matching report note.
6. Settings -> `AI 调用配置` now includes compact daily/weekly report switches, HH:mm time fields, save/re-schedule, and `立即生成一次日报`.
7. Planning Desk shows a purple hint when `AI 日报` or `AI 周报` is active.
8. README, CHANGELOG, Wiki, TODO, and current-state docs were updated for `1.9.1`.
9. The Android `今日看板` launcher widget received an additional visual pass: announcements now precede the greeting like the in-app board, the header/card spacing is tighter, and light/dark card opacity was raised for a less generic RemoteViews-list look.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/todoalarm/TodoApplication.kt`
- `app/src/main/java/com/example/todoalarm/alarm/BootReceiver.kt`
- `app/src/main/java/com/example/todoalarm/alarm/DailyReportScheduler.kt`
- `app/src/main/java/com/example/todoalarm/alarm/DailyReportReceiver.kt`
- `app/src/main/java/com/example/todoalarm/alarm/DailyReportNotifier.kt`
- `app/src/main/java/com/example/todoalarm/data/DailyReportGenerator.kt`
- `app/src/main/java/com/example/todoalarm/data/AppSettingsStore.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `app/src/main/assets/wiki/index.html`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

## Verification Status

Completed locally:

1. `./gradlew.bat :app:compileDebugKotlin`
2. `./gradlew.bat :app:mergeDebugResources`
3. `node --check app/src/main/assets/desktop-web/app.js`
4. `./gradlew.bat testDebugUnitTest`
5. `./gradlew.bat assembleDebug`
6. `git diff --check`
7. `./gradlew.bat :app:mergeDebugResources` after the `1.9.3` widget resource pass
8. `./gradlew.bat :app:mergeDebugResources` after the `1.9.4` widget preview/layout pass
9. `./gradlew.bat :app:compileDebugKotlin assembleDebug`
10. `./gradlew.bat testDebugUnitTest`
11. `git diff --check`
12. `./gradlew.bat :app:compileDebugKotlin assembleDebug` after `1.9.5` scheduling fallback and Kotlin cache cleanup
13. `./gradlew.bat testDebugUnitTest`
14. `git diff --check`
15. Installed `PaykiTodo-1.9.5-debug.apk` on `emulator-5554`; installed package reports `versionCode=199`, `versionName=1.9.5`
16. With `SCHEDULE_EXACT_ALARM` denied, daily report at current time +2 minutes registered a fallback `GENERATE_DAILY_REPORT` alarm, generated a new top `AI 日报` entry, and posted notification id `91000`
17. Tapping the `AI 日报已生成` notification opened `规划台` directly on the `AI 日报` document with the AI auto-report hint visible
18. Disabling daily reports removed PaykiTodo report alarms from `dumpsys alarm`
19. Enabling weekly reports registered a Sunday `GENERATE_WEEKLY_REPORT` alarm; reports were then disabled again to leave the emulator clean
20. `./gradlew.bat :app:mergeDebugResources` after the `1.9.6` widget layout/resource pass
21. `./gradlew.bat :app:compileDebugKotlin` after the `1.9.6` widget binding changes
22. `./gradlew.bat assembleDebug`; output metadata reports `PaykiTodo-1.9.6-debug.apk`, `versionCode=200`, `versionName=1.9.6`
23. `./gradlew.bat testDebugUnitTest`
24. `node --check app/src/main/assets/desktop-web/app.js`
25. `git diff --check`

Real launcher verification is still needed for the widget visual pass because RemoteViews rendering varies by launcher.

Local device status observed in this session:

1. `adb` is available at `C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
2. `emulator-5554` is available and was used for the `1.9.5` AI report scheduling verification.
3. The Android Emulator was also useful for phone-side UI smoke tests during the 1.9.x goal audit: it installed `com.paykitodo.app`, launched the Daily Board, opened the free-focus path, displayed `FocusActivity`, showed the early-complete confirmation, and refreshed the daily focus-session count.
4. Future sessions may use the emulator as a standard pre-phone debugging step for launch crashes, navigation, and visible UI regressions. Tell the user when starting a new emulator window; if one is already running, reuse it.
5. Continue physical-device checks for OEM alarm policy, haptics, launcher widget rendering, notification shade visuals, and reboot/time-change recovery.

Then verify on a real device after installing `PaykiTodo-1.9.6-debug.apk`:

1. confirm the same daily-report flow on the user's real device / OEM ROM;
2. tap `立即生成一次日报` and confirm `AI 日报` is created / updated;
3. test Sunday weekly report generation into `AI 周报`;
4. test boot/time/timezone recovery on device;
5. test launcher widget rendering from the real launcher picker, including the tighter topbar, todo group/notes/`⏰ DDL` rows, and in-progress schedule gold treatment.

## Required Reading For A New Session

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/UI_DESIGN_RULES.md`
7. `docs/current/PLANNING_DESK_DESIGN.md`
8. `docs/current/DESKTOP_WEB_ARCHITECTURE.md`
9. `docs/current/PAYKITODO_SESSION_LEDGER.md`
10. `docs/current/AI_RECOGNITION_VERIFICATION.md`

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
