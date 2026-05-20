# PaykiTodo — 规划台 Outliner 编辑体验补齐：备忘录级键盘行为

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

---

## 背景

v1.12.10-1.12.11 实现了"像备忘录一样写"的基本框架（底部输入行 + 回车创建节点），但**键盘编辑行为远未达到备忘录水平**。

用户反馈：
- "编辑模式下，光标在第二行（输入行），按手机键盘的删除键，不能删除当前这一行跳到上一行去修改内容"
- "你需要整体把关，我没考虑到的你也应该要实现"

**核心问题**：当前每个节点是独立的 `OutlinedTextField`，底部输入行也是独立的 TextField。它们之间没有"跨行"的键盘行为。在真正的备忘录/笔记 app 里，所有行是一个连续的文本流，Backspace/Delete/方向键可以自然地跨行操作。

---

## 需要补齐的备忘录级键盘行为

### K1. 输入行 Backspace → 跳到上一个节点

**场景**：光标在底部输入行（或子任务输入行），输入行内容为空，按 Backspace。

**预期行为**：
- 输入行消失（或保持但失焦）
- 上一个节点进入编辑状态，光标在文本末尾
- 如果上一个节点有文本，光标定位到末尾，用户可以继续编辑

**当前行为**：什么都不发生（因为 TextField 内容为空，Backspace 无字符可删）。

### K2. 节点编辑时 Backspace 在行首 → 与上一个节点合并

**场景**：用户正在编辑某个节点，光标在文本最前面（position 0），按 Backspace。

**预期行为**：
- 当前节点的文本追加到上一个同级节点的文本末尾
- 当前节点被删除
- 光标定位在合并点（原上一个节点文本的末尾位置）

**示例**：
```
○ 入党资料要写完
○ |数据库复习        ← 光标在"数"前面，按 Backspace
```
变成：
```
○ 入党资料要写完数据库复习|    ← 合并，光标在"完"和"数"之间
```

**当前行为**：只有文本完全为空时才删除节点（`if (text.isBlank()) { onDelete() }`），光标在行首但文本非空时 Backspace 不做任何跨行操作。

### K3. 输入行有内容时 Backspace → 正常删除字符

**场景**：输入行有文字，光标不在最前面，按 Backspace。

**预期行为**：正常删除光标前一个字符（这个应该已经正常工作）。

### K4. 输入行有内容、光标在最前面时 Backspace → 内容合并到上一个节点

**场景**：输入行有文字 "数据库复习"，光标在"数"前面，按 Backspace。

**预期行为**：
- 输入行的文本追加到上一个节点的文本末尾
- 输入行清空
- 上一个节点进入编辑状态，光标在合并点

### K5. 节点编辑时 Enter → 在当前位置断行

**场景**：用户正在编辑节点 "入党资料要写完数据库复习"，光标在"完"和"数"之间，按 Enter。

**预期行为**：
- 当前节点文本变为 "入党资料要写完"
- 在当前节点下方创建新的同级节点，文本为 "数据库复习"
- 光标跳到新节点的文本开头

**当前行为**：Enter 会提交当前编辑并在下方打开一个空的同级输入行。不会断行。

### K6. 视觉：输入行不应该有明显边框

**当前问题**：输入行用的是 `OutlinedTextField`，有明显的绿色边框（截图可见）。这让它看起来像一个"表单输入框"而不是"文本区域的下一行"。

**预期**：输入行应该视觉上融入整体，像是大纲的自然延续。建议：
- 去掉 `OutlinedTextField` 的边框，改用 `BasicTextField` 或把 border 设为透明
- 只保留 placeholder 文字提示
- 聚焦时可以有轻微的底部下划线或背景色变化，但不要完整边框

### K7. 点击节点进入编辑时，光标应在点击位置

**当前行为**：点击节点文字进入编辑后，光标可能在末尾或开头。

**预期行为**：光标应该尽量在用户点击的位置附近（如果技术上做不到精确定位，至少放在末尾）。

### K8. 上下方向键（如果手机键盘有）

**场景**：部分手机键盘有方向键，或者用户连接了外接键盘。

**预期行为**：
- 在节点编辑状态按 ↓ → 如果光标在文本末尾，跳到下一个节点（或输入行）
- 在节点编辑状态按 ↑ → 如果光标在文本开头，跳到上一个节点
- 在输入行按 ↑ → 跳到最后一个节点

**优先级**：低（大多数手机键盘没有方向键），但如果容易实现就加上。

---

## 实现要点

### 关于光标位置追踪

要实现 K2/K4/K5，需要知道光标在 TextField 中的精确位置（selection.start）。Compose 的 `TextFieldValue` 可以追踪 `selection`：

```kotlin
var textFieldValue by remember { mutableStateOf(TextFieldValue(text = node.text)) }

// 判断光标是否在行首
val cursorAtStart = textFieldValue.selection.start == 0 && textFieldValue.selection.end == 0
```

### 关于 Backspace 拦截

当前代码用 `onPreviewKeyEvent` 拦截 `Key.Backspace`，但只在 `text.isBlank()` 时触发。需要改为：

```kotlin
Key.Backspace -> {
    if (text.isBlank()) {
        // K1: 空行删除，跳到上一个节点
        onDeleteAndFocusPrevious()
        true
    } else if (cursorAtStart) {
        // K2: 光标在行首，与上一个节点合并
        onMergeWithPrevious(text)
        true
    } else {
        false // 正常删除字符
    }
}
```

### 关于 Enter 断行

当前 Enter 行为是"提交 + 创建空 sibling"。需要改为：

```kotlin
Key.Enter -> {
    val before = text.substring(0, selection.start)
    val after = text.substring(selection.start)
    // 当前节点文本改为 before
    onTextCommit(before)
    // 创建新 sibling，文本为 after，光标在开头
    onCreateSiblingWithText(after)
    true
}
```

### 关于输入行视觉

把 `PlanningOutlineInputLine` 里的 `OutlinedTextField` 改为 `BasicTextField`：

```kotlin
BasicTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.weight(1f).focusRequester(focusRequester),
    textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    decorationBox = { innerTextField ->
        Box {
            if (value.isEmpty()) {
                Text(placeholder, color = MaterialTheme.colorScheme.outline)
            }
            innerTextField()
        }
    }
)
```

---

## 新增回调

`PlanningOutlineRow` 需要新增：

```kotlin
onDeleteAndFocusPrevious: () -> Unit,       // K1: 空行 Backspace
onMergeWithPrevious: (String) -> Unit,      // K2: 行首 Backspace，传入当前文本
onCreateSiblingWithText: (String) -> Unit,  // K5: Enter 断行，传入光标后的文本
```

`PlanningOutlineInputLine` 需要新增：

```kotlin
onBackspaceEmpty: () -> Unit,               // K1: 空输入行 Backspace
onBackspaceAtStart: (String) -> Unit,       // K4: 输入行行首 Backspace，传入当前文本
```

---

## 通用要求

1. 版本号升级即可
2. 每轮 `./gradlew.bat :app:compileDebugKotlin` + `git diff --check` 通过
3. 最后一轮 `./gradlew.bat :app:assembleDebug` 验证
4. commit 严格按 AGENTS.md 的 bullet 列表格式
5. 不要 push 到 GitHub
6. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`

---

## 桌面端 Web 同理

桌面端 Web Outliner（`app/src/main/assets/desktop-web/app.js`）也需要实现完全相同的键盘行为：

- Backspace 在空行 → 删除当前行，焦点跳到上一个节点末尾
- Backspace 在行首（文本非空）→ 与上一个节点合并
- Enter 在光标中间 → 断行为两个节点
- 输入行视觉上不应有明显边框，融入大纲
- 上下方向键跨行（桌面端键盘一定有方向键，优先级比手机端高）

桌面端是标准 DOM + JS，键盘事件通过 `keydown` 监听，光标位置通过 `selectionStart` / `selectionEnd` 获取，实现比 Compose 更直接。

---

## 不做的事

- ❌ 不改数据模型（纯 UI/交互层修改）
- ❌ 不动预览模式逻辑
- ❌ 不动快速捕获入口

---

## 验证清单

- [ ] 底部输入行为空时按 Backspace → 上一个节点进入编辑，光标在末尾
- [ ] 编辑节点时光标在行首按 Backspace → 与上一个节点合并，光标在合并点
- [ ] 编辑节点时光标在中间按 Enter → 断行：当前节点保留光标前文本，新节点获得光标后文本
- [ ] 输入行有内容、光标在最前面按 Backspace → 内容合并到上一个节点
- [ ] 输入行没有明显边框，视觉上像大纲的自然延续
- [ ] 子任务输入行同样具备以上所有行为
- [ ] 正常的字符删除不受影响（光标不在行首时 Backspace 正常删字符）

### 桌面端 Web
- [ ] 桌面端空行 Backspace → 删除当前行，焦点跳到上一个节点末尾
- [ ] 桌面端行首 Backspace → 与上一个节点合并
- [ ] 桌面端 Enter 在光标中间 → 断行为两个节点
- [ ] 桌面端上下方向键可跨行移动焦点
- [ ] 桌面端输入行视觉融入大纲，无明显边框

---

## 致执行端的提醒

1. **这是纯交互层修改**，不涉及数据库、同步、备份等。但要注意合并节点时 linkedTodoId 的处理：如果两个节点都有 linkedTodoId，合并后保留前一个节点的 linkedTodoId，删除后一个节点的 linked item。
2. **TextFieldValue 的 selection 追踪是关键**：当前代码用的是 `String` 类型的 `text` 状态，需要改为 `TextFieldValue` 才能追踪光标位置。
3. **手机键盘的 Backspace 事件**：Android 软键盘的 Backspace 不一定能被 `onPreviewKeyEvent` 捕获（取决于 IME 实现）。可能需要同时监听 `onValueChange` 中文本长度的变化来判断是否发生了删除。如果 `onPreviewKeyEvent` 不可靠，备选方案是：在 `onValueChange` 中检测"文本变短了 + 新光标在位置 0" → 触发合并逻辑。
4. **输入行去边框后要保证可点击区域足够大**：没有边框后用户可能不知道哪里可以点击输入。保留 placeholder 文字和足够的 padding。
