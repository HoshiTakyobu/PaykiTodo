# PaykiTodo — 快速捕获 + 规划台 Outliner 重构

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

---

## 设计哲学

### 核心矛盾

纸质笔记 3 步完成的事（拿笔 → 写 → 完成），手机上要 9 步（解锁 → 找图标 → 进规划台 → 选文档 → 打字 → 等 AI 10-30s → 逐条预览 → 逐条配置 → 完成）。

纸赢不是因为功能强，是因为它没有竞争对手——一张纸不会给你弹推送。

### 定位

PaykiTodo 不应该试图取代纸。定位是：
- **纸负责当下的规划和执行**（主战场）
- **PaykiTodo 负责提醒和归档**（在合适的时候震你一下）
- **缩短 capture 路径到 ≤2 步**（拍纸 → 完成，或语音 → 完成）

### 规划台的根本问题

现在的规划台是「编辑器」——要写得对、要选对语法、要看预览、要逐条决定。但用户真实的心智模型不是写文档，是 **dump**：脑子里有事，往里塞，不想再"编辑"它。

收件箱模型：
- 任何来源（语音、拍照、分享）都进同一个地方
- AI 用历史数据猜默认值
- 进去后**立刻可用，不需要确认**
- "整理"是可选异步动作，不是阻塞步骤

---

## 两阶段交付

- **阶段一**：快速捕获表面（S1-S4），不动规划台核心架构，只加入口
- **阶段二**：规划台 outliner 重构（O1-O7），数据模型重写，大纲即数据

---

# 阶段一：快速捕获表面

> 目标：任何场景下 ≤2 步把信息送入 PaykiTodo，不需要打开规划台编辑文档。
> 阶段一仍使用现有的 preview/import 流程（因为 outliner 还没做）。

---

## S1. 系统分享菜单接入

### 场景
群里有人发"周三下午3-5点开会"，长按文字 → 分享 → PaykiTodo → 直接走识别预览。绕开规划台。

### 文件变更

#### 新建 `app/src/main/java/com/example/todoalarm/ui/ShareReceiverActivity.kt`

接收 `ACTION_SEND` / `ACTION_SEND_MULTIPLE`：
- `text/plain` → 提取文本 → 调用 `PlanningRecognitionService` 走 local parse + AI
- `image/*` → 提取 Uri → 走现有 vision pipeline（压缩 → vision provider → Markdown → parse）
- 识别完成后弹出 preview bottom sheet（复用 `PlanningDeskPanel` 的 preview 组件，需抽取为独立 Composable）
- 用户确认导入 / 取消 → `finish()`
- 导入成功后追加到当前活跃规划台文档末尾（带 `#imported` 标记）

#### `app/src/main/AndroidManifest.xml`

```xml
<activity
    android:name=".ui.ShareReceiverActivity"
    android:exported="true"
    android:theme="@style/Theme.TodoAlarm"
    android:label="添加到 PaykiTodo">
    <intent-filter>
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="image/*" />
    </intent-filter>
</activity>
```

### 边界处理
- 空文本 → toast "没有可识别的内容" → finish()
- 识别出 0 条候选 → toast "未能识别出待办或日程" → finish()
- 导入成功 → toast "已添加 N 条" → finish()

### 验证
- [ ] 从微信/QQ 长按文字 → 分享 → 出现 "添加到 PaykiTodo"
- [ ] 分享 "明天下午3点到5点开会" → 弹 preview → 导入后日历有对应日程
- [ ] 分享图片（课表截图）→ 走 vision → 弹 preview
- [ ] 空文本 → toast → 自动关闭

---

## S2. 长按图标"拍照添加"快捷方式

### 场景
桌面长按 app 图标 → 出现"拍纸添加" → 点一下直接相机 → 拍 → 走现有视觉识别。绕开"打开规划台 → 更多 → 从图片识别"那 4 步。

### 文件变更

#### 新建 `app/src/main/res/xml/shortcuts.xml`

```xml
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="capture_camera"
        android:enabled="true"
        android:icon="@drawable/ic_shortcut_camera"
        android:shortcutShortLabel="@string/shortcut_camera_label"
        android:shortcutLongLabel="@string/shortcut_camera_long_label">
        <intent
            android:action="com.paykitodo.ACTION_CAPTURE_CAMERA"
            android:targetPackage="com.paykitodo.app"
            android:targetClass="com.example.todoalarm.ui.CaptureActivity" />
    </shortcut>
    <shortcut
        android:shortcutId="capture_voice"
        android:enabled="true"
        android:icon="@drawable/ic_shortcut_mic"
        android:shortcutShortLabel="@string/shortcut_voice_label"
        android:shortcutLongLabel="@string/shortcut_voice_long_label">
        <intent
            android:action="com.paykitodo.ACTION_CAPTURE_VOICE"
            android:targetPackage="com.paykitodo.app"
            android:targetClass="com.example.todoalarm.ui.VoiceCaptureActivity" />
    </shortcut>
</shortcuts>
```

#### `AndroidManifest.xml` — MainActivity 加 meta-data

```xml
<meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />
```

#### 新建 `CaptureActivity.kt`

1. 启动系统相机 Intent (`ACTION_IMAGE_CAPTURE`)
2. 拿到照片 Uri
3. 调用后台处理（见 S3）
4. `finish()` — 不等结果

### 验证
- [ ] 长按桌面图标 → 出现"拍照添加"和"语音添加"
- [ ] 点"拍照添加" → 系统相机 → 拍照 → 后台处理 → 通知
- [ ] 取消拍照 → 无通知

---

## S3. AI 识别后台化

### 问题
当前规划台"识别"按钮阻塞前台 10-30s，用户不能切走。

### 改动

#### 后台处理器 `BackgroundCaptureProcessor.kt`

```kotlin
object BackgroundCaptureProcessor {
    // 所有捕获来源（分享、拍照、语音）统一走这里
    fun processImage(context: Context, uri: Uri) { /* WorkManager / coroutine */ }
    fun processText(context: Context, text: String) { /* local parse + AI fallback */ }
}
```

#### 通知 channel `capture_processing`
- 处理中：标题 "正在识别..."，indeterminate 进度条
- 完成后：标题 "已识别 N 条待办/日程，点击查看"
- 点击打开 preview（复用 `ShareReceiverActivity` 的 preview 组件）

#### 规划台"识别"按钮改为后台
```kotlin
// 旧：前台阻塞
val result = recognitionService.recognize(markdown)
showPreview(result)

// 新：后台 + 通知
BackgroundCaptureProcessor.processText(context, markdown)
Toast.makeText(context, "正在后台识别，稍后通知", Toast.LENGTH_SHORT).show()
```

#### 桌面端 Web 保持同步
桌面端浏览器无法接收 Android 通知，所以桌面端"识别"仍走同步路径。

### 验证
- [ ] 规划台点"识别" → 立刻 toast → 可以切走做别的
- [ ] 10-30s 后收到通知
- [ ] 点通知打开 preview
- [ ] 桌面端 Web 识别仍是同步（不受影响）

---

## S4. 语音输入

### 场景
说"明天下午三点到五点开会" → 直接进识别。说永远比打字比手写要快很多。

### 文件变更

#### 新建 `VoiceCaptureActivity.kt`

```kotlin
class VoiceCaptureActivity : ComponentActivity() {
    // 使用 Android SpeechRecognizer
    // 语言：zh-CN
    // 实时显示转写文本（EXTRA_PARTIAL_RESULTS）
    // "完成"按钮 → BackgroundCaptureProcessor.processText(transcript)
    // finish()
}
```

#### UI
- 大麦克风图标（动画波纹表示在听）
- 实时转写文本显示
- "完成"按钮（transcript 非空时启用）
- "取消"按钮

#### 权限
- `android.permission.RECORD_AUDIO`（manifest 声明 + runtime 请求）
- 拒绝权限 → toast "需要麦克风权限" → finish()

### 验证
- [ ] 长按图标 → "语音添加" → 麦克风权限请求（首次）
- [ ] 说"明天下午三点开会" → 实时显示转写
- [ ] 点"完成" → 后台识别 → 通知
- [ ] 拒绝权限 → toast → 关闭

---

## 阶段一总结

完成后用户的 capture 路径：
- 看到文字 → 分享 → PaykiTodo（2 步）
- 纸上内容 → 长按图标 → 拍照（2 步）
- 想到事情 → 长按图标 → 说话（2 步）

版本号升级即可，不限定具体号。数据库版本保持 19（无 schema 变更）。

---

# 阶段二：规划台 Outliner 重构

> 目标：把规划台从「markdown 编辑器 + 导入流水线」变成「大纲即数据」。
> 写下来的那一刻它就已经是待办了，不需要"识别 → preview → 导入"。

---

## 用户原话描述的理想形态

> "现在手机上不是都有备忘录/笔记软件嘛，在闲暇之余就能把要做些什么先给写进去，可以是很简单的草稿，比如说就是'入党资料要写完\n数据库复习'。然后当我要拿起来深化的时候，就希望它能够左边有一个朝下的展开的小箭头，然后点开我就能够自然的把深化的具体的小点给写进去了。"

> "具体的小点如果没标时间的话，那就是无DDL的待办。这里点击完成了之后，规划台里面点开，那个事项左边那种待办的小圆圈会被勾选，然后后面那个事项的具体文字会加上删除线，然后字体颜色从原来的灰色变成黑色，就像Obsidian渲染的markdown一样。"

> "如果标上时间了，那就是日程。对于日程，可以在设置界面里面去敲定，要不要把结束时间作为这个日程的DDL，放到待办里面去呈现。那如果单纯的标了一个时间点，那就是个DDL待办了。"

> "上面可以加一条提示的，相当于可以隐藏的表头一样，那些常用的属性，比如说时间，事件名，地点，放在前面。"

> "完成状态，不是在大纲里面点选——当然规划台里面点选也是可以的——主要其实还是待办完成了，然后规划台里面的那个项目相应的被勾掉了。不过反之亦然。"

---

## 核心设计差异

| | 现在的规划台 | 目标形态 |
|---|---|---|
| 数据模型 | 平铺 markdown 文本 → 解析 → 导入到数据库 | 树形大纲本身就是数据 |
| 完成状态 | 导入后在"待办"页面勾选，规划台里看不到 | 待办页面勾选 → 规划台同步显示删除线；反之亦然 |
| 深化方式 | 写 markdown 缩进语法 | 点箭头展开，自然写子项 |
| 类型推断 | 靠 #ddl #schedule 标签或 AI | 靠有没有写时间、写的是时间点还是时间段 |
| 心智模型 | "我在写一篇文档，然后把它变成待办" | "我在列清单，清单本身就是我的待办" |

**关键原则：写下来的那一刻它就已经是待办了。没有"导入"这个概念。**

---

## O1. 数据模型

### 新建表 `planning_nodes`

```sql
CREATE TABLE planning_nodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    noteId INTEGER NOT NULL,
    parentNodeId INTEGER,
    sortOrder INTEGER NOT NULL,
    text TEXT NOT NULL,
    createdAtMillis INTEGER NOT NULL,
    updatedAtMillis INTEGER NOT NULL,

    -- 时间字段（决定类型）
    startAtMillis INTEGER,    -- 时间段开始
    endAtMillis INTEGER,      -- 时间段结束（有 start+end = 日程）
    dueAtMillis INTEGER,      -- DDL 时间点（有 due = DDL 待办）
    location TEXT,

    -- 链接到 todo_items / calendar event
    linkedTodoId INTEGER,

    -- UI 状态
    collapsed INTEGER NOT NULL DEFAULT 0,

    -- 完成状态（与 todo_items.completed 双向同步）
    completed INTEGER NOT NULL DEFAULT 0,
    completedAtMillis INTEGER,

    FOREIGN KEY (noteId) REFERENCES planning_notes(id) ON DELETE CASCADE,
    FOREIGN KEY (parentNodeId) REFERENCES planning_nodes(id) ON DELETE CASCADE,
    FOREIGN KEY (linkedTodoId) REFERENCES todo_items(id) ON DELETE SET NULL
);

CREATE INDEX idx_planning_nodes_note ON planning_nodes(noteId);
CREATE INDEX idx_planning_nodes_parent ON planning_nodes(parentNodeId);
CREATE INDEX idx_planning_nodes_linked_todo ON planning_nodes(linkedTodoId);
```

### Entity

```kotlin
@Entity(tableName = "planning_nodes", ...)
data class PlanningNode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val parentNodeId: Long?,
    val sortOrder: Int,
    val text: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val startAtMillis: Long?,
    val endAtMillis: Long?,
    val dueAtMillis: Long?,
    val location: String?,
    val linkedTodoId: Long?,
    val collapsed: Boolean = false,
    val completed: Boolean = false,
    val completedAtMillis: Long?
)
```

### 数据库迁移 `MIGRATION_19_20`

1. 创建 `planning_nodes` 表
2. 迁移现有 `planning_notes.contentMarkdown` → 按行/缩进解析为树形 nodes
3. 迁移现有 `planning_line_mappings` → 设置对应 node 的 `linkedTodoId`
4. 保留 `planning_notes.contentMarkdown` 字段（作为 legacy 备份，不再作为编辑源）

---

## O2. 类型推断 — 写下来就是待办

### 核心规则

**每个 node 写下来的那一刻就自动是一个待办。** 不需要手动"提升"。

类型由时间字段决定：
- `startAtMillis == null && endAtMillis == null && dueAtMillis == null` → **无 DDL 待办**
- `dueAtMillis != null` → **DDL 待办**（有截止时间）
- `startAtMillis != null && endAtMillis != null` → **日程**（时间段）

### 自动创建 linkedTodoId

当 node 被创建时，**自动**创建对应的 `TodoItem`：
```kotlin
// 用户在大纲里写了一行 "入党资料要写完"
// → 自动创建 TodoItem(title="入党资料要写完", dueDate=null)
// → 设置 node.linkedTodoId = newTodoId
```

当用户给 node 添加时间时：
- 添加时间点 → 更新 linked TodoItem 的 DDL
- 添加时间段 → 创建 CalendarEvent，并根据设置决定是否把结束时间作为 DDL 放到待办（**复用现有设置项**）

### 父节点 vs 子节点

- 用户写 "入党资料要写完" → 自动创建 todo
- 用户展开它，添加子项 "填写个人信息表" / "写思想汇报" → 子项各自创建 todo
- 父节点仍然是 todo（代表整体任务），子节点是具体步骤
- 父节点完成 = 所有子节点都完成（或手动标记整体完成）

---

## O3. 双向同步

### 待办页面勾选 → 规划台同步

这是**主要流程**（用户原话："主要其实还是待办完成了，然后规划台里面的那个项目相应的被勾掉了"）：

```kotlin
// TodoRepository.completeTodo(todoId) 内部：
suspend fun completeTodo(todoId: Long) {
    todoDao.markCompleted(todoId, System.currentTimeMillis())
    // 同步到 planning_nodes
    val nodes = planningNodeDao.getNodesByLinkedTodo(todoId)
    nodes.forEach { node ->
        planningNodeDao.update(node.copy(
            completed = true,
            completedAtMillis = System.currentTimeMillis()
        ))
    }
}
```

### 规划台勾选 → 待办页面同步

反之亦然：

```kotlin
// PlanningNodeRepository.toggleNodeComplete(nodeId) 内部：
suspend fun toggleNodeComplete(nodeId: Long) {
    val node = planningNodeDao.getById(nodeId)
    val newCompleted = !node.completed
    planningNodeDao.update(node.copy(completed = newCompleted, ...))
    // 同步到 todo_items
    node.linkedTodoId?.let { todoId ->
        if (newCompleted) todoDao.markCompleted(todoId, ...)
        else todoDao.markUncompleted(todoId)
    }
}
```

### 文本/时间编辑同步

- node 是 canonical source（大纲是源）
- 编辑 node.text → 更新 linked TodoItem.title
- 编辑 node.dueAtMillis → 更新 linked TodoItem.dueDate
- 编辑 node.startAtMillis/endAtMillis → 更新 linked CalendarEvent

---

## O4. UI — 大纲编辑器

### 替换现有 TextField

规划台打开后不再是 markdown 文本框，而是树形大纲：

```kotlin
@Composable
fun OutlineEditor(nodes: List<PlanningNode>, ...) {
    LazyColumn {
        items(flattenedVisibleNodes) { node ->
            OutlineRow(node, depth = calculateDepth(node))
        }
    }
}
```

### 每行结构

```
[缩进] [折叠箭头] [完成圆圈] [文本] [时间/地点 chip]
```

- **缩进**：`depth * 24.dp`
- **折叠箭头**：仅当有子节点时显示（▶ / ▼）
- **完成圆圈**：所有 node 都有（因为每个 node 都是 todo）
- **文本**：inline 可编辑的 BasicTextField
- **时间/地点 chip**：如果有时间或地点，显示小标签

### 完成状态视觉（严格按用户描述）

```kotlin
// 未完成：灰色文字，无装饰
TextStyle(color = Color.Gray)

// 已完成：黑色文字 + 删除线（像 Obsidian 渲染 markdown 的 - [x]）
TextStyle(
    color = Color.Black,  // 从灰色变成黑色
    textDecoration = TextDecoration.LineThrough
)
```

圆圈状态：未完成 = 空心圆，已完成 = 实心勾选

### 键盘交互

- **Enter**：创建同级 sibling node
- **Tab**：缩进（变成上一个 node 的子节点）
- **Shift+Tab**：取消缩进
- **Backspace**（行首空文本）：删除当前 node

### 长按菜单

- 设置时间（弹出时间选择器）
- 设置地点
- 删除

---

## O5. 隐藏表头提示

用户原话："上面可以加一条提示的，相当于可以隐藏的表头一样，那些常用的属性，比如说时间，事件名，地点"

### 实现

当 node 获得焦点且正在编辑时，在该 node 上方显示一行淡色提示：

```
时间 | 事项 | 地点
```

- 用户输入包含时间格式 → "时间"高亮
- 用户输入包含 `@地点` → "地点"高亮
- 提示行可以通过设置隐藏

---

## O6. Markdown 兼容

### 导出

用户仍可以"导出为 Markdown"（用于分享、备份、复制出去）：
```
- [ ] 入党资料要写完
  - [x] 填写个人信息表
  - [ ] 写思想汇报
- [ ] 数据库复习 ddl 5.28 23:59
```

### 从 Markdown 导入

粘贴 Markdown 文本 → 解析为 nodes（用于从其他 app 迁移数据）。

### 旧文档迁移

现有 `planning_notes.contentMarkdown` 在 `MIGRATION_19_20` 中一次性解析为树形 nodes。

---

## O7. 桌面端同步

### 新增 API

```
GET  /api/planning/nodes?noteId=123    → 返回该文档的所有 nodes（树形）
POST /api/planning/nodes/create        → 创建 node
POST /api/planning/nodes/update        → 更新 node（text/time/location/completed）
POST /api/planning/nodes/delete        → 删除 node
POST /api/planning/nodes/reorder       → 拖拽排序
```

### 桌面 Web UI

- 树形大纲编辑器（类似 Workflowy）
- 支持折叠/展开、inline 编辑
- 完成状态同步显示

---

## 阶段二与阶段一的衔接

当阶段二完成后，阶段一的捕获入口（分享、拍照、语音）不再走 preview/import 流程，而是：
- 捕获的文本/图片 → AI 解析 → **直接作为新 node 插入到大纲中**
- 通知："已添加 N 条到规划台"
- 用户打开规划台就能看到新增的 node，已经是 todo 了

---

# 通用要求

1. 阶段一和阶段二可以分开交付，不需要一次做完
2. 版本号升级即可，不限定具体号
3. 每轮 `./gradlew.bat :app:compileDebugKotlin` + `git diff --check` 通过
4. 最后一轮 `./gradlew.bat :app:assembleDebug` 验证
5. commit 严格按 AGENTS.md 的 bullet 列表格式
6. 不要 push 到 GitHub
7. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`、`docs/current/PROJECT_STATUS.md`、`docs/current/FEATURE_LEDGER.md`

---

# 不做的事（明确排除）

- ❌ AI 学习用户偏好（用户已明确排除）
- ❌ AI 眼镜扫描（硬件依赖）
- ❌ 通知栏 Quick Tile（用户没有选这个）
- ❌ CRDT 冲突解决（保持 last-write-wins）
- ❌ 阶段一不动规划台核心架构
- ❌ 阶段二不删除 markdown 导出功能

---

# 致执行端的提醒

1. **阶段一优先**：S1-S4 是日常高频痛点，先做这些立刻能感受到改善。
2. **阶段二是大重构**：O1-O7 涉及数据模型重写，需要整块时间。
3. **O2 的核心原则**：每个 node 写下来就是 todo，不需要手动"提升"。这和我之前写的"提升"概念不同——没有提升这个动作，写下来就是。
4. **O3 双向同步防死循环**：node 更新 → todo 更新 → 不要再触发 node 更新。用 flag 或 source 标记。
5. **O4 完成状态视觉**：严格按用户描述——灰色文字变黑色 + 删除线，像 Obsidian 渲染 `- [x]`。
6. **日程结束时间作为 DDL**：这个设置已经存在于 app 中（设置 → 日历与提醒），outliner 创建日程时复用这个设置。
7. **迁移脚本要测试**：`MIGRATION_19_20` 要在有真实数据的设备上测试，确保不丢数据。

---

# 最终愿景

1. 纸上写今天要做的事（纸仍是主战场）
2. 拍照 / 语音 / 分享 → PaykiTodo 后台识别 → 通知
3. 打开规划台 → 大纲里已经有了，已经是 todo
4. PaykiTodo 在合适的时候震一下提醒
5. 完成后在待办页面勾掉 → 规划台大纲同步显示删除线（反之亦然）

**纸负责规划，PaykiTodo 负责提醒和归档。**
