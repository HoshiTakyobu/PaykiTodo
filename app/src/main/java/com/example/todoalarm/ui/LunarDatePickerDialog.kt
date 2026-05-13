package com.example.todoalarm.ui

import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.todoalarm.data.LunarCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun LunarDatePickerDialog(
    title: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val initialLunar = remember(initialDate) { LunarCalendar.labelFor(initialDate) }
    val initialLunarYear = remember(initialDate, initialLunar) {
        (initialDate.year - 1..initialDate.year + 1).firstOrNull { candidateYear ->
            LunarCalendar.findDate(candidateYear, initialLunar.month, initialLunar.day, initialLunar.isLeapMonth) == initialDate
        } ?: initialDate.year
    }
    var year by remember(initialDate) { mutableStateOf(initialLunarYear) }
    var month by remember(initialDate) { mutableStateOf(initialLunar.month) }
    var day by remember(initialDate) { mutableStateOf(initialLunar.day) }
    var isLeapMonth by remember(initialDate) { mutableStateOf(initialLunar.isLeapMonth) }
    LaunchedEffect(month) {
        if (day > 30) day = 30
    }
    val candidateDate = remember(year, month, day, isLeapMonth) {
        LunarCalendar.findDate(year, month, day, isLeapMonth)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "当前：${formatLunarPickerDateTitle(initialDate)} · ${initialDate.dayOfWeek.shortLabelForLunarPicker()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    LunarWheelColumn(
                        value = year,
                        range = 1900..2100,
                        label = { Text("年份") },
                        modifier = Modifier.weight(1.2f),
                        format = { it.toString() },
                        onValueChange = { year = it }
                    )
                    LunarWheelColumn(
                        value = month,
                        range = 1..12,
                        label = { Text("月") },
                        modifier = Modifier.weight(0.8f),
                        format = { it.toString().padStart(2, '0') },
                        onValueChange = { month = it }
                    )
                    LunarWheelColumn(
                        value = day,
                        range = 1..30,
                        label = { Text("日") },
                        modifier = Modifier.weight(0.8f),
                        format = { it.toString().padStart(2, '0') },
                        onValueChange = { day = it }
                    )
                }
                FilterChip(
                    selected = isLeapMonth,
                    onClick = { isLeapMonth = !isLeapMonth },
                    label = { Text("闰月") }
                )
                if (candidateDate == null) {
                    Text(
                        text = "没有找到对应公历日期，请检查年份、月份、日期或闰月设置。",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "对应公历：${formatLunarPickerDateTitle(candidateDate)} · ${candidateDate.dayOfWeek.shortLabelForLunarPicker()}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = candidateDate != null,
                onClick = { candidateDate?.let(onConfirm) }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun LunarWheelColumn(
    value: Int,
    range: IntRange,
    label: @Composable () -> Unit,
    format: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        label()
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(138.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    wrapSelectorWheel = true
                    minValue = range.first
                    maxValue = range.last
                    setFormatter { picked -> format(picked) }
                    this.value = value.coerceIn(range.first, range.last)
                    setOnValueChangedListener { _, _, newValue -> onValueChange(newValue) }
                }
            },
            update = { picker ->
                picker.setOnValueChangedListener(null)
                picker.wrapSelectorWheel = true
                if (picker.minValue != range.first) picker.minValue = range.first
                if (picker.maxValue != range.last) picker.maxValue = range.last
                picker.setFormatter { picked -> format(picked) }
                val clamped = value.coerceIn(range.first, range.last)
                if (picker.value != clamped) picker.value = clamped
                picker.setOnValueChangedListener { _, _, newValue -> onValueChange(newValue) }
            }
        )
    }
}

private fun formatLunarPickerDateTitle(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))
}

private fun DayOfWeek.shortLabelForLunarPicker(): String = when (this) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}
