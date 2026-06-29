# Widget 与循环范围弹窗 UI 改版交接文档

状态：设计交接稿，当前轮次不直接实施 Widget / 循环范围弹窗代码改版。

当前源码基线：`1.14.18 / versionCode 338`。本文件用于把用户反馈、现有代码结构、可执行改版方向整理给后续前端 / UI 实现工具。

## 1. 本文覆盖的问题

### 1.1 今日看板 Widget

用户反馈：

1. 桌面上方 `PaykiTodo` 今日看板小组件里，几乎所有内容都被做成圆角矩形卡片，整体可能过于单调，像一堆相同容器堆叠。
2. `今日日程（0）` 后面的数量在 Widget 上需要取消，显示为 `今日日程` 即可。
3. Widget 需要继续接近应用内每日看板，但不能机械复制成所有元素都是圆角卡片。

### 1.2 倒数日 Widget

用户反馈：

1. 没有倒数目标时，空状态文字出现在小组件中间，观感怪异。
2. 倒数日 Widget 应更像一个轻量信息入口：没有数据时应该温和提示，而不是占据视觉中心。

### 1.3 循环待办 / 循环日程范围选择弹窗

用户反馈：

1. 取消循环待办时的范围选择界面太丑。
2. 该界面与“修改循环待办 / 删除循环待办 / 取消循环日程 / 删除循环日程”等范围选择场景共用，应统一改善。

### 1.4 本轮实际代码改动

本轮只实际删除 AI 报告页内容区重复的 `AI 报告` 标题。Widget 和循环范围弹窗只做设计交接，不改实现。

相关实际修改文件：

- `app/src/main/java/com/example/todoalarm/ui/AiReportPanel.kt`

## 2. Widget 现有代码结构

### 2.1 Android 今日看板 Widget

主要代码：

- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetProvider.kt`
  - Widget Provider。
  - 负责绑定 `RemoteViews`、`RemoteViewsService`、点击 PendingIntent、刷新调度。
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
  - `RemoteViewsService` / factory。
  - 将 `DailyBoardSnapshotBuilder` 的数据转成不同类型的 `WidgetBoardRow`。
  - 决定每一行用哪个 XML 布局、展示什么文案、点击进入哪里。

主要布局：

- `app/src/main/res/layout/widget_todo.xml`
  - 今日看板 Widget 根布局。
  - `@id/widget_root` 使用 `@drawable/widget_board_background`。
  - 内层 `LinearLayout` 当前外边距为 `12dp`。
  - 内容区域是 `@id/widget_list` 的 `ListView`。
- `app/src/main/res/layout/widget_todo_greeting_card.xml`
  - 问候卡。
  - 当前是圆角背景 `@drawable/widget_greeting_background`。
- `app/src/main/res/layout/widget_todo_section.xml`
  - 分区标题行。
  - 例如 `今日待办（N）`、`今日日程（N）`。
  - 这不是卡片，但字号较大，和下面卡片的层级容易混在一起。
- `app/src/main/res/layout/widget_todo_empty_card.xml`
  - 空状态卡片。
  - 当前也是圆角软卡片 `@drawable/widget_todo_soft_card_background`。
- `app/src/main/res/layout/widget_todo_task_card.xml`
  - 待办卡片。
  - 当前是圆角软卡片 + 左侧色条。
- `app/src/main/res/layout/widget_todo_schedule_card.xml`
  - 今日/明日日程聚合卡。
  - 当前是圆角卡片背景 `@drawable/widget_todo_item_background`，内部再有多层圆角小背景。
- `app/src/main/res/layout/widget_todo_preview.xml`
  - Widget 选择器里的静态预览布局。
  - 当前还写着示例 `今日日程（1）`，后续如果正式去掉数量，预览也应同步。

当前 `TodoWidgetService.kt` 中的关键行：

```kotlin
output += sectionRow(-1L, "今日待办（${snapshot.todoItems.size}）", highlight = true, opensTasks = true)
...
output += sectionRow(-3L, "今日日程（${snapshot.visibleTodayEvents.size}）", highlight = true, opensCalendar = true)
```

用户当前明确要求：`今日日程（0）` 改为 `今日日程`。建议后续只改日程标题，不必顺手改 `今日待办（N）`，因为用户只点名了日程数量。

建议代码目标：

```kotlin
output += sectionRow(-3L, "今日日程", highlight = true, opensCalendar = true)
```

同时同步 `widget_todo_preview.xml` 中的示例文字：

```xml
android:text="今日日程"
```

### 2.2 倒数日 Widget

主要代码：

- `app/src/main/java/com/example/todoalarm/widget/CountdownWidgetProvider.kt`
  - 绑定倒数日 Widget 根布局。
  - 当前调用：
    - `setRemoteAdapter(R.id.widget_countdown_list, serviceIntent)`
    - `setEmptyView(R.id.widget_countdown_list, R.id.widget_countdown_empty)`
    - `setOnClickPendingIntent(R.id.widget_countdown_root, openBoardIntent)`
- `app/src/main/java/com/example/todoalarm/widget/CountdownWidgetService.kt`
  - 通过 `DailyBoardSnapshotBuilder` 获取 `countdownItems`。
  - 过滤已经过去的目标。
  - 每行使用 `widget_countdown_item.xml`。

主要布局：

- `app/src/main/res/layout/widget_countdown.xml`
  - 根背景同样是 `@drawable/widget_board_background`。
  - 内部 `FrameLayout` padding 为 `12dp`。
  - `@id/widget_countdown_list` 填满空间。
  - 空状态 `@id/widget_countdown_empty` 当前：

```xml
android:layout_gravity="center"
android:background="@drawable/widget_todo_soft_card_background"
android:gravity="center"
android:text="还没有设置倒数目标。编辑待办或日程时打开倒数日开关。"
android:textStyle="bold"
```

问题根因：空状态被放在整个 Widget 的中心，且本身又是一个较重的圆角卡片；在桌面小组件这种“环境 UI”里，会显得像一条提示强行悬在中间。

- `app/src/main/res/layout/widget_countdown_item.xml`
  - 倒数日条目。
  - 左侧固定 `54dp` 倒计时列。
  - 中间 `5dp x 48dp` 竖线。
  - 右侧标题最多 3 行、元信息最多 2 行。

## 3. Widget 当前视觉问题分析

### 3.1 圆角矩形过度重复

当前 Widget 的主要层级是：

1. 根背景：圆角 / 背景图或半透明背景。
2. 问候卡：圆角矩形。
3. 待办空状态：圆角矩形。
4. 待办条目：圆角矩形。
5. 日程聚合卡：圆角矩形。
6. 日程聚合卡内部消息 / 日程行：又是圆角矩形。
7. 倒数日空状态：圆角矩形。
8. 倒数日条目：圆角矩形。

这种设计的问题不是“圆角本身不好”，而是所有层级都用了相同的容器语言。结果是：

- 视觉层级不清：标题、内容、空状态、聚合区都像同级卡片。
- 信息密度偏低：每层卡片都有 padding / margin / elevation，空间被容器吃掉。
- 桌面小组件不像“看板”，更像“卡片列表”。
- 和应用内每日看板靠近时，应借鉴它的层次和色条，不应简单复制圆角卡片数量。

### 3.2 分区标题与卡片关系不够明确

`widget_todo_section.xml` 当前标题字号 `21sp`，标题行不带明确底，也没有更轻量的辅助层级。标题下面紧跟大卡片时，视觉上容易变成：

```text
大标题
大圆角卡
大标题
大圆角卡
```

这会强化“重复堆叠”的感觉。

### 3.3 `今日日程（0）` 的数量语义不适合 Widget

手机每日看板已经去掉 `今日日程` 后的数量，原因是已结束日程会从今日可见日程中隐藏，数字容易和用户直觉冲突。

在 Widget 上，数量更不必要：

- Widget 面积小，`（0）` 会吸引注意力但没有行动价值。
- 用户更关心有没有具体日程内容。
- 空状态文案已经能表达“暂无日程”。

因此 Widget 日程标题建议固定为 `今日日程`。

### 3.4 倒数日空状态居中显得突兀

倒数日 Widget 在无数据时，用户期望看到一个“这里暂无目标”的轻提示，而不是一个居中的粗体卡片。居中布局会给人一种错误感：

- 像加载失败；
- 像系统占位；
- 像一条弹窗被嵌进 Widget。

建议改为顶部对齐的轻量空状态，保留桌面小组件的安静感。

## 4. Widget 改版设计建议

### 4.1 总体设计原则

后续实现时建议遵循：

1. **只保留一个主要背景容器**：根 Widget 背景承担整体圆角和边界。
2. **减少同级圆角卡片数量**：不是所有信息都需要卡片底。
3. **内容条目优先用色条 / 缩进 / 字重区分**：待办和日程可以靠左侧色条、图标、行距形成层级。
4. **问候卡可以保留卡片感**：它是顶部情绪入口，允许更突出。
5. **分区标题应轻量**：更像看板标题，不像一个按钮或卡片。
6. **空状态应轻量**：不要大面积居中，不要强提示，最好贴近所属分区。
7. **RemoteViews 限制优先**：不要设计 Compose 才好做的动态测量或复杂自适应；XML + Service 填充能稳定实现才是目标。

### 4.2 今日看板 Widget 推荐结构

建议视觉结构：

```text
┌─────────────────────────────┐
│  早上好，Payki              │  ← 问候区，保留轻卡片或半透明面板
│  今天是 6月29日 ...         │
│                             │
│  今日待办（2）              │  ← 轻量标题行，不加外框
│  ▌ 任务 A                   │  ← 条目可有轻底，但不必每层都重
│     DDL 18:00               │
│  ▌ 任务 B                   │
│                             │
│  今日日程                   │  ← 固定不显示数量
│  29                         │  ← 聚合日程卡可保留一张完整卡
│  周一   ▌ 10:00-12:00 ...   │
│         明天                │
│         暂无日程            │
└─────────────────────────────┘
```

设计重点：

- 问候区是顶部唯一“明显卡片”。
- `今日待办` 和 `今日日程` 是标题，不应像按钮。
- 待办条目可以从当前圆角软卡片降级为“浅底行 + 左侧色条”，或继续保留轻卡片但减少 elevation / radius / padding。
- 日程聚合卡建议保留一张，因为它包含今天日期块、今天/明天两个小节，结构复杂，卡片有必要。
- 日程聚合卡内部不建议再大量嵌套圆角卡片；事件行可以用透明底 + 左色条 + 文字层级。

### 4.3 具体文件改造建议

#### 4.3.1 删除 Widget 日程标题数量

文件：

- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`

改：

```kotlin
output += sectionRow(-3L, "今日日程", highlight = true, opensCalendar = true)
```

同步预览：

- `app/src/main/res/layout/widget_todo_preview.xml`

改：

```xml
android:text="今日日程"
```

#### 4.3.2 降低分区标题重量

文件：

- `app/src/main/res/layout/widget_todo_section.xml`

建议：

- `android:textSize="21sp"` 降到 `18sp` 或 `19sp`。
- 保留 bold，但减少 shadow 强度。
- `paddingTop` 可根据前后间距调为 `8dp`，但 `paddingBottom` 不宜太大。
- 如果 `widget_section_meta` 为空，可以由代码隐藏，避免右侧占位或视觉噪声。

目标：标题像“看板分区”，不是一张新的卡片。

#### 4.3.3 待办条目轻量化

文件：

- `app/src/main/res/layout/widget_todo_task_card.xml`
- `app/src/main/res/drawable/widget_todo_soft_card_background.xml`
- `app/src/main/res/drawable/widget_task_meta_background.xml`

建议方案 A：轻卡片保留，但弱化容器。

- `elevation` 从 `2dp` 降为 `0dp` 或移除。
- 背景 alpha 降低，边框弱化。
- `minHeight` 从 `68dp` 视情况降到 `60dp`。
- 左侧色条保留，作为主要视觉锚点。

建议方案 B：改成“无外卡片行”。

- 根布局背景改透明。
- 每条待办只保留左侧色条、标题、DDL chip。
- 用 `ListView` dividerHeight 处理条目间距。

推荐优先方案 A，因为改动风险更低，RemoteViews 行点击区域和现有深浅色资源更稳定。

#### 4.3.4 日程聚合卡减少内部嵌套卡片感

文件：

- `app/src/main/res/layout/widget_todo_schedule_card.xml`
- `app/src/main/res/drawable/widget_schedule_inner_background.xml`
- `app/src/main/res/drawable/widget_schedule_event_background.xml`

建议：

- 外层日程聚合卡保留。
- 内部今天/明天空状态背景可改为透明或极低 alpha。
- 日程事件行背景可减少圆角和底色，让左侧色条成为主识别元素。
- `widget_schedule_today_message` / `widget_schedule_tomorrow_message` 的 padding 可略减，避免空状态比真实事项更抢眼。

### 4.4 倒数日 Widget 空状态设计

当前：

```xml
android:layout_gravity="center"
android:background="@drawable/widget_todo_soft_card_background"
android:gravity="center"
android:textStyle="bold"
```

建议改为顶部轻提示：

```text
┌─────────────────────────────┐
│  暂无倒数目标                │
│  在待办或日程中打开倒数日     │
│                             │
│                             │
└─────────────────────────────┘
```

具体建议：

- `layout_gravity` 从 `center` 改为 `top`。
- `gravity` 从 `center` 改为 `start|center_vertical`。
- 文案拆成主副两行更好；如果仍用一个 `TextView`，可写成：
  - `暂无倒数目标\n在待办或日程中打开倒数日`
- 字重不要整体 bold；主标题可 bold，副标题普通。若继续单 TextView，就不建议 bold。
- 背景可以保留轻卡片，但宽度不应像弹窗；也可以使用透明底 + 左侧小色条。

如果用 RemoteViews 兼容性更强的简单方案：

- 保留一个 `TextView`。
- 改为顶部对齐。
- 改文案和 padding。
- 去掉 `android:textStyle="bold"`。

目标 XML 方向：

```xml
<TextView
    android:id="@+id/widget_countdown_empty"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="top"
    android:background="@drawable/widget_todo_soft_card_background"
    android:gravity="start|center_vertical"
    android:paddingStart="14dp"
    android:paddingTop="12dp"
    android:paddingEnd="14dp"
    android:paddingBottom="12dp"
    android:text="暂无倒数目标\n在待办或日程中打开倒数日"
    android:textColor="@color/widget_text_muted"
    android:textSize="13sp" />
```

如果后续要更精致，可以改成单独 empty layout，但当前 `setEmptyView` 只能指定同一布局里的 View ID，最稳妥是继续维护 `widget_countdown.xml` 里的 `TextView`。

## 5. 循环范围弹窗现有代码结构

主要文件：

- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`

相关类型：

```kotlin
private enum class ScopeDialogMode {
    EDIT_TODO,
    CANCEL_TODO,
    DELETE_TODO,
    EDIT_EVENT,
    CANCEL_EVENT,
    DELETE_EVENT
}
```

触发路径：

- `requestCancelTodo(item)`
  - 循环待办且非历史：`scopeDialogMode = ScopeDialogMode.CANCEL_TODO`
  - 非循环：直接 `onCancelTodo(item, RecurrenceScope.CURRENT)`
- `requestDeleteTodo(item)`
  - 循环待办：`ScopeDialogMode.DELETE_TODO`
- `requestCancelCalendarEvent(item)`
  - 循环日程：`ScopeDialogMode.CANCEL_EVENT`
- 日程删除入口：
  - 循环日程：`ScopeDialogMode.DELETE_EVENT`
- 待办 / 日程编辑入口：
  - 循环事项：`ScopeDialogMode.EDIT_TODO` 或 `EDIT_EVENT`

弹窗调用位置：

```kotlin
scopeDialogTarget?.let { item ->
    val mode = scopeDialogMode
    RecurrenceScopeDialog(
        title = when (scopeDialogMode) {
            ScopeDialogMode.EDIT_TODO, ScopeDialogMode.EDIT_EVENT -> "选择修改范围"
            ScopeDialogMode.CANCEL_TODO -> "取消循环待办"
            ScopeDialogMode.CANCEL_EVENT -> "取消循环日程"
            ScopeDialogMode.DELETE_TODO -> "选择删除范围"
            ScopeDialogMode.DELETE_EVENT -> "选择删除范围"
            null -> "选择范围"
        },
        message = recurrenceScopeMessage(mode),
        scopeLabel = { scope -> recurrenceScopeLabel(mode, scope) },
        ...
    )
}
```

当前弹窗实现：

```kotlin
@Composable
private fun RecurrenceScopeDialog(
    title: String,
    message: String,
    scopeLabel: (RecurrenceScope) -> String,
    onDismiss: () -> Unit,
    onSelect: (RecurrenceScope) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Column {
                RecurrenceScope.entries.forEach { scope ->
                    TextButton(onClick = { onSelect(scope) }) {
                        Text(scopeLabel(scope))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
```

当前文案：

```kotlin
private fun recurrenceScopeMessage(mode: ScopeDialogMode?): String {
    return when (mode) {
        ScopeDialogMode.CANCEL_TODO -> "请选择要取消哪一部分循环待办。取消会停止对应提醒并进入历史记录，不会直接删除。"
        ScopeDialogMode.CANCEL_EVENT -> "请选择要取消哪一部分循环日程。取消会停止对应提醒并进入历史记录，不会直接删除。"
        ScopeDialogMode.DELETE_TODO -> "删除不会进入历史记录，请选择要删除哪一部分循环待办。"
        ScopeDialogMode.EDIT_TODO -> "请选择这次修改影响哪一部分循环待办。"
        ScopeDialogMode.EDIT_EVENT -> "请选择这次修改影响哪一部分循环日程。"
        ScopeDialogMode.DELETE_EVENT -> "删除不会进入历史记录，请选择要删除哪一部分循环日程。"
        null -> "循环事项需要先确定这次操作影响的范围。"
    }
}
```

按钮标签：

```kotlin
RecurrenceScope.CURRENT -> "仅${verb}当前$noun"
RecurrenceScope.CURRENT_AND_FUTURE -> "${verb}当前及之后$noun"
RecurrenceScope.ALL -> "${verb}全部循环$noun"
```

## 6. 循环范围弹窗当前问题分析

### 6.1 AlertDialog 不适合这个选择场景

这个弹窗不是简单确认，而是三选一的高风险操作。普通 `AlertDialog` 的 confirmButton 区域被塞入竖向 `Column`，语义上不自然：

- confirmButton 本来期望一个确认按钮，不适合放 3 个主操作。
- dismissButton 与三项操作视觉关系混乱。
- 所有选项都是普通 `TextButton`，用户很难快速判断影响范围。

### 6.2 文案过长且压迫

例如：

`请选择要取消哪一部分循环待办。取消会停止对应提醒并进入历史记录，不会直接删除。`

问题：

- 在小弹窗里一整句显得挤。
- 用户真正要判断的是三个范围，而不是先读一段说明。
- “进入历史记录 / 不会直接删除”的语义很重要，但应放到对应选项说明或底部提示，而不是压在顶部。

### 6.3 三个范围没有解释差异

当前按钮只写：

- `仅取消当前任务`
- `取消当前及之后任务`
- `取消全部循环任务`

这对熟悉循环语义的人够用，但对普通用户仍容易误解：

- “当前”是否只影响今天这一次？
- “当前及之后”是否保留过去记录？
- “全部循环”是否包括已经完成 / 已取消的历史？

应该用主标题 + 副说明解释。

### 6.4 危险程度没有视觉层级

取消进入历史记录，删除是硬删除。二者风险不同。

当前 `DELETE_TODO` / `DELETE_EVENT` 和 `CANCEL_TODO` / `CANCEL_EVENT` 用同一个视觉样式，只靠文字区分。建议：

- 取消：使用中性或橙色提醒。
- 删除：使用红色危险样式。
- 修改：使用主色或中性样式。

## 7. 循环范围弹窗改版设计建议

### 7.1 推荐形态：自定义 ModalBottomSheet 或定制 Dialog

优先建议：使用 `ModalBottomSheet` 或项目已有底部预览 / 危险操作 sheet 的视觉语言，而不是继续用原生 `AlertDialog`。

原因：

- 用户是在事项预览 / 编辑流程里触发，底部 sheet 更符合手机端当前交互。
- 三个选项卡片可以纵向排布，解释文字更从容。
- 底部可以放 `取消`，不会和操作选项抢位置。

如果不想引入 BottomSheet，也至少把 `AlertDialog` 内容改成自定义 `text` 区域里的三张 option card，`confirmButton` 留空或只保留关闭。

### 7.2 推荐布局

```text
┌─────────────────────────────┐
│  取消循环待办                │
│  选择这次要影响的范围         │
│                             │
│  ┌───────────────────────┐  │
│  │ 仅取消当前任务          │  │
│  │ 只归档这一次，后续循环照常 │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │ 取消当前及之后任务      │  │
│  │ 保留过去记录，停止后续循环 │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │ 取消全部循环任务        │  │
│  │ 整个系列进入已取消历史   │  │
│  └───────────────────────┘  │
│                             │
│  关闭                       │
└─────────────────────────────┘
```

删除场景：

```text
删除循环待办
删除不会进入历史记录，请谨慎选择范围

[仅删除当前任务]
只删除这一次，不写入历史记录

[删除当前及之后任务]
删除本次和未来实例，过去记录保留

[删除全部循环任务]
删除整个循环系列，无法在历史记录中恢复
```

修改场景：

```text
选择修改范围
决定这次编辑影响哪一部分循环事项

[仅修改当前任务/日程]
只改变这一次，后续循环保持原规则

[修改当前及之后任务/日程]
从这一次开始应用新内容或新时间

[修改全部循环任务/日程]
整个循环系列使用新的设置
```

### 7.3 建议新增数据模型

在 `DashboardScreen.kt` 里可以新增内部展示模型，避免把文字拼接散落在 UI 中：

```kotlin
private data class RecurrenceScopeOptionUi(
    val scope: RecurrenceScope,
    val title: String,
    val description: String,
    val destructive: Boolean = false
)
```

生成函数：

```kotlin
private fun recurrenceScopeOptions(mode: ScopeDialogMode?): List<RecurrenceScopeOptionUi>
```

这样不同 mode 可以清晰返回：

- title：`仅取消当前任务`
- description：`只归档这一次，后续循环照常生成`
- destructive：删除场景为 true，取消场景为 false 或 warning

### 7.4 建议文案

#### 7.4.1 取消循环待办

标题：`取消循环待办`

副标题：`取消会进入历史记录，不等于删除。`

选项：

1. `仅取消当前任务`
   - `只归档这一次，后续循环任务照常生成。`
2. `取消当前及之后任务`
   - `保留过去记录，从这一次开始停止后续循环。`
3. `取消全部循环任务`
   - `整个循环系列都会标记为已取消。`

#### 7.4.2 取消循环日程

标题：`取消循环日程`

副标题：`取消会进入历史记录，不等于删除。`

选项：

1. `仅取消当前日程`
   - `只取消这一次，后续日程照常生成。`
2. `取消当前及之后日程`
   - `保留过去记录，从这一次开始停止后续日程。`
3. `取消全部循环日程`
   - `整个循环系列都会标记为已取消。`

#### 7.4.3 删除循环待办 / 日程

标题：`删除循环待办` / `删除循环日程`

副标题：`删除不会进入历史记录，无法通过历史记录恢复。`

选项：

1. `仅删除当前任务/日程`
   - `只移除这一次，不写入历史记录。`
2. `删除当前及之后任务/日程`
   - `删除本次和未来实例，过去记录保留。`
3. `删除全部循环任务/日程`
   - `删除整个循环系列，请谨慎操作。`

#### 7.4.4 修改循环待办 / 日程

标题：`选择修改范围`

副标题：`决定这次编辑影响哪一部分循环事项。`

选项：

1. `仅修改当前任务/日程`
   - `只改变这一次，后续循环保持原规则。`
2. `修改当前及之后任务/日程`
   - `从这一次开始应用新的内容、时间或提醒。`
3. `修改全部循环任务/日程`
   - `整个循环系列使用新的设置。`

### 7.5 视觉细节建议

1. 顶部标题：
   - `titleLarge` 或 `titleMedium` bold。
   - 删除场景可以用红色图标或红色小提示，但标题本身不必全红。
2. 选项卡：
   - 高度约 `68dp` 到 `76dp`。
   - 圆角 `18dp` 到 `22dp`。
   - 主标题 `bodyLarge` / `titleSmall` bold。
   - 副说明 `bodySmall`，颜色 `onSurfaceVariant`。
3. 选项点击反馈：
   - `Surface(onClick = ...)` 或 `Card(onClick = ...)`。
   - 删除场景的 option border / icon 使用 `error`，但不要把整张卡染成高饱和红。
4. 底部关闭：
   - 放成一条 `TextButton("关闭")` 或顶部右侧 close icon。
   - 不和三项操作混在 `confirmButton` 区。
5. 尺寸：
   - 手机端 bottom sheet 左右 padding `18dp` 到 `20dp`。
   - 选项间距 `10dp`。
   - 底部留白适配导航栏。

### 7.6 建议修改点

文件：

- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`

建议重构：

1. 保留 `ScopeDialogMode`。
2. 将 `RecurrenceScopeDialog` 从 `AlertDialog` 改为自定义 bottom-sheet 风格组件。
3. 新增：
   - `recurrenceScopeDialogTitle(mode)`
   - `recurrenceScopeDialogSubtitle(mode)`
   - `recurrenceScopeOptions(mode)`
4. 弃用或精简：
   - `recurrenceScopeMessage(mode)`
   - `recurrenceScopeLabel(mode, scope)`
5. 确保选择后的业务逻辑不变：
   - `onCancelTodo(item, selectedScope)`
   - `onCancelCalendarEvent(item, selectedScope)`
   - `onDeleteTodo(item, selectedScope)`
   - `onDeleteCalendarEvent(item, selectedScope)`
   - 编辑场景设置 `editScope = selectedScope` 后打开对应 editor。

不要改动仓库层循环语义，除非另开专项验证。当前需求是 UI 选择面板优化，不是循环数据逻辑重写。

## 8. 后续验收清单

### 8.1 Widget

- [ ] 今日看板 Widget 标题显示 `今日日程`，不显示 `（0）` / `（1）`。
- [ ] Widget 预览图同步显示 `今日日程`。
- [ ] 问候、待办、日程、空状态不再全部像同一种圆角卡片。
- [ ] 倒数日 Widget 无目标时，空状态顶部对齐或贴近内容起点，不再悬在中心。
- [ ] 浅色 / 深色桌面下主文字、弱文字、色条都可读。
- [ ] 小尺寸 Widget 下不会因为 padding / card 嵌套导致只露出很少内容。
- [ ] 点击行为不回归：待办行进待办详情，日程行进日历/日程详情，空状态和标题按原有路由。

### 8.2 循环范围弹窗

- [ ] 取消循环待办出现三项范围选择，每项有主标题和解释。
- [ ] 取消循环日程复用同一视觉语言。
- [ ] 删除循环待办 / 日程有明确危险语义，不和取消混淆。
- [ ] 修改循环待办 / 日程仍能选择当前、当前及之后、全部。
- [ ] 点击范围后的业务行为和当前版本一致，不引入循环实例重建/删除语义变化。
- [ ] 小屏手机无需明显滚动即可看到标题和至少主要选项。
- [ ] 深色模式下卡片文字、说明文字、危险色可读。

## 9. 建议实施顺序

1. 先做 `今日日程` 去数量，这是最小确定修复。
2. 再改倒数日 Widget 空状态，风险低，收益明显。
3. 再做今日看板 Widget 容器层级瘦身，逐个布局改，避免一次性视觉大重写。
4. 最后改循环范围弹窗为底部选项 sheet，并单独做循环待办 / 循环日程取消、删除、修改的手工验证。

## 10. 明确不建议做的事

- 不建议把 Widget 全部改成 Compose Glance，当前项目已有 RemoteViews 路径，短期迁移成本和兼容风险都高。
- 不建议为了“好看”删除现有点击 PendingIntent 路由，Widget 最重要的是入口可用。
- 不建议在循环范围弹窗 UI 改版时顺手修改循环数据逻辑；这类逻辑历史上很容易产生未来实例残留或误删。
- 不建议继续使用 `AlertDialog.confirmButton` 塞多个主操作按钮，这是当前弹窗观感差的直接结构原因之一。
