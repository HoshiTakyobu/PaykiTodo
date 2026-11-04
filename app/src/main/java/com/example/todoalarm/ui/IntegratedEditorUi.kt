package com.example.todoalarm.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun IntegratedEditorText(value: String, onValueChange: (String) -> Unit, placeholder: String, headline: Boolean = false) {
    val style = if (headline) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
    BasicTextField(value = value, onValueChange = onValueChange,
        textStyle = style.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), maxLines = if (headline) 3 else 6,
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = 8.dp).semantics { contentDescription = placeholder },
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
                field()
            }
        })
}
