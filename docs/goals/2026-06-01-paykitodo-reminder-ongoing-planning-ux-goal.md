# PaykiTodo 1.13.22 强提醒闭环与规划台输入体验目标

## 背景

PaykiTodo 的核心产品目标不是普通清单软件，而是“强提醒 + 自律执行”的个人工具。当前用户反馈集中在两条主线：

1. 到了提醒时间，待办 / 日程没有稳定全屏弹出，违背“不能错过”的初衷。
2. 规划台虽然有规则识别和 AI 源，但手机端输入体验仍像被限制在狭窄行内，不像备忘录式自由书写。

本轮目标不做大而空的重构，而是把提醒链路和规划台输入体验收口到可真实使用的状态。

## 版本目标

- 目标版本：`1.13.22 / versionCode 270`
- 数据库版本：优先不升级；只有确实需要新字段时才升级 Room schema。
- 输出目标：生成可安装 debug APK。
- Git 策略：实现、验证后正常 commit；不 push，除非用户另行授权。

## 明确问题

### 问题 1：提醒到点不能全屏显示

用户期望：

- 待办提醒到点时，应打开全屏提醒界面，而不是只发一条普通通知。
- 日程提醒到点时，也应打开全屏提醒界面。
- 该行为是 PaykiTodo 的核心，不应被“普通通知”替代。

需要检查：

- 通知渠道 importance 是否足够；
- full-screen intent 是否正确设置；
- ReminderActivity 是否具备锁屏 / 点亮 / showWhenLocked / turnScreenOn 能力；
- AndroidManifest activity flags / exported / launchMode 是否合理；
- ReminderReceiver / scheduler 是否只发通知而没有可靠启动全屏；
- notification permission、exact alarm、厂商后台限制下的边界说明是否清楚。

验收标准：

- 待办提醒和日程提醒都会走同一套全屏提醒入口。
- 提醒通知包含 full-screen intent。
- 提醒触发时至少执行一次 Activity 启动兜底，不只依赖通知栏点击。
- ReminderActivity 在锁屏场景声明 show-when-locked / turn-screen-on。
- 代码里能明确看出提醒类型不会只有普通通知。

### 问题 2：日程进行中通知必须常驻

用户期望：

- 到了日程开始时间后，通知栏持续显示该日程。
- 该通知是“划都划不走”的 ongoing 通知。
- 只有日程结束、完成、取消、删除或应用主动清理时才消失。

需要检查：

- 是否为每个有效日程安排开始 / 结束检查；
- 开始时是否发 ongoing notification；
- notification 是否 `setOngoing(true)` / `setAutoCancel(false)`；
- 结束时是否取消；
- 编辑 / 删除 / 完成 / 取消日程时是否同步重排或取消；
- 重启恢复是否覆盖未来和正在进行中的日程。

验收标准：

- 正在进行中的日程会出现 ongoing notification。
- 通知不能通过普通横划清除。
- 日程结束后通知自动消失。
- 修改、删除、取消、完成日程会清理旧通知并重排新通知。

### 问题 3：待办提醒页必须形成闭环

用户反馈：

- 当前待办提醒界面太简陋。
- 没有“取消待办”的按钮。
- 从通知栏手动点进提醒页时，如果进入时已经过了 DDL，延后提醒不能只延后提醒时间，还应把 DDL 推到合理的未来。
- 如果用户打开全屏提醒界面时 DDL 已经过期，需要有完整规则，而不是只处理一小段情况。

本轮定义的闭环规则：

1. 待办提醒页至少提供：
   - 我已完成；
   - 延后 5 分钟；
   - 延后 10 分钟；
   - 自定义延后；
   - 取消待办。
2. “取消待办”对待办执行取消，不是删除；取消后应停止该待办后续提醒。
3. 延后提醒时计算 `nextReminderAt = now + snoozeMinutes`。
4. 如果待办没有 DDL：
   - 只安排下一次提醒；
   - 不凭空创建 DDL。
5. 如果待办有 DDL 且 `dueAt <= now`，或 `dueAt <= nextReminderAt`：
   - 自动把 DDL 推到 `nextReminderAt` 之后；
   - 为避免提醒和 DDL 同一瞬间竞争，目标 DDL 设为 `nextReminderAt + 1 分钟`。
6. 如果待办有 DDL 且 `dueAt > nextReminderAt`：
   - 只延后提醒，不改 DDL。
7. 自定义延后必须校验分钟数为正整数。
8. 完成 / 取消 / 延后之后，全屏提醒页关闭，声音 / 震动停止。

验收标准：

- ReminderActivity 的待办提醒按钮能覆盖上述动作。
- 逾期 DDL 下执行延后会持久化更新 DDL。
- 非逾期且 DDL 晚于下一提醒时，不误改 DDL。
- 取消待办会取消待办状态并清理提醒。
- 单元测试覆盖逾期 DDL 延后、未逾期 DDL 延后、无 DDL 延后三类核心规则。

### 问题 4：规划台手机输入体验不应像受限单行表格

用户反馈：

- 配置了 AI 源，但输入仍不灵活。
- 当前像在“特定的一行”里输入，长内容看不到前后。
- 手机端规划台应更接近备忘录：自由书写、可多行、可长文本检查，再识别。

本轮原则：

- 不取消现有 Outliner / Markdown 能力。
- 优先给用户一个明显的“自由书写草稿区”，让用户可以像备忘录一样输入：
  - `10:00-12:00 事件1`
  - `12:00-13:00 事件2`
  - `任务M ddl 15:00`
- AI 是增强入口，不是强迫用户学习死板语法。
- 手机端长文本输入需要支持多行显示和横向 / 纵向查看，不让用户困在狭窄行里。

验收标准：

- 规划台手机端有明确的自由书写入口或模式，视觉上像一个大文本编辑区。
- 该编辑区支持多行文本、长行查看、粘贴多行。
- 用户可以从该编辑区直接触发识别，识别结果仍进入预览，不直接写数据库。
- AI 源存在时，识别按钮文案或说明清楚表达“优先 AI / 本地兜底”。
- 保留当前 Outliner / Markdown 兼容能力，不破坏已有规划文档。

## 非目标

- 不在本轮做全新的 AI 模型管理系统。
- 不在本轮承诺所有国产模型返回格式都完美兼容；本轮只改善入口体验和已有 AI 调用链路可理解性。
- 不在本轮彻底重写规划台数据模型。
- 不在本轮保证所有国产 Android ROM 都允许后台全屏弹出；但必须做到 Android 标准 API 下的强提醒链路完整，并在代码 / 文档中标注厂商限制。

## 实施计划

### 阶段 A：提醒链路审计

1. 阅读 ReminderActivity、ReminderReceiver、ReminderForegroundService、AlarmScheduler、AndroidManifest。
2. 确认 todo / event reminder 的触发路径。
3. 确认 notification channel、full-screen intent、Activity 启动兜底是否齐全。
4. 确认声音 / 震动停止点。

### 阶段 B：日程 ongoing 通知审计

1. 搜索 `ongoing_event` 和 event-start / event-end alarm 相关代码。
2. 确认正在进行中通知是否 setOngoing。
3. 检查日程编辑 / 删除 / 完成 / 取消后的通知清理与重排。
4. 必要时补齐 start/end receiver 或 watchdog。

### 阶段 C：待办提醒页闭环

1. 提取或新增纯函数计算延后提醒与 DDL 推迟结果。
2. 为该纯函数补单元测试。
3. ReminderActivity 增加取消待办按钮。
4. 延后操作走新的闭环逻辑。
5. 确认完成 / 取消 / 延后都会停止当前提醒播放并关闭页面。

### 阶段 D：规划台手机输入体验

1. 阅读 PlanningDeskPanel 当前 Outliner 和 Markdown 兼容 UI。
2. 设计最小侵入的“自由书写”入口：
   - 可以是编辑区切换；
   - 或在现有模式上强化为大文本草稿区；
   - 必须保留文档内容与识别预览。
3. 优化识别按钮附近说明：
   - 有 AI 源时说明优先 AI；
   - 无 AI 或 AI 失败时说明本地规则兜底。
4. 确认长行 / 多行输入能查看和编辑。

### 阶段 E：文档、版本、验证

1. 版本升级到 `1.13.22 / versionCode 270`。
2. 更新 `docs/current/CURRENT_TASK.md`、`PROJECT_STATUS.md`、`FEATURE_LEDGER.md`、`SESSION_HANDOFF.md`。
3. 执行：
   - `./gradlew.bat :app:compileDebugKotlin`
   - `./gradlew.bat :app:testDebugUnitTest`
   - `git diff --check`
   - `./gradlew.bat :app:assembleDebug`
4. 如触及 desktop web，再执行：
   - `node --check app/src/main/assets/desktop-web/app.js`
5. 检查 APK metadata。
6. 提交代码，commit message 使用中文详细说明功能变化。

## 风险与边界

1. Android 10+ 后台启动 Activity 有限制。全屏提醒应优先依赖 high-importance notification full-screen intent，同时在 alarm receiver 中做标准可行的启动兜底。
2. Android 13+ 通知权限未授予时，通知无法正常显示；全屏提醒能力也会受影响。
3. 国产 ROM 可能额外限制锁屏弹窗、后台启动、精确闹钟、电池优化。本轮只能补齐应用侧链路，不能替用户自动解除系统权限限制。
4. Ongoing notification 对用户是强打扰，必须只覆盖“正在进行中”的日程，不能在未开始或已结束后长期残留。
5. 待办逾期延后自动推 DDL 是强规则，必须只在 DDL 已过期或不晚于下一提醒时触发，避免用户原本较晚的 DDL 被误改。

## 完成审计清单

完成前必须逐项核对：

- [x] 目标文档存在于 `docs/goals/2026-06-01-paykitodo-reminder-ongoing-planning-ux-goal.md`。
- [x] 待办提醒 full-screen intent 路径存在。
- [x] 日程提醒 full-screen intent 路径存在。
- [x] ReminderActivity 具备锁屏显示 / 点亮屏幕能力。
- [x] 日程进行中通知使用 ongoing 且自动结束清理。
- [x] 日程编辑 / 删除 / 完成 / 取消清理或重排 ongoing 通知。
- [x] 待办提醒页包含取消待办动作。
- [x] 逾期 DDL 延后提醒会把 DDL 推到下一提醒之后。
- [x] 未逾期且 DDL 晚于下一提醒时不误改 DDL。
- [x] 无 DDL 待办延后提醒不凭空创建 DDL。
- [x] 规划台手机端提供明确自由书写 / 大文本输入体验。
- [x] 规划台识别仍进入预览确认，不直接写数据库。
- [x] 版本升级到 `1.13.22 / versionCode 270`。
- [x] 当前文档同步更新。
- [x] 编译、单测、diff check、debug 构建通过。
- [x] APK metadata 确认新版本号。
- [ ] Git commit 完成，且不包含密钥、APK、签名材料。
