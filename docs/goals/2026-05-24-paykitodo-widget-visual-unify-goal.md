# PaykiTodo — Widget 视觉统一收尾 + 边界回归修复

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

---

# 背景

最近几轮（commit `1d39fa4` → `2dfae47` → `ef570fb`）做了大量 widget 视觉重做和日历/列表性能优化，但仍有 6 处可观察到的"逆天"问题——已经实测过实际代码，**全部确认存在**：

1. 倒数日 widget 还残留无效圆圈（用户原始需求漏改）
2. 今日看板内 3 种卡片背景共存（视觉割裂）
3. 事件卡片 padding 极其臃肿
4. 倒数日条目 padding 不对称
5. 「已逾期」badge 深色模式不可读
6. 三日视图 Pager 边界翻页只显示 2 天

下面每条都给出**已验证的 file_path:line + 当前代码原貌**，不要再猜。

---

# 1. 倒数日 widget 移除无效勾选圆圈（漏改回归）

## 现状（已验证）
原始需求第 5 条要求"取消 widget 里面待办的圆圈圈"，今日看板待办卡已经移除（commit `1d39fa4`），但**倒数日 widget 整组都没动**。

`app/src/main/res/layout/widget_countdown_item.xml:17-25`：
```xml
<TextView
    android:id="@+id/widget_countdown_check"
    android:layout_width="22dp"
    android:layout_height="22dp"
    android:layout_marginEnd="6dp"
    android:background="@drawable/widget_checkbox_ring"
    android:gravity="center"
    android:includeFontPadding="false"
    android:text="" />
```

`app/src/main/res/layout/widget_countdown_preview.xml:42` 和 `:134` 也仍有 `widget_checkbox_ring` 引用。

`app/src/main/java/com/example/todoalarm/widget/CountdownWidgetService.kt:77`：
```kotlin
setViewVisibility(R.id.widget_countdown_check, if (item.isTodo) View.VISIBLE else View.GONE)
```
代码层面会针对待办型倒数日条目把它显示出来——这就是用户在桌面看到的"逆天"圆圈。

## 改动

### 文件 1：`widget_countdown_item.xml`
删除 `widget_countdown_check` 整个 TextView（第 17-25 行）。

### 文件 2：`widget_countdown_preview.xml`
删除两处 `widget_checkbox_ring` 占位 TextView（行 39-44 附近 和 行 131-137 附近）。

### 文件 3：`CountdownWidgetService.kt`
删除第 77 行 `setViewVisibility(R.id.widget_countdown_check, ...)`，避免编译报错。

### 文件 4：`widget_checkbox_ring.xml`
搜全项目确认无引用后，删除 drawable 文件本身：
```bash
grep -rn "widget_checkbox_ring" app/src/
```
应只剩 widget 相关层，删除前面三处后此 drawable 应无引用——直接删 `app/src/main/res/drawable/widget_checkbox_ring.xml`。

## 验证
- [ ] 倒数日 widget 上的圆圈消失
- [ ] 倒数日 preview 也不再显示圆圈
- [ ] 编译通过，无未引用资源警告

---

# 2. 今日看板内 3 种卡片背景统一（视觉割裂大头）

## 现状（已验证）
今日看板 widget 渲染 7 种行类型（GREETING / SECTION / EMPTY / TODO / SCHEDULE / EVENT / ANNOUNCEMENT），其中卡片背景目前**有 3 套不同的 drawable**：

| Layout 文件 | 当前 background | 风格 |
|---|---|---|
| `widget_todo_task_card.xml:7` | `widget_todo_soft_card_background` | **新版**：`#FAFFFFFF` + `#14000000` 描边 + 20dp 圆角 |
| `widget_todo_empty_card.xml:7` | `widget_todo_soft_card_background` | **新版** |
| `widget_todo_schedule_card.xml:6` | `widget_todo_item_background` | **旧版**：`#FFFFFBF2` 米黄 + `#D8BE92` 棕色描边 + 24dp 圆角 |
| `widget_todo_event_card.xml:7` | `widget_todo_item_background` | **旧版** |
| `widget_todo_greeting_card.xml:6` | `widget_greeting_background` | **更旧**：`#FFFFFBF2` 米黄渐变 + `#D8BE92` 棕色描边 |

实际效果：用户拉开 widget 看到 — 顶部"早上好"问候卡是米黄+棕边的旧风格，中间待办卡是干净白底新风格，下面日程卡又是米黄+棕边——**像三个 app 拼起来的**。

`app/src/main/res/drawable/widget_todo_item_background.xml`：
```xml
<solid android:color="#FFFFFBF2" />
<corners android:radius="24dp" />
<stroke android:width="1dp" android:color="#D8BE92" />
```

`app/src/main/res/drawable/widget_greeting_background.xml`：
```xml
<gradient android:angle="0"
    android:startColor="#FFFFFBF2"
    android:centerColor="#FFFFF7EB"
    android:endColor="#FFFFFCF5" />
<corners android:radius="24dp" />
<stroke android:width="1dp" android:color="#D8BE92" />
```

## 目标
**所有今日看板内卡片用同一种 background**：`widget_todo_soft_card_background`（已经在用的新版）。问候卡可以保留极淡渐变作为差异，但描边和圆角必须与其他卡片一致。

## 改动

### 方案 A（推荐，最简洁）
直接把 `widget_todo_item_background.xml` 和 `widget_greeting_background.xml` 改成与 soft_card 一致的样式，引用关系都不动。

**`app/src/main/res/drawable/widget_todo_item_background.xml`** 改为：
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#FAFFFFFF" />
    <corners android:radius="20dp" />
    <stroke android:width="1dp" android:color="#14000000" />
</shape>
```

**`app/src/main/res/drawable-night/widget_todo_item_background.xml`** 改为：
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#E825262B" />
    <corners android:radius="20dp" />
    <stroke android:width="1dp" android:color="#18FFFFFF" />
</shape>
```

**`app/src/main/res/drawable/widget_greeting_background.xml`** 改为（保留极淡渐变作为问候卡微妙差异）：
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:angle="135"
        android:startColor="#FFFCFAFA"
        android:endColor="#FAF7F4F1" />
    <corners android:radius="20dp" />
    <stroke android:width="1dp" android:color="#14000000" />
</shape>
```

**`app/src/main/res/drawable-night/widget_greeting_background.xml`** 改为：
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:angle="135"
        android:startColor="#EE2A2D33"
        android:endColor="#E822252B" />
    <corners android:radius="20dp" />
    <stroke android:width="1dp" android:color="#18FFFFFF" />
</shape>
```

### 同步圆角
`widget_todo_event_card.xml` 和 `widget_todo_greeting_card.xml` 的根 LinearLayout 都还**没有 `clipToOutline="true"`**（已确认：grep 结果只有 task_card 和 schedule_card 子项有此属性）。补上：

`widget_todo_event_card.xml:2-13`，根 LinearLayout 加：
```xml
android:clipToOutline="true"
```

`widget_todo_greeting_card.xml:2-13`，根 LinearLayout 加：
```xml
android:clipToOutline="true"
```

否则即使背景圆角统一，子内容（如长按高亮）也不会被裁。

## 验证
- [ ] 今日看板从顶部刷到底部，所有卡片背景同色（白/深灰）、同圆角（20dp）、同描边
- [ ] 浅色和深色模式都一致
- [ ] 问候卡的极淡渐变还在（不至于太死板），但描边与其他卡片一致

---

# 3. 事件卡片 padding 巨幅瘦身

## 现状（已验证）
`widget_todo_event_card.xml:2-13`：
```xml
android:minHeight="108dp"
android:paddingStart="18dp"
android:paddingTop="15dp"
android:paddingEnd="18dp"
android:paddingBottom="15dp"
```

而同一个 widget 里的待办卡（`widget_todo_task_card.xml`）已经收紧到：
- `minHeight="68dp"`
- 内容区 `paddingTop/Bottom="10dp"`

事件卡比待办卡**高出 40dp**，padding 多 5-8dp，这就是用户原始需求第 7 条"padding 太大"的另一个重灾区——只是上一轮没改到。

## 改动

`widget_todo_event_card.xml:2-13`，根 LinearLayout 改为：
```xml
android:minHeight="72dp"
android:paddingStart="10dp"
android:paddingTop="10dp"
android:paddingEnd="10dp"
android:paddingBottom="10dp"
```

如果有日期块（`widget_event_date_block`）用 `layout_width="56dp"` 占左侧，可以缩到 `48dp` 以匹配收紧后的整体节奏——但**先不动数字字号**，仅缩外框，避免破坏可读性。

## 验证
- [ ] 单条事件卡视觉高度与待办卡接近（差 ≤ 8dp）
- [ ] 事件标题、时间、地点不会被截断
- [ ] 4×5 grid 下 widget 能多显示 1-2 条内容

---

# 4. 倒数日条目 padding 对称化

## 现状（已验证）
`widget_countdown_item.xml:11-14`：
```xml
android:paddingStart="14dp"
android:paddingTop="6dp"
android:paddingEnd="14dp"
android:paddingBottom="6dp"
```

横向 14dp、纵向 6dp，**横纵比 2.3:1**，视觉上左右松、上下挤，跟同一 app 的其他卡片节奏不一致（其他卡片大致 1:1 或 5:6）。

## 改动

改为：
```xml
android:paddingStart="10dp"
android:paddingTop="8dp"
android:paddingEnd="10dp"
android:paddingBottom="8dp"
```

横纵比拉到 5:4，更接近其他卡片节奏。

## 验证
- [ ] 倒数日条目左右不再视觉过松
- [ ] 数字（如 `9d`）和文字位置不被挤压
- [ ] minHeight=68dp 仍能容纳所有内容

---

# 5. 「已逾期」badge 深色模式补 night 变体

## 现状（已验证）
`app/src/main/res/drawable/widget_task_overdue_badge_background.xml`：
```xml
<solid android:color="#18D14343" />
<corners android:radius="999dp" />
```

底色是红色 9% 不透明度（`0x18 = 24/255 ≈ 9.4%`）。

`app/src/main/res/drawable-night/` 目录下**没有 `widget_task_overdue_badge_background.xml`**（已用 `ls` 确认）。即深色模式 fallback 到浅色版本。

深色模式下卡片底是 `#E825262B`（深灰半透明），叠上 `#18D14343` 后底色几乎不可见；文字 `widget_danger = #E89A9A`（粉红）漂在深灰上——badge 失去存在感。

## 改动

新建 `app/src/main/res/drawable-night/widget_task_overdue_badge_background.xml`：
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#40D14343" />
    <corners android:radius="999dp" />
</shape>
```

底色 25% 不透明（`0x40 = 64/255 ≈ 25%`），叠在深灰卡片上能形成清晰红色 pill；与浅粉文字配对仍有足够对比。

**不要修改浅色版本**——浅色版本叠在白底上视觉刚好。

## 验证
- [ ] 深色模式下"已逾期"标签清晰可见，红色 pill 形状明确
- [ ] 浅色模式不变化
- [ ] badge 不会因为底色加深而抢眼到喧宾夺主

---

# 6. 三日视图 Pager 边界翻页只显示 2 天的回归

## 现状（已验证）
`app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt:519-525`：
```kotlin
} ) { page ->
    val pageIndex = page.coerceIn(0, dateWindow.lastIndex)
    val pageVisibleRange = if (viewMode == CalendarViewMode.THREE_DAY) {
        (pageIndex - 1).coerceAtLeast(0)..(pageIndex + 1).coerceAtMost(dateWindow.lastIndex)
    } else {
        pageIndex..pageIndex
    }
```

**问题**：当 `pageIndex == 0`（dateWindow 第一天），三日视图的可见范围变成 `0..1`——**只显示 2 天**。同理 `pageIndex == dateWindow.lastIndex` 时变成 `lastIndex-1..lastIndex`——也只显示 2 天。

用户翻到日期窗口边界时会看到三日视图突然"少一格"，明显的视觉异常。

## 目标
边界处仍显示完整的 3 天，通过偏移 pageIndex 来实现（即第 0 天作为"右起第三天"显示）。

## 改动

### 方案
当 pageIndex 接近边界时，让 pageIndex 自身偏移，保证可见范围始终是 3 天宽：

```kotlin
val pageVisibleRange = if (viewMode == CalendarViewMode.THREE_DAY) {
    val anchorIndex = pageIndex.coerceIn(1, (dateWindow.lastIndex - 1).coerceAtLeast(1))
    (anchorIndex - 1)..(anchorIndex + 1).coerceAtMost(dateWindow.lastIndex)
} else {
    pageIndex..pageIndex
}
```

边界情况：
- `pageIndex = 0` → `anchorIndex = 1` → range = `0..2`（仍显示 3 天，但当前焦点是第 0 天）
- `pageIndex = lastIndex` → `anchorIndex = lastIndex - 1` → range = `(lastIndex-2)..lastIndex`（仍显示 3 天）
- 中间值不受影响

`pageHorizontalOffsetPx` 同步用 `anchorIndex - 1` 计算（不是 `pageIndex - 1`）：
```kotlin
val pageHorizontalOffsetPx = when (viewMode) {
    CalendarViewMode.THREE_DAY -> {
        val anchorIndex = pageIndex.coerceIn(1, (dateWindow.lastIndex - 1).coerceAtLeast(1))
        ((anchorIndex - 1) * dayColumnWidthPx).coerceIn(0f, maxHorizontalOffsetPx)
    }
    CalendarViewMode.DAY -> (pageIndex * dayColumnWidthPx).coerceIn(0f, maxHorizontalOffsetPx)
    else -> 0f
}
```

### 注意
这个修改**不改变翻页步长**（仍是每页 1 天）。如果用户/产品确认希望"三日视图每页跳 3 天"，那是**另一个需求**，不在本轮范围。本轮只修边界 bug。

## 验证
- [ ] 三日视图翻到第一页（dateWindow 起始日）：仍显示 3 天，最左侧那一栏是起始日
- [ ] 翻到最后一页：仍显示 3 天，最右侧那一栏是结束日
- [ ] 翻页过程中没有 1 天 / 2 天 / 3 天宽度跳动
- [ ] 中间页面表现不变
- [ ] 事件块横向拖拽改期仍工作（不被 Pager 拦截，已验证 `userScrollEnabled = !calendarEventDragActive` 已接好）

---

# 通用要求

1. 版本号升级到 1.13.16 / versionCode 264
2. 数据库版本保持 25，无 schema 变更
3. 每轮 `./gradlew.bat :app:compileDebugKotlin` + `git diff --check` 通过
4. 最后一轮 `./gradlew.bat :app:assembleDebug` 验证
5. commit 严格按 AGENTS.md 的 bullet 列表格式
6. 不要 push 到 GitHub
7. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`

---

# 实现顺序建议

1. **#5（badge night 变体）** — 2 分钟，新建一个文件
2. **#4（倒数日 padding 对称）** — 2 分钟，改 4 个数值
3. **#3（事件卡 padding 瘦身）** — 5 分钟
4. **#1（移除倒数日圆圈）** — 10 分钟，4 处文件改动 + 编译验证
5. **#2（卡片背景统一）** — 15 分钟，4 个 drawable + 2 个 layout 加 clipToOutline
6. **#6（Pager 边界修复）** — 15 分钟，2 处计算逻辑 + 边界手动测

总时长约 1 小时，全部为低风险修改，无需重构。

---

# 不做的事

- ❌ 不再调整今日看板整体配色基调（上一轮已定为暖白/深灰）
- ❌ 不重写 HorizontalPager 翻页步长（除非产品确认需要）
- ❌ 不动应用内 Compose 待办卡片（只 widget + 日历 Pager 边界）
- ❌ 不引入新设置项、新 schema
- ❌ 不为问候卡新建独立背景 drawable（直接复用现有 + 极淡渐变区分即可）
- ❌ 不动 `CountdownWidgetService` 的数据侧逻辑（只删 setViewVisibility 那行）

---

# 已经验证不存在的"传闻问题"（不要修）

之前 explore 子代理报告过的，但实测**已修复或本就不存在**：

- ✅ `DashboardChrome.kt` 的 6 处 `items()` 调用全部已带 `contentType = { ... }`（行 799 / 891 / 919 / 967 / 983 / 1007 已确认）
- ✅ `HorizontalPager` 与事件拖拽手势冲突已通过 `userScrollEnabled = !calendarEventDragActive`（CalendarPanel.kt:517）+ `onEventDragActiveChange = { active -> calendarEventDragActive = active }`（行 609）正确接好
- ✅ `widget_vertical_pill.xml` 已改为纯矩形（无 corners 节点），加上 task_card / schedule_card 的 `clipToOutline="true"`，色条与卡片已经一体化
