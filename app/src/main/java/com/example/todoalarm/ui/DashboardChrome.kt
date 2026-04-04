package com.example.todoalarm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoalarm.R
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.theme.PaykiGreetingFontFamily
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun DashboardBackgroundBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    return Brush.linearGradient(
        colors = listOf(
            colors.primary.copy(alpha = 0.16f),
            colors.tertiary.copy(alpha = 0.13f),
            colors.secondary.copy(alpha = 0.1f),
            colors.surfaceVariant.copy(alpha = 0.12f),
            colors.background
        ),
        start = Offset.Zero,
        end = Offset(1300f, 2100f)
    )
}

@Composable
internal fun DashboardDrawer(current: DashboardSection, onSelect: (DashboardSection) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "应用图标",
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Text(
                "PaykiTodo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Menu, contentDescription = "打开菜单", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        title = {
            Text(
                "PaykiTodo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F3E38)
            )
        }
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
    onRequestAccessibilityService: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = dashboardPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (section) {
            DashboardSection.ACTIVE -> {
                item { GreetingCard() }
                item { SectionTitle("今日待办") }
                if (uiState.todayItems.isEmpty()) {
                    item { EmptyStateCard("今天还没有安排任务。") }
                } else {
                    items(uiState.todayItems, key = { it.id }) { item ->
                        ActiveTodoCard(item, { onEdit(item) }, { onCompleteTodo(item) })
                    }
                }
                item { SectionTitle("计划中") }
                if (uiState.upcomingItems.isEmpty()) {
                    item { EmptyStateCard("后续时间暂时没有新计划。") }
                } else {
                    items(uiState.upcomingItems, key = { it.id }) { item ->
                        ActiveTodoCard(item, { onEdit(item) }, { onCompleteTodo(item) })
                    }
                }
            }
            DashboardSection.HISTORY -> {
                if (uiState.completedItems.isEmpty()) {
                    item { EmptyStateCard("完成后的任务会保存在这里。") }
                } else {
                    items(uiState.completedItems, key = { it.id }) { item ->
                        CompletedTodoCard(item, { onEdit(item) }, { onRestoreTodo(item) })
                    }
                }
            }
            DashboardSection.SETTINGS -> item {
                SettingsPanel(
                    permissions = permissions,
                    selectedThemeMode = uiState.settings.themeMode,
                    defaultSnooze = uiState.settings.defaultSnoozeMinutes,
                    crashLog = permissions.lastCrashLog,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                    onRequestFullScreenPermission = onRequestFullScreenPermission,
                    onRequestNotificationPolicyAccess = onRequestNotificationPolicyAccess,
                    onRequestIgnoreBatteryOptimization = onRequestIgnoreBatteryOptimization,
                    onRequestAccessibilityService = onRequestAccessibilityService,
                    onThemeModeChange = onThemeModeChange,
                    onDefaultSnoozeChange = onDefaultSnoozeChange,
                    onCopyCrashLog = permissions.copyCrashLog,
                    onClearCrashLog = permissions.clearCrashLog
                )
            }
        }
    }
}

@Composable
private fun GreetingCard() {
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
            Text(
                text = "${timeGreeting()}，Payki",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = PaykiGreetingFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    quote,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        quoteIndex += 1
                        quote = DAILY_QUOTES[quoteIndex % DAILY_QUOTES.size]
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
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
    ) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun EmptyStateCard(text: String) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
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
                cubicTo(size.width * 0.12f, size.height * 0.52f, size.width * 0.28f, size.height * 0.48f, size.width * 0.42f, size.height * 0.54f)
                cubicTo(size.width * 0.58f, size.height * 0.62f, size.width * 0.74f, size.height * 0.44f, size.width, size.height * 0.56f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainBack, color = Color(0x886B8A7A))

            val mountainMid = Path().apply {
                moveTo(0f, size.height * 0.72f)
                cubicTo(size.width * 0.16f, size.height * 0.66f, size.width * 0.32f, size.height * 0.63f, size.width * 0.5f, size.height * 0.71f)
                cubicTo(size.width * 0.67f, size.height * 0.78f, size.width * 0.84f, size.height * 0.66f, size.width, size.height * 0.73f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainMid, color = Color(0xAA466259))

            val mountainFront = Path().apply {
                moveTo(0f, size.height * 0.8f)
                cubicTo(size.width * 0.14f, size.height * 0.76f, size.width * 0.34f, size.height * 0.75f, size.width * 0.46f, size.height * 0.83f)
                cubicTo(size.width * 0.62f, size.height * 0.91f, size.width * 0.82f, size.height * 0.81f, size.width, size.height * 0.84f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainFront, color = Color(0xFF2A3F42))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x220C1F24), Color(0x5521372E), Color(0x330A1114))
                    )
                )
        )

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
    "专注当下这一步，进度自然会出现。",
    "先完成，再优化，今天就会更稳。",
    "把最重要的一件事先做完。",
    "少一点犹豫，多一点推进。",
    "行动会缩小焦虑，拖延会放大焦虑。",
    "DDL 不是压力，是方向。",
    "不用一次做完，只要继续往前。",
    "把任务拆小，完成感会回来。",
    "先处理事实，再处理情绪。",
    "持续推进，本身就是能力。"
)
