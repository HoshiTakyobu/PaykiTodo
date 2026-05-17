# PaykiTodo 1.9.0 — 专注模式（番茄钟）

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

## 背景

用户是 CS 学生，在保研期间需要专注完成单个任务的能力。希望在 todo 上长按"开始专注"，进入全屏倒计时；结束时震动 + 询问"完成 / 延续 5 分钟 / 放弃"。配合每日看板做"今日专注 X 分钟"统计。

本轮利用现有的提醒服务和 Room 基础设施，不引入新依赖。

---

## Part 1：数据模型

### 文件

- 新建 `app/src/main/java/com/example/todoalarm/data/FocusSession.kt`
- `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`：版本从 11 升到 12
- `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`：新增 `MIGRATION_11_12`
- `app/src/main/java/com/example/todoalarm/TodoApplication.kt`：迁移列表加上 `MIGRATION_11_12`

### 模型

```kotlin
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val todoId: Long? = null,         // 可空，允许不绑定 todo 的自由专注
    val title: String,                // 专注主题；如绑定 todo 则取 todo.title
    val plannedMinutes: Int,
    val actualMinutes: Int,           // 实际进行分钟（含延续）
    val startedAtMillis: Long,
    val endedAtMillis: Long,          // 结束时刻（完成 / 放弃 都填）
    val completed: Boolean,           // true = 用户标记完成；false = 放弃
    val extensionCount: Int = 0       // 延续次数
)
```

### DAO

`TodoDao.kt` 加：

```kotlin
@Insert
suspend fun insertFocusSession(session: FocusSession): Long

@Query("SELECT * FROM focus_sessions WHERE startedAtMillis BETWEEN :startMillis AND :endMillis ORDER BY startedAtMillis DESC")
suspend fun getFocusSessionsInRange(startMillis: Long, endMillis: Long): List<FocusSession>

@Query("SELECT COALESCE(SUM(actualMinutes), 0) FROM focus_sessions WHERE completed = 1 AND startedAtMillis BETWEEN :startMillis AND :endMillis")
suspend fun getCompletedFocusMinutesInRange(startMillis: Long, endMillis: Long): Int
```

### Repository

`TodoRepository.kt` 加：

```kotlin
suspend fun saveFocusSession(session: FocusSession): Long
suspend fun getTodayFocusMinutes(): Int   // 今天 00:00 - 23:59 已完成专注分钟数
suspend fun getTodayFocusSessions(): List<FocusSession>
```

---

## Part 2：专注时长设置

### 文件

- `app/src/main/java/com/example/todoalarm/data/AppSettingsStore.kt`

### 新增字段

```kotlin
val focusDefaultMinutes: Int = 25,         // 默认专注时长，5-90 分钟
val focusExtensionMinutes: Int = 5,        // 单次延续时长，1-30 分钟
val focusKeepScreenOn: Boolean = true,     // 倒计时期间保持屏幕常亮
val focusBlockNotifications: Boolean = false  // 倒计时期间是否压制通知（仅文档化，不真实拦截系统通知）
```

新增 `updateFocusPreferences(default: Int, extension: Int, keepScreenOn: Boolean, blockNotifications: Boolean)` 方法。

### 设置 UI

`SettingsPanel.kt` 新增 `SettingsSection.FOCUS`，设置项：

- 默认专注时长：Slider 5-90 分钟，刻度 5 分钟
- 单次延续时长：Slider 1-30 分钟，刻度 1 分钟
- 屏幕常亮：Switch
- 压制通知：Switch + 说明文字"专注期间不再弹出新提醒（专注内的事项除外）"

放在"常用设置"分组中，图标用 `Icons.Rounded.Timer` 或 `HourglassEmpty`。

---

## Part 3：FocusActivity 全屏倒计时

### 文件

- 新建 `app/src/main/java/com/example/todoalarm/ui/FocusActivity.kt`（参考现有 `ReminderActivity.kt` 的结构，单 Activity + Compose）
- `AndroidManifest.xml` 注册：

```xml
<activity
    android:name=".ui.FocusActivity"
    android:exported="false"
    android:launchMode="singleTask"
    android:showOnLockScreen="true"
    android:theme="@style/Theme.PaykiTodo"
    android:turnScreenOn="false" />
```

不要 `showWhenLocked="true"` —— 专注是用户主动启动的，不需要锁屏唤起。

### Intent extras

```kotlin
const val EXTRA_FOCUS_TODO_ID = "extra_focus_todo_id"     // 可选
const val EXTRA_FOCUS_TITLE = "extra_focus_title"          // 必填
const val EXTRA_FOCUS_MINUTES = "extra_focus_minutes"      // 必填
```

### UI 设计

全屏，背景使用主题 `background` 色（深色模式下深色，浅色模式下浅色）。中央竖向布局：

- 顶部：当前专注主题（todo 标题 / 自由专注标题），24sp
- 中间：超大圆环倒计时
  - 圆环使用 Compose `Canvas`，外圈灰色描边，内圈用 `primary` 色按进度绘制弧形
  - 圆环内部居中显示"MM:SS"格式的剩余时间，72sp，等宽字体（`FontFamily.Monospace`）
  - 圆环下方一行小字"已专注 X 分钟" / "延续 N 次"
- 底部：三个按钮横排
  - "暂停 / 继续"（toggle，灰色）
  - "完成"（primary 色实心）
  - "放弃"（错误色描边）

### 倒计时逻辑

用 Compose 的 `LaunchedEffect` + `delay(1000)` 循环更新剩余秒数：

```kotlin
var remainingSeconds by remember { mutableIntStateOf(plannedMinutes * 60) }
var paused by remember { mutableStateOf(false) }
var actualSeconds by remember { mutableIntStateOf(0) }

LaunchedEffect(paused) {
    while (!paused && remainingSeconds > 0) {
        delay(1000)
        remainingSeconds--
        actualSeconds++
    }
    if (remainingSeconds == 0 && !paused) {
        // 触发完成提示
    }
}
```

### 屏幕常亮

如 `settings.focusKeepScreenOn` 为 true，`onCreate` 加 `window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)`。

### 倒计时归零

倒计时归零时：

1. 触发震动（200ms 短震 + 200ms 间隔 + 400ms 长震）
2. 弹出 AlertDialog："专注完成 / 延续 X 分钟 / 放弃"
3. 用户选择：
   - 完成 → 写入 FocusSession（`completed = true`，`actualMinutes = actualSeconds / 60`），关闭 Activity
   - 延续 → `remainingSeconds += extensionMinutes * 60`，`extensionCount++`，关闭对话框继续倒计时
   - 放弃 → 写入 FocusSession（`completed = false`），关闭 Activity

### 用户主动放弃

用户点底部"放弃"按钮 → 弹确认对话框"确认放弃当前专注？" → 确认后写入 FocusSession（`completed = false`，`actualMinutes = 当前已用分钟`）。

### 用户提前完成

用户点底部"完成"按钮 → 弹确认对话框"提前结束？已专注 X 分钟" → 确认后写入 FocusSession（`completed = true`），关闭 Activity。

### 后退键

`onBackPressed` 等同于点"放弃"按钮。

---

## Part 4：从 todo 启动专注

### 文件

- `app/src/main/java/com/example/todoalarm/ui/TodoCards.kt`：`ActiveTodoCard` 长按菜单已有"删除"，加一项"开始专注"
- 或者更简单：直接在 todo 编辑器（`TodoEditorDialog.kt`）顶部加一个"开始专注"按钮

### 入口设计

推荐两个入口：

1. **TodoCard 长按菜单** —— 在 `ActiveTodoCard` 现有 `combinedClickable` 的 `onLongClick` 中弹出 menu，包含"开始专注 · 25 分钟"和"删除"两项
2. **看板顶部 FAB** —— 每日看板右下角已有 + 号 FAB，长按时弹出 BottomSheet"开始自由专注"，可输入主题 + 选择时长

### 启动逻辑

```kotlin
val intent = Intent(context, FocusActivity::class.java).apply {
    putExtra(FocusActivity.EXTRA_FOCUS_TODO_ID, todo.id)
    putExtra(FocusActivity.EXTRA_FOCUS_TITLE, todo.title)
    putExtra(FocusActivity.EXTRA_FOCUS_MINUTES, settings.focusDefaultMinutes)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}
context.startActivity(intent)
```

---

## Part 5：每日看板"今日专注"统计卡

### 文件

- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`：在 `DashboardSection.BOARD` 分支中，紧跟 `OnboardingCard` 后、`BoardBlockTitle("今日待办")` 前加入新区
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt` 的 `TodoUiState` 加 `todayFocusMinutes: Int` 和 `todayFocusSessionCount: Int`

### 卡片视觉

ElevatedCard，圆角 28dp，内部水平布局：

- 左侧大数字 "X"，48sp 加粗，单位"分钟"小字
- 中间分隔
- 右侧两行：
  - "今日已专注"
  - "N 次专注 · M 次完成"

点击整张卡片 → 跳转到"专注历史" Section（如果实现了的话；否则直接跳过这个交互）。

### 数据来源

`TodoViewModel` 启动时和每次写库后调用 `repository.getTodayFocusMinutes()` 和 `repository.getTodayFocusSessions()`，更新 UiState。

---

## Part 6：完成专注后的 UI 反馈

### 文件

- `app/src/main/java/com/example/todoalarm/ui/FocusActivity.kt`

### 设计

用户在 FocusActivity 选择"完成"后，先不直接退出，而是进入 1.5 秒的"完成动画"页：

- 中央显示 ✓ 图标（绿色）+ "专注完成"
- 下方"已专注 X 分钟 · 延续 N 次"
- 1.5 秒后自动 finish()，回到上一界面

简单实现：用 `LaunchedEffect(Unit) { delay(1500); finish() }`。

---

## Part 7：通用要求

1. 版本号升级到 `1.9.0` / `versionCode` 在当前基础上 +1（190）
2. 数据库版本 +1（12），新增 `MIGRATION_11_12`
3. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`、`docs/current/PROJECT_STATUS.md`、`docs/current/FEATURE_LEDGER.md`
4. `FEATURE_LEDGER.md` 新增"专注模式"小节
5. **commit 严格按 AGENTS.md 的 bullet 列表格式**：
   ```
   新增专注模式与今日专注统计

   完成内容概要：
   - 新增 FocusSession Room 实体和 MIGRATION_11_12，记录每次专注的主题、计划/实际时长、完成或放弃和延续次数
   - 设置 -> 专注模式新增默认时长（5-90 分钟）、延续时长（1-30 分钟）、屏幕常亮、压制通知开关
   - 新增 FocusActivity 全屏倒计时：圆环进度、暂停继续、完成确认、延续 N 分钟、放弃放弃；归零震动并弹出三选一对话框
   - 待办卡片长按菜单新增「开始专注 · X 分钟」入口；每日看板顶部新增今日专注分钟统计卡
   - 版本升级到 1.9.0 / versionCode 190
   - 验证：assembleDebug、testDebugUnitTest、git diff --check 通过
   ```
6. 不要 push 到 GitHub

---

## 实现顺序建议

1. **第一轮**：Part 1（数据模型 + 迁移） + Part 2（设置项 + UI）— 数据底座
2. **第二轮**：Part 3（FocusActivity 倒计时核心）— 主体功能
3. **第三轮**：Part 4（从 todo 启动） + Part 5（看板统计卡） + Part 6（完成动画）— 集成 UI

---

## 验证清单

- [ ] 长按一条 todo 看到"开始专注"菜单，点击后进入全屏倒计时
- [ ] 倒计时期间屏幕保持常亮（如设置开启）
- [ ] 暂停/继续可正常切换
- [ ] 倒计时归零震动 + 弹"完成 / 延续 / 放弃"对话框
- [ ] 选"延续 5 分钟"后倒计时增加 5 分钟，extensionCount +1
- [ ] 选"完成"后看到 1.5 秒完成动画，自动退出
- [ ] 写入数据库的 FocusSession 时间字段正确
- [ ] 每日看板顶部今日专注分钟数随完成的 session 累加
- [ ] 不绑定 todo 的自由专注（看板 FAB 长按入口）也能正常工作
- [ ] 设置里调整默认时长后，新启动的专注用新时长
- [ ] 后退键等同于"放弃"，弹确认对话框
