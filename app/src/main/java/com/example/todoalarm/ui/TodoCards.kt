package com.example.todoalarm.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.theme.Clay
import com.example.todoalarm.ui.theme.Leaf
import com.example.todoalarm.ui.theme.Ocean
import com.example.todoalarm.ui.theme.Signal
import kotlinx.coroutines.delay

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun ActiveTodoCard(
    item: TodoItem,
    groups: List<TaskGroup>,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var completing by remember(item.id) { mutableStateOf(false) }
    var showDetails by remember(item.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember(item.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val progress by animateFloatAsState(targetValue = if (completing) 1f else 0f, label = "complete_progress")

    LaunchedEffect(completing) {
        if (completing) {
            delay(650)
            onComplete()
        }
    }

    TodoCardShell(
        item = item,
        groups = groups,
        onClick = { showDetails = true },
        onLongClick = null,
        contentClickable = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.padding(top = 2.dp), contentAlignment = Alignment.Center) {
                Checkbox(
                    checked = completing,
                    onCheckedChange = { if (it && !completing) completing = true },
                    modifier = Modifier.graphicsLayer(scaleX = 1.18f, scaleY = 1.18f),
                    enabled = !completing
                )
                Firework(progress)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = { showDetails = true },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showDeleteConfirm = true
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TitleRow(item = item, groups = groups, progress = progress)
                if (item.notes.isNotBlank()) {
                    StrikeText(
                        text = item.notes,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                        progress = progress,
                        color = if (item.missed) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.hasDueDate) {
                    DeadlineMeta(
                        value = formatLocalDateTime(reminderAtMillisToDateTime(item.dueAtMillis)),
                        accent = if (item.missed) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                    )
                }
                if (item.isRecurring) {
                    Text(
                        text = "循环任务",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDetails) {
        TodoDetailsDialog(
            item = item,
            groups = groups,
            onDismiss = { showDetails = false },
            showCreated = true,
            showStatusTime = false,
            onEdit = {
                showDetails = false
                onEdit()
            },
            onCancel = {
                showDetails = false
                onCancel()
            }
        )
    }

    if (showDeleteConfirm) {
        PaykiDecisionBottomSheet(
            title = "删除任务",
            message = "确定删除“${item.title}”？删除后无法恢复。",
            confirmLabel = "删除",
            confirmLabelColor = Color(0xFFD14343),
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            }
        )
    }
}

@Composable
internal fun CompletedTodoCard(
    item: TodoItem,
    groups: List<TaskGroup>,
    onEdit: () -> Unit,
    onRestore: () -> Unit
) {
    var showDetails by remember(item.id) { mutableStateOf(false) }

    TodoCardShell(item = item, groups = groups, onClick = { showDetails = true }) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TitleRow(item = item, groups = groups, progress = 0f)
            if (item.notes.isNotBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showDetails) {
        TodoDetailsDialog(
            item = item,
            groups = groups,
            onDismiss = { showDetails = false },
            showCreated = true,
            showStatusTime = true,
            onEdit = {
                showDetails = false
                onEdit()
            },
            onRestore = {
                showDetails = false
                onRestore()
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TodoCardShell(
    item: TodoItem,
    groups: List<TaskGroup>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    contentClickable: Boolean = true,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(categoryColor(resolveTaskGroup(item, groups)))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (contentClickable) {
                            Modifier.combinedClickable(
                                onClick = onClick,
                                onLongClick = onLongClick
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun TitleRow(
    item: TodoItem,
    groups: List<TaskGroup>,
    progress: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryChip(resolveTaskGroup(item, groups))
        StrikeTitle(
            text = item.title,
            progress = progress,
            textColor = if (item.missed) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CategoryChip(group: ResolvedTaskGroup) {
    val tint = categoryColor(group)
    Surface(shape = RoundedCornerShape(12.dp), color = tint.copy(alpha = 0.12f)) {
        Text(
            text = group.name,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            color = tint,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MetaLine(
    icon: String,
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$icon $label",
            color = accent,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = accent.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DeadlineMeta(value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⏰ DDL",
            color = accent,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = accent,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StrikeTitle(
    text: String,
    progress: Float,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    StrikeText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp),
        fontWeight = FontWeight.Bold,
        progress = progress,
        color = textColor,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TodoDetailsDialog(
    item: TodoItem,
    groups: List<TaskGroup>,
    onDismiss: () -> Unit,
    showCreated: Boolean,
    showStatusTime: Boolean,
    onEdit: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null
) {
    val group = resolveTaskGroup(item, groups)
    val tint = if (item.missed) Color(0xFFC62828) else categoryColor(group)

    PaykiBottomSheet(
        onDismiss = onDismiss,
        showDragHandle = false,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    onCancel?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Rounded.Close, contentDescription = "取消任务", tint = Color(0xFFD14343))
                        }
                    }
                    onRestore?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "恢复", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    onEdit?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Rounded.Edit, contentDescription = "修改", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(20.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = tint
                ) {}
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = item.title,
                        color = tint,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = group.name,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))

            item.notes.takeIf { it.isNotBlank() }?.let {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(14.dp),
                        color = if (item.missed) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            if (item.hasDueDate) {
                TodoInfoRow(label = "DDL", value = formatLocalDateTime(reminderAtMillisToDateTime(item.dueAtMillis)))
            } else {
                TodoInfoRow(label = "DDL", value = "未设置")
            }
            if (showCreated) {
                TodoInfoRow(label = "创建", value = formatLocalDateTime(reminderAtMillisToDateTime(item.createdAtMillis)))
            }
            if (showStatusTime) {
                TodoInfoRow(
                    label = if (item.completed) "完成" else "取消",
                    value = (item.completedAtMillis ?: item.canceledAtMillis)?.let {
                        formatLocalDateTime(reminderAtMillisToDateTime(it))
                    } ?: "未记录"
                )
            }
        }
    }
}

@Composable
private fun TodoInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(52.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun StrikeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    progress: Float = 0f,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    var layoutResult by remember(text, style, fontWeight, maxLines, overflow) {
        mutableStateOf<TextLayoutResult?>(null)
    }
    val strikeColor = lerp(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onSurfaceVariant, progress)

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            drawContent()
            val layout = layoutResult ?: return@drawWithContent
            if (progress <= 0f) return@drawWithContent

            val lineCount = layout.lineCount
            if (lineCount == 0) return@drawWithContent

            repeat(lineCount) { index ->
                val lineLength = (layout.getLineRight(index) - layout.getLineLeft(index)).coerceAtLeast(0f)
                if (lineLength <= 0f) return@repeat
                val lineTop = layout.getLineTop(index)
                val lineBottom = layout.getLineBottom(index)
                val y = lineTop + (lineBottom - lineTop) * 0.58f
                val left = layout.getLineLeft(index)
                val drawLength = lineLength * progress
                drawLine(
                    color = strikeColor,
                    start = Offset(left, y),
                    end = Offset(left + drawLength, y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        },
        style = style,
        fontWeight = fontWeight,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { layoutResult = it },
        textAlign = TextAlign.Start
    )
}

@Composable
private fun Firework(progress: Float) {
    if (progress <= 0f || progress >= 1f) return
    Canvas(modifier = Modifier.size(44.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val colors = listOf(Signal, Ocean, Clay, Leaf)
        val radius = size.minDimension * 0.52f * progress
        repeat(8) { index ->
            val angle = Math.toRadians((index * 45.0) - 90.0)
            drawCircle(
                color = colors[index % colors.size].copy(alpha = 1f - progress),
                radius = size.minDimension * 0.06f * (1.15f - progress * 0.5f),
                center = Offset(
                    x = center.x + kotlin.math.cos(angle).toFloat() * radius,
                    y = center.y + kotlin.math.sin(angle).toFloat() * radius
                )
            )
        }
    }
}

private fun categoryColor(group: ResolvedTaskGroup): Color {
    return runCatching { colorFromHex(group.colorHex) }.getOrElse {
        when (group.name) {
            "重要" -> Clay
            "紧急" -> Signal
            "专注" -> Ocean
            else -> Leaf
        }
    }
}
