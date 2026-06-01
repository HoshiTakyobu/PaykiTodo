package com.example.todoalarm.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    resolvedGroup: ResolvedTaskGroup? = null,
    forceShowDetailsKey: Int = 0,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var completing by remember(item.id) { mutableStateOf(false) }
    var showDetails by remember(item.id) { mutableStateOf(false) }
    var showCancelConfirm by remember(item.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember(item.id) { mutableStateOf(false) }
    var showActionSheet by remember(item.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val progress by animateFloatAsState(targetValue = if (completing) 1f else 0f, label = "complete_progress")
    val displayGroup = remember(item.id, item.groupId, item.categoryKey, item.itemType, item.allDay, item.accentColorHex, resolvedGroup, groups) {
        resolvedGroup ?: resolveTaskGroup(item, groups)
    }

    LaunchedEffect(completing) {
        if (completing) {
            delay(650)
            onComplete()
        }
    }
    LaunchedEffect(forceShowDetailsKey) {
        if (forceShowDetailsKey > 0) {
            showDetails = true
        }
    }

    TodoCardShell(
        group = displayGroup,
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
                LightweightCompletionToggle(
                    checked = completing,
                    enabled = !completing,
                    onCheck = { completing = true }
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
                            showActionSheet = true
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TitleRow(item = item, group = displayGroup, progress = progress)
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
            },
            onDelete = {
                showDetails = false
                onDelete()
            }
        )
    }

    if (showActionSheet) {
        PaykiBottomSheet(onDismiss = { showActionSheet = false }, showDragHandle = true) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
                TodoActionRow(
                    icon = Icons.Rounded.Close,
                    title = "取消待办",
                    tint = Color(0xFFD97706),
                    onClick = {
                        showActionSheet = false
                        showCancelConfirm = true
                    }
                )
                TodoActionRow(
                    icon = Icons.Rounded.Delete,
                    title = "删除",
                    tint = Color(0xFFD14343),
                    onClick = {
                        showActionSheet = false
                        showDeleteConfirm = true
                    }
                )
            }
        }
    }

    if (showCancelConfirm) {
        PaykiDecisionBottomSheet(
            title = "取消待办",
            message = "取消后会进入历史记录，后续提醒会停止；删除则会直接移除，不进入历史记录。",
            confirmLabel = "取消待办",
            confirmLabelColor = Color(0xFFD97706),
            onDismiss = { showCancelConfirm = false },
            onConfirm = {
                showCancelConfirm = false
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
private fun LightweightCompletionToggle(
    checked: Boolean,
    enabled: Boolean,
    onCheck: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(enabled = enabled, onClick = onCheck),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val strokeWidth = 2.2.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val ringColor = if (checked) primary else outline.copy(alpha = if (enabled) 0.72f else 0.32f)
            drawCircle(
                color = ringColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            if (checked) {
                drawLine(
                    color = primary,
                    start = Offset(size.width * 0.30f, size.height * 0.52f),
                    end = Offset(size.width * 0.44f, size.height * 0.66f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primary,
                    start = Offset(size.width * 0.44f, size.height * 0.66f),
                    end = Offset(size.width * 0.72f, size.height * 0.34f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun TodoActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Text(title, color = tint, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
internal fun CompletedTodoCard(
    item: TodoItem,
    groups: List<TaskGroup>,
    onEdit: () -> Unit,
    onRestore: () -> Unit,
    resolvedGroup: ResolvedTaskGroup? = null
) {
    var showDetails by remember(item.id) { mutableStateOf(false) }
    val displayGroup = remember(item.id, item.groupId, item.categoryKey, item.itemType, item.allDay, item.accentColorHex, resolvedGroup, groups) {
        resolvedGroup ?: resolveTaskGroup(item, groups)
    }

    TodoCardShell(group = displayGroup, onClick = { showDetails = true }) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TitleRow(item = item, group = displayGroup, progress = 0f)
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
    group: ResolvedTaskGroup,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    contentClickable: Boolean = true,
    content: @Composable () -> Unit
) {
    val accent = categoryColor(group)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = accent,
                        size = Size(width = 8.dp.toPx(), height = size.height)
                    )
                }
        ) {
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
                    .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun TitleRow(
    item: TodoItem,
    group: ResolvedTaskGroup,
    progress: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryChip(group)
        if (item.hiddenFromBoard) {
            HiddenReminderChip()
        }
        StrikeTitle(
            text = item.title,
            progress = progress,
            textColor = if (item.missed) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HiddenReminderChip() {
    val tint = MaterialTheme.colorScheme.primary
    Surface(shape = RoundedCornerShape(12.dp), color = tint.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = tint
            )
            Text(
                text = "仅提醒",
                color = tint,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
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
internal fun TodoDetailsDialog(
    item: TodoItem,
    groups: List<TaskGroup>,
    onDismiss: () -> Unit,
    showCreated: Boolean,
    showStatusTime: Boolean,
    onEdit: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null
) {
    val group = resolveTaskGroup(item, groups)
    val tint = if (item.missed) Color(0xFFC62828) else categoryColor(group)
    var confirmCancel by remember(item.id) { mutableStateOf(false) }
    var confirmDelete by remember(item.id) { mutableStateOf(false) }
    val requestCancel = {
        if (onCancel != null) {
            confirmCancel = true
        }
    }
    val requestDelete = {
        if (onDelete != null) {
            confirmDelete = true
        }
    }

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
                        TextButton(onClick = requestCancel) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFD97706)
                            )
                            Text("取消待办", color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    onDelete?.let {
                        IconButton(onClick = requestDelete) {
                            Icon(Icons.Rounded.Delete, contentDescription = "删除待办", tint = Color(0xFFD14343))
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
            if (item.hiddenFromBoard) {
                TodoInfoRow(label = "显示", value = "仅提醒，不在看板/日历显示")
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                onCancel?.let {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = requestCancel
                    ) {
                        Text("取消待办（归档）")
                    }
                }
                onDelete?.let {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = requestDelete
                    ) {
                        Text("删除")
                    }
                }
                onRestore?.let {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = it
                    ) {
                        Text("恢复")
                    }
                }
                onEdit?.let {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = it
                    ) {
                        Text("修改")
                    }
                }
            }
        }
    }

    if (confirmCancel && onCancel != null) {
        PaykiDecisionBottomSheet(
            title = "取消待办",
            message = "取消后会停止提醒，并进入历史记录；这不是删除，后续可以在历史记录里查看。",
            confirmLabel = "取消待办",
            confirmLabelColor = Color(0xFFD97706),
            onDismiss = { confirmCancel = false },
            onConfirm = {
                confirmCancel = false
                onCancel()
            }
        )
    }

    if (confirmDelete && onDelete != null) {
        PaykiDecisionBottomSheet(
            title = "删除待办",
            message = "删除后会直接移除，不进入历史记录，也无法恢复。",
            confirmLabel = "删除",
            confirmLabelColor = Color(0xFFD14343),
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            }
        )
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
    if (progress <= 0f) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            fontWeight = fontWeight,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = TextAlign.Start
        )
        return
    }

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
            else -> Leaf
        }
    }
}
