# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.7.25` / `versionCode 182`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.7.25-debug.apk`.
- Latest completed verification before final handoff:
  - `./gradlew.bat compileDebugKotlin`
  - `./gradlew.bat testDebugUnitTest --tests com.example.todoalarm.data.PlanningMarkdownParserTest`
  - `./gradlew.bat testDebugUnitTest --tests com.example.todoalarm.data.PlanningAiRecognizerTest`
  - `./gradlew.bat testDebugUnitTest --tests com.example.todoalarm.data.PlanningRecognitionServiceTest`
  - `./gradlew.bat assembleDebug`
- This round implements four requested behavior changes:
  - Daily-board onboarding is readable in dark mode and can be reset from Settings.
  - Planning Desk local parser recognizes more Chinese natural DDL expressions.
  - Settings AI config supports multiple ordered providers and Planning Desk now uses them for optional AI recognition.
  - Todo editor folds advanced fields behind 更多选项 by default.
- A follow-up patch routes desktop-web Planning Desk parsing through the same shared AI-first / local-fallback recognition service as the phone UI, and desktop preview summaries now surface fallback messages.
- Another small follow-up removes the built-in Planning Desk example placeholder from both phone and desktop editors; examples remain available from help/tutorial surfaces only.
- A follow-up `1.7.24` hotfix strengthens the dark-theme text shadow for floating daily-board block titles over the wallpaper background, without changing version metadata.
- Do not push to GitHub unless the user explicitly asks.

## Latest Changes In 1.7.25

1. Upgraded app version metadata to `1.7.25` / `versionCode 182`.
2. Onboarding card now uses `surfaceVariant` + `onSurface`; Settings -> 关于 -> 使用说明 adds `重新显示新手引导`.
3. `AppSettingsStore` now has `resetOnboarding()` and stores `hasSeenOnboarding`.
4. `PlanningMarkdownParser` now recognizes:
   - date-context fuzzy DDL words: `晚上` = 22:00, `下午` = 17:00, `上午` / `中午` = 12:00, `早上` = 09:00
   - before-time DDL: `5点前`, `16:30之前`, `明天下午3点前`
   - non-checkbox DDL keyword todos with `截止`, `deadline`, or `ddl`
   - recurrence hint words, surfaced only as preview messages
5. Settings -> AI 调用配置 now edits `List<PlanningAiProvider>` with add/edit/delete/long-press delete/enable-disable/up-down ordering.
6. AI provider JSON storage keeps backward compatibility with old single-provider fields, preserves API keys on backup import, and excludes keys from backup export.
7. `PlanningAiRecognizer` now provides the built-in JSON-only prompt, calls enabled AI providers in order, parses AI JSON into `PlanningParsedCandidate`, skips `#imported` source lines, and keeps preview confirmation mandatory.
8. `PlanningAiCaller.callWithFallback` retries eligible providers in order on auth/rate-limit/server/network failures; AI failure or incomplete config falls back to local `PlanningMarkdownParser`.
9. Phone and desktop Planning Desk recognition now share `PlanningRecognitionService`, so `/api/planning/parse` and the phone-side parse action use the same AI prompt, provider ordering, and fallback messaging.
10. Planning Desk recognition is async in the UI and shows `识别中` while network recognition is running; desktop preview metadata now also displays shared recognition messages.
11. Planning Desk editors on phone and desktop now open as fully blank writing surfaces; examples live in tutorial/help content instead of inline placeholders.
12. `TodoEditorDialog` now shows title, DDL, and group first; notes/reminders/recurrence/ring/vibration are inside animated 更多选项, with auto-expand for existing advanced todos.
13. Floating daily-board block titles such as `今日待办` / `今日日程` now use a stronger dark-theme text shadow so they remain readable over the dark wallpaper; greeting and schedule cards remain protected by high-opacity surfaces.
14. Existing `1.7.23` baseline changes are included in this working tree: onboarding card introduction, tomorrow no-schedule planning jump, desktop-sync copy buttons, reminder placeholder copy, and the Planning Desk tutorial page for other import methods.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/AppSettingsStore.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiProvider.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiCaller.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiRecognizer.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningMarkdownParser.kt`
- `app/src/test/java/com/example/todoalarm/data/PlanningAiRecognizerTest.kt`
- `app/src/test/java/com/example/todoalarm/data/PlanningMarkdownParserTest.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoEditorDialog.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `CHANGELOG.md`
- `README.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`

## Current Verification Focus

1. Install `PaykiTodo-1.7.25-debug.apk`.
2. Verify the app launches without crash.
3. Verify Settings -> 关于 -> 使用说明 -> 重新显示新手引导 causes the onboarding card to show again on 每日看板.
4. Verify onboarding card text remains readable in dark mode.
5. Verify `今日待办` / `今日日程` titles stay readable over the dark wallpaper and do not look overly heavy in light mode.
6. Verify Settings -> AI 调用配置 can add/edit/delete/long-press delete providers, toggle enabled, move providers up/down, save, leave, and reopen with the same values.
7. Verify enabling AI recognition with a complete provider lets both phone and desktop Planning Desk free-form text produce editable preview candidates.
8. Verify provider failure, timeout, disabled config, or incomplete config falls back to local rules instead of blocking recognition, and that desktop preview metadata shows the fallback message.
9. Verify backup JSON still excludes real AI API keys.
10. Verify Todo editor new-todo mode shows only title / DDL / group until 更多选项 is expanded.
11. Verify editing a todo with notes/custom reminder offsets/recurrence/non-default ring or vibration auto-expands 更多选项.
12. Verify Planning Desk preview parses `# 今日计划` + `- [ ] 晚上交论文` to today's 22:00 and `- [ ] 上午开会` to today's 12:00.
13. Verify `- [ ] 5点前交作业`, `- [ ] 明天下午3点前提交`, and `- [ ] 16:30之前发邮件` produce expected DDLs.
14. Verify `交论文 截止明天 23:59，每天复盘` becomes a todo with a natural inference message and recurrence hint.
15. Verify existing Planning Desk parser cases still work: `任务M ddl 15:00`, explicit `#ddl` priority, compact date headings, natural schedules, and `#imported` write-back.

## Deferred Larger Work

- Planning Desk AI recognition is now wired into preview generation, but still needs device-side testing with the user's real providers.
- Drag-and-drop planning, Gantt chart, AI auto-planning, complex project tree, Markdown highlighting/rich editor, and deeper desktop parity remain deferred.
- Device-side UI verification is still required for 1.7.25.

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

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
