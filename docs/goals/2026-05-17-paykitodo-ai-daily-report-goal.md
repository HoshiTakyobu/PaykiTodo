# PaykiTodo 1.9.x — AI 日报 / 周报

你正在维护 PaykiTodo 项目，工作目录是 `G:\Workspace\Project\PaykiTodo`。
请先按 `AGENTS.md` 要求读取 `docs/current/` 下的文档。**特别注意 commit message 必须用 `完成内容概要：` + bullet 列表的格式**。

## 背景

用户希望 AI 每天 22:00 自动生成一份日报：
- 今天完成了哪些待办、有哪些日程
- 明天 DDL 是什么
- 建议明天先做哪几项

报告写入规划台一篇专属文档（标题"AI 日报"），用户可以随时翻看。
周日 22:00 额外生成一份周报，写入"AI 周报"文档。

复用现有的 `PlanningAiCaller` 和多 AI 源轮询基础设施，不引入新依赖。

---

## Part 1：调度入口

### 文件

- 新建 `app/src/main/java/com/example/todoalarm/alarm/DailyReportScheduler.kt`
- `app/src/main/java/com/example/todoalarm/TodoApplication.kt`：`onCreate` 中调用 `DailyReportScheduler.scheduleNext(this)`
- `app/src/main/java/com/example/todoalarm/alarm/BootReceiver.kt`：开机后也重新调度

### 设计

复用 `AlarmManager` 模式，类似 `AlarmScheduler` 但更轻量：

```kotlin
object DailyReportScheduler {
    private const val REQUEST_CODE_DAILY = 90001
    private const val REQUEST_CODE_WEEKLY = 90002

    fun scheduleNext(context: Context) {
        val settings = (context.applicationContext as TodoApplication).settingsStore.currentSettings()
        if (!settings.dailyReportEnabled) return

        scheduleDaily(context, settings.dailyReportHour, settings.dailyReportMinute)
        if (settings.weeklyReportEnabled) {
            scheduleWeekly(context, settings.weeklyReportHour, settings.weeklyReportMinute)
        }
    }

    private fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val now = ZonedDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val triggerMillis = target.toInstant().toEpochMilli()
        val intent = Intent(context, DailyReportReceiver::class.java).apply {
            action = DailyReportReceiver.ACTION_GENERATE_DAILY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_DAILY, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    // scheduleWeekly：找到下一个周日的指定时间
}
```

### Receiver

新建 `app/src/main/java/com/example/todoalarm/alarm/DailyReportReceiver.kt`：

```kotlin
class DailyReportReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TodoApplication
                when (intent.action) {
                    ACTION_GENERATE_DAILY -> DailyReportGenerator.generateDaily(app)
                    ACTION_GENERATE_WEEKLY -> DailyReportGenerator.generateWeekly(app)
                }
                DailyReportScheduler.scheduleNext(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_GENERATE_DAILY = "com.example.todoalarm.GENERATE_DAILY_REPORT"
        const val ACTION_GENERATE_WEEKLY = "com.example.todoalarm.GENERATE_WEEKLY_REPORT"
    }
}
```

`AndroidManifest.xml` 注册 receiver（`exported="false"`）。

---

## Part 2：报告生成器

### 文件

- 新建 `app/src/main/java/com/example/todoalarm/data/DailyReportGenerator.kt`

### 数据收集

```kotlin
object DailyReportGenerator {
    suspend fun generateDaily(app: TodoApplication) {
        val today = LocalDate.now()
        val context = collectDailyContext(app, today)
        val report = if (canUseAi(app)) {
            runCatching { callAi(app, context, ReportType.DAILY) }.getOrNull()
        } else null
        val content = report ?: buildLocalDaily(context)
        writeToReportNote(app, "AI 日报", today, content)
    }

    suspend fun generateWeekly(app: TodoApplication) {
        // 类似 daily，但收集本周数据
    }

    private suspend fun collectDailyContext(app: TodoApplication, date: LocalDate): DailyContext {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowEnd = date.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val items = app.repository.getActiveTodos() // 复用现有
        return DailyContext(
            todayCompleted = items.filter { it.completed && it.completedAtMillis in dayStart..dayEnd },
            todayMissed = items.filter { it.missed && it.dueAtMillis in dayStart..dayEnd },
            todayEvents = items.filter { it.isEvent && eventOverlapsDay(it, dayStart, dayEnd) },
            tomorrowEvents = items.filter { it.isEvent && eventOverlapsDay(it, dayEnd, tomorrowEnd) },
            tomorrowDdls = items.filter { !it.completed && !it.canceled && it.hasDueDate && it.dueAtMillis in dayEnd..tomorrowEnd },
            focusMinutes = app.repository.getTodayFocusMinutes()  // 如果专注模式已实现
        )
    }
}
```

### AI Prompt

```kotlin
private fun buildDailyPrompt(context: DailyContext, date: LocalDate): String {
    return """
        请根据以下数据生成一份简短的中文日报，控制在 200 字以内：

        日期：${date.format(DateTimeFormatter.ISO_DATE)}

        今天完成的待办（${context.todayCompleted.size} 条）：
        ${context.todayCompleted.joinToString("\n") { "- ${it.title}" }.ifBlank { "（无）" }}

        今天错过的待办（${context.todayMissed.size} 条）：
        ${context.todayMissed.joinToString("\n") { "- ${it.title}" }.ifBlank { "（无）" }}

        今天的日程（${context.todayEvents.size} 条）：
        ${context.todayEvents.joinToString("\n") { "- ${it.title}" }.ifBlank { "（无）" }}

        明天的日程（${context.tomorrowEvents.size} 条）：
        ${context.tomorrowEvents.joinToString("\n") { "- ${it.title}" }.ifBlank { "（无）" }}

        明天 DDL（${context.tomorrowDdls.size} 条）：
        ${context.tomorrowDdls.joinToString("\n") { "- ${it.title} (${formatDateTime(it.dueAtMillis)})" }.ifBlank { "（无）" }}

        今日专注：${context.focusMinutes} 分钟

        格式要求：
        1. 第一段（1-2 句）：今天的总结，肯定完成的部分
        2. 第二段（1-2 句）：明天的重点提示，特别是 DDL 紧迫的事项
        3. 第三段（可选）：温和的建议，避免说教

        不要使用 Markdown 标题，直接用自然段落。不要寒暄。
    """.trimIndent()
}
```

`callAi` 复用 `PlanningAiCaller.callWithFallback`，system prompt 写"你是一个温和的日程助手，输出简短的中文段落"。

### 本地兜底

如果 AI 不可用，生成一段固定模板：

```kotlin
private fun buildLocalDaily(context: DailyContext): String {
    return buildString {
        append("今天完成 ${context.todayCompleted.size} 条待办")
        if (context.focusMinutes > 0) append("，专注 ${context.focusMinutes} 分钟")
        append("。\n\n")
        if (context.tomorrowDdls.isNotEmpty()) {
            append("明天有 ${context.tomorrowDdls.size} 条 DDL：\n")
            context.tomorrowDdls.forEach { append("- ${it.title}\n") }
        } else {
            append("明天无 DDL。\n")
        }
    }
}
```

---

## Part 3：写入规划台报告文档

### 文件

- `app/src/main/java/com/example/todoalarm/data/DailyReportGenerator.kt` 中的 `writeToReportNote`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`：可能需要新增 `getOrCreateReportNote(title: String): PlanningNote`

### 文档格式

报告文档内容是一份不断追加的日记，最新的在最上面：

```markdown
## 2026-05-17 周日 22:00

今天完成了 5 条待办，专注 75 分钟。明天有 2 条 DDL：
- 算法作业（明天 23:59）
- 英语论文初稿（明天 18:00）

建议明早先动笔写论文，作业在下午做。

---

## 2026-05-16 周六 22:00

（更早的日报）

---
```

### 实现

```kotlin
private suspend fun writeToReportNote(
    app: TodoApplication,
    title: String,
    date: LocalDate,
    body: String
) {
    val note = app.repository.getOrCreateReportNote(title)
    val header = "## ${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEE HH:mm", Locale.CHINA))}\n\n"
    val newContent = header + body.trim() + "\n\n---\n\n" + note.contentMarkdown
    app.repository.updatePlanningNote(note.id, newContent)
}
```

`getOrCreateReportNote`：
```kotlin
suspend fun getOrCreateReportNote(title: String): PlanningNote {
    val existing = todoDao.findPlanningNoteByTitle(title)
    if (existing != null) return existing
    val now = System.currentTimeMillis()
    val newNote = PlanningNote(
        title = title,
        contentMarkdown = "",
        createdAtMillis = now,
        updatedAtMillis = now,
        archived = false
    )
    val id = todoDao.insertPlanningNote(newNote)
    return newNote.copy(id = id)
}
```

需要在 `TodoDao` 加 `findPlanningNoteByTitle(title: String): PlanningNote?`。

---

## Part 4：通知用户日报已生成

### 设计

报告生成后，通过现有通知通道发一条非全屏通知：
- 标题"AI 日报已生成"
- 文本：报告第一段截取前 50 字
- 点击通知 → 打开 App 并定位到"AI 日报"文档（复用现有 widget 深链 `EXTRA_OPEN_PLANNING_NOTE_ID`）

实现：

```kotlin
private fun postReportNotification(context: Context, noteId: Long, title: String, preview: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_OPEN_PLANNING_NOTE_ID, noteId)
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 91000, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val builder = NotificationCompat.Builder(context, "ai_report_channel")
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle(title)
        .setContentText(preview)
        .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
    NotificationManagerCompat.from(context).notify(91000, builder.build())
}
```

`TodoApplication.onCreate` 中创建 `ai_report_channel` 通知通道（low priority）。

---

## Part 5：设置 UI

### 文件

- `app/src/main/java/com/example/todoalarm/data/AppSettingsStore.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`

### 新增字段

```kotlin
val dailyReportEnabled: Boolean = false,
val dailyReportHour: Int = 22,
val dailyReportMinute: Int = 0,
val weeklyReportEnabled: Boolean = false,
val weeklyReportHour: Int = 22,
val weeklyReportMinute: Int = 0
```

### 设置入口

`SettingsPanel.kt` 的 AI 调用配置中，新增子区"AI 日报 / 周报"：

- 启用日报：Switch（关闭时上方时间选择器置灰）
- 日报时间：HH:mm 选择器，默认 22:00
- 启用周报（每周日生成）：Switch
- 周报时间：HH:mm 选择器
- 说明文字：
  - "日报会在每天该时间生成，并写入规划台「AI 日报」文档。"
  - "周报会在每周日该时间生成，并写入规划台「AI 周报」文档。"
  - "AI 配置不可用时会用本地模板生成简短报告。"
- 调试按钮："立即生成一次日报" —— 点击后立刻调用 `DailyReportGenerator.generateDaily`，绕过定时

每次保存设置时调用 `DailyReportScheduler.scheduleNext(context)` 重新调度。

---

## Part 6：报告文档的特殊渲染

### 文件

- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`

### 设计

如果当前规划文档标题是"AI 日报"或"AI 周报"，规划台编辑器顶部显示一个紫色提示条："这是 AI 自动生成的报告文档，建议不要手动编辑当前内容；可以在此保留过往日报作为复盘记录。"

不阻止用户编辑，只是提示。

---

## Part 7：通用要求

1. 版本号升级到 `1.9.x`（具体由 GPT 决定，比如 1.9.1）/ `versionCode` +1
2. 不需要数据库迁移（复用现有 PlanningNote 表）
3. 更新 `CHANGELOG.md`、`docs/current/SESSION_HANDOFF.md`、`docs/current/PROJECT_STATUS.md`、`docs/current/FEATURE_LEDGER.md`
4. `FEATURE_LEDGER.md` 新增"AI 日报 / 周报"小节
5. **commit 严格按 AGENTS.md 的 bullet 列表格式**
6. 不要 push 到 GitHub

---

## 实现顺序建议

1. **第一轮**：Part 1（调度器 + Receiver） + Part 5（设置 UI）— 调度框架
2. **第二轮**：Part 2（数据收集 + AI prompt） + Part 3（写入文档）— 内容生成
3. **第三轮**：Part 4（通知） + Part 6（文档提示）— UX 收尾

---

## 验证清单

- [ ] 设置启用日报，设到当前时间 +1 分钟，等待自动触发
- [ ] 触发后规划台出现"AI 日报"文档，内容包含今天完成 / 明天 DDL 等
- [ ] AI 不可用时（关闭 AI 配置或断网），日报降级为本地模板，仍能生成
- [ ] 通知栏弹出"AI 日报已生成"，点击打开 App 并定位到日报文档
- [ ] 第二天定时再次触发，新日报追加到文档顶部，旧日报保留在下方
- [ ] 周日 22:00 额外生成周报到独立文档
- [ ] 设置里"立即生成一次日报"按钮能立刻触发，方便调试
- [ ] 关闭日报开关后下一次定时不再触发
- [ ] 开机重启后调度仍然有效（BootReceiver 触发 scheduleNext）
