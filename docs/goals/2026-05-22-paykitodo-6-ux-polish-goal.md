# PaykiTodo — 6 项体验精进 + Undo

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

---

# 1. 手机端规划台节点拖拽排序

## 问题
桌面端有上移/下移按钮和同层级拖拽，但手机端只能通过 Tab/Shift+Tab 改层级，无法在同级之间调整顺序。

## 目标
编辑模式下，长按节点行 → 进入拖拽状态 → 上下拖动 → 松手放置到新位置。

## 实现

### 文件
`app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`

### 方案

使用 Compose 的 `Modifier.pointerInput` + `detectDragGesturesAfterLongPress` 实现：

1. 长按节点行 → 行视觉变化（轻微抬起 + 阴影 + 半透明）
2. 拖动时：被拖动的行跟随手指，其他行自动让位（动画）
3. 松手：调用 `onReorderNode(nodeId, newSortOrder)` 更新 `sortOrder`
4. 只允许同级拖拽（同一个 parentNodeId 下），跨层级拖拽拦截（和桌面端一致）

### 约束
- 预览模式下不支持拖拽
- 子任务区域内也支持拖拽（同级子节点之间）
- 拖拽过程中不触发节点编辑

### 验证
- [ ] 长按节点 → 进入拖拽态（视觉反馈）
- [ ] 上下拖动 → 其他同级节点让位
- [ ] 松手 → 顺序更新，刷新后保持
- [ ] 跨层级拖拽被拦截
- [ ] 预览模式下长按不触发拖拽（仍弹三点菜单）

---

# 2. 待办页循环待办折叠

## 问题
循环 90 天的"吃药"待办，在"未来"区域每天出现一条，列表极长、卡顿、视觉重复。

## 目标
同一个循环系列的待办在列表中折叠为一行，展开后才显示所有实例。

## 实现

### 文件
- `app/src/main/java/com/example/todoalarm/ui/TodoItemSections.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`

### 方案

1. 在 ViewModel 层，对"未来"区域的待办按 `seriesId` 分组
2. 同一个 `seriesId` 的待办只显示**最近一条**，后面加一行摘要：
   ```
   ○ 吃药 — 每天重复（还有 87 天）  [展开 ▸]
   ```
3. 点击"展开" → 显示该系列所有未来实例
4. 再点"折叠" → 收回

### 视觉
- 折叠态：正常待办卡片样式，右侧加一个小 badge 显示剩余实例数
- 展开态：所有实例正常显示，顶部有"折叠"按钮

### 性能
- 折叠态下 LazyColumn 只渲染 1 个 item 而不是 90 个
- 这直接解决卡顿问题

### 验证
- [ ] 循环 90 天的待办在"未来"区域只显示 1 行 + badge
- [ ] 点击展开 → 显示所有实例
- [ ] 列表不再卡顿
- [ ] "今日"区域的循环待办不折叠（今天该做的必须可见）
- [ ] 已过期的循环待办不折叠（需要处理）

---

# 3. 提醒页面视觉升级

## 当前状态

`ReminderActivity.kt` 当前布局：
- 顶部：渐变背景 + "PaykiTodo 提醒" 标题 + 事项名 + 副标题
- 中部：ElevatedCard 包含分组 pill、标题、时间/地点/备注
- 底部：操作按钮区（完成/延后/取消/自定义推迟）

整体已经有圆角卡片 + 渐变背景，但视觉上偏"信息展示"，不够"紧急感"。

## 目标

参考主流提醒/闹钟 app（iOS 闹钟、Google Clock、滴答清单提醒）的设计语言：
- **大字标题居中**，让用户一眼看到"该做什么"
- **时间信息突出**，用大号字体或醒目颜色
- **操作按钮大且明确**，拇指容易点到
- **减少信息层级**，不需要嵌套卡片

## 改动

### 布局重构

```
┌─────────────────────────────────┐
│         (渐变背景)               │
│                                  │
│      ⏰ 16:30                    │  ← 大号时间，居中，醒目色
│                                  │
│    交论文                         │  ← 超大标题，居中
│    截止：明天 23:59               │  ← 副标题，柔和色
│                                  │
│    📍 图书馆3楼                   │  ← 地点（如有）
│    📝 记得带U盘                   │  ← 备注（如有，可折叠）
│                                  │
│  ┌─────────┐  ┌─────────┐       │
│  │  ✓ 完成  │  │ ⏰ 延后  │       │  ← 大按钮，圆角，高对比
│  └─────────┘  └─────────┘       │
│  ┌─────────┐  ┌─────────┐       │
│  │  ✕ 取消  │  │ 自定义   │       │  ← 次要按钮
│  └─────────┘  └─────────┘       │
└─────────────────────────────────┘
```

### 关键视觉改动

1. **时间数字放大**：当前时间或 DDL 时间用 `headlineLarge` + 醒目色（accent），让用户第一眼看到"几点"
2. **标题放大**：事项名用 `displaySmall` 或 `headlineLarge`，不要藏在卡片里
3. **去掉嵌套卡片**：当前是 Surface 里套 ElevatedCard 里套 Surface，层级太多。改为扁平布局：背景渐变 + 内容直接排列
4. **按钮放大**：最小高度 56dp，圆角 16dp，文字 + 图标并排
5. **分组 pill 缩小**：不需要那么突出，放在标题上方用小字即可
6. **备注区可折叠**：默认只显示 2 行，点击展开

### 动画
- 页面打开时标题从下方 fade in + slide up
- 按钮区域 stagger 动画（依次出现）

### 验证
- [ ] 提醒弹出后第一眼能看到时间和事项名
- [ ] 按钮足够大，单手拇指容易点到
- [ ] 备注长文本不会撑爆页面
- [ ] 深色模式下仍然可读
- [ ] 日程提醒和待办提醒都正常显示

---

# 4. 日程进行中通知栏常驻

## 问题
日程开始后没有持续的通知提醒用户"当前正在进行什么"。用户希望日程进行期间通知栏有一条常驻通知，滑走后自动重现，直到日程结束。

## 实现

### 文件
- 新建 `app/src/main/java/com/example/todoalarm/alarm/OngoingEventNotifier.kt`
- `app/src/main/java/com/example/todoalarm/alarm/AlarmScheduler.kt`（触发时机）
- `app/src/main/AndroidManifest.xml`（notification channel）

### 方案

1. **新建 notification channel**：`ongoing_event`，重要性 LOW（不弹头部，只在通知栏显示）
2. **日程开始时**（提醒触发 or 到达 startAt 时间）：发一条 ongoing notification
   ```kotlin
   NotificationCompat.Builder(context, "ongoing_event")
       .setSmallIcon(R.drawable.ic_stat_payki_todo)
       .setContentTitle(event.title)
       .setContentText("进行中 · ${formatTimeRange(event.startAt, event.endAt)}")
       .setSubText(event.location.takeIf { it.isNotBlank() })
       .setOngoing(true)           // 不可滑走
       .setAutoCancel(false)
       .setWhen(event.startAtMillis)
       .setUsesChronometer(true)   // 显示已进行时长
       .setCategory(NotificationCompat.CATEGORY_EVENT)
       .setContentIntent(openEventDetailPendingIntent)
       .build()
   ```
3. **日程结束时**：取消该通知
   - 用 `AlarmManager` 在 `endAtMillis` 时触发取消
   - 或在 `EventCheckInWatchdog` 的分钟 tick 里检查并取消已结束日程的通知
4. **通知样式**：
   - 标题：事项名
   - 内容：`进行中 · 15:00-17:00`
   - 地点（如有）：作为 subText
   - 点击：打开该日程的详情预览
   - `setOngoing(true)` 确保滑走后重现

### 哪些日程触发

- 只有设置了提醒的日程才触发常驻通知（没设提醒的日程不打扰）
- 或者：所有日程都触发（更简单，但可能打扰）
- **建议**：在设置 → 日历与提醒 加一个开关「日程进行时显示通知」，默认开

### 验证
- [ ] 日程开始 → 通知栏出现常驻通知
- [ ] 通知显示事项名 + 时间范围 + 地点
- [ ] 滑走通知 → 自动重现（ongoing）
- [ ] 日程结束 → 通知自动消失
- [ ] 点击通知 → 打开日程详情
- [ ] 设置中可关闭此功能

---

# 5. Undo 撤销操作

## 范围

本轮只做规划台 Outliner 的 undo（最高频误操作场景）。待办/日程编辑器的 undo 留给后续。

## 实现

### 文件
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`（或新建 UndoManager）

### 方案

维护一个内存中的 undo stack（每个文档独立）：

```kotlin
data class PlanningUndoEntry(
    val description: String,       // "删除节点：入党资料要写完"
    val undoAction: suspend () -> Unit  // 恢复操作
)

class PlanningUndoStack(private val maxSize: Int = 20) {
    private val stack = ArrayDeque<PlanningUndoEntry>()
    
    fun push(entry: PlanningUndoEntry) { ... }
    fun pop(): PlanningUndoEntry? { ... }
    fun canUndo(): Boolean = stack.isNotEmpty()
    fun clear() { stack.clear() }
}
```

### 支持 undo 的操作

| 操作 | undo 行为 |
|------|-----------|
| 删除节点 | 恢复节点（含子节点） |
| 合并节点（Backspace） | 拆回两个节点 |
| 发布草稿 | 撤回发布（删除 linkedTodoId，恢复 isDraft） |
| 批量发布 | 撤回所有刚发布的节点 |
| 编辑节点文本 | 恢复旧文本 |

### UI 入口

规划台顶部工具栏加一个 undo 按钮（`Icons.Rounded.Undo`），仅在 `canUndo()` 时启用：

```kotlin
IconButton(
    onClick = { scope.launch { undoStack.pop()?.undoAction?.invoke() } },
    enabled = undoStack.canUndo()
) {
    Icon(Icons.Rounded.Undo, contentDescription = "撤销")
}
```

### 生命周期
- undo stack 是内存态，切换文档或退出规划台时清空
- 不持久化（避免复杂性）
- 最多保留 20 步

### 验证
- [ ] 删除节点后点 undo → 节点恢复
- [ ] 合并节点后点 undo → 拆回两个
- [ ] 发布草稿后点 undo → 撤回发布
- [ ] 连续 undo 多步正常
- [ ] 切换文档后 undo 按钮灰掉

---

# 6. 抽屉「更多」样式统一

## 问题
当前「更多」是一个自定义的 Row（图标 + 文字 + 副标题），和上面的 `DrawerSectionButton` 样式不一致。

## 当前代码
`DashboardChrome.kt:270-289`：用 Row + Icon + Column(Text+Text) 手写的，没有复用 `DrawerSectionButton`。

## 改动

把「更多」改为和其他抽屉入口一样的 `DrawerSectionButton` 样式，只是加一个展开/折叠指示：

```kotlin
// 复用 DrawerSectionButton 的视觉，但加 trailing 展开图标
DrawerSectionButton(
    section = DashboardSection.MORE,  // 新增一个 MORE 枚举（或用自定义参数）
    selected = false,
    onClick = { moreExpanded = !moreExpanded },
    trailingIcon = if (moreExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore
)
```

或者更简单：直接在现有 `DrawerSectionButton` 组件上加一个可选的 `trailingContent` 参数，「更多」用它来显示展开箭头。

### 视觉目标
- 「更多」和「每日看板」「待办」「日历」「规划台」「设置」的行高、字体、图标大小、左侧 padding 完全一致
- 唯一区别：右侧有一个展开/折叠箭头
- 去掉当前的副标题"归档与历史"（太占空间，展开后自然能看到里面是什么）

### 验证
- [ ] 「更多」和其他抽屉入口视觉统一（行高、字体、图标）
- [ ] 点击展开/折叠正常
- [ ] 展开后 AI 报告 / 历史记录 正常显示和跳转

---

# 通用要求

1. 版本号升级即可
2. 数据库版本保持 `24`（本轮无 schema 变更）
3. 每轮 `./gradlew.bat :app:compileDebugKotlin` + `git diff --check` 通过
4. 最后一轮 `./gradlew.bat :app:assembleDebug` 验证
5. commit 严格按 AGENTS.md 的 bullet 列表格式
6. 不要 push 到 GitHub
7. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`

---

# 实现顺序建议

1. **#6（抽屉样式）** — 最简单，5 分钟
2. **#2（循环待办折叠）** — 解决卡顿，用户感知强
3. **#5（Undo）** — 防误操作
4. **#1（手机端拖拽）** — 交互增强
5. **#3（提醒页面视觉）** — 视觉重构
6. **#4（日程常驻通知）** — 新功能，涉及 notification + alarm

---

# 不做的事

- ❌ 不动草稿/发布逻辑
- ❌ 不动桌面端 Web（本轮只改手机端）
- ❌ 不做待办/日程编辑器的 undo（只做规划台）
- ❌ 不做全局搜索（留给后续）
