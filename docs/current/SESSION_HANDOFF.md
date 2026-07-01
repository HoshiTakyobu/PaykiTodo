# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.15.0`
  - `versionCode = 342`
  - database version = `29`
- Latest debug APK:
  - `app/build/outputs/apk/debug/PaykiTodo-1.15.0-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`

## Completed In This Round

本轮按 `docs/goals/2026-06-30-paykitodo-1.15.0-activity-timer-goal.md` 落地「实际记录层」（活动计时器 + 随手补记），新增数据库版本 29 与 `activity_records` 表，未改动循环/完成语义。

1. **数据层（任务 1）**
   - 新增 `ActivityRecord.kt`：`activity_records` 实体、`ActivityRecordState`（RUNNING/PAUSED/FINISHED）、`ActivityRecordSource`（TIMER/MANUAL）、`effectiveDurationMillis` 扩展。
   - `DatabaseMigrations.kt` 新增 `MIGRATION_28_29`（建表 + 两个索引），`AppDatabase.kt` 注册实体并升到 `version = 29`，`TodoApplication.kt` 挂载迁移；Room schema `app/schemas/.../29.json` 已导出。
   - `TodoDao.kt` 新增 activity 记录的增删改查、观察当前活动记录、按范围/全部查询已完成记录、清空。
   - `TodoRepository.kt` 新增秒表状态机：`startActivityTimer`（开始前先 finish 旧记录）、`pauseActivityTimer`、`resumeActivityTimer`、`finishActivityTimer`（有效用时 <5 秒丢弃不写库）、`addManualRecord`、`updateActivityRecordDetails`、`deleteActivityRecord`、观察/范围查询。

2. **服务层（任务 2）**
   - 新增 `ActivityTimerService.kt`：`specialUse` 前台服务，常驻通知 `setUsesChronometer(true)` 自动走秒，通知渠道 `activity_timer`（LOW、无声），带「暂停/继续」「结束」动作按钮；`start`/`refresh`/`stop` 入口。
   - 新增 `ActivityTimerReceiver.kt`：处理 `ACTION_PAUSE_TIMER`/`ACTION_RESUME_TIMER`/`ACTION_FINISH_TIMER`，转发 Repository 并刷新/停止服务。
   - `AndroidManifest.xml` 新增 service（`foregroundServiceType="specialUse"` + subtype 属性）与 receiver 声明。
   - 新增矢量图标 `ic_play_arrow.xml`、`ic_pause.xml`、`ic_stop.xml`。

3. **ViewModel 层（任务 3）**
   - `TodoViewModel.kt` 暴露 `activeActivityRecord`、`todayActivityRecords` 流（合并进看板 UI state），`startTimer`/`pauseTimer`/`resumeTimer`/`finishTimer`/`addManualActivityRecord`/`deleteActivityRecord` 动作联动前台服务。

4. **UI 层（任务 4）**
   - 看板顶部计时条（每秒刷新走动时长）、「今日待办」与「今日日程」之间的「今日做了什么」卡、右下角「开始记录」FAB。
   - 新增 `ActivityTimerSection.kt`、`ActivityRecordDialogs.kt`（开始记录 / 随手补记 / 记录详情对话框），`DashboardScreen.kt`、`DashboardChrome.kt`、`MainActivity.kt` 接线回调。

5. **报告层（任务 5）**
   - `DailyReportGenerator.kt`：`DailyContext`/`WeeklyContext` 新增实际记录字段与分钟合计，数据 prompt 与写作要求接入「今日/本周实际记录」，新增 `activityLine` 汇总。

6. **备份/版本/文档（任务 6）**
   - `BackupModels.kt` 的 `AppSnapshot` 新增 `activityRecords`；`BackupManager.kt` 导出/导入 JSON；`TodoRepository.kt` 导出全部已完成记录、导入去重并重置自增主键。
   - 版本升到 `1.15.0 / versionCode 342`；更新 README、CHANGELOG、PROJECT_STATUS、CURRENT_TASK、本交接与 LEDGER。

## Validation

Passed:

- `./gradlew.bat --no-daemon :app:compileDebugKotlin`
- `./gradlew.bat --no-daemon :app:assembleDebug`
- APK output-metadata inspection：`versionName = 1.15.0`、`versionCode = 342`、`PaykiTodo-1.15.0-debug.apk`
- Room schema `app/schemas/com.example.todoalarm.data.AppDatabase/29.json` 已生成

未执行：

- 模拟器/真机安装冒烟：本会话环境 `adb` 不在 PATH，无法从这里安装启动。下次会话应在有 `adb` 的环境安装 `PaykiTodo-1.15.0-debug.apk`，走一遍 goal 文档验证清单（开始/暂停/继续/结束、<5 秒丢弃、通知走秒与动作按钮、杀进程后计时恢复、看板计时条与「今日做了什么」卡、随手补记、备份/恢复含 activity_records）。
- 真实 provider key 下日报/周报接入实际记录的观感抽查（本地无 key，key 不入库）。

## Still Required Before Reporting Complete

1. 已提交本轮：见 git 历史。
2. 待下次会话补做安装冒烟与真实 key 报告观感抽查。

## Notes For Next Session

- 计时器为秒表模型：同一时刻只允许一条非 FINISHED 记录，`startActivityTimer` 会先结束旧记录（沿用 <5 秒丢弃）。若未来要支持多计时并行，需要重构该约束与 `getActiveActivityRecord` 的 LIMIT 1 查询。
- 前台服务不自持久化状态，重启后靠库中活动记录恢复；若发现杀进程后通知丢失，检查 `ActivityTimerService.start` 是否在进程重建后被重新触发。
- goal 文档中「不做」项（电脑同步、独立 Widget、待办自动完成、动循环逻辑）本轮均未触碰，保持后续可扩展。
- Do not commit or write Desktop Sync tokens, AI keys, private Base URLs, signing files, generated APKs, or local diagnostic logs.
