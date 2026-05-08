# PaykiTodo

> 一个以“强提醒”和“本地可控”为核心的 Android 待办与日历应用。  
> 面向真的需要被提醒到、需要时间轴规划、需要本地数据掌控权的使用场景。

## 项目定位

PaykiTodo 当前仍然以 **Android 本地单机应用** 为主，但已经开始补齐更完整的个人生产力链路：

- 待办、DDL、全屏提醒、锁屏提醒
- 日历、时间轴、批量导入
- 循环任务、循环日程、分组与模板
- JSON 备份恢复
- 手机与电脑同局域网下的轻量控制台

它不是依赖账号体系的云平台，也不是普通轻通知型待办工具。当前更强调：

- 到点时尽量把提醒真正送到用户面前
- 兼顾后台、锁屏、解锁回前台等复杂场景
- 保持数据本地、可导出、可恢复、可观察
- 让日历与待办形成同生态体验

## 当前版本

- 当前仓库代码版本：`1.6.10`
- `versionCode = 82`
- 平台目标：Android 14 / API 34
- 应用包名：`com.paykitodo.app`
- 当前调试 APK：`app/build/outputs/apk/debug/PaykiTodo-1.6.10-debug.apk`

## 当前主要能力

### 待办系统

- 新增、编辑、删除待办
- 支持标题、备注、分组、DDL、提醒时间
- 支持不设置 DDL 的普通待办
- 支持完成、取消、恢复
- 支持循环任务与循环范围编辑
- 首页保留 `已错过 / 今日待办 / 计划中` 三段式逻辑
- 支持分组筛选，不破坏首页整体布局

### 日历系统

- 时间轴式日历基础已经建立
- 单日 / 多日 / 月 / 列表等视图在代码中存在并持续打磨
- 支持普通日程、全天日程、循环日程
- 支持地点、备注、颜色、提醒方式、提醒提前量
- 支持时间轴点击建日程
- 支持文本批量导入
- 支持模板和学期级生成相关能力

### 提醒能力

- `AlarmManager` 精确调度
- 通知栏提醒与全屏提醒双路径
- 锁屏、后台、解锁回提醒页等场景的多轮兜底
- 辅助功能提醒链路
- 前台服务 + 活跃提醒会话维持
- 提醒链路诊断与短延迟测试入口
- 提醒音支持内置音与系统通知提示音

### 电脑同步 / 局域网控制台

- 手机端内置局域网网页控制台
- 可在 `设置 -> 电脑同步` 中开启
- 手机与电脑同一局域网时，电脑浏览器可直接连接手机
- 当前网页端支持基础查看与部分创建/操作
- 操作直接写入手机端同一份 Room 数据库

### 数据与排障

- JSON 导出 / 导入
- 自动备份相关能力
- 崩溃日志查看与复制
- 内置 Wiki 使用说明
- 关于页与版本信息

## 当前仓库状态说明

这个仓库现在不是“干净发布态”，而是一个**带有未提交进行中改动的开发中工作树**。

当前活跃改动集中在：

- 图标资源链路
- 看板 / 首页视觉与结构
- 日历交互与表现
- 文档清账与会话交接体系

所以新 session 接手时，不应默认这是一个刚好停在稳定版本标签上的仓库。

## 本地开发

### 环境要求

- Android Studio
- Android SDK 34
- Android Studio 自带 `jbr`

### 运行方式

1. 用 Android Studio 打开项目根目录 `Project/PaykiTodo`
2. 确认 Gradle JDK 指向 Android Studio 自带 `jbr`
3. 运行 `app` 模块

### 命令行构建

在项目根目录执行：

```powershell
./gradlew.bat assembleDebug
```

## 新 Session 入口

如果你是新开的 Codex 会话，不要先依赖旧对话记忆。

优先读取：

1. [AGENTS.md](/G:/Workspace/Project/PaykiTodo/AGENTS.md)
2. [PROJECT_INTENT.md](/G:/Workspace/Project/PaykiTodo/docs/current/PROJECT_INTENT.md)
3. [PROJECT_STATUS.md](/G:/Workspace/Project/PaykiTodo/docs/current/PROJECT_STATUS.md)
4. [FEATURE_LEDGER.md](/G:/Workspace/Project/PaykiTodo/docs/current/FEATURE_LEDGER.md)
5. [CURRENT_TASK.md](/G:/Workspace/Project/PaykiTodo/docs/current/CURRENT_TASK.md)
6. [SESSION_HANDOFF.md](/G:/Workspace/Project/PaykiTodo/docs/current/SESSION_HANDOFF.md)
7. [START_NEW_SESSION.txt](/G:/Workspace/Project/PaykiTodo/START_NEW_SESSION.txt)

## 相关文档

- [CHANGELOG.md](/G:/Workspace/Project/PaykiTodo/CHANGELOG.md)
- [TODO.md](/G:/Workspace/Project/PaykiTodo/TODO.md)
- [PAYKITODO_SESSION_LEDGER.md](/G:/Workspace/Project/PaykiTodo/docs/current/PAYKITODO_SESSION_LEDGER.md)
- [PaykiTodo-1.6.1-Desktop-Sync-Principle.md](/G:/Workspace/Project/PaykiTodo/docs/PaykiTodo-1.6.1-Desktop-Sync-Principle.md)
- [PaykiTodo-Release-Signing-Template.md](/G:/Workspace/Project/PaykiTodo/docs/PaykiTodo-Release-Signing-Template.md)

## 历史文档说明

`docs/` 下很多带版本号的文档是历史快照，例如 `1.4.9`、`1.5.0`、`1.6.1`。

它们可以作为历史参考，但**不应默认当作当前项目基线**。当前基线以 `docs/current/` 和代码现状为准。

## 版权

© Copyright Hoshi Takyobu, 2026-2026
