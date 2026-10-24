package com.example.todoalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun LoginServerDialog(
    currentServerUrl: String,
    defaultServerUrl: String,
    onDismiss: () -> Unit,
    onSelect: suspend (String) -> String?,
) {
    var address by remember(currentServerUrl) { mutableStateOf(currentServerUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("登录服务器") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("服务器更换或登录过期时，可在这里选择新的地址。完成后再登录，本机待办和日程会保留。", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; error = null },
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://服务器域名") },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                )
                TextButton(enabled = !busy, onClick = { address = defaultServerUrl; error = null }) { Text("使用默认服务器") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && address.isNotBlank(), onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        error = onSelect(address.trim().trimEnd('/'))
                        if (error == null) onDismiss()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = failure.message ?: "服务器地址无效"
                    } finally {
                        busy = false
                    }
                }
            }) { Text(if (busy) "处理中" else "完成") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("取消") } },
    )
}
