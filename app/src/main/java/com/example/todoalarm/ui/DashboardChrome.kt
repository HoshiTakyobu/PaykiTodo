package com.example.todoalarm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoItem
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun DashboardBackgroundBrush(): Brush {
    return Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            MaterialTheme.colorScheme.background
        )
    )
}

@Composable
internal fun DashboardDrawer(current: DashboardSection, onSelect: (DashboardSection) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Text("PT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("PaykiTodo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
        DashboardSection.entries.forEach { section ->
            NavigationDrawerItem(
                label = { Text(section.label) },
                selected = current == section,
                onClick = { onSelect(section) },
                icon = { Icon(section.icon, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardTopBar(onMenu: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onSurface),
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Menu, contentDescription = "打开侧边菜单", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        title = { Text("PaykiTodo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
    )
}

@Composable
internal fun DashboardFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(Icons.Rounded.Add, contentDescription = "新增任务")
    }
}

@Composable
internal fun DashboardBody(
    section: DashboardSection,
    padding: PaddingValues,
    uiState: TodoUiState,
    permissions: PermissionSnapshot,
    onEdit: (TodoItem) -> Unit,
    onCompleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit,
    onRequestNotificationPolicyAccess: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onReminderDefaultsChange: (Boolean, Boolean, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = dashboardPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (section) {
            DashboardSection.ACTIVE -> {
                item { GreetingCard() }
                item { SectionTitle("今日待办") }
                if (uiState.todayItems.isEmpty()) item { EmptyStateCard("今天还没有安排进来的任务。") }
                else items(uiState.todayItems, key = { it.id }) { item -> ActiveTodoCard(item, { onEdit(item) }, { onCompleteTodo(item) }) }
                item { SectionTitle("计划中的任务") }
                if (uiState.upcomingItems.isEmpty()) item { EmptyStateCard("后续时间段暂时没有新的计划。") }
                else items(uiState.upcomingItems, key = { it.id }) { item -> ActiveTodoCard(item, { onEdit(item) }, { onCompleteTodo(item) }) }
            }
            DashboardSection.HISTORY -> {
                if (uiState.completedItems.isEmpty()) item { EmptyStateCard("完成后的任务会保留在这里。") }
                else items(uiState.completedItems, key = { it.id }) { item -> CompletedTodoCard(item, { onEdit(item) }, { onRestoreTodo(item) }) }
            }
            DashboardSection.SETTINGS -> item {
                SettingsPanel(
                    permissions = permissions,
                    selectedThemeMode = uiState.settings.themeMode,
                    defaultSnooze = uiState.settings.defaultSnoozeMinutes,
                    ringEnabled = uiState.settings.defaultRingEnabled,
                    vibrateEnabled = uiState.settings.defaultVibrateEnabled,
                    voiceEnabled = uiState.settings.defaultVoiceEnabled,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                    onRequestFullScreenPermission = onRequestFullScreenPermission,
                    onRequestNotificationPolicyAccess = onRequestNotificationPolicyAccess,
                    onRequestIgnoreBatteryOptimization = onRequestIgnoreBatteryOptimization,
                    onThemeModeChange = onThemeModeChange,
                    onDefaultSnoozeChange = onDefaultSnoozeChange,
                    onReminderDefaultsChange = onReminderDefaultsChange
                )
            }
        }
    }
}

@Composable
private fun GreetingCard() {
    val scope = rememberCoroutineScope()
    var quote by rememberSaveable { mutableStateOf(seedQuote()) }
    var quoteIndex by rememberSaveable { mutableStateOf(quoteSeed()) }

    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${timeGreeting()}，Payki", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(quote, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(
                    onClick = {
                        scope.launch {
                            quoteIndex = quoteIndex + 1
                            quote = QuoteRepository.fetchRemoteQuote() ?: DAILY_QUOTES[quoteIndex % DAILY_QUOTES.size]
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "更换短句", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
internal fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
internal fun EmptyStateCard(text: String) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun LaunchScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF2D9BB),
                            Color(0xFFBCD4D7),
                            Color(0xFF607E77)
                        )
                    )
                )
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x55FFF1D9),
                radius = size.minDimension * 0.16f,
                center = center.copy(y = size.height * 0.23f)
            )

            val mountainBack = Path().apply {
                moveTo(0f, size.height * 0.62f)
                cubicTo(
                    size.width * 0.12f, size.height * 0.52f,
                    size.width * 0.28f, size.height * 0.48f,
                    size.width * 0.42f, size.height * 0.54f
                )
                cubicTo(
                    size.width * 0.58f, size.height * 0.62f,
                    size.width * 0.74f, size.height * 0.44f,
                    size.width, size.height * 0.56f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainBack, color = Color(0x886B8A7A))

            val mountainMid = Path().apply {
                moveTo(0f, size.height * 0.72f)
                cubicTo(
                    size.width * 0.16f, size.height * 0.66f,
                    size.width * 0.32f, size.height * 0.63f,
                    size.width * 0.5f, size.height * 0.71f
                )
                cubicTo(
                    size.width * 0.67f, size.height * 0.78f,
                    size.width * 0.84f, size.height * 0.66f,
                    size.width, size.height * 0.73f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainMid, color = Color(0xAA466259))

            val mountainFront = Path().apply {
                moveTo(0f, size.height * 0.8f)
                cubicTo(
                    size.width * 0.14f, size.height * 0.76f,
                    size.width * 0.34f, size.height * 0.75f,
                    size.width * 0.46f, size.height * 0.83f
                )
                cubicTo(
                    size.width * 0.62f, size.height * 0.91f,
                    size.width * 0.82f, size.height * 0.81f,
                    size.width, size.height * 0.84f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainFront, color = Color(0xFF2A3F42))
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x220C1F24), Color(0x5521372E), Color(0x330A1114)))))
        ElevatedCard(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PaykiTodo",
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun timeGreeting(): String = when (LocalTime.now().hour) {
    in 0..4 -> "凌晨好"
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}

private fun quoteSeed(): Int = LocalDate.now().dayOfYear % DAILY_QUOTES.size
private fun seedQuote(): String = DAILY_QUOTES[quoteSeed()]

private val DAILY_QUOTES = listOf(
    "把今天最重要的一件事稳稳做完。",
    "先推进一点，比原地不动更重要。",
    "按节奏完成，而不是被情绪推着走。",
    "把注意力放回眼前这一件事。",
    "一点一点做，今天也会留下成果。",
    "DDL 不是压力，它只是你的方向标。",
    "完成比完美更值得优先争取。",
    "开始动手，焦虑就会慢慢退下去。",
    "只盯住下一步，整件事就没那么重。",
    "今天比昨天更稳一点就够了。",
    "先完成，再打磨。",
    "拖延会放大问题，行动会缩小问题。",
    "把劲用在关键点上。",
    "你不需要一下子做完，只需要继续往前。",
    "把今天收拾好，明天就会轻很多。",
    "专注半小时，也能撬动整天的节奏。",
    "少一点犹豫，多一点推进。",
    "先解决最难受的那一件。",
    "你的系统感，会替你省下情绪成本。",
    "按计划做，小胜会自己累起来。",
    "眼下这一小步，就是最实际的突破。",
    "别等状态，先做再说。",
    "今天推进的每一点，都会在未来还给你。",
    "稳住节奏，比偶尔爆发更重要。",
    "专注当下，别预支还没发生的压力。",
    "你需要的不是更多想法，而是下一次点击完成。",
    "先把今天过得像样，长期自然会变好。",
    "现在开始，永远比等会儿开始更强。",
    "一件件清掉，脑子就会越来越轻。",
    "在有限时间里，做最值得做的事。",
    "完成一个，就离轻松一点更近一步。",
    "清晰、具体、立刻去做。",
    "把任务拆小，完成感就会回来。",
    "先处理事实，再处理情绪。",
    "持续推进，本身就是很强的能力。",
    "让行动替你建立信心。"
)
