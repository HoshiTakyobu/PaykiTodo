# Current Task

## Active Development Focus

The current round is PaykiTodo `1.7.25` / `versionCode 182`, focused on turning the existing Planning Desk AI provider configuration into a real optional recognition path while preserving the `1.7.24` refinements:

1. Make the daily-board onboarding card readable in dark mode and resettable from Settings.
2. Expand Planning Desk local natural-language parsing for common Chinese DDL expressions.
3. Use Planning Desk AI configuration for actual OpenAI-compatible recognition with ordered fallback.
4. Fold advanced Todo editor fields behind a 更多选项 section so new-todo creation starts simpler.

## Completed In 1.7.25

1. The onboarding card now uses `surfaceVariant` + `onSurface`, avoiding low-contrast text in dark mode.
2. Settings -> 关于 -> 使用说明 now includes `重新显示新手引导`; tapping it sets `hasSeenOnboarding = false`, and the next daily-board visit shows the card again.
3. `PlanningMarkdownParser` now recognizes date-context fuzzy DDL words such as `晚上交论文` / `上午开会`, before-time forms such as `5点前交作业`, `16:30之前发邮件`, and `明天下午3点前提交`, plus bare non-checkbox DDL keyword lines such as `交论文 截止明天 23:59`.
4. Planning Desk preview messages now flag natural inference with `根据自然文本推断，建议确认`, and loop hints such as `每天` / `每周` add a reminder to configure recurrence after import.
5. Settings -> AI 调用配置 stores `List<PlanningAiProvider>` as JSON in SharedPreferences, migrates old single-provider fields into the first provider, and preserves local API keys across backup import when backup JSON excludes keys.
6. The AI config UI supports adding, editing, deleting, long-press deletion, enable/disable switches, and up/down priority ordering for providers.
7. Planning Desk “识别” now uses enabled AI providers first when configuration is complete. The internal prompt requires JSON-only output, converts AI items into existing preview candidates, and keeps preview confirmation mandatory.
8. `PlanningAiCaller.callWithFallback` retries enabled providers in order on 401/403/429/5xx and network timeout/DNS failures, but not on 400 or cancellation; AI failure or incomplete configuration falls back to local rules.
9. `TodoEditorDialog` defaults new todos to title, DDL, and group; notes, reminder input, recurrence, ring, and vibration live under 更多选项 with `AnimatedVisibility`.
10. Existing todos auto-expand 更多选项 when they contain notes, custom reminder offsets, recurrence, or non-default ring/vibration state.
11. Daily-board floating block titles now use stronger dark-theme text shadow over the wallpaper background, while light-theme shadow remains subtle.
12. Version metadata is now `1.7.25` / `versionCode 182`.

## Verification Completed This Round

1. `./gradlew.bat compileDebugKotlin` succeeded.
2. `./gradlew.bat testDebugUnitTest --tests com.example.todoalarm.data.PlanningMarkdownParserTest` succeeded.
3. `./gradlew.bat assembleDebug` succeeded after the daily-board title readability hotfix.
4. `./gradlew.bat testDebugUnitTest --tests com.example.todoalarm.data.PlanningAiRecognizerTest` succeeded.

Final release verification still requires:

1. `./gradlew.bat assembleDebug` after the `1.7.25` AI integration.
2. Device-side smoke testing of Planning Desk AI recognition, AI fallback to local rules, onboarding reset, daily-board dark/light readability, AI provider list persistence, Todo editor folding, and Planning Desk natural DDL preview behavior.

## Immediate Practical Next Steps

When testing, use:

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.7.25-debug.apk`.
2. Open 每日看板, dismiss the onboarding card, then use Settings -> 关于 -> 使用说明 -> 重新显示新手引导 and confirm the card returns on the board.
3. Open Settings -> AI 调用配置 and verify adding, editing, enabling/disabling, deleting, moving providers up/down, and enabling AI recognition persists after leaving and reopening Settings.
4. Open the Todo editor for a new todo and confirm only title / DDL / group are visible until 更多选项 is expanded.
5. Edit an existing todo with notes, custom reminders, recurrence, or non-default ring/vibration and confirm 更多选项 auto-expands.
6. In Planning Desk, write free-form text and verify AI recognition creates preview candidates when an AI source is enabled, while invalid/disabled AI configuration falls back to local rules.
7. In Planning Desk preview, verify `# 今日计划` followed by `- [ ] 晚上交论文`, `- [ ] 5点前交作业`, `- [ ] 明天下午3点前提交`, and `交论文 截止明天 23:59，每天复盘`.

## Commit Message Rule

PaykiTodo commit messages should describe product behavior changes and bug/debug reasoning. They should not primarily document push state, validation commands, or vague process notes. Commit subjects must not append version-bump tails such as `并升级到1.7.11`.

## Current External Dependency

No external file is needed for the current `1.7.25` verification task. If testing AI config, use disposable API keys; real keys should remain local to the phone and must not be committed.
