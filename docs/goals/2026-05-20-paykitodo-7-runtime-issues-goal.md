# PaykiTodo — 7 项运行问题修正

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

---

## 优先级说明

**#7（崩溃 + 无法启动）是 P0**，必须第一个修。其余按编号顺序。

---

# P0：#7 循环待办创建闪退 + 无法启动

## 现象
用户创建了 2 个循环 90 天的 DDL 待办（每天提醒吃药），创建第 3 个时点击确认 → 闪退。之后 app 怎么都打不开。

## 可能原因分析

1. **AlarmManager 配额耗尽**：Android 12+ 对 exact alarm 有数量限制。3 个循环 × 90 天 = 270 个 alarm，加上已有的提醒，可能超出系统配额。`AlarmScheduler.kt` 在 `setAlarmClock` / `setExactAndAllowWhileIdle` 时如果系统拒绝，可能抛异常未被 catch。

2. **启动时恢复提醒导致二次崩溃**：`TodoApplication.onCreate` 里 `applicationScope.launch` 会恢复所有提醒。如果数据库里已经有 270+ 条待调度的 alarm，启动时批量调度再次触发同样的异常 → 启动崩溃循环。

3. **OOM / ANR**：一次性创建 90 个 TodoItem 实例（循环展开）+ 90 个 alarm 调度，可能在主线程或启动阶段耗尽内存/时间。

## 修复方案

### 7a. AlarmScheduler 加 try-catch 防御

`app/src/main/java/com/example/todoalarm/alarm/AlarmScheduler.kt`

所有 `alarmManager.setAlarmClock` / `setExactAndAllowWhileIdle` / `setExact` 调用包裹 try-catch：
```kotlin
try {
    alarmManager.setAlarmClock(...)
} catch (e: SecurityException) {
    CrashLogger.recordNonFatal(e)
    // 降级为 inexact alarm 或跳过
} catch (e: Exception) {
    CrashLogger.recordNonFatal(e)
}
```

### 7b. 循环待办展开限制

`app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`

循环待办创建时，不要一次性展开全部 90 天的实例。改为：
- 只展开未来 7-14 天的实例
- 每天启动时 / 每天定时检查，补充展开接下来 7 天的实例
- 或者：循环待办只存模板 + 当天/明天的实例，提醒触发后再生成下一个

### 7c. 启动恢复加保护

`app/src/main/java/com/example/todoalarm/TodoApplication.kt`

启动时的提醒恢复逻辑包裹顶层 try-catch：
```kotlin
applicationScope.launch {
    try {
        // 现有恢复逻辑
    } catch (e: Exception) {
        CrashLogger.recordNonFatal(e)
        // 不要让启动崩溃
    }
}
```

### 7d. 用户数据恢复

如果用户当前数据库已经有问题导致无法启动：
- 在 `TodoApplication.onCreate` 最前面加一个 `SafeStartupGuard`：检测上次启动是否在 3 秒内崩溃（用 SharedPreferences 记录启动时间戳），如果连续崩溃 ≥ 2 次，跳过提醒恢复，只打开 app 让用户能进入设置/备份。
- 或者提供一个"安全模式"入口（长按 app 图标 → 安全模式快捷方式）。

### 验证
- [ ] 创建 3 个循环 90 天的 DDL 待办，不闪退
- [ ] 创建后 app 可正常重启
- [ ] 循环待办的提醒在对应日期正常触发
- [ ] 如果 alarm 配额不足，降级处理而不是崩溃

---

# #1 倒数日点击应跳转预览而非编辑

## 现象
点击倒数日内容（widget 或每日看板），先跳到编辑界面，退出编辑才看到预览。用户只想看详情。

## 文件
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`（看板倒数日行点击）
- `app/src/main/java/com/example/todoalarm/widget/CountdownWidgetService.kt`（widget 点击 intent）
- `app/src/main/java/com/example/todoalarm/ui/TodoCards.kt`（待办预览 sheet）

## 改动

当前倒数日行点击 → 打开 `TodoEditorDialog` 或 `CalendarEventEditorDialog`。

改为：点击 → 打开**预览 BottomSheet**（`ActiveTodoCard` 里的 `showDetails` 那个预览 sheet），不进编辑器。预览 sheet 里有"编辑"按钮，用户想编辑再点进去。

Widget 的 PendingIntent 也要改：从打开编辑器改为打开 app 并展示对应 item 的预览。

### 验证
- [ ] 每日看板点击倒数日条目 → 弹出预览 sheet（显示标题、DDL、备注等）
- [ ] 预览 sheet 有"编辑"按钮可进入编辑器
- [ ] Widget 点击倒数日条目 → 打开 app 并弹出预览 sheet
- [ ] 不会先闪一下编辑器再跳预览

---

# #2 规划台多级备注（非子任务）支持

## 现象
用户在规划台写：
```
期末复习
  参考群里的文件
  考前三天出去住
```
"参考群里的文件"和"考前三天出去住"不是任务，是备注/提醒自己的信息。但当前系统会把所有叶子节点都当作待办。

## 方案

### 特殊前缀标记为备注

用户在行首加 `// ` 或 `> `（类似代码注释或 markdown 引用），该节点标记为**备注**，不会被发布为待办/日程：

```
期末复习
  // 参考群里的文件
  // 考前三天出去住
  整理错题本          ← 这个才是子任务
```

### 数据模型

`planning_nodes` 新增字段：
```sql
ALTER TABLE planning_nodes ADD COLUMN isNote INTEGER NOT NULL DEFAULT 0;
```

数据库版本 `23 → 24`。

### 识别逻辑

在 `createNode` / 节点文本更新时：
- 文本以 `// ` 或 `> ` 开头 → `isNote = true`
- 否则 → `isNote = false`

`isNote = true` 的节点：
- 不创建 linkedTodoId（即使是叶子节点）
- 不参与"所有子项完成 → 父项完成"的计算
- 视觉上用斜体或更淡的颜色区分

### 预览模式下的切换

预览模式三点菜单加一项："标记为备注 / 取消备注"，手动切换 `isNote`。

### 验证
- [ ] 输入 `// 参考群里的文件` → 节点显示为备注样式（斜体/淡色）
- [ ] 备注节点不出现在待办页面
- [ ] 备注节点不影响父节点的完成状态计算
- [ ] 预览模式可手动切换备注/任务

---

# #3 Widget 留白过多

## 现象
- 今日看板 widget 的日程之间、日程本身留白太多太宽
- 倒数日 widget 滑到顶部/底部时和边缘留白太多
- 1×4 宽度本来能显示两条，结果只显示一条半

## 文件
- `app/src/main/res/layout/widget_todo_schedule_card.xml`
- `app/src/main/res/layout/widget_countdown_item.xml`
- `app/src/main/res/layout/widget_countdown.xml`
- `app/src/main/res/layout/widget_todo.xml`

## 改动

### 日程卡片减 padding

`widget_todo_schedule_card.xml` 当前外层 padding 是 16dp，内层 12-14dp。改为：
- 外层 padding：`8dp`（上下）`12dp`（左右）
- 内层 padding：`8dp`
- 日程行之间间距从当前值减半

### 倒数日 widget 减边距

`widget_countdown.xml` / `widget_countdown_item.xml`：
- ListView 的 padding 减到 `4dp`
- 每个 item 的上下 padding 减到 `6dp`
- 确保 1×4 宽度下至少能完整显示 2 条

### 验证
- [ ] 今日看板 widget 日程区域更紧凑，同样高度能显示更多条
- [ ] 倒数日 widget 1×4 宽度下能完整显示 ≥2 条
- [ ] 文字不被裁切，仍然可读
- [ ] 不同 DPI 设备下不会过于拥挤

---

# #4 循环待办「仅提醒，不显示」选项

## 现象
用户想让"每天吃药"、"每天记账"这类循环待办只触发提醒，不在看板和日历中显示。核心诉求：**只用提醒功能，不占看板空间**。

## 方案

### 待办编辑器新增开关

`TodoEditorDialog.kt` 的"更多选项"区域加一个 Switch：
- 标签：「仅提醒，不在看板/日历显示」
- 字段名：`TodoItem.hiddenFromBoard: Boolean = false`

### 数据模型

```sql
ALTER TABLE todo_items ADD COLUMN hiddenFromBoard INTEGER NOT NULL DEFAULT 0;
```

数据库版本配合 #2 一起升（`23 → 24`）。

### 影响范围

`hiddenFromBoard = true` 的待办：
- ✅ 正常调度提醒（AlarmScheduler 不跳过）
- ✅ 出现在待办页面（用户能管理它）
- ❌ 不出现在每日看板的"今日待办"区
- ❌ 不出现在今日看板 widget
- ❌ 不出现在桌面端 board
- ❌ 不出现在 AI 日报的"今日待办"统计

### 待办页面视觉

`hiddenFromBoard = true` 的待办在待办页面用一个小图标标记（如 `Icons.Rounded.NotificationsActive` + 淡色背景），表示"仅提醒"。

### 循环模板同步

`RecurringTaskTemplate` 也加 `hiddenFromBoard` 字段，循环展开的实例继承该设置。

### 验证
- [ ] 创建循环待办 → 开启"仅提醒" → 看板不显示该待办
- [ ] 提醒时间到 → 正常弹全屏/通知提醒
- [ ] 待办页面能看到该待办（带"仅提醒"标记）
- [ ] Widget 不显示该待办
- [ ] 关闭"仅提醒" → 恢复在看板显示

---

# #5 每日看板空白处点击跳转

## 现象
Widget 点击空白区域会跳转到对应模块（点日程区 → 日历，点待办区 → 待办）。但 app 内的每日看板点击空白处没反应。

## 改动

`app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`

每个看板卡片区域（今日待办、今日日程、明日日程、倒数日）的外层容器加 `clickable`：
- 点击"今日待办"区域空白 → 跳转到待办页面
- 点击"今日日程"/"明日日程"区域空白 → 跳转到日历页面
- 点击"倒数日"区域空白 → 跳转到待办页面（倒数日本质是待办/日程）

注意：不要和卡片内部的点击冲突。用 `Modifier.clickable` 在外层 Surface/Column 上，内部 item 的点击事件会消费掉，不会穿透到外层。

### 验证
- [ ] 点击"今日待办"标题或空白区域 → 跳转待办页面
- [ ] 点击"今日日程"标题或空白区域 → 跳转日历
- [ ] 点击具体的待办条目 → 仍然打开该条目的预览（不跳转页面）
- [ ] 点击具体的日程条目 → 仍然打开该日程的预览

---

# #6 BottomSheet 编辑器键盘收起后缩小问题

## 现象
新建日程时打开上拉编辑 BottomSheet → 点输入框 → 键盘弹出 → 全屏 sheet 被压成半屏 → 收起键盘后 sheet 变得更小 → 容易误触外部导致编辑丢失。

## 文件
- `app/src/main/java/com/example/todoalarm/ui/EditorBottomSheet.kt`（如果有）
- `app/src/main/java/com/example/todoalarm/ui/TodoEditorDialog.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarEventEditorDialog.kt`

## 改动

### 方案 A：强制 skipPartiallyExpanded

```kotlin
val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true  // 不允许半屏状态
)
```

这样 sheet 只有两个状态：全屏展开 / 完全关闭。键盘弹出/收起不会让 sheet 卡在半屏。

### 方案 B：confirmValueChange 阻止意外关闭

```kotlin
val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true,
    confirmValueChange = { targetValue ->
        if (targetValue == SheetValue.Hidden && hasUnsavedChanges) {
            // 弹确认对话框
            showDiscardConfirm = true
            false  // 阻止关闭
        } else {
            true
        }
    }
)
```

### 方案 C：改用 Dialog 而非 BottomSheet

如果 BottomSheet 在键盘交互上始终有问题，考虑把编辑器改为全屏 Dialog（`Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))`）。全屏 Dialog 不受 sheet 拖拽影响。

**建议先试方案 A + B**，如果仍有问题再考虑方案 C。

### 验证
- [ ] 打开日程编辑 sheet → 点输入框 → 键盘弹出 → sheet 保持全屏
- [ ] 收起键盘 → sheet 仍然全屏
- [ ] 有未保存内容时误触外部 → 弹确认对话框而非直接关闭
- [ ] 正常保存/取消流程不受影响

---

# 通用要求

1. **#7 必须第一个修**，否则用户 app 打不开
2. 数据库版本 `23 → 24`（合并 #2 和 #4 的字段新增）
3. 备份/恢复 JSON 加 `isNote`、`hiddenFromBoard` 字段；旧备份按 false 兜底
4. 每轮 `./gradlew.bat :app:compileDebugKotlin` + `git diff --check` 通过
5. 最后一轮 `./gradlew.bat :app:assembleDebug` 验证
6. commit 严格按 AGENTS.md 的 bullet 列表格式
7. 不要 push 到 GitHub
8. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`、`docs/current/PROJECT_STATUS.md`、`docs/current/FEATURE_LEDGER.md`

---

# 实现顺序

1. **#7（崩溃修复）** — P0，用户现在打不开 app
2. **#6（BottomSheet 键盘问题）** — 高频痛点
3. **#1（倒数日点击跳预览）** — 简单改动
4. **#5（看板空白处点击跳转）** — 简单改动
5. **#3（Widget 留白）** — XML 调整
6. **#4（循环待办仅提醒）** — 新字段 + 逻辑
7. **#2（规划台备注节点）** — 新字段 + 解析逻辑

---

# 不做的事

- ❌ 不动规划台草稿/发布逻辑（上一轮已做）
- ❌ 不动桌面端 Web（本轮只修手机端，除非 #4 需要桌面端配合）
- ❌ 不做循环待办的"智能展开"重构（#7 只做防崩溃 + 限制，不重写整个循环系统）
