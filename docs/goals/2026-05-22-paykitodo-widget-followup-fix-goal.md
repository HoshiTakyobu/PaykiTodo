# PaykiTodo — Widget 改造收尾修复 + 日历 Pager 回归排查

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

本轮是上一轮 `2026-05-22-paykitodo-widget-ux-overhaul-goal.md`（commit `1d39fa4`）的收尾修复。上一轮 8 项需求基本完成，但有 5 处坑需要补：3 个视觉割裂 + 1 个深色模式对比度 + 1 个日历翻页功能性回归。

---

# 1. 待办卡片色条与卡片真正一体化

## 问题
上一轮把色条 `widget_vertical_pill.xml` 改成了 `topLeft/bottomLeft = 20dp` 的圆角矩形（试图与卡片 20dp 圆角对齐），但根 LinearLayout 没开 `clipToOutline`，色条作为独立 ImageView 叠在卡片背景的左边缘——视觉上仍然像两块拼接：色条左侧是圆的，右侧（与卡片白底交界处）是直角，仍有一条裂缝。

## 目标
色条是卡片背景的一部分。视觉上看就是"一个圆角白色卡片，左侧 6dp 染上分组色"，不是"一个色条 + 一个白卡片"。

## 实现

### 文件
- `app/src/main/res/drawable/widget_vertical_pill.xml`
- `app/src/main/res/layout/widget_todo_task_card.xml`

### 方案

1. **`widget_vertical_pill.xml` 改回纯矩形**：去掉所有圆角，让它就是一个矩形色块（`scaleType=fitXY` 拉伸时不会变形）：
   ```xml
   <shape>
       <solid android:color="@android:color/white" />
   </shape>
   ```
   （不写 corners 节点）

2. **根 LinearLayout 开启 `clipToOutline`**：
   `widget_todo_task_card.xml` 的根 `LinearLayout`（`@+id/widget_item_root`）加上：
   ```xml
   android:clipToOutline="true"
   ```
   这样卡片的 20dp 圆角背景会自动裁掉色条左侧的两个尖角，色条和卡片真正成为一体。

3. **同步检查日程子事件行**：
   `widget_todo_schedule_card.xml` 的 `widget_schedule_today_event_1/2`、`widget_schedule_tomorrow_event_1/2` 也是同样的"色条 + 内容"结构，同样问题。这几个 LinearLayout 也加 `clipToOutline="true"`，背景 `widget_schedule_event_background` 已经是 16dp 圆角，会自动裁色条。

### 验证
- [ ] 待办卡片色条与卡片左侧无视觉裂缝，看起来是一个整体
- [ ] 日程行同样无裂缝
- [ ] 不同分组颜色都正确显示
- [ ] 圆角自然，色条顶/底无鼓包

---

# 2. 日程卡片背景同步换风格

## 问题
上一轮把"今日看板背景"和"待办卡片背景"换成了暖白 + 极淡黑边的现代风格（`widget_board_background.xml`、`widget_todo_soft_card_background.xml`），但**日程卡片用的是另一个 drawable** `widget_todo_item_background.xml`，**没被改**——白天版还是 `#FFFFFBF2` 米黄底 + `#D8BE92` 棕色描边的旧风格。

结果：今日看板里待办卡片是新风格，日程卡片是旧风格，两种背景色明显并存，视觉割裂。

## 目标
日程卡片背景与待办卡片背景视觉一致。

## 实现

### 文件
- `app/src/main/res/drawable/widget_todo_item_background.xml`
- `app/src/main/res/drawable-night/widget_todo_item_background.xml`

### 方案

直接把这两个文件改成与 `widget_todo_soft_card_background.xml` 一致的样式：

**白天**：
```xml
<shape>
    <solid android:color="#FAFFFFFF" />
    <corners android:radius="20dp" />
    <stroke
        android:width="1dp"
        android:color="#14000000" />
</shape>
```

**深色** (`drawable-night`)：
```xml
<shape>
    <solid android:color="#E825262B" />
    <corners android:radius="20dp" />
    <stroke
        android:width="1dp"
        android:color="#18FFFFFF" />
</shape>
```

或者更省事：直接把 `widget_todo_schedule_card.xml:6` 的 `android:background="@drawable/widget_todo_item_background"` 改为引用 `@drawable/widget_todo_soft_card_background`，然后判断 `widget_todo_item_background` 是否还有其他地方引用——如果没有，可以删除。

### 验证
- [ ] 今日看板里待办卡片和日程卡片背景颜色、圆角、描边视觉一致
- [ ] 浅色 / 深色模式都一致
- [ ] 无其他位置因为复用 `widget_todo_item_background` 受影响

---

# 3. "已逾期" badge 深色模式对比度修复

## 问题
上一轮新建的 `widget_task_overdue_badge_background.xml` 只有浅色版本（`#18D14343` 红色 9% 不透明），没建 `drawable-night` 变体。在深色模式下：
- 卡片底色：`#E825262B`（深灰半透明）
- badge 底色：`#18D14343`（极淡红，叠在深灰上几乎看不见）
- 文字：`widget_danger = #E89A9A`（粉红字）

→ 粉红字几乎漂在深灰底上，视觉对比弱，"已逾期"标签存在感不足。

## 目标
深色模式下"已逾期" badge 同样醒目。

## 实现

### 文件
- `app/src/main/res/drawable-night/widget_task_overdue_badge_background.xml`（新建）

### 方案

新建 night 变体，提高底色不透明度并使用更深的红：
```xml
<shape>
    <solid android:color="#40D14343" />
    <corners android:radius="999dp" />
</shape>
```

底色 25% 不透明的红，叠在深灰卡片上能形成清晰的红色 pill，配合 `#E89A9A` 的浅粉字依然有足够对比。

### 验证
- [ ] 深色模式下逾期任务的"已逾期"标签清晰可见
- [ ] 浅色模式不受影响
- [ ] 标签不会过分抢眼到喧宾夺主

---

# 4. 倒数日 Widget padding 回退

## 问题
上一轮把倒数日 Widget 的外层 padding 从 `4dp` **加大**到 `6dp`（`widget_countdown.xml:20-23`），目的是与今日看板保持一致。但用户在原需求里说的是"padding 太大要收紧"。倒数日 Widget 原本就是 4dp，现在反而被加大，违背了"收紧"诉求。

## 目标
倒数日 Widget 保持紧凑，外层 padding 回到 4dp。

## 实现

### 文件
- `app/src/main/res/layout/widget_countdown.xml`
- `app/src/main/res/layout/widget_countdown_preview.xml`（如果同步改了，检查一下）

### 方案
`paddingStart/Top/End/Bottom` 从 6dp 改回 4dp。
`dividerHeight` 保持 3dp（这个收紧合理）。

### 验证
- [ ] 倒数日 Widget 内容区比上一轮更紧凑
- [ ] 倒数日卡片之间间距合理，不挤
- [ ] preview 同步更新

---

# 5. 日历 HorizontalPager 改造的功能性回归排查

## 问题
上一轮用 `HorizontalPager` 重写了日视图 / 三日视图的横向翻页，性能确实改善，但有几个**功能性回归风险**没有验证：

### 风险 A：三日视图的翻页步长改变
- **原行为**：`scrollable + horizontalOffsetPx`，三日视图是横向连续滚动，可以连续拖动展示任意 3 天窗口
- **新行为**：HorizontalPager 每页 = "以 pageIndex 为中心的 3 天"，**翻一页跳 1 天**

这是行为变化，不是 bug。需要确认这是预期行为，否则三日视图的"快速浏览一周"用户场景可能变慢。

### 风险 B：水平拖事件块跨日 ↔ Pager 翻页手势冲突
原代码里 `dragModifier` 用于让用户在日视图里**长按事件块横向拖**改日期。HorizontalPager 默认会消费所有水平手势，可能导致：
- 长按事件块拖到第二天 → 实际触发了 Pager 翻页
- 或事件拖拽与 Pager 翻页同时响应，体验混乱

需要验证 `dragModifier` 是否还在 Pager 子页里生效，以及手势优先级是否正确。

### 风险 C：垂直 ScrollState 共享
所有 Pager 子页共用同一个 `verticalScroll` ScrollState（`CalendarPanel.kt:224`），如果在 Compose 里多个可见 page 同时挂同一个 ScrollState，可能触发 "multiple scrollables share same state" 警告，或者翻页时小时位置抖动。

## 目标
验证 HorizontalPager 改造没有破坏：
- 三日视图的浏览体验
- 事件块横向拖拽改期
- 翻页时小时位置稳定

如果发现回归，修复。

## 实现

### 验证步骤（动手实测，不要只看代码）

1. **三日视图翻页步长确认**：
   - 打开日历切到三日视图
   - 左右滑动，确认是"翻 1 天"还是"翻 3 天"
   - 与 GPT 同事/产品确认这是预期行为；如果原来是"翻 3 天"，需要调整 Pager 的页索引语义（每页 = 3 天，不是滑动 1 天）

2. **事件块横向拖拽**：
   - 在日视图长按一个事件块
   - 尝试横向拖到第二天
   - 观察：拖拽是触发了改期，还是触发了 Pager 翻页

   如果冲突：
   - 方案 A：在 Pager 上加 `userScrollEnabled = false`，自己用 `detectHorizontalDragGestures` 做翻页判定，并优先让事件拖拽消费手势
   - 方案 B：检测到事件拖拽 active 时，给 PagerState 调用 `pagerState.requestScrollStop()` 或类似方法暂停翻页

3. **垂直滚动稳定性**：
   - 滚到下午 3 点附近
   - 翻几次页
   - 确认每页打开时小时位置都在下午 3 点（不是顶部，也不是抖动）
   - 检查 logcat 是否有 Compose 的 ScrollState 共享警告

4. **边界翻页**：
   - 翻到 dateWindow 的第 0 天 → 确认不会卡死或崩溃
   - 翻到最后一天 → 同上

### 修复（如果发现回归）

按上面的方案 A/B 修。如果三日视图翻页步长是回归的，把 Pager 的语义改成"每页 = 3 天"：
- `rememberPagerState(...) { (dateWindow.size + 2) / 3 }`
- `page` 转换成 `pageIndex = page * 3` 作为这一页的起始日

### 验证
- [ ] 三日视图翻页步长与产品预期一致
- [ ] 长按事件块横向拖能正常改日期，不被 Pager 拦截
- [ ] 翻页时小时位置稳定
- [ ] 边界日期翻页不崩溃
- [ ] logcat 无 ScrollState 警告

---

# 6. （可选）`getLoadingView` 文案区分

## 问题
上一轮 `getLoadingView()` 复用了 `widget_todo_empty_card` 布局，文字"加载中…"和"今日看板暂无内容"用同一种视觉，用户难以区分。不过这是细节，不影响功能。

## 实现（如果时间够）

在 loadingView 中给文字加一个 emoji 或前缀区分：
```kotlin
setTextViewText(R.id.widget_empty_card_title, "⏳ 加载中…")
```

或者新建一个独立的 `widget_todo_loading_card.xml`，文字居中 + 一个简单的省略号动画（RemoteViews 限制下能做的极简方案）。

### 验证
- [ ] 加载中和空状态文案/视觉可区分

---

# 通用要求

1. 版本号升级到下一个补丁版（1.13.9 / versionCode 257）
2. 数据库版本保持 25，无 schema 变更
3. 每轮 `./gradlew.bat :app:compileDebugKotlin` + `git diff --check` 通过
4. 最后一轮 `./gradlew.bat :app:assembleDebug` 验证
5. commit 严格按 AGENTS.md 的 bullet 列表格式
6. 不要 push 到 GitHub
7. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`

---

# 实现顺序建议

1. **#4（倒数日 padding 回退）** — 1 分钟
2. **#3（badge night 变体）** — 2 分钟
3. **#2（日程卡片背景统一）** — 5 分钟
4. **#1（色条 clipToOutline 一体化）** — 10 分钟
5. **#5（日历 Pager 回归排查）** — 30-60 分钟，最复杂，需要实机操作
6. **#6（loadingView 文案）** — 可选

---

# 不做的事

- ❌ 不再调整 Widget 整体配色（上一轮已定）
- ❌ 不重写 HorizontalPager 改造（除非 #5 验证发现严重回归，此时只做最小修复）
- ❌ 不动应用内待办卡片（只 Widget 修）
- ❌ 不引入新 schema、新设置项
