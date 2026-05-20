# PaykiTodo — UI 修正：日历头、分组筛选、规划台按钮回归 + 草稿/发布分离

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

---

## 背景

v1.12.10-1.12.11 砍掉了规划台太多按钮，导致用户找不到图片识别等核心功能。同时日历界面和分组筛选有布局问题。本轮全部修正。

用户反馈原话：
- "现在的日历界面，按钮还另起了一行，希望它能够和上面的'2026年5月'放在一行"
- "分组筛选中，新建分组得拉到所有分组的最后才能看到，或者这里能不能换个方法呈现？"
- "规划台里面预览旁边的文档的按钮太小了！以及那些图片识别的按钮哪去了？你现在这是大砍功能啊，这可不行！"
- "怎么我一回车，就把任务直接添加到待办中了？我还没批准呢！还不如之前的识别按钮呢！"

---

# 第一部分：日历头部按钮归并到一行

## 问题
当前 `CalendarPanel.kt:780-840` 头部分为两行：
- 第一行：`yyyy年M月` 标题（占满整宽 + 下拉箭头）
- 第二行：今天 / 批量导入 / 视图模式 / 更多 操作按钮

用户希望合并到一行。

## 改动

### 文件
`app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`

### 目标布局

```
┌──────────────────────────────────────────┐
│ 2026年5月 ▼   [今天][+][视图▼][⋯]        │
└──────────────────────────────────────────┘
```

- 标题区（`yyyy年M月` + 下拉箭头）放左侧，**不要 weight(1f) 占满**，按内容宽度
- 操作按钮组放右侧，紧凑排列
- 标题区仍然可点击打开日期选择器（保留现有 `clickable(onClick = onPickDate)` 行为）
- 整行高度统一，按钮垂直居中对齐

### 实现思路

把原来的两个 Surface/Row 合并为一个 Row：

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    // 标题区（紧凑，不占满）
    Surface(
        modifier = Modifier.clickable(onClick = onPickDate),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(...) {
            Text(titleMonth.format(...), ...)
            Icon(KeyboardArrowDown, ...)
        }
    }
    Spacer(Modifier.weight(1f))  // 推按钮组到右侧
    // 操作按钮组
    CalendarHeaderActionButton(label = "今天", onClick = onToday)
    CalendarHeaderActionButton(icon = Icons.Rounded.Add, ..., onClick = onOpenBatchImport)
    Box { /* 视图模式 */ }
    Box { /* 更多 */ }
}
```

### 兼容窄屏

如果手机屏幕过窄装不下 4 个按钮 + 标题：
- 用 `horizontalScroll` 包住操作按钮组（保留现有横滚行为）
- 标题保持固定在最左侧不参与滚动
- 或用 `BoxWithConstraints` 检测宽度，过窄时把"今天"和"+"按钮收进溢出菜单

### 验证
- [ ] `2026年5月 ▼` 和操作按钮在同一行
- [ ] 标题点击仍能打开日期选择器
- [ ] 所有操作按钮（今天、批量导入、视图、更多）功能正常
- [ ] 窄屏下不溢出，不重叠

---

# 第二部分：分组筛选「新建」入口前置

## 问题
`TodoFilterBar.kt:95-101` 当前布局：
```
[全部] [分组A] [分组B] [分组C] ... [新建]
```
分组多时"新建"被推到最右，需要横滚才能看到。

## 改动

### 方案：把「新建」放到「全部」旁边，固定在最左

```
[全部] [+] [分组A] [分组B] [分组C] ...
```

「+」是一个紧凑的圆形 IconButton，使用 `Icons.Rounded.Add`，不带文字。点击后弹出现有的 `TodoGroupEditorDialog`。

### 文件
`app/src/main/java/com/example/todoalarm/ui/TodoFilterBar.kt`

### 实现

把原来在 `sortedGroups.forEach` 后面的「新建」chip，移到 `TodoGroupChip(label = "全部", ...)` 之后：

```kotlin
TodoGroupChip(
    label = "全部",
    color = MaterialTheme.colorScheme.primary,
    selected = selectedGroupIds.isEmpty(),
    leadingIcon = true,
    onClick = { onSelectGroup(null) }
)
// 新位置：紧跟在「全部」后面
Surface(
    modifier = Modifier
        .size(36.dp)
        .clickable { creating = true },
    shape = CircleShape,
    color = MaterialTheme.colorScheme.primaryContainer
) {
    Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = "新建分组",
        modifier = Modifier.padding(8.dp),
        tint = MaterialTheme.colorScheme.onPrimaryContainer
    )
}
sortedGroups.forEach { group -> ... }
// 原来末尾的「新建」chip 删除
```

### 验证
- [ ] 进入待办页，「+」紧跟在「全部」chip 右边
- [ ] 不论分组多少，「+」始终可见，不需要横滚
- [ ] 点击「+」弹出新建分组对话框
- [ ] 长按已有分组仍可编辑/删除
- [ ] 多选分组时末尾的交集/并集 chip 仍正常显示

---

# 第三部分：规划台按钮回归 + 草稿/发布分离

这是本轮最大的改动。

## 3.1 Enter 行为改为「草稿态」

### 当前问题
`PlanningOutlineEditor` 的输入行 / 节点编辑：用户按回车，`createNode` 立刻被调用，节点会立刻创建 `linkedTodoId`，对应的待办/日程立刻进入待办页面。用户没机会确认。

### 用户原话
> "Enter 只是让那个东西以节点的形式呈现，但是真正要确定下来的话，可以通过点按钮单独发布，也可以把所有的全部罗列完之后，点击按钮把全部的发布了。"

### 目标行为

**回车** → 节点出现在大纲里（你能看到自己写的），但**不创建** linkedTodoId，**不进入**待办页面。这是「草稿态」。

**发布** → 通过下面两种方式之一显式触发：
1. **单条发布**：节点行右侧有一个小按钮（编辑模式下也可见），点击该节点发布为正式待办/日程
2. **批量发布**：规划台顶部工具栏有「发布全部」按钮，把当前文档所有草稿节点一次性发布

### 数据模型改动

`planning_nodes` 表新增字段：
```sql
ALTER TABLE planning_nodes ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0;
```

数据库版本 `22 → 23`，新增 `MIGRATION_22_23`。

`PlanningNode` entity 加 `val isDraft: Boolean = false`。

新建节点时默认 `isDraft = true`，且**不创建** linkedTodoId。

发布动作：
- 把 `isDraft = false`
- 创建对应的 TodoItem / CalendarEvent
- 设置 `linkedTodoId`

### 视觉区分

草稿节点和正式节点视觉要明显区分：
- **草稿节点**：圆圈是虚线边框（或更淡的颜色），节点行有一个小标记（如左侧细色条用浅灰色）
- **正式节点**：圆圈实心边框（当前样式），左侧色条用分组颜色

### UI 入口

#### 单条发布
节点行右侧加一个 `Icons.Rounded.Send` 或 `Icons.Rounded.Upload` 小按钮（仅在 `isDraft = true` 时显示）：
```kotlin
if (node.isDraft) {
    IconButton(modifier = Modifier.size(32.dp), onClick = onPublish) {
        Icon(Icons.Rounded.Send, contentDescription = "发布为正式事项")
    }
}
```

#### 批量发布
规划台顶部工具栏加一个按钮 `发布全部草稿`，仅在当前文档有 ≥1 个草稿节点时启用：
```kotlin
Button(onClick = onPublishAllDrafts, enabled = draftNodeCount > 0) {
    Icon(Icons.Rounded.Send, ...)
    Text("发布${draftNodeCount}条")
}
```

### 自动备份不要被草稿触发

`PlanningRepository` 创建草稿节点时**不**触发自动备份。只有 `publishNode` / `publishAllDrafts` 才触发。

### 反向兼容

现有所有节点（v1.12.11 及之前）默认 `isDraft = false`（migration 设默认值 0），保持当前行为。

---

## 3.2 「文档列表」按钮放大 + 显眼

### 当前问题
`PlanningDeskPanel.kt` 顶部工具栏的「文档列表」是一个 40dp 的 IconButton，在一堆按钮里太小，看不清。

### 改动
把「文档列表」按钮改为带文字 + 图标的 OutlinedButton，和「今日」「Markdown」「预览」按钮同级：

```kotlin
OutlinedButton(
    modifier = Modifier.height(40.dp),
    onClick = { documentSheetVisible = true }
) {
    Icon(Icons.Rounded.Article, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(4.dp))
    Text(activeNote?.title?.take(8) ?: "文档")
}
```

按钮上显示当前文档名（截断到 8 字以防过长），点击打开文档列表 sheet。这样用户随时知道在哪个文档里。

---

## 3.3 图片识别按钮回归

### 当前问题
v1.12.10 把「从图片识别日程」从溢出菜单里删了。用户没法再从规划台直接走图片识别。

### 改动

把「从图片识别日程」加回到溢出菜单（`PlanningDeskPanel.kt:437-462` 的 DropdownMenu 内）：

```kotlin
DropdownMenuItem(
    text = { Text("从图片识别日程") },
    onClick = {
        overflowMenuExpanded = false
        imagePicker.launch("image/*")
    },
    enabled = visionProviders.isNotEmpty(),
    leadingIcon = { Icon(Icons.Rounded.Image, null) }
)
```

如果没有配置 vision provider，菜单项 `enabled = false` 并加副文字提示「请先在 设置 → AI 调用配置 中标记支持图片识别的源」。

### 图片识别的草稿态

图片识别现在走 `BackgroundCaptureProcessor` 后台 + 通知。识别结果直接写入规划台节点，但**应该写为草稿节点**（`isDraft = true`），而不是直接发布。这样和分享、语音、回车输入的行为一致——所有捕获默认进草稿，等用户显式发布。

`BackgroundCaptureProcessor` 写入节点时把 `isDraft = true`。

---

## 3.4 顶部工具栏整理

当前工具栏按钮乱：今日 / 大纲↔Markdown 切换 / 预览↔编辑 / 导出 / 导入 / 捕获 / 文档列表 / 更多。需要整理。

### 目标布局

**编辑模式**（默认）下顶部工具栏：

```
[今日] [文档:xxx] [预览] [发布N条] [⋯]
```

- 「今日」：跳到今日笔记
- 「文档:xxx」：当前文档名 + 切换文档（前面 3.2 已说）
- 「预览」：切换到只读预览模式（每行右边显示三点菜单可深度配置）
- 「发布N条」：发布全部草稿（前面 3.1 已说，N=0 时隐藏）
- 「⋯」溢出菜单：新建文档 / 重命名 / 使用说明 / 归档 / 删除文档 / 从图片识别日程 / Markdown 兼容模式

**Markdown 兼容模式**下顶部工具栏：

```
[今日] [文档:xxx] [大纲] [预览/编辑] [识别] [⋯]
```

- 「大纲」：切回大纲模式
- 「预览/编辑」：Markdown 文本预览/编辑切换
- 「识别」：现有的 Markdown 文本识别按钮
- 「⋯」溢出菜单：包含 Markdown 导出 / Markdown 导入

### 验证清单

- [ ] 顶部工具栏不超出一行（必要时用 horizontalScroll）
- [ ] 「文档:xxx」显示当前文档名，点击可切换
- [ ] 草稿节点视觉上和正式节点有区别（虚线圆圈或淡色标记）
- [ ] 草稿节点右侧有「发布」按钮（小图标），点击发布为正式事项
- [ ] 顶部「发布N条」按钮显示当前文档草稿数，点击批量发布
- [ ] 草稿节点的内容**不出现**在待办页面、日历、看板、widget
- [ ] 发布后节点变为正式态，对应待办/日程进入相应位置
- [ ] 溢出菜单包含「从图片识别日程」
- [ ] 图片识别完成后写入草稿节点，不直接发布
- [ ] 分享、语音、拍照捕获也写入草稿节点

---

# 第四部分：桌面端 Web 同步改造

## 4.1 草稿态显示和发布

`app/src/main/assets/desktop-web/app.js` 的规划台 Outliner 渲染需要：

- 渲染节点时检查 `isDraft` 字段，草稿节点用淡色边框和不同的圆圈样式
- 草稿节点行右侧显示「发布」小按钮，点击调用 `POST /api/planning/nodes/{id}/publish`
- 顶部工具栏加「发布全部草稿」按钮，调用 `POST /api/planning/nodes/publish-all?noteId=X`

## 4.2 桌面端 API

新增：
```
POST /api/planning/nodes/{id}/publish        # 发布单个节点
POST /api/planning/nodes/publish-all          # 批量发布（body: noteId）
```

`/api/planning/nodes` GET 响应 JSON 加 `isDraft` 字段。

## 4.3 桌面端日历头部

桌面 Web 日历的月份标题 + 操作按钮如果分两行也合并到一行（如已是一行则不动）。

---

# 通用要求

1. 版本号升级即可
2. 数据库版本 `22 → 23`，新增 `MIGRATION_22_23`
3. 备份/恢复 JSON 加 `isDraft` 字段；旧备份导入按 false 兜底
4. 每轮 `./gradlew.bat :app:compileDebugKotlin` + `git diff --check` 通过
5. 涉及桌面端的轮次 `node --check app/src/main/assets/desktop-web/app.js`
6. 最后一轮 `./gradlew.bat :app:assembleDebug` 验证
7. commit 严格按 AGENTS.md 的 bullet 列表格式，每部分一个 commit
8. 不要 push 到 GitHub
9. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`、`docs/current/PROJECT_STATUS.md`、`docs/current/FEATURE_LEDGER.md`
10. 教程内容（`planningTutorialPages`）也要更新，加入「草稿/发布」说明

---

# 实现顺序建议

1. **第一轮 第一部分（日历头部归并）** — 纯 UI，零数据风险
2. **第二轮 第二部分（分组筛选「+」前置）** — 纯 UI
3. **第三轮 3.2 + 3.3 + 3.4（按钮回归 + 工具栏整理）** — UI 调整，不动数据
4. **第四轮 3.1（草稿/发布分离）** — 数据库 22→23 + 新逻辑，本轮最重
5. **第五轮 第四部分（桌面端同步）** — 桌面端配套
6. **第六轮 教程更新** — 收尾

---

# 不做的事

- ❌ 不动快速捕获入口（分享/拍照/语音）的入口 Activity，只改它们写入节点时的 `isDraft` 标记
- ❌ 不做「自动发布」（比如定时把草稿转正），所有发布都必须用户显式触发
- ❌ 不做发布的撤销（草稿删除即可，正式事项删除走现有删除流程）
- ❌ 不动现有的 syncEnabled 逻辑（结构标题），它和草稿态是不同维度

---

# 致执行端的提醒

1. **草稿态和现有「有子项节点不创建 linkedTodoId」是两个独立维度**：
   - 有子项 → 结构标题，永远不创建 linkedTodoId（v1.12.10 已实现）
   - 叶子 + 草稿 → 暂时不创建 linkedTodoId，发布后才创建
   - 叶子 + 正式 → 创建 linkedTodoId，进入待办/日历
2. **发布时的字段解析**：发布草稿节点时，需要解析节点文本里的「时间, 事件名, 地点」格式，得到 `dueAt` / `startAt` / `endAt` / `location`，再创建对应的 TodoItem / CalendarEvent。这部分逻辑应该已经在现有 createNode 里有了，挪到 publish 时调用即可。
3. **批量发布要做事务**：`publishAllDrafts` 应该一次性扫描该文档所有 `isDraft = true` 的节点，串行发布。如果中间一条失败（比如时间格式无法解析），其他的应该继续，最后通过 toast 报告 "已发布 X 条，Y 条失败"。
4. **快捷捕获的草稿数提示**：分享/拍照/语音完成后的通知文案改为「已添加 N 条草稿，前往规划台发布」，点击通知打开规划台对应文档。
5. **教程截图要重新生成**：草稿/发布的工作流是新概念，教程里要明确"回车只是写下来，发布才入库"。
