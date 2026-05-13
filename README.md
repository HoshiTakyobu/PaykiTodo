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

- 当前仓库代码版本：`1.6.71`
- `versionCode = 143`
- 平台目标：Android 14 / API 34
- 应用包名：`com.paykitodo.app`
- 当前调试 APK：`app/build/outputs/apk/debug/PaykiTodo-1.6.71-debug.apk`

## 当前主要能力

### 待办系统

- 新增、编辑、删除待办
- 支持标题、备注、分组、DDL、多提醒时间
- 支持不设置 DDL 的普通待办
- 支持待办文本批量导入
- 支持完成、取消、恢复
- 支持循环任务与循环范围编辑
- 首页保留 `已错过 / 今日待办 / 计划中` 三段式逻辑
- 支持分组筛选，不破坏首页整体布局

### 日历系统

- 时间轴式日历基础已经建立
- 单日 / 多日 / 月 / 列表等视图在代码中存在并持续打磨
- 支持普通日程、全天日程、循环日程
- 支持地点、备注、颜色、提醒方式、多提醒时间
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

这个仓库当前已经整理到一个**已构建的 `1.6.71` 开发基线**。

这条基线已经包含：

- 日历当前时间轴文字位置修正
- 横向切到离今天较远的日期后，当前时间标签与红线仍持续可见
- 启动器与通知图标链路调整
- 启动器 adaptive icon 前景使用图片版 `ic_launcher_art`，不再使用旧矢量 mark
- 内置 Wiki 目录可正常切换章节
- 设置页使用说明和提示音入口可直接跳转
- 电脑端日程时间轴点击日程卡片进入编辑/删除，展示色优先使用分组颜色；日程卡片点击查找已改为兼容字符串 / 数字 ID，循环星期字段兼容字符串 / 数组，全天日程以轻量胶囊入口显示，桌面资源禁用缓存并带版本参数
- 每日看板默认入口与看板视觉更新
- 每日看板待办区包含已错过但仍活动的待办
- 每日看板会隐藏已结束日程，并高亮正在进行的日程
- 电脑端已有日程点击编辑链路已再次加固，日程卡片自身会直接打开编辑器，时间网格线不再拦截点击
- 删除确认窗口已统一为危险操作底部弹窗样式
- 日历时间轴预添加日程状态支持长按空白处取消
- 待办预览底部弹窗统一、删除确认补齐、提醒配置保留、电脑同步通知点击跳转
- 仓库内新会话交接与维护文档体系
- 每日看板移除添加入口，只保留看板阅读功能
- 我的任务页右下角保留“批量待办”和新建待办按钮，日历批量入口保留在日历页
- 每日看板日程色条高度与右侧文字块高度对齐
- 待办批量导入的 DDL 支持 `16:30` / `16：30` 这种缺省当天日期的写法
- 每日看板日程卡片统一左侧竖线对齐，非进行中日程不再显示外层边框，正在进行日程保留金色边框与轻微内侧高光
- 启动页图标改用透明背景版本，避免白色方块覆盖背景太阳圆
- 自定义延后不再限制 180 分钟；延后目标晚于当前 DDL 时会同步修改该待办 DDL
- 电脑端网页控制台支持编辑已有待办的标题、备注、DDL、提醒、分组、循环、铃声和震动，并补齐日程/全天日程编辑入口
- 电脑端网页控制台的待办 / 日程编辑弹窗已改为更接近手机端底部弹窗的视觉语言，字段卡片化、保存入口固定在顶部右侧，时间轴卡片按钮更轻量
- 电脑端 Web UI 已从 Kotlin 大字符串拆分到 `app/src/main/assets/desktop-web/`，手机端同步服务只负责读取静态资源并提供 API
- 内置 Wiki 在手机窄屏下也保持左侧目录 / 右侧正文布局；每日看板可区分今天无日程和今天日程已全部结束；抽屉菜单顶部图标改为圆形裁切
- 提醒声音策略已扩展为“播放通道 + PaykiTodo 内部音量 + 可选临时系统通道提升 + 工作模式”，播放通道和日历默认项改为紧凑下拉，音量改为滑条与数值输入；工作模式保持静音 + 强制加强震动 + 全屏/无障碍兜底
- 手机端日程 / 待办编辑底部面板去掉泛泛说明副标题，设置页减少重复解释文案
- 电脑端网页控制台增加轻量卡片进入、标签切换、弹窗淡入和按钮按压反馈，并支持系统减少动态效果设置
- 电脑端待办编辑支持 no-DDL，关闭后同步禁用 DDL、提醒和循环字段
- 电脑端删除操作统一改为应用内危险确认弹窗，不再使用浏览器原生 confirm
- 电脑端网页左侧会显示随 APK 自动替换的版本号；如果电脑访问页仍没有版本号或仍加载无 `?v=` 的 `app.js`，说明手机上运行的仍是旧 APK
- 电脑端待办和日程卡片改为先打开详情预览，再从预览中编辑、删除、完成或取消，交互更接近手机端预览底部弹窗
- 电脑端待办和日程编辑使用统一提醒时间输入，支持 `5,15,16:30,05-10 15:00,2026-05-10 14:30` 这类混合语法并保存为多提醒偏移
- 电脑同步关闭后，即使系统重新拉起前台服务，也会自停并关闭本地同步协调器，避免无连接状态下继续暴露访问地址
- 日历顶层当前时间刷新下沉到时间轴和当前红线组件，减少每 30 秒整页重组

- 电脑端已有日程卡片点击恢复为直接进入编辑器，并通过本地 Node 模拟验证
- 手机端和电脑端循环选项增加“每年同农历月日”，可用于农历生日类年度提醒
- 电脑同步关闭状态下会强制清空运行态和可访问地址，避免旧地址残留
- 日历标题栏改为左侧月份标题、右侧操作按钮的紧凑布局，降低月份标题被挤没的概率
- 日历三日/单日顶部日期栏、月视图和列表视图开始显示农历标签，作为农历提醒和农历循环的基础能力

如果后续又出现本地未提交改动，以 `git status` 为准；不要依赖旧聊天记录推断仓库状态。

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
- [DESKTOP_WEB_ARCHITECTURE.md](/G:/Workspace/Project/PaykiTodo/docs/current/DESKTOP_WEB_ARCHITECTURE.md)
- [PaykiTodo-1.6.1-Desktop-Sync-Principle.md](/G:/Workspace/Project/PaykiTodo/docs/PaykiTodo-1.6.1-Desktop-Sync-Principle.md)
- [PaykiTodo-Release-Signing-Template.md](/G:/Workspace/Project/PaykiTodo/docs/PaykiTodo-Release-Signing-Template.md)

## 历史文档说明

`docs/` 下很多带版本号的文档是历史快照，例如 `1.4.9`、`1.5.0`、`1.6.1`。

它们可以作为历史参考，但**不应默认当作当前项目基线**。当前基线以 `docs/current/` 和代码现状为准。

## 版权

© Copyright Hoshi Takyobu, 2026-2026
