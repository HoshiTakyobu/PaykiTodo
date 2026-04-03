package com.example.todoalarm.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoalarm.data.TodoCategory
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.theme.Clay
import com.example.todoalarm.ui.theme.Leaf
import com.example.todoalarm.ui.theme.Ocean
import com.example.todoalarm.ui.theme.Signal
import kotlinx.coroutines.delay

@Composable
internal fun ActiveTodoCard(
    item: TodoItem,
    onEdit: () -> Unit,
    onComplete: () -> Unit
) {
    var completing by remember(item.id) { mutableStateOf(false) }
    var showDetails by remember(item.id) { mutableStateOf(false) }
    val progress by animateFloatAsState(targetValue = if (completing) 1f else 0f, label = "complete_progress")

    LaunchedEffect(completing) {
        if (completing) {
            delay(650)
            onComplete()
        }
    }

    TodoCardShell(item = item, onClick = { showDetails = true }) {
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

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TitleRow(item = item, progress = progress)
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DeadlineMeta(formatLocalDateTime(reminderAtMillisToDateTime(item.dueAtMillis)))
            }

            IconButton(onClick = onEdit, enabled = !completing) {
                Icon(Icons.Rounded.Edit, contentDescription = "编辑任务")
            }
        }
    }

    if (showDetails) {
        TodoDetailsDialog(item = item, onDismiss = { showDetails = false }, showCreated = false, showCompleted = false)
    }
}

@Composable
internal fun CompletedTodoCard(
    item: TodoItem,
    onEdit: () -> Unit,
    onRestore: () -> Unit
) {
    var showDetails by remember(item.id) { mutableStateOf(false) }

    TodoCardShell(item = item, onClick = { showDetails = true }) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TitleRow(item = item, progress = 0f)
            if (item.notes.isNotBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MetaLine(
                icon = "🗓",
                label = "创建",
                value = formatLocalDateTime(reminderAtMillisToDateTime(item.createdAtMillis)),
                accent = MaterialTheme.colorScheme.secondary
            )
            DeadlineMeta(formatLocalDateTime(reminderAtMillisToDateTime(item.dueAtMillis)))
            MetaLine(
                icon = "✅",
                label = "完成",
                value = item.completedAtMillis?.let { formatLocalDateTime(reminderAtMillisToDateTime(it)) } ?: "未记录",
                accent = MaterialTheme.colorScheme.tertiary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryChip(TodoCategory.fromKey(item.categoryKey))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "编辑历史任务")
                    }
                    OutlinedButton(onClick = onRestore) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("恢复")
                    }
                }
            }
        }
    }

    if (showDetails) {
        TodoDetailsDialog(item = item, onDismiss = { showDetails = false }, showCreated = true, showCompleted = true)
    }
}

@Composable
private fun TodoCardShell(
    item: TodoItem,
    onClick: () -> Unit,
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
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(categoryColor(TodoCategory.fromKey(item.categoryKey)))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
    progress: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryChip(TodoCategory.fromKey(item.categoryKey))
        StrikeTitle(text = item.title, progress = progress, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CategoryChip(category: TodoCategory) {
    val tint = categoryColor(category)
    Surface(shape = RoundedCornerShape(12.dp), color = tint.copy(alpha = 0.12f)) {
        Text(
            text = category.label,
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
private fun DeadlineMeta(value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⏰ DDL",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.primary,
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
    modifier: Modifier = Modifier
) {
    val strikeColor = lerp(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onSurfaceVariant, progress)
    Text(
        text = text,
        modifier = modifier.drawWithContent {
            drawContent()
            if (progress > 0f) {
                val y = size.height * 0.58f
                drawLine(
                    color = strikeColor,
                    start = Offset(0f, y),
                    end = Offset(size.width * progress, y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        },
        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TodoDetailsDialog(
    item: TodoItem,
    onDismiss: () -> Unit,
    showCreated: Boolean,
    showCompleted: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(TodoCategory.fromKey(item.categoryKey))
                Text(item.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showCreated) {
                    MetaLine(
                        icon = "🗓",
                        label = "创建",
                        value = formatLocalDateTime(reminderAtMillisToDateTime(item.createdAtMillis)),
                        accent = MaterialTheme.colorScheme.secondary
                    )
                }
                DeadlineMeta(formatLocalDateTime(reminderAtMillisToDateTime(item.dueAtMillis)))
                if (showCompleted) {
                    MetaLine(
                        icon = "✅",
                        label = "完成",
                        value = item.completedAtMillis?.let { formatLocalDateTime(reminderAtMillisToDateTime(it)) } ?: "未记录",
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
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

private fun categoryColor(category: TodoCategory): Color = when (category) {
    TodoCategory.IMPORTANT -> Clay
    TodoCategory.URGENT -> Signal
    TodoCategory.FOCUS -> Ocean
    TodoCategory.ROUTINE -> Leaf
}
