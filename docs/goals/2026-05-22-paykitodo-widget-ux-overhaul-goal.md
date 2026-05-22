# PaykiTodo — Widget 体验大修 + 日历/待办列表性能优化

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

---

# 1. Widget 背景固定 — 不跟随应用壁纸

## 问题
当前今日看板 Widget 的背景图 (`widget_dashboard_bg_image.xml`) 引用了应用内深色/浅色模式的壁纸 (`dashboard_bg_light` / night 变体)，导致 Widget 背景色随应用主题切换而变化。用户希望 Widget 背景是固定的、独立于应用主题的。

## 目标
Widget 背景使用固定的、不随应用主题变化的视觉方案。参考主流 Widget（iOS 天气、Google Keep、滴答清单）的做法：使用半透明磨砂白/深灰底色 + 微妙渐变，不依赖应用内壁纸。

## 实现

### 文件
- `app/src/main/res/drawable/widget_board_background.xml`
- `app/src/main/res/drawable-night/widget_board_background.xml`
- `app/src/main/res/drawable/widget_board_scrim.xml`
- `app/src/main/res/drawable-night/widget_board_scrim.xml`
- `app/src/main/res/layout/widget_todo.xml`

### 方案

1. **移除 `widget_dashboard_bg_image` 引用**：在 `widget_todo.xml` 中删除 `widget_background_image` 这个 ImageView，不再引用应用壁纸
2. **固定背景渐变**：
   - 浅色模式：柔和的奶白渐变 `#F8F6F2` → `#F0EDE8`，圆角 28dp，无边框或极淡边框
   - 深色模式：深灰渐变 `#1C1C1E` → `#2C2C2E`，圆角 28dp
3. **Scrim 调整**：保留轻微的顶部到底部渐变 scrim 增加层次感，但降低不透明度（从当前值降到 5-8%）
4. **倒数日 Widget 同步处理**：`widget_countdown.xml` 做相同改动

### 视觉参考
- Google Keep Widget：纯白/纯深灰底，无壁纸
- iOS 提醒事项 Widget：半透明磨砂底
- 滴答清单 Widget：纯色底 + 微妙阴影

### 验证
- [ ] Widget 背景不再随应用内主题/壁纸切换而变化
- [ ] 浅色模式下 Widget 底色为固定的暖白色
- [ ] 深色模式下 Widget 底色为固定的深灰色
- [ ] 文字在两种模式下都清晰可读
- [ ] 倒数日 Widget 同步更新

---

# 2. 无日程时 Widget 下半部分显示 "loading" 问题修复

## 问题
当今天和明天都没有日程时，Widget 的日程区域显示系统默认的 loading 指示器，点击无响应。根本原因：`getLoadingView()` 返回 `null`，Android 在 RemoteViewsFactory 数据加载期间显示默认 loading spinner；同时 `scheduleRow` 在无事件时仍然生成了 SCHEDULE 类型的行，但该行的 `today_message` 和 `tomorrow_message` 可能因为 visibility 逻辑问题导致内容不可见。

## 目标
无日程时，日程区域应显示友好的空状态文案（如"今天暂无日程"），而不是 loading 或空白。

## 实现

### 文件
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`

### 方案

1. **提供自定义 `getLoadingView()`**：返回一个简单的 RemoteViews，显示"加载中…"文字，而不是系统默认的 loading spinner。这样即使数据加载慢，用户也能看到有意义的内容。

```kotlin
override fun getLoadingView(): RemoteViews {
    return RemoteViews(context.packageName, R.layout.widget_todo_empty_card).apply {
        setTextViewText(R.id.widget_empty_card_title, "加载中…")
        setTextColor(R.id.widget_empty_card_title, mutedText)
    }
}
```

2. **检查 `scheduleViews` 中的 visibility 逻辑**：确保当 `row.events` 为空时，`widget_schedule_today_message` 的 visibility 为 VISIBLE 且文字内容不为空。当前逻辑：
   - `row.events.isEmpty()` → `today_message` VISIBLE — 正确
   - 但 `row.title`（即 todayMessage）在无事件时应为 "今天暂无日程" — 检查 `scheduleRow()` 中 `todayMessage` 的赋值逻辑

3. **确保 `widget_schedule_today_message` 和 `widget_schedule_tomorrow_message` 在无事件时都正确显示文案**，不会因为 background drawable 或 padding 问题导致看起来像空白。

### 验证
- [ ] 今天无日程 → 日程区域显示"今天暂无日程"
- [ ] 明天无日程 → 显示"明天暂无日程 · 去规划台安排一下？"
- [ ] 数据加载期间显示"加载中…"而非系统 loading spinner
- [ ] 点击日程区域能正常跳转到日历页面

---

# 3. Widget 待办卡片视觉统一 — 与应用内待办卡片一致

## 问题
当前 Widget 中待办卡片的紫色色条（分组颜色）和内容区域视觉上非常分离——色条是独立的窄条，内容区域是另一个块，看起来像两个不相关的元素拼在一起。而应用内的 `TodoCardShell` 是一个整体的 `ElevatedCard`，左侧色条和内容区域是一体的，视觉上是一个完整的卡片。

## 目标
Widget 待办卡片的视觉呈现应与应用内 `TodoCardShell` 保持一致：一个完整的圆角卡片，左侧有分组色条，内容紧凑地排列在右侧。不是"色条 + 分离的内容块"，而是"一体化卡片，左边缘带颜色"。

## 实现

### 文件
- `app/src/main/res/layout/widget_todo_task_card.xml`
- `app/src/main/res/drawable/widget_todo_soft_card_background.xml`（可能需要调整）
- `app/src/main/res/drawable/widget_vertical_pill.xml`（可能需要调整）

### 方案

1. **色条与卡片一体化**：
   - 当前色条是 8dp 宽的独立 ImageView，使用 `widget_vertical_pill` drawable
   - 改为：色条宽度缩小到 5-6dp，去掉独立的圆角（pill 形状），让它作为卡片左边缘的一部分
   - 卡片背景 `widget_todo_soft_card_background` 应该是一个完整的圆角矩形，色条在其内部左侧

2. **参考应用内实现**：
   - 应用内 `TodoCardShell`：`ElevatedCard(shape = RoundedCornerShape(24.dp))` 包裹 `Row`，Row 内左侧是 `Box(width=8.dp, fillMaxHeight, background=categoryColor)`，右侧是内容 Column
   - Widget 应模仿这个结构：整个卡片是一个圆角背景，左侧色条是卡片的一部分（左侧圆角被色条覆盖）

3. **具体改动**：
   - `widget_todo_soft_card_background.xml`：确保是完整的圆角矩形（如 16dp 圆角），半透明白底
   - 色条 ImageView：改为 `android:layout_width="5dp"`，去掉独立的 pill 圆角，改用简单的矩形 + 左侧圆角（与卡片左边缘对齐）
   - 或者：用 `clipToOutline` + 卡片圆角来自动裁剪色条的左侧边缘

### 验证
- [ ] Widget 待办卡片看起来是一个整体，色条是卡片的一部分
- [ ] 视觉上与应用内待办卡片风格一致
- [ ] 不同分组颜色都能正确显示
- [ ] 卡片圆角自然，色条不突兀

---

# 4. 移除 Widget 待办的勾选圆圈

## 问题
Widget 中每个待办卡片左侧有一个空心圆圈（`widget_task_check`），模拟 checkbox。但在 Widget 中完成待办不稳定（RemoteViews 的交互限制），用户更倾向于点进应用内完成。这个圆圈占空间且功能鸡肋。

## 目标
移除 Widget 待办卡片中的勾选圆圈，让色条直接紧邻内容区域，更紧凑。

## 实现

### 文件
- `app/src/main/res/layout/widget_todo_task_card.xml`

### 方案

删除 `widget_task_check` TextView（22dp x 22dp 的圆圈），同时调整内容区域的 `layout_marginStart`：
- 当前：色条(8dp) → 圆圈(22dp, marginStart=14dp) → 内容(marginStart=12dp)
- 改为：色条(5-6dp) → 内容(marginStart=10dp)

### 验证
- [ ] Widget 待办卡片无勾选圆圈
- [ ] 内容区域左侧紧邻色条，间距合理
- [ ] 卡片整体更紧凑

---

# 5. "!" 语义不明修复

## 问题
当前待办卡片中，逾期任务用一个红色的 "!" 标记（`widget_task_badge`），但没有任何上下文说明这个感叹号代表什么。用户反馈语义不明。

## 目标
将 "!" 替换为更明确的文字标签，让用户一眼看懂。

## 实现

### 文件
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`

### 方案

将 `trailing = if (item.missed) "!" else ""` 改为更明确的标签：

```kotlin
trailing = if (item.missed) "已逾期" else ""
```

同时调整 badge 的样式：
- 字号从 15sp 降到 11sp
- 加一个小的红色背景 pill（类似 `widget_task_meta_background` 但红色）
- 或者直接用红色文字 + 无背景，但文字要比 "!" 更有信息量

### 验证
- [ ] 逾期任务显示"已逾期"而非"!"
- [ ] 标签颜色醒目（红色），但不喧宾夺主
- [ ] 非逾期任务无标签

---

# 6. Widget padding 过大修复

## 问题
Widget 整体 padding 太大，浪费了宝贵的 Widget 空间，导致能显示的内容更少。

## 目标
收紧 Widget 的内外 padding，让内容区域更大，能显示更多待办和日程。

## 实现

### 文件
- `app/src/main/res/layout/widget_todo.xml`
- `app/src/main/res/layout/widget_todo_task_card.xml`
- `app/src/main/res/layout/widget_todo_schedule_card.xml`
- `app/src/main/res/layout/widget_todo_section.xml`（如果存在）
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`（dividerHeight）

### 方案

1. **Widget 外层 padding**：
   - 当前：`paddingStart=10dp, paddingTop=9dp, paddingEnd=10dp, paddingBottom=10dp`
   - 改为：`paddingStart=6dp, paddingTop=6dp, paddingEnd=6dp, paddingBottom=6dp`

2. **ListView dividerHeight**：
   - 当前：`5dp`
   - 改为：`3dp`

3. **待办卡片内部 padding**：
   - 当前内容区域：`paddingTop=13dp, paddingBottom=13dp`
   - 改为：`paddingTop=10dp, paddingBottom=10dp`
   - 卡片 `minHeight` 从 86dp 降到 68dp

4. **日程卡片 padding**：
   - 当前：`paddingStart=12dp, paddingTop=8dp, paddingEnd=12dp, paddingBottom=8dp`
   - 改为：`paddingStart=8dp, paddingTop=6dp, paddingEnd=8dp, paddingBottom=6dp`

### 验证
- [ ] Widget 整体更紧凑，能显示更多内容
- [ ] 文字不会因为 padding 过小而贴边
- [ ] 各卡片之间间距合理，不拥挤也不浪费

---

# 7. Widget 背景设计 — 参考主流 Widget 风格

## 问题
当前 Widget 背景方案（渐变 + 壁纸 + scrim 三层叠加）过于复杂，且依赖应用壁纸。需要一个独立的、符合主流 Widget 设计语言的背景方案。

## 目标
参考 iOS/Android 主流 Widget 的背景设计，提供一个干净、现代的 Widget 背景。

## 实现

### 参考设计
- **Google Calendar Widget**：纯白底 + 极淡灰色边框 + 圆角
- **iOS 天气 Widget**：渐变色底（蓝天/夜空），但颜色固定不随主题变
- **滴答清单 Widget**：白底 + 微妙阴影
- **Notion Widget**：白底 + 1dp 淡灰边框

### 方案

采用"干净白底 + 微妙质感"路线：

**浅色模式** (`widget_board_background.xml`)：
```xml
<shape>
    <solid android:color="#F7F5F2" />
    <corners android:radius="24dp" />
    <stroke android:width="0.5dp" android:color="#1A000000" />
</shape>
```

**深色模式** (`drawable-night/widget_board_background.xml`)：
```xml
<shape>
    <solid android:color="#1E1E20" />
    <corners android:radius="24dp" />
    <stroke android:width="0.5dp" android:color="#1AFFFFFF" />
</shape>
```

**Scrim**：简化为极淡的顶部渐变（增加标题区可读性），或直接移除。

### 验证
- [ ] Widget 背景干净现代，不花哨
- [ ] 浅色/深色模式都美观
- [ ] 与系统桌面壁纸搭配和谐（不抢眼）
- [ ] 文字对比度足够

---

# 8. 日历翻动卡顿优化

## 问题
日历面板（`CalendarPanel.kt`）在翻动（切换日期/周）时非常卡顿。当前使用 `rememberScrollableState` + 手动处理手势，而非 Compose 的 `HorizontalPager`。同时待办任务多时列表滚动也卡。

## 目标
日历翻动和待办列表滚动流畅，无明显掉帧。

## 实现

### 文件
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`（待办列表部分）

### 方案

#### 日历翻动优化

1. **替换手动 scrollable 为 HorizontalPager**：
   - 当前：`rememberScrollableState { delta -> ... }` 手动处理水平滑动
   - 改为：使用 `HorizontalPager`（Compose Foundation）实现日期切换，自带流畅的 fling 动画和惰性加载
   - 每页渲染一天/三天的日程视图

2. **减少重组范围**：
   - 日历格子中每个时间槽应该是独立的 Composable，避免整个日历在任何状态变化时全部重组
   - 使用 `key()` 和 `remember` 缓存不变的日程块

3. **日程块渲染优化**：
   - 对不在可视区域的日程块使用 `LazyColumn` 或条件渲染
   - 避免一次性渲染 24 小时 × 所有日程

#### 待办列表滚动优化

1. **确保 LazyColumn 使用 `key`**：
   - `DashboardChrome.kt` 中的 LazyColumn 的 `items()` 调用必须提供稳定的 key（如 `item.id`）
   - 避免列表项不必要的重组

2. **待办卡片轻量化**：
   - 检查 `ActiveTodoCard` 是否有昂贵的计算在 Composition 中执行
   - 将格式化、颜色计算等移到 ViewModel 层，卡片只负责渲染

3. **循环待办折叠**（如果尚未实现）：
   - 同一 `seriesId` 的循环待办在"未来"区域折叠为一行
   - 大幅减少列表项数量

### 验证
- [ ] 日历左右翻动流畅，无明显卡顿
- [ ] 待办列表（50+ 条）滚动流畅
- [ ] 切换日期时日程加载无白屏闪烁
- [ ] 三日视图翻动同样流畅

---

# 通用要求

1. 版本号升级
2. 数据库版本保持不变（本轮无 schema 变更）
3. 每轮 `./gradlew.bat :app:compileDebugKotlin` + `git diff --check` 通过
4. 最后一轮 `./gradlew.bat :app:assembleDebug` 验证
5. commit 严格按 AGENTS.md 的 bullet 列表格式
6. 不要 push 到 GitHub
7. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`

---

# 实现顺序建议

1. **#4（移除圆圈）** — 最简单，1 分钟，删一个 View
2. **#5（! → 已逾期）** — 改一行代码
3. **#6（padding 收紧）** — 改几个数值
4. **#1 + #7（背景固定 + 主流风格）** — 合并做，改 drawable XML
5. **#3（卡片视觉统一）** — 调整布局结构
6. **#2（loading 修复）** — 加 getLoadingView + 检查 visibility 逻辑
7. **#8（性能优化）** — 最复杂，涉及日历架构调整

---

# 不做的事

- ❌ 不做 Widget 上直接勾选完成待办（已明确移除）
- ❌ 不动倒数日 Widget 的内容逻辑（只同步背景改动）
- ❌ 不动应用内的待办卡片样式（只改 Widget）
- ❌ 不做 Widget 配置页面（背景选择等留给后续）
- ❌ 不做日历的月视图/年视图优化（只优化日/三日视图的翻动）
