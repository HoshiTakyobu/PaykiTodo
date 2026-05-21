# PaykiTodo — M1 闹钟模式 + M2 每日 Brief + M4 全局搜索 + M5 数据健康检查

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

---

# M1. 关键提醒「闹钟模式」— 持续响铃直到用户操作

## 问题
当前提醒响一次就停。对"吃药"、"DDL 当天"这种绝对不能漏的事情，一次响铃不够强。

## 目标
新增「闹钟模式」开关：开启后提醒会**持续响铃 + 振动**，每隔 30 秒重试一次，直到用户手动确认/延后/取消。像手机闹钟一样，不操作就不停。

## 实现

### 数据模型

`todo_items` 新增字段：
```sql
ALTER TABLE todo_items ADD COLUMN alarmMode INTEGER NOT NULL DEFAULT 0;
```

`recurring_task_templates` 同步新增 `alarmMode`。

数据库版本 `24 → 25`。

### 待办编辑器

`TodoEditorDialog.kt` 的"更多选项"区域加 Switch：
- 标签：「闹钟模式（持续响铃直到操作）」
- 副标题：「适合吃药、重要 DDL 等绝对不能漏的提醒」

### 提醒播放逻辑

`app/src/main/java/com/example/todoalarm/alarm/ReminderAlertController.kt`

当 `todoItem.alarmMode = true` 时：
```kotlin
// 不使用一次性 MediaPlayer，改为循环播放
mediaPlayer.isLooping = true

// 振动也改为持续模式
val pattern = longArrayOf(0, 800, 400, 800, 400)  // 持续振动模式
vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))  // repeat=0 表示循环
```

### 停止条件

只有以下操作才停止响铃：
- 用户点"完成"
- 用户点"延后 X 分钟"
- 用户点"取消任务"
- 用户点"我知道了"（日程）

**不会自动停止**。如果用户 5 分钟不操作，仍然在响。

### 超时保护

为防止用户手机放在包里响一整天：
- 最长持续响铃 **5 分钟**
- 5 分钟后自动降级为普通通知（停止响铃，发一条高优通知"你有一条未处理的提醒"）
- 5 分钟后每隔 2 分钟再响一次（30 秒），共重试 3 次
- 之后彻底停止，留下通知

### 全屏提醒页面适配

`ReminderActivity.kt` 在闹钟模式下：
- 顶部加一个醒目的脉冲动画（红色圆环 pulse）
- 标题区加文字「⏰ 闹钟模式 — 操作后停止响铃」
- 按钮区更突出（更大、更醒目的颜色）

### 验证
- [ ] 创建待办 → 开启闹钟模式 → 提醒时间到 → 持续响铃+振动
- [ ] 点"完成" → 立刻停止
- [ ] 点"延后" → 立刻停止
- [ ] 不操作 5 分钟 → 降级为通知 + 间歇重试
- [ ] 循环待办继承闹钟模式设置
- [ ] 普通模式（alarmMode=false）行为不变

---

# M2. 每日 Brief 通知

## 问题
用户每天需要主动打开 app 才能知道今天有什么事。希望有一条轻量通知作为每日接触点。

## 目标
每天用户自定义时间（默认 8:00），发一条通知摘要今日待办和日程。

## 实现

### 设置项

`SettingsPanel.kt` → 新增区块「每日摘要通知」：
- 开关：「每天发送今日摘要」（默认开）
- 时间选择：HH:mm（默认 08:00）
- 字段：`AppSettings.dailyBriefEnabled: Boolean`、`AppSettings.dailyBriefHour: Int`、`AppSettings.dailyBriefMinute: Int`

### 通知内容

```
标题：今日 · 5月22日 周四
内容：3 件待办 · 2 个日程 · 距期末 7 天
```

如果有倒数日目标（≤7 天内），附加最近的一个。

点击通知 → 打开每日看板。

### 调度

复用 `DailyReportScheduler` 的模式：
- 新建 `DailyBriefScheduler`，用 `AlarmManager` 在每天指定时间触发
- `DailyBriefReceiver` 接收广播 → 查询今日数据 → 发通知
- 启动时 / 设置变更时重新调度

### Notification Channel

`daily_brief`，重要性 DEFAULT（有声音但不全屏）。

### 验证
- [ ] 设置中可开启/关闭每日摘要
- [ ] 设置时间后，到点收到通知
- [ ] 通知内容正确显示今日待办数、日程数、最近倒数日
- [ ] 点击通知打开看板
- [ ] 关闭后不再收到

---

# M4. 全局搜索

## 问题
待办/日程/规划台节点/AI 报告越来越多，找不到特定条目。

## 目标
在 app 内提供一个全局搜索入口，搜索范围覆盖：待办标题+备注、日程标题+地点、规划台节点文本、AI 报告内容。

## 实现

### 入口

每日看板顶部加一个搜索图标按钮，点击打开搜索页面。
或者：抽屉顶部加搜索入口。

### 搜索页面

新建 `app/src/main/java/com/example/todoalarm/ui/SearchPanel.kt`：

```
┌─────────────────────────────────┐
│ 🔍 [搜索框]              [取消]  │
├─────────────────────────────────┤
│ 待办 (3)                         │
│   ○ 入党资料要写完               │
│   ○ 数据库复习                   │
│   ✓ 交论文（已完成）             │
├─────────────────────────────────┤
│ 日程 (1)                         │
│   📅 写论文 15:00-17:00          │
├─────────────────────────────────┤
│ 规划台 (2)                       │
│   📝 入党资料要写完 (5月19日)     │
│   📝 参考群里的文件              │
├─────────────────────────────────┤
│ AI 报告 (1)                      │
│   📊 5月20日日报 — "...入党..."  │
└─────────────────────────────────┘
```

### 搜索逻辑

使用 Room 的 `LIKE '%keyword%'` 查询（简单方案）：

```kotlin
@Query("SELECT * FROM todo_items WHERE title LIKE '%' || :q || '%' OR notes LIKE '%' || :q || '%' LIMIT 20")
suspend fun searchTodos(q: String): List<TodoItem>

@Query("SELECT * FROM todo_items WHERE isEvent = 1 AND (title LIKE '%' || :q || '%' OR location LIKE '%' || :q || '%') LIMIT 20")
suspend fun searchEvents(q: String): List<TodoItem>

@Query("SELECT * FROM planning_nodes WHERE text LIKE '%' || :q || '%' LIMIT 20")
suspend fun searchNodes(q: String): List<PlanningNode>

@Query("SELECT * FROM ai_reports WHERE content LIKE '%' || :q || '%' LIMIT 10")
suspend fun searchReports(q: String): List<AiReport>
```

### 结果点击

- 点击待办 → 打开待办预览 sheet
- 点击日程 → 打开日程详情
- 点击规划台节点 → 跳转到对应规划文档并高亮该节点
- 点击 AI 报告 → 打开报告详情

### 性能

- 输入防抖 300ms
- 每类最多返回 20 条
- 后续如果数据量大可以加 Room FTS，但当前 LIKE 够用

### 验证
- [ ] 搜索"论文" → 显示包含"论文"的待办、日程、规划台节点
- [ ] 点击搜索结果 → 跳转到对应详情
- [ ] 空搜索词 → 不显示结果
- [ ] 搜索响应 < 500ms（正常数据量下）

---

# M5. 数据健康检查 + 一键清理

## 问题
长期使用后数据库会积累大量已完成待办、空文档、孤儿节点，让 app 越来越慢/乱。

## 目标
设置里加一个「数据健康检查」入口，扫描问题数据并提供清理选项。

## 实现

### 入口

`SettingsPanel.kt` → 数据管理区域 → 「数据健康检查」按钮

### 检查项

点击后扫描以下问题：

| 检查项 | 条件 | 建议操作 |
|--------|------|----------|
| 过期已完成待办 | `completed = true` 且完成时间 > 30 天前 | 归档或删除 |
| 空规划文档 | `planning_notes` 无任何 `planning_nodes` 子节点 | 删除 |
| 孤儿草稿节点 | `isDraft = true` 且创建时间 > 14 天前 | 发布或删除 |
| 过期 AI 报告 | 超出保留策略的报告（如果用户设了 30 天保留但有漏网的） | 删除 |
| 无提醒的过期待办 | DDL 已过 > 7 天，未完成，无提醒 | 提醒用户处理 |

### 结果页面

```
┌─────────────────────────────────┐
│ 数据健康检查                     │
├─────────────────────────────────┤
│ ✓ 已完成待办（>30天）    42 条   │  [清理]
│ ✓ 空规划文档              3 个   │  [清理]
│ ⚠ 过期草稿节点           8 条   │  [查看]
│ ✓ 过期 AI 报告            0 条   │  —
│ ⚠ 未处理过期待办         2 条   │  [查看]
├─────────────────────────────────┤
│        [一键清理安全项]           │
└─────────────────────────────────┘
```

### 「一键清理安全项」

只清理明确安全的：
- 删除 30 天前的已完成待办
- 删除空规划文档
- 删除超出保留策略的 AI 报告

**不自动删除**草稿节点和未处理待办（这些需要用户逐条决定）。

### 清理前确认

弹确认对话框：「将清理 42 条已完成待办、3 个空文档。此操作不可撤销，建议先备份。」

### 验证
- [ ] 设置中有"数据健康检查"入口
- [ ] 点击后显示各项扫描结果和数量
- [ ] "一键清理"只删除安全项
- [ ] 清理前有确认对话框
- [ ] 清理后数据确实被删除，列表刷新
- [ ] 不会误删未完成待办或正在使用的文档

---

# 通用要求

1. 数据库版本 `24 → 25`（M1 新增 `alarmMode` 字段）
2. 备份/恢复 JSON 加 `alarmMode`、`dailyBriefEnabled/Hour/Minute` 字段
3. 每轮 `./gradlew.bat :app:compileDebugKotlin` + `git diff --check` 通过
4. 最后一轮 `./gradlew.bat :app:assembleDebug` 验证
5. commit 严格按 AGENTS.md 的 bullet 列表格式
6. 不要 push 到 GitHub
7. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`、`docs/current/PROJECT_STATUS.md`、`docs/current/FEATURE_LEDGER.md`

---

# 实现顺序建议

1. **M2（每日 Brief）** — 最简单，1-2h，用户立刻能感受到
2. **M1（闹钟模式）** — 核心功能补强，2-3h
3. **M4（全局搜索）** — 基建，2-3h
4. **M5（数据健康检查）** — 收尾，1-2h

---

# 不做的事

- ❌ 不做 Room FTS（LIKE 够用，FTS 留给后续）
- ❌ 不做自动清理（所有清理必须用户确认）
- ❌ 不做跨设备同步搜索（只搜本地数据）
- ❌ 不做 Widget 上直接勾选完成
