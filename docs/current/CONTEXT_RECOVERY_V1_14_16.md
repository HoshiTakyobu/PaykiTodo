# v1.14.16 Context Recovery

记录时间：2026-06-23

本文件用于恢复一次上下文断裂前后的关键信息。它不是产品需求文档，也不是发布说明；它只保存已经从本机文件、SQLite 日志、Git 提交和项目 current 文档中验证到的事实，方便后续会话继续工作。

## 结论

v1.14.16 的开发和构建已经发生，且本地证据完整；丢失的是主对话线程中可直接被模型读取的近期上下文。

主要异常是：

- 主 PaykiTodo 线程 `019d4e16-8f61-78e3-9028-1dccf67f1661` 的 `state_5.sqlite` 记录显示 `updated_at = 2026-06-23 23:47:35`。
- 但同一线程的 `rollout_path` 仍指向 `C:\Users\hp\.codex-cpa\sessions\2026\06\16\rollout-2026-06-16T17-32-52-019d4e16-8f61-78e3-9028-1dccf67f1661.jsonl`。
- 同一线程的 `recency_at` 停在 `2026-06-17 14:03:07`。
- 因此 UI/history 里能看到 2026-06-23 的 PaykiTodo 消息，但可用于恢复完整模型上下文的 rollout 文件没有同步续写到 6 月 23 日。

这解释了用户看到的现象：明明刚聊过并构建过 v1.14.16，但当前模型上下文里少了很多过程信息。

## 可恢复的原始用户需求

来源：`C:\Users\hp\.codex\history.jsonl`，线程 `019d4e16-8f61-78e3-9028-1dccf67f1661`。

### v1.14.15 相关前置反馈

时间戳：`1782221801`

用户反馈了规划台提醒识别和电脑同步地址问题：

- 电脑已连通手机，希望通过 `http://192.168.0.100:18765/` 检查电脑端链路。
- 对输入：

```text
06-25 14:00-22:00 【合唱】大艺展省赛录音 @成都大学 提醒时间：提前1h，06-24 22:10
```

用户发现提醒时间被识别进标题，而不是提醒字段。

用户希望：

- 查清规划台识别链路到底走 AI 还是本地规则。
- 不要把 `提醒时间：XXX` 做成唯一死规则，要允许自然语言变体。
- 手机 AI 源配置完成后，设置页应直接告诉用户当前是否有可用源、可用数量，不要让用户逐个点测试。
- 电脑同步显示连接地址前，应过滤掉移动数据、VPN、虚拟网卡等电脑无法访问的 `10.*`、`172.*` 普通地址。

对应提交：

- `8eedbeb 修正规划台提醒子句识别和电脑同步地址展示`
- 版本：`1.14.15 / versionCode 335`

### v1.14.16 直接用户反馈

时间戳：`1782225632`

用户提出 5 个问题：

1. 浅色模式通知栏下 PaykiTodo 进行中日程通知文字白色/灰色，看不清；希望适配深色/浅色，并尽量不要折叠。
2. 通过电脑同步检查 `丁螺环酮I` 等循环待办：明明截止日期是 8 月 18 日，但常规呈现只有 13 次，最后一次到 7 月 6 日；用户担心循环截止日期或后续实例丢失。
3. 电脑端左侧菜单需要单独的 `待办` 入口，不要把完整待办管理藏在每日看板卡片后。
4. 电脑端日程时间轴需要支持直接拖拽日程，15 分钟为最小单位，支持跨天拖动；已经发生的日程整体颜色变淡，和手机端一致。
5. 电脑端日程颜色选择器没有颜色显示，而且一条一条铺开；应改为集中的小色块。

之后用户又发送：

- 时间戳 `1782226523`：`卡了，请你继续`
- 时间戳 `1782229310`：询问为什么上下文丢失，要求找回 v1.14.16 过程上下文。

## v1.14.16 已交付内容

来源 1：`C:\Users\hp\.codex\logs_2.sqlite`，日志 id `23314811`。

当时最终回复文本记录：

```text
已完成并提交。

Commit:
- `9e5b444 修复通知可读性和电脑端待办日程交互`

版本:
- `1.14.16 / versionCode 336`

APK:
- `G:\Workspace\Project\PaykiTodo\app\build\outputs\apk\debug\PaykiTodo-1.14.16-debug.apk`
```

本轮修复：

1. 进行中日程通知
   - 代码层强制设置 RemoteViews 文本颜色。
   - 移除更容易被系统通知栏二次装饰的 custom big view / decorated style。
   - 说明：是否仍显示展开箭头，部分由 Android/OEM 通知面板控制，不能 100% 由 App 决定。
2. 循环待办逻辑
   - 只读检查手机端数据后确认 `丁螺环酮I/II/III` 的循环截止日期仍在，没丢。
   - 当前只生成近未来实例，是性能策略，不是截止日期被截断。
   - 电脑端待办卡片已补充说明，避免误以为只剩 13 次。
3. 电脑端新增独立 `待办` 入口
   - 侧边栏拆出 `每日看板` 和 `待办`。
   - `待办` 页会直接加载完整待办管理列表。
4. 电脑端日程时间轴
   - 支持拖拽日程。
   - 支持 15 分钟粒度。
   - 支持拖到其它可见日期列。
   - 已结束日程颜色淡化。
   - 循环日程拖拽默认只改当前实例，避免误改整个系列。
5. 电脑端日程颜色选择器
   - 修复成紧凑可见色块，不再是一条条空白长行。

验证记录：

- `node --check app/src/main/assets/desktop-web/app.js`
- `compileDebugKotlin`
- `testDebugUnitTest`
- `git diff --check`
- `assembleDebug`
- APK 元数据确认：`versionName=1.14.16`，`versionCode=336`

## Git 证据

当前本地提交：

```text
9e5b444e14f6a5ff4c7d3671f9de83c678d8ac3e
修复通知可读性和电脑端待办日程交互
AuthorDate: Tue Jun 23 23:09:21 2026 +0800
CommitDate: Tue Jun 23 23:09:21 2026 +0800
```

涉及文件：

- `CHANGELOG.md`
- `README.md`
- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/java/com/example/todoalarm/alarm/OngoingEventNotifier.kt`
- `docs/current/CURRENT_TASK.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/PAYKITODO_SESSION_LEDGER.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/SESSION_HANDOFF.md`

## 项目文件证据

当前项目文件已经记录：

- `app/build.gradle.kts`: `paykiVersionName = "1.14.16"`，`paykiVersionCode = 336`
- `CHANGELOG.md`: 有 `## v1.14.16`
- `README.md`: 当前源码版本为 `v1.14.16`
- `docs/current/CURRENT_TASK.md`: 记录 v1.14.16 的范围、状态和验证
- `docs/current/SESSION_HANDOFF.md`: 记录 v1.14.16 当前状态、APK 路径和下轮注意事项
- `docs/current/PROJECT_STATUS.md`: 记录 v1.14.16 build facts 和验证命令
- `docs/current/PAYKITODO_SESSION_LEDGER.md`: 记录 v1.14.16 会话摘要

## 为什么上下文会看起来丢失

从本机状态看，最可能的原因不是 PaykiTodo 项目本身丢了，而是 Codex 会话状态发生了错位：

- `history.jsonl` 记录了主线程 6 月 23 日的新用户消息。
- `state_5.sqlite` 中主线程更新时间也更新到了 6 月 23 日。
- 但主线程 `rollout_path` 仍指向 6 月 16 日的 rollout 文件。
- `logs_2.sqlite` 中 v1.14.16 的最终交付文本存在，但不是以可直接恢复主线程上下文的方式写入主线程 rollout。
- 6 月 23 日还存在另一个 rollout：`019ef506-be02-79f2-85bd-747049b4da56`，但它的 state 标题/first message 是音轨分离问题，不是 PaykiTodo；说明当天有多线程/多任务状态交叉。

实际效果就是：

- UI 侧或 history 层还能看到近期消息。
- 模型侧恢复上下文时只拿到较旧的主 rollout 或压缩摘要。
- 因此 v1.14.16 的“聊天过程”不完整，但“需求、交付、提交、验证、APK”仍可通过上述证据恢复。

## 后续处理建议

1. 后续 PaykiTodo 开发优先读取：
   - `docs/current/PROJECT_STATUS.md`
   - `docs/current/CURRENT_TASK.md`
   - `docs/current/SESSION_HANDOFF.md`
   - 本文件
2. 若再次出现上下文错位，优先查：
   - `C:\Users\hp\.codex\history.jsonl`
   - `C:\Users\hp\.codex\logs_2.sqlite`
   - `C:\Users\hp\.codex\state_5.sqlite`
   - `C:\Users\hp\.codex\sessions\YYYY\MM\DD\*.jsonl`
3. 不要把 Desktop Sync token、API key、Base URL 或签名材料写进恢复文档。
4. 对 v1.14.16 之后的代码开发，仍以 Git 当前状态和 `docs/current/` 为准，不应依赖未恢复的聊天细节。
