# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is being advanced to `1.9.0` / `versionCode 193`.
- Latest local debug APK after the current build round: `app/build/outputs/apk/debug/PaykiTodo-1.9.0-debug.apk`.
- The Android `今日看板` launcher-widget visual follow-up was committed separately as `1d768ef` and is not part of the current uncommitted focus-session work.
- Current uncommitted focus-session work implements 专注模式 and still needs full final verification plus commit.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.0 Focus-Mode Changes

1. `FocusSession` was added as Room table `focus_sessions`, with indices for `startedAtMillis` and `todoId`.
2. Database version moved to `12`; `MIGRATION_11_12` creates the focus-session table and indices.
3. Repository / DAO now save focus sessions, observe them, query today's sessions / completed minutes, and include focus sessions in backup / restore.
4. App settings now store focus default duration, extension duration, keep-screen-on, and a documented-only notification-suppression preference.
5. Settings -> `专注模式` exposes duration sliders and switches for those preferences.
6. `FocusActivity` implements full-screen countdown, circular progress, pause / continue, finish confirmation, abandon confirmation, zero-time vibration, extension, save-before-exit behavior, and completion feedback.
7. Active todo long-press action sheets now include `开始专注 · X 分钟` while delete remains behind confirmation.
8. Daily board now includes a `今日已专注` statistics card and a `自由专注` entry.

## Files Most Relevant To 1.9.0

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/todoalarm/TodoApplication.kt`
- `app/src/main/java/com/example/todoalarm/data/FocusSession.kt`
- `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/data/AppSettingsStore.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupModels.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/ui/FocusActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoCards.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `app/src/main/assets/wiki/index.html`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

## Verification Status

Completed so far:

1. `./gradlew.bat :app:compileDebugKotlin`

Still required before committing `1.9.0`:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat testDebugUnitTest`
3. `./gradlew.bat assembleDebug`
4. `git diff --check`

Then verify on a real Android device after installing `PaykiTodo-1.9.0-debug.apk`:

1. long-press a todo and start a bound focus session;
2. start a free focus session from the daily-board focus card;
3. pause / continue, finish early, abandon, let countdown reach zero, extend once, and complete;
4. confirm completed focus minutes update on the daily-board focus card;
5. confirm screen-on and haptic behavior on device.

## Pending Next Goal

After the `1.9.0` focus-mode commit, implement `docs/goals/2026-05-17-paykitodo-ai-daily-report-goal.md` as `1.9.1` / next versionCode.

The two goal docs are intentionally untracked local task specs unless the user explicitly asks to keep them in git.

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
