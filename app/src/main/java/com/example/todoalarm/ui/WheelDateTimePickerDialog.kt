package com.example.todoalarm.ui

import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun WheelDateTimePickerDialog(
    title: String,
    initialDateTime: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit
) {
    val currentYear = LocalDate.now().year
    val minYear = remember(initialDateTime, currentYear) { min(initialDateTime.year - 5, currentYear - 10) }
    val maxYear = remember(initialDateTime, currentYear) { max(initialDateTime.year + 5, currentYear + 10) }

    var year by remember(initialDateTime) { mutableStateOf(initialDateTime.year) }
    var month by remember(initialDateTime) { mutableStateOf(initialDateTime.monthValue) }
    var day by remember(initialDateTime) { mutableStateOf(initialDateTime.dayOfMonth) }
    var hour by remember(initialDateTime) { mutableStateOf(initialDateTime.hour) }
    var minute by remember(initialDateTime) { mutableStateOf(initialDateTime.minute) }

    val maxDay = remember(year, month) { YearMonth.of(year, month).lengthOfMonth() }
    LaunchedEffect(year, month, maxDay) {
        if (day > maxDay) day = maxDay
    }

    val result = remember(year, month, day, hour, minute) {
        LocalDateTime.of(year, month, day.coerceAtMost(maxDay), hour, minute)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = formatLocalDateTime(result),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    WheelNumberPickerColumn(
                        modifier = Modifier.weight(1.25f),
                        label = "年",
                        value = year,
                        range = minYear..maxYear,
                        format = { it.toString() },
                        onValueChange = { year = it }
                    )
                    WheelNumberPickerColumn(
                        modifier = Modifier.weight(1f),
                        label = "月",
                        value = month,
                        range = 1..12,
                        format = { it.toString().padStart(2, '0') },
                        onValueChange = { month = it }
                    )
                    WheelNumberPickerColumn(
                        modifier = Modifier.weight(1f),
                        label = "日",
                        value = day.coerceAtMost(maxDay),
                        range = 1..maxDay,
                        format = { it.toString().padStart(2, '0') },
                        onValueChange = { day = it }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    WheelNumberPickerColumn(
                        modifier = Modifier.weight(1f),
                        label = "时",
                        value = hour,
                        range = 0..23,
                        format = { it.toString().padStart(2, '0') },
                        onValueChange = { hour = it }
                    )
                    WheelNumberPickerColumn(
                        modifier = Modifier.weight(1f),
                        label = "分",
                        value = minute,
                        range = 0..59,
                        format = { it.toString().padStart(2, '0') },
                        onValueChange = { minute = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(result) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun WheelNumberPickerColumn(
    label: String,
    value: Int,
    range: IntRange,
    format: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    wrapSelectorWheel = range.last > range.first
                    minValue = range.first
                    maxValue = range.last
                    setFormatter { picked -> format(picked) }
                    this.value = value.coerceIn(range.first, range.last)
                    setOnValueChangedListener { _, _, newValue -> onValueChange(newValue) }
                }
            },
            update = { picker ->
                picker.setOnValueChangedListener(null)
                picker.wrapSelectorWheel = range.last > range.first
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
