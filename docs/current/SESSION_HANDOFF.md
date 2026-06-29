# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.14.21`
  - `versionCode = 341`
  - database version = `28`
- Latest debug APK:
  - `app/build/outputs/apk/debug/PaykiTodo-1.14.21-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`

## Completed In This Round

本轮升级 AI 日报/周报与规划识别器的 prompt 工程，未改动数据库版本与循环语义。

1. **AI 日报/周报 prompt 全面化**
   - `DailyReportGenerator.kt` 新增 `buildReportSystemPrompt(reportType)`，把原来"简短、不寒暄"的一句话 system prompt 扩成一份完整写作规范：纯文本中文小节、覆盖数据区每个维度、先客观分析再给具体建议、无数据维度简短带过、不编造。
   - 重写 `buildDailyPrompt`：日报目标 350-500 字，加入完成率、星期、今日/明日日程（带地点后缀）、签到投入、明日 DDL；写作要求拆成"今日回顾 / 需要补救 / 时间投入 / 明日重点 / 一句建议"中文小节。
   - 重写 `buildWeeklyPrompt`：周报目标 450-650 字，取数从 12 条放宽到 20 条，写作要求拆成"本周概览 / 完成亮点 / 遗留与反思 / 节奏观察 / 下周重点 / 一句建议"。
   - 新增辅助 `todoLine`（标题 + DDL + 备注摘要 40 字内）、`eventLocationSuffix`（地点后缀）、`weekdayLabel`（中文星期）。
   - 保留原有硬约束：没有"今日日程签到投入"字段时不推断、不写"0 分钟"；只用数据区内容、不编造。

2. **规划识别器相对日期推算强化**
   - `PlanningAiRecognizer.kt` 的 `buildUserPrompt` 注入"当前时间（星期）"和"今天/明天/后天"绝对日期锚点。
   - `buildSystemPrompt` 在"今天是 X"处补星期，新增 `weekdayLabel`；把第 10 条相对日期规则扩写为逐字推算规范：今天/明天/后天/大后天/昨天、单独星期=未来最近那天、本周X/下周X/下下周X、X 天后、时段词到时刻映射（早上 09:00、上午 12:00、中午 12:00、下午 17:00、傍晚 18:00、晚上 22:00）、推理示例、无法确定时 `dueAt=null` 并在 message 说明，绝不把相对词原样留在标题里。

3. **版本与文档**
   - 版本元数据升级到 `1.14.21 / versionCode 341`。
   - 更新 `README.md` 当前源码版本、`CHANGELOG.md`、`PROJECT_STATUS.md`、`CURRENT_TASK.md` 和本交接。

## Validation

Passed:

- `./gradlew.bat --no-daemon :app:compileDebugKotlin`
- `./gradlew.bat --no-daemon :app:assembleDebug`
- APK metadata inspection (`aapt dump badging`) for `versionName = 1.14.21`, `versionCode = 341`, output `app/build/outputs/apk/debug/PaykiTodo-1.14.21-debug.apk`
- Installed on `emulator-5554` and launched `MainActivity` without crash.

未执行：真实 provider key 下的日报/周报实际生成观感抽查与规划识别相对日期解析冒烟（本地无 key，且 key 不入库）。

## Still Required Before Reporting Complete

1. Commit the round with a concrete Chinese commit message.

## Notes For Next Session

- 本轮纯 prompt 文案改动，没有改动报告的本地兜底生成（`buildLocalDaily` 等）逻辑结构；如果要让本地兜底也更全面，需另外改这些函数。
- 规划识别相对日期推算依赖模型遵循 prompt；若后续发现仍有把"周五"原样留在标题的情况，应考虑在解析后做一层后处理校正，而不是只靠 prompt。
- 日报/周报字数目标已上调，注意观察实际 token 消耗与 provider 超时；如有截断可在 `PlanningAiRequest` 侧调整。
- Do not commit or write Desktop Sync tokens, AI keys, private Base URLs, signing files, generated APKs, or local diagnostic logs.
