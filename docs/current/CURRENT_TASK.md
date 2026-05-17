# Current Task

## Active Development Focus

The current round is PaykiTodo `1.9.8` / `versionCode 202`.

Primary goal: move AI daily / weekly reports out of Planning Desk and into an independent read-only-ish `AI 报告` archive, while fixing the `了解 AI 日报` help surface that was visibly biased left and hard to read.

## Completed In This Round

1. Added Room entity `AiReport` and enum `AiReportType` with a new `ai_reports` table.
2. Bumped database version to `13` and added `MIGRATION_12_13` to create `ai_reports` plus indices.
3. Added DAO / Repository APIs to observe, insert, query, delete, clear, export, and import AI reports.
4. Added one-time legacy migration: Planning Desk notes titled `AI 日报` / `AI 周报` are parsed into individual `AiReport` rows, then removed from Planning Desk; `lastOpenedPlanningNoteId` is cleared if it pointed at a removed report note.
5. Changed `DailyReportGenerator.generateDaily` / `generateWeekly` to save `AiReport` rows instead of prepending Markdown into Planning Desk notes.
6. Report records now keep generation time, covered period, provider name, and local-fallback state.
7. Report notifications now carry `EXTRA_OPEN_AI_REPORT_ID`; tapping a report notification routes to `AI 报告` and opens the matching detail sheet.
8. Drawer navigation now includes `AI 报告` after `规划台` and before `历史记录`.
9. Added `AiReportPanel`: all / daily / weekly filters, preview cards, provider/local pills, detail view, empty-state guidance, and delete confirmation.
10. Removed the obsolete Planning Desk purple auto-report hint.
11. Settings -> `AI 调用配置` -> `AI 日报 / 周报` now says reports are written to `AI 报告` archive, not Planning Desk.
12. `了解 AI 日报` changed from the problematic bottom sheet to a centered dialog and its “报告在哪看” guidance points to notification deep links and drawer -> `AI 报告`.
13. JSON backup / restore now includes `aiReports`.
14. In-app Wiki, README, CHANGELOG, and current docs were updated for the new report archive behavior.
15. Version metadata moved to `1.9.8` / `versionCode 202`.

## Verification Completed This Round

1. `./gradlew.bat :app:compileDebugKotlin` passed after the AI report archive implementation.
2. `./gradlew.bat assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.9.8-debug.apk`.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `node --check app/src/main/assets/desktop-web/app.js` passed.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` confirms `versionCode=202`, `versionName=1.9.8`, and `outputFile=PaykiTodo-1.9.8-debug.apk`.
7. Android Emulator smoke verification reused `emulator-5554`: the app launched without startup crash, drawer -> `AI 报告` worked, a migrated report detail opened, and Settings -> AI 调用配置 -> `了解 AI 日报` showed a centered/readable dialog.

## Immediate Practical Next Steps

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.8-debug.apk` on the user's physical phone for the device-only checks below.
2. Do not push unless the user explicitly asks.

## Device Verification Needed After Installing 1.9.8

1. Upgrade from a build that has Planning Desk `AI 日报` / `AI 周报` notes and confirm those notes disappear from Planning Desk.
2. Open drawer -> `AI 报告`; confirm migrated historical reports appear in reverse chronological order.
3. Switch `全部` / `日报` / `周报` filters and confirm filtering is correct.
4. Tap a report card and confirm the full detail content opens.
5. Long-press a card and confirm delete requires confirmation.
6. Tap Settings -> AI 调用配置 -> `立即生成一次日报`; confirm a new report appears at the top of `AI 报告`, not in Planning Desk.
7. Tap an AI report notification and confirm it opens the matching report detail.
8. Export JSON backup and confirm `aiReports` exists; restore and confirm report history survives.
9. Open Settings -> AI 调用配置 -> `了解 AI 日报` and confirm the dialog is centered and readable on the phone.

## Local Device Verification Reality

- `adb` is available at `C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- `emulator-5554` has been used in recent rounds for pre-phone smoke tests and may still be available.
- Latest recorded emulator pass installed `app/build/outputs/apk/debug/PaykiTodo-1.9.8-debug.apk` on `emulator-5554` and checked app launch, `AI 报告`, report detail, Settings -> AI 调用配置, and the centered `了解 AI 日报` dialog.
- Emulator verification is useful for startup, navigation, and visible UI regressions; real device verification is still required for OEM alarm behavior, notification shade visuals, haptics, launcher widget rendering, and reboot/time-change recovery.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.
