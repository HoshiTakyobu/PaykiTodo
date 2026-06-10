# PaykiTodo — Widget 视觉全面重做 + UI 细节精进

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用中文，描述版本相关的行为变化**。

当前版本：**1.13.68 / versionCode 316**

---

# 背景

Widget 在前几轮改造后仍有明显问题：
1. 视觉风格与应用内差距大（Widget 偏旧/花哨，应用内现代简洁）
2. 间距过松导致信息密度低
3. 全天事项截断无提示
4. 多处视觉割裂（3 种卡片背景共存、倒数日还有圆圈）

本轮目标：**Widget 视觉向应用内统一 + 关键 UX 痛点修复**。

---

# 第一部分：Widget 视觉全面重做（向应用内靠拢）

## 设计原则

应用内主界面（DashboardChrome / CalendarPanel / TodoCards）的视觉特征：
- 深色背景（深蓝紫渐变）
- 卡片：圆角 24dp、轻微描边、半透明白底（浅色）或深灰底（深色）
- 色条：左侧 8dp 实色，与卡片一体
- 字体：层级清晰，主标题 bold 15-17sp，次要信息 12-13sp
- 间距：紧凑但透气（卡片间 12-14dp）

**Widget 现状**：
- 背景：固定暖白/深灰 + 极淡 scrim（上一轮改的）
- 卡片：3 种不同背景（米黄渐变 / 白底 / 旧米黄），圆角不统一
- 色条：6dp 窄条，部分卡片没有
- 间距：过松（外层 6dp、卡片间 3dp、卡片内 10-18dp 不等）

## 目标视觉方案

### 方案 A：深色半透明磨砂风（推荐，最接近应用内）

**Widget 背景**：
- 浅色：半透明白磨砂 `#F5FFFFFF`（98% 白） + 极淡灰边 `#0F000000`
- 深色：半透明深灰磨砂 `#F01C1E20`（94% 深灰） + 极淡白边 `#18FFFFFF`
- 圆角 24dp（与应用内卡片一致）
- 移除 scrim 层（简化）

**所有卡片统一背景**：
- 浅色：`#FAFFFFFF`（纯白 98%）+ `#12000000` 描边
- 深色：`#F0252730`（深蓝灰 94%）+ `#18FFFFFF` 描边
- 圆角 20dp
- 色条 8dp（与应用内一致），左侧与卡片圆角融合（`clipToOutline="true"`）

**字体颜色调整**：
- 主标题：浅色 `#1A1A1A`，深色 `#F2F2F2`（提高对比度）
- 次要文字：浅色 `#5A5A5A`，深色 `#B8B8B8`（当前 `#B5AA98` 偏黄）
- 强调色（逾期/签到中）：浅色 `#D32F2F`，深色 `#EF9A9A`

**间距收紧**：
- Widget 外层 padding：`4dp`（当前 6dp）
- ListView dividerHeight：`2dp`（当前 3dp）
- 卡片内部：横 `10dp` / 纵 `8dp`（当前 10-18dp 不等）

### 实现文件清单

#### 1. Widget 背景
**`app/src/main/res/drawable/widget_board_background.xml`**：
```xml
<shape>
    <solid android:color="#F5FFFFFF" />
    <corners android:radius="24dp" />
    <stroke android:width="1dp" android:color="#0F000000" />
</shape>
```

**`app/src/main/res/drawable-night/widget_board_background.xml`**：
```xml
<shape>
    <solid android:color="#F01C1E20" />
    <corners android:radius="24dp" />
    <stroke android:width="1dp" android:color="#18FFFFFF" />
</shape>
```

**删除 scrim 层**：`widget_todo.xml:10-16` 删除 `widget_background_scrim` 那个 TextView。

#### 2. 卡片背景统一
把以下 3 个 drawable 都改成同一套样式：
- `widget_todo_soft_card_background.xml`（待办卡）
- `widget_todo_item_background.xml`（日程/事件卡）
- `widget_greeting_background.xml`（问候卡）

**浅色统一模板**：
```xml
<shape>
    <solid android:color="#FAFFFFFF" />
    <corners android:radius="20dp" />
    <stroke android:width="1dp" android:color="#12000000" />
</shape>
```

**深色统一模板** (`drawable-night/`)：
```xml
<shape>
    <solid android:color="#F0252730" />
    <corners android:radius="20dp" />
    <stroke android:width="1dp" android:color="#18FFFFFF" />
</shape>
```

问候卡可以保留极淡渐变作为微妙差异（可选）：
```xml
<gradient android:angle="135"
    android:startColor="#FFFAFAFA"
    android:endColor="#FAF5F5F5" />
```

#### 3. 颜色值调整
**`app/src/main/res/values/colors.xml`** widget 相关颜色：
```xml
<color name="widget_text_primary">#1A1A1A</color>
<color name="widget_text_secondary">#5A5A5A</color>
<color name="widget_text_muted">#8A8A8A</color>
<color name="widget_danger">#D32F2F</color>
```

**`app/src/main/res/values-night/colors.xml`**：
```xml
<color name="widget_text_primary">#F2F2F2</color>
<color name="widget_text_secondary">#B8B8B8</color>
<color name="widget_text_muted">#8A8A8A</color>
<color name="widget_danger">#EF9A9A</color>
```

#### 4. 外层间距收紧
**`widget_todo.xml:19-26`**：
```xml
android:paddingStart="4dp"
android:paddingTop="4dp"
android:paddingEnd="4dp"
android:paddingBottom="4dp"
```

**`widget_todo.xml:39`**：
```xml
android:dividerHeight="2dp"
```

**同步 `widget_countdown.xml`** 和预览文件。

---

# 第二部分：倒数日 Widget 移除圆圈（漏改回归）

## 现状
`widget_countdown_item.xml:17-25` 还有 `widget_countdown_check` + `widget_checkbox_ring`。

## 改动
1. 删除 `widget_countdown_item.xml:17-25` 的 `widget_countdown_check` TextView
2. 删除 `widget_countdown_preview.xml` 中两处 `widget_checkbox_ring` 占位
3. 删除 `CountdownWidgetService.kt:77` 的 `setViewVisibility(R.id.widget_countdown_check, ...)`
4. 删除 `drawable/widget_checkbox_ring.xml`（确认无其他引用后）

---

# 第三部分：日程卡片间距收紧

## 现状（用户截图反馈）
`widget_todo_schedule_card.xml`：
- "明天"标签 `layout_marginTop="8dp"`（行 237）
- 下方第一个事件 `layout_marginTop="4dp"`（行 265）
- 事件卡 `minHeight="50dp"`，内部 `paddingTop/Bottom="6dp"`

视觉上"明天"和事件卡之间空太多，事件卡内部也松。

## 改动
**`widget_todo_schedule_card.xml`**：

### 1. "明天"标签上方间距收紧
行 237：`android:layout_marginTop="8dp"` → `android:layout_marginTop="6dp"`

### 2. "明天"标签下方事件卡间距收紧
行 265：`android:layout_marginTop="4dp"` → `android:layout_marginTop="2dp"`

同理行 340（tomorrow_event_2）也改成 `2dp`。

### 3. 事件卡内部收紧
行 269-272：
```xml
android:minHeight="50dp"  → android:minHeight="46dp"
android:paddingTop="6dp"  → android:paddingTop="5dp"
android:paddingBottom="6dp" → android:paddingBottom="5dp"
```

同步改 `today_event_1/2` 和 `tomorrow_event_1/2` 全部 4 个事件块。

### 4. "今天"日程区域内容与"明天"标签间距
"今天"最后一个事件（`today_event_2`）和"明天"标签之间当前是 8dp（tomorrow_label 的 marginTop），保持不变或改成 10dp（稍微拉开两个分组）。

---

# 第四部分：事件卡片 padding 瘦身

## 现状
`widget_todo_event_card.xml:2-13`：
- `minHeight="108dp"`（待办卡是 68dp，高出 40dp）
- `paddingStart/End="18dp"`，`paddingTop/Bottom="15dp"`

## 改动
```xml
android:minHeight="72dp"
android:paddingStart="10dp"
android:paddingTop="10dp"
android:paddingEnd="10dp"
android:paddingBottom="10dp"
```

日期块（`widget_event_date_block`）宽度从 `56dp` 缩到 `48dp`（可选）。

---

# 第五部分：倒数日条目 padding 对称化

## 现状
`widget_countdown_item.xml:11-14`：横 14dp / 纵 6dp。

## 改动
```xml
android:paddingStart="10dp"
android:paddingTop="8dp"
android:paddingEnd="10dp"
android:paddingBottom="8dp"
```

---

# 第六部分：「已逾期」badge 深色模式补 night 变体

## 现状
`drawable/widget_task_overdue_badge_background.xml` 只有浅色版（`#18D14343`，9% 不透明）。

## 改动
新建 `drawable-night/widget_task_overdue_badge_background.xml`：
```xml
<shape>
    <solid android:color="#40D14343" />
    <corners android:radius="999dp" />
</shape>
```

---

# 第七部分：日历全天事项自适应高度

## 现状
`CalendarPanel.kt:1707`：
```kotlin
.height((rowCount * 30).dp + 14.dp)
```
`rowCount` 上限 3，第 4 条开始直接不渲染（`if (rowIndex >= rowCount) return@forEachIndexed`）。

## 目标（用户需求）
- 没有全天事项 → 显示 1 行高度（占位）
- 有几条 → 自适应显示几条（不截断）
- 最多显示 6 条，超过 6 条时第 6 行显示 "+N more"

## 改动

### 1. 移除 rowCount 上限
```kotlin
val rowCount = events.size.coerceIn(1, 6)  // 改为最多 6 条
```

### 2. 高度改为 wrapContent + 最大高度约束
```kotlin
.heightIn(min = 44.dp, max = (6 * 30).dp + 14.dp)
```

### 3. 渲染逻辑
```kotlin
events.forEachIndexed { rowIndex, item ->
    if (rowIndex >= 6) return@forEachIndexed  // 最多 6 条
    // ... 原有渲染逻辑
}

// 如果超过 6 条，渲染 "+N more" 行
if (events.size > 6) {
    Text(
        text = "+${events.size - 6} more",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.offset(x = timeAxisWidth + 8.dp, y = (6 * 28).dp)
    )
}
```

### 4. 空状态优化
当 `events.isEmpty()` 时，全天事项区域改为 `height(36.dp)`（最小高度），不显示任何内容（连"全天"标签都不显示），节省空间。

---

# 第八部分：三日视图 Pager 边界修复

## 现状
`CalendarPanel.kt:519-525`：当 `pageIndex == 0` 时，可见范围变成 `0..1`（只显示 2 天）。

## 改动
```kotlin
val pageVisibleRange = if (viewMode == CalendarViewMode.THREE_DAY) {
    val anchorIndex = pageIndex.coerceIn(1, (dateWindow.lastIndex - 1).coerceAtLeast(1))
    (anchorIndex - 1)..(anchorIndex + 1).coerceAtMost(dateWindow.lastIndex)
} else {
    pageIndex..pageIndex
}
```

`pageHorizontalOffsetPx` 同步用 `anchorIndex - 1` 计算：
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

---

# 通用要求

1. 版本号升级到 **1.13.69 / versionCode 317**
2. 数据库版本保持不变（无 schema 变更）
3. 每轮 `./gradlew.bat :app:compileDebugKotlin` + `git diff --check` 通过
4. 最后一轮 `./gradlew.bat :app:assembleDebug` 验证
5. Commit message 用中文，描述行为变化（按 AGENTS.md 规定）
6. 不要 push 到 GitHub
7. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`、`docs/current/PROJECT_STATUS.md`

---

# 实现顺序建议

1. **第六部分（badge night 变体）** — 2 分钟
2. **第五部分（倒数日 padding）** — 2 分钟
3. **第四部分（事件卡 padding）** — 5 分钟
4. **第二部分（移除倒数日圆圈）** — 10 分钟
5. **第三部分（日程卡间距收紧）** — 10 分钟
6. **第一部分（Widget 视觉重做）** — 30 分钟（涉及多个 drawable + 颜色调整）
7. **第七部分（全天事项自适应）** — 20 分钟
8. **第八部分（Pager 边界修复）** — 15 分钟

总时长约 1.5-2 小时。

---

# 不做的事

- ❌ 不动规划台（用户反馈"用不起来"，需要重新设计交互，不是改 UI 能解决）
- ❌ 不重写 HorizontalPager 翻页步长（除非用户明确要求）
- ❌ 不引入新功能、新设置项
- ❌ 不动应用内主界面视觉（只 Widget + 日历全天事项 + Pager 边界）
- ❌ 不为 Widget 新建深色/浅色主题切换开关（跟随系统）

---

# 验证清单

## Widget 视觉
- [ ] 浅色/深色模式下背景统一为半透明磨砂风
- [ ] 所有卡片（问候/待办/日程/事件/空状态）背景、圆角、描边一致
- [ ] 色条 8dp、与卡片融合（无缝隙）
- [ ] 字体对比度足够（深色模式 widget_text_secondary 可读）
- [ ] 倒数日 Widget 无圆圈
- [ ] 倒数日与今日看板风格一致

## 间距与密度
- [ ] Widget 外层 padding 4dp，卡片间 2dp
- [ ] "明天"与事件卡间距不再过大
- [ ] 事件卡内部紧凑但不拥挤
- [ ] 4×5 grid Widget 能显示更多内容

## 功能正确性
- [ ] 全天事项 0 条 → 区域最小化
- [ ] 全天事项 1-6 条 → 自适应显示全部
- [ ] 全天事项 > 6 条 → 显示前 6 条 + "+N more"
- [ ] 三日视图翻到边界（第 0 天/最后一天）仍显示 3 天
- [ ] 深色模式"已逾期" badge 清晰可见
