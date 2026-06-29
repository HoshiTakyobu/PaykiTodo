# Current Task

## Active Development Focus

Active immediate task: finish `1.14.21 / versionCode 341` AI 报告 prompt 升级与规划识别器相对日期推算强化轮次。

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, or personal backups/logs.

## Current Round Scope

This round upgrades the prompt engineering behind AI 日报/周报 and the planning recognizer, without touching repository/recurrence semantics or data schema:

1. AI 日报/周报 system prompt 与数据 prompt 从"简短"改为"全面、具体、覆盖每个数据维度"，纯文本中文小节、先分析再建议。
2. 数据 prompt 增加完成率、星期、待办 DDL/备注摘要、日程地点后缀，周报取数放宽到 20 条。
3. 规划识别器注入星期与绝对日期锚点，强制把相对日期词逐字推算成绝对日期，绝不把相对词留在标题里。
4. 不改动数据库版本（保持 `28`）、不改动循环语义。

## Current Status

Implemented so far:

1. 版本号升级到 `1.14.21 / versionCode 341`；数据库版本保持 `28`。
2. `DailyReportGenerator.kt`：新增 `buildReportSystemPrompt`，重写 `buildDailyPrompt`/`buildWeeklyPrompt`，新增 `todoLine`/`eventLocationSuffix`/`weekdayLabel` 辅助；日报目标 350-500 字、周报 450-650 字，中文小节分段，保留"无签到投入不写 0 分钟""不编造"约束。
3. `PlanningAiRecognizer.kt`：`buildUserPrompt`/`buildSystemPrompt` 注入今天星期与今天/明天/后天绝对日期锚点，新增 `weekdayLabel`，扩写相对日期推算规则（周X/下周X/X 天后等逐字算绝对日期、时段词到时刻映射、推理示例、无法确定时 dueAt=null）。

## Validation

Completed for this round:

1. `./gradlew.bat --no-daemon :app:compileDebugKotlin`
2. `./gradlew.bat --no-daemon :app:assembleDebug`
3. APK metadata inspection for `versionName = 1.14.21`, `versionCode = 341`, output `app/build/outputs/apk/debug/PaykiTodo-1.14.21-debug.apk`
4. Installed on `emulator-5554` and launched `MainActivity` without crash.

## Remaining

1. Commit the completed round with a concrete Chinese commit message.
2. 真实 provider key 下抽查一次日报/周报实际生成观感与规划识别相对日期解析（本地无 key，未冒烟）。
