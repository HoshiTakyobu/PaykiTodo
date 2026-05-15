# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.7.24` / `versionCode 181`.
- Latest debug APK path after build: `app/build/outputs/apk/debug/PaykiTodo-1.7.24-debug.apk`.
- Latest completed verification before final handoff:
  - `./gradlew.bat compileDebugKotlin`
  - `./gradlew.bat testDebugUnitTest --tests com.example.todoalarm.data.PlanningMarkdownParserTest`
- This round implements four requested behavior changes:
  - Daily-board onboarding is readable in dark mode and can be reset from Settings.
  - Planning Desk local parser recognizes more Chinese natural DDL expressions.
  - Settings AI config supports multiple ordered providers for future fallback calls.
  - Todo editor folds advanced fields behind 更多选项 by default.
- Do not push to GitHub unless the user explicitly asks.

## Latest Changes In 1.7.24

1. Upgraded app version metadata to `1.7.24` / `versionCode 181`.
2. Onboarding card now uses `surfaceVariant` + `onSurface`; Settings -> 关于 -> 使用说明 adds `重新显示新手引导`.
3. `AppSettingsStore` now has `resetOnboarding()` and stores `hasSeenOnboarding`.
4. `PlanningMarkdownParser` now recognizes:
   - date-context fuzzy DDL words: `晚上` = 22:00, `下午` = 17:00, `上午` / `中午` = 12:00, `早上` = 09:00
   - before-time DDL: `5点前`, `16:30之前`, `明天下午3点前`
   - non-checkbox DDL keyword todos with `截止`, `deadline`, or `ddl`
   - recurrence hint words, surfaced only as preview messages
5. Settings -> AI 调用配置 now edits `List<PlanningAiProvider>` with add/edit/delete/long-press delete/enable-disable/up-down ordering.
6. AI provider JSON storage keeps backward compatibility with old single-provider fields, preserves API keys on backup import, and excludes keys from backup export.
7. `PlanningAiCaller.callWithFallback` is available for later integration and retries eligible providers in order on auth/rate-limit/server/network failures.
8. `TodoEditorDialog` now shows title, DDL, and group first; notes/reminders/recurrence/ring/vibration are inside animated 更多选项, with auto-expand for existing advanced todos.
9. Existing `1.7.23` baseline changes are included in this working tree: onboarding card introduction, tomorrow no-schedule planning jump, desktop-sync copy buttons, reminder placeholder copy, and the Planning Desk tutorial page for other import methods.

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/AppSettingsStore.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiProvider.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiCaller.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningMarkdownParser.kt`
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

1. Install `PaykiTodo-1.7.24-debug.apk`.
2. Verify the app launches without crash.
3. Verify Settings -> 关于 -> 使用说明 -> 重新显示新手引导 causes the onboarding card to show again on 每日看板.
4. Verify onboarding card text remains readable in dark mode.
5. Verify Settings -> AI 调用配置 can add/edit/delete/long-press delete providers, toggle enabled, move providers up/down, save, leave, and reopen with the same values.
6. Verify backup JSON still excludes real AI API keys.
7. Verify Todo editor new-todo mode shows only title / DDL / group until 更多选项 is expanded.
8. Verify editing a todo with notes/custom reminder offsets/recurrence/non-default ring or vibration auto-expands 更多选项.
9. Verify Planning Desk preview parses `# 今日计划` + `- [ ] 晚上交论文` to today's 22:00 and `- [ ] 上午开会` to today's 12:00.
10. Verify `- [ ] 5点前交作业`, `- [ ] 明天下午3点前提交`, and `- [ ] 16:30之前发邮件` produce expected DDLs.
11. Verify `交论文 截止明天 23:59，每天复盘` becomes a todo with a natural inference message and recurrence hint.
12. Verify existing Planning Desk parser cases still work: `任务M ddl 15:00`, explicit `#ddl` priority, compact date headings, natural schedules, and `#imported` write-back.

## Deferred Larger Work

- Actual Planning Desk AI recognition is still not wired into import. Local rule parsing remains the default.
- Drag-and-drop planning, Gantt chart, AI auto-planning, complex project tree, Markdown highlighting/rich editor, and deeper desktop parity remain deferred.
- Device-side UI verification is still required for 1.7.24.

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
