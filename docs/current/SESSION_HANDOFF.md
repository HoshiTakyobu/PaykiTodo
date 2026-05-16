# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.2` / `versionCode 196`.
- This round carries forward AI 日报 / 周报 after the `1.9.0` focus-mode baseline and applies a follow-up launcher-widget visual hotfix.
- Latest debug APK after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.2-debug.apk`.
- Local commits before this hotfix: `c20d18a` for focus mode and `104c9aa` for AI reports plus the previous widget visual pass.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.2 Widget Hotfix Changes

1. Android `今日看板` launcher widget default provider dimensions now prefer a square / vertical daily-board card instead of a shallow flat list.
2. The widget top header is lighter and shorter so the card stack becomes the main visual subject.
3. Greeting, focus, todo, empty, and schedule cards use more solid daily-board-style rounded surfaces and less glass/list-border styling.
4. Todo and schedule vertical color strips now have more breathing room from text; schedule rows have slightly larger title sizing and vertical padding to resemble the in-app daily board.
5. Version metadata moved to `1.9.2` / `versionCode 196`.

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

Real launcher verification is still needed for the widget visual pass because RemoteViews rendering varies by launcher.

Local device verification blocker observed in this session:

1. `adb` is available at `C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe`, but no device is attached.
2. `Pixel_8` AVD exists, but its configured system image `system-images\android-34\google_apis_playstore\x86_64\` is missing.
3. No local `sdkmanager.bat` was found, so the missing system image cannot be installed automatically from this environment.
4. Continue device-only checks after connecting a phone or repairing the local Android SDK / AVD.

Then verify on a real device after installing `PaykiTodo-1.9.2-debug.apk`:

1. enable daily report at current time + 1 minute and wait for automatic generation;
2. disable daily report and confirm the next scheduled daily report is canceled;
3. tap `立即生成一次日报` and confirm `AI 日报` is created / updated;
4. test with AI disabled or broken to confirm local template fallback;
5. confirm the notification appears when permission is granted and opens the matching report note;
6. test Sunday weekly report generation into `AI 周报`;
7. test boot/time/timezone recovery on device.

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
