# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.8` / `versionCode 202`.
- Main user request: the `了解 AI 日报` surface was visibly biased left and unreadable; also implement `docs/goals/2026-05-17-paykitodo-1.9.8-goal.md`.
- Core behavior change: AI 日报 / 周报 no longer live in Planning Desk documents. They now live in independent `AI 报告` archive records backed by Room table `ai_reports`.
- Latest debug APK target after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.8-debug.apk`.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.8 AI Report Archive Pass

1. Added `AiReport` / `AiReportType` and Room table `ai_reports`.
2. Database version moved to `13`; `MIGRATION_12_13` creates `ai_reports` and indices.
3. DAO / Repository now support observing, saving, querying, deleting, clearing, exporting, and importing AI reports.
4. `LegacyAiReportMigration` runs once at app startup: it parses legacy Planning Desk `AI 日报` / `AI 周报` notes into individual report rows, deletes those notes, and clears `lastOpenedPlanningNoteId` if it pointed to a removed report.
5. `DailyReportGenerator.generateDaily` / `generateWeekly` save `AiReport` rows with generated time, period range, provider name, and local-fallback flag.
6. `DailyReportNotifier` now writes `MainActivity.EXTRA_OPEN_AI_REPORT_ID` and fallback text says the report is written to AI report archive.
7. `MainActivity` / `DashboardLaunchRoute` route report notifications to `DashboardSection.AI_REPORTS`.
8. Drawer now contains `AI 报告` after `规划台`.
9. `AiReportPanel` lists reports with `全部 / 日报 / 周报` filters, preview cards, provider/local pills, detail sheet, empty state, and delete confirmation.
10. Planning Desk no longer shows the obsolete purple auto-report hint for `AI 日报` / `AI 周报`.
11. Settings -> `AI 调用配置` -> `AI 日报 / 周报` text points to `AI 报告` archive instead of Planning Desk.
12. `了解 AI 日报` is now a centered `AlertDialog`, avoiding the left-biased bottom-sheet rendering that the user reported.
13. Backup JSON now includes `aiReports`, and restore writes them back to `ai_reports`.
14. README, CHANGELOG, in-app Wiki, and current docs were updated for this behavior.

## Verification Status

Completed:

1. `./gradlew.bat :app:compileDebugKotlin` passed after implementing the archive and centered dialog.
2. `./gradlew.bat assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.9.8-debug.apk`.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `node --check app/src/main/assets/desktop-web/app.js` passed.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=202`, `versionName=1.9.8`, and `outputFile=PaykiTodo-1.9.8-debug.apk`.
7. Android Emulator smoke verification reused `emulator-5554`, installed `PaykiTodo-1.9.8-debug.apk`, launched the app without startup crash, opened drawer -> `AI 报告`, opened a migrated report detail, opened Settings -> AI 调用配置, and confirmed `了解 AI 日报` appears as a centered/readable dialog instead of the previous left-biased sheet.

Remaining after local completion:

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.8-debug.apk` on the user's physical phone and verify OEM-specific notification, alarm, lock-screen, widget, and haptic behavior.
2. Do not push unless the user explicitly asks.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/AiReport.kt`
- `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/data/LegacyAiReportMigration.kt`
- `app/src/main/java/com/example/todoalarm/data/DailyReportGenerator.kt`
- `app/src/main/java/com/example/todoalarm/alarm/DailyReportNotifier.kt`
- `app/src/main/java/com/example/todoalarm/TodoApplication.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/AiReportPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupModels.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/assets/wiki/index.html`
- `docs/goals/2026-05-17-paykitodo-1.9.8-goal.md`

## Known Worktree Notes

- Branch is `main`; previous local branch state was far ahead of origin. Do not push without user authorization.
- Existing untracked temp UI dumps such as `.tmp-*.xml` were present before this round and should not be committed unless intentionally needed.
- Goal prompt files under `docs/goals/` should remain separate archive commits after secret scanning.

## Android Emulator Verification Rule

If an Android Emulator is started or reused, record:

- device id / AVD name
- installed APK path
- checked screens or flows
- what was actually verified

This avoids confusion when an emulator window remains open on the user's desktop.

Latest recorded emulator use:

- Device id: `emulator-5554`
- Installed APK: `app/build/outputs/apk/debug/PaykiTodo-1.9.8-debug.apk`
- Checked flows: app launch, drawer navigation to `AI 报告`, report detail opening, Settings -> AI 调用配置, and `了解 AI 日报`
- Verified result: `AI 报告` archive is reachable and populated from legacy migration data; the `了解 AI 日报` help surface is centered/readable on the emulator
- Boundary: this emulator pass does not replace real-phone verification for OEM notification, vibration, lock-screen, widget, alarm, reboot, or battery-management behavior
