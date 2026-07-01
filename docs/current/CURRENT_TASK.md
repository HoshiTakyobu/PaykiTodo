# Current Task

## Active Development Focus

Active immediate task: finish `1.15.0 / versionCode 342` 活动计时器（实际记录层）轮次，落地 `docs/goals/2026-06-30-paykitodo-1.15.0-activity-timer-goal.md` 的六项任务。

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, or personal backups/logs.

## Current Round Scope

This round adds an independent 实际记录层 (activity records) that sits alongside todos/events and answers "我今天到底做了什么"：

1. 数据层：新增 `ActivityRecord` 实体（RUNNING/PAUSED/FINISHED、TIMER/MANUAL），数据库升级 `28 → 29`（新增 `activity_records` 表 + 两个索引），DAO/Repository 秒表模型（同一时刻只允许一条非 FINISHED 记录，finish 时有效用时 <5 秒直接丢弃）。
2. 服务层：`ActivityTimerService` 前台服务（`specialUse` 类型、`activity_timer` LOW 通知渠道、`setUsesChronometer` 自动走秒、暂停/继续/结束动作按钮）+ `ActivityTimerReceiver` 广播接收器，均已在 Manifest 声明。
3. ViewModel 层：暴露 `activeActivityRecord` / `todayActivityRecords` 流并接入合并状态；提供 start/pause/resume/finish/addManual/delete 动作，动作后同步 start/refresh/stop 前台服务。
4. UI 层：看板顶部计时条（每秒刷新）、「今日做了什么」卡、右下角「开始记录」FAB、开始记录/随手补记/记录详情对话框。
5. 报告层：`DailyContext` / `WeeklyContext` 新增 `todayActivityRecords`/`todayActivityMinutes` 与周维度字段，日报/周报数据 prompt 与写作要求接入实际记录。
6. 备份/版本/文档：`AppSnapshot` 新增 `activityRecords`，导出全部已完成记录、导入按 id 重置去重；版本升级 `1.15.0 / 342`；同步 README、CHANGELOG、STATUS、HANDOFF、LEDGER。

不做（本轮明确不做）：电脑同步、独立 Widget、挂待办自动完成、改动循环/完成逻辑。

## Current Status

全部六项任务代码已实现并接通（数据层 → 服务 → ViewModel → UI → 报告 → 备份），编译与打包通过。

## Validation

Completed for this round:

1. `./gradlew.bat --no-daemon :app:compileDebugKotlin` — 通过。
2. `./gradlew.bat --no-daemon :app:assembleDebug` — 通过。
3. Room schema `29.json` 已生成于 `app/schemas/com.example.todoalarm.data.AppDatabase/29.json`。
4. APK metadata inspection：`output-metadata.json` 记录 `versionName = 1.15.0`、`versionCode = 342`、输出 `app/build/outputs/apk/debug/PaykiTodo-1.15.0-debug.apk`。

未执行（本地环境限制）：

- 真机/模拟器安装冒烟（本机 `adb` 不在 PATH，未做启动、计时、暂停、继续、结束、<5 秒丢弃、杀进程续跑、看板计时条、今日做了什么卡、补记对话框、备份/恢复的手动验证）。
- 真实 provider key 下 AI 日报/周报实际记录段落观感抽查（本地无 key，key 不入库）。

## Remaining

1. Commit the completed round with a concrete Chinese commit message.
2. 在有 `adb` 与模拟器/真机的环境按目标文档「验证清单」逐条冒烟，尤其是杀进程后计时续跑与 <5 秒丢弃。
