package com.example.todoalarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.todoalarm.sync.AccountAuthResult
import kotlinx.coroutines.launch

private enum class AccountAuthMode {
    LOGIN,
    REGISTER,
    RECOVER
}

@Composable
fun AccountAuthScreen(
    onLogin: suspend (String, String) -> AccountAuthResult,
    onRegister: suspend (String, String, String) -> AccountAuthResult,
    onRecover: suspend (String, String, String) -> String?,
    onRecoveryCode: (String) -> Unit,
    onExportMigration: () -> Unit = {},
    onImportMigration: () -> Unit = {},
) {
    var mode by remember { mutableStateOf(AccountAuthMode.LOGIN) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    var recoveryCode by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var agreementVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("PaykiTodo", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                when (mode) {
                    AccountAuthMode.LOGIN -> "登录后继续使用你的待办和日程"
                    AccountAuthMode.REGISTER -> "创建 PaykiTodo 账号"
                    AccountAuthMode.RECOVER -> "使用恢复码重置密码"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("账号") },
                enabled = !busy,
            )
            if (mode == AccountAuthMode.REGISTER) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("显示名") },
                    enabled = !busy,
                )
            }
            if (mode == AccountAuthMode.RECOVER) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = recoveryCode,
                    onValueChange = { recoveryCode = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("恢复码") },
                    placeholder = { Text("注册或首次登录时显示的 16 位代码") },
                    enabled = !busy,
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(if (mode == AccountAuthMode.RECOVER) "新密码" else "密码") },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !busy,
            )
            if (mode == AccountAuthMode.REGISTER || mode == AccountAuthMode.RECOVER) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = passwordConfirmation,
                    onValueChange = { passwordConfirmation = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("确认密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !busy,
                )
            }

            if (mode != AccountAuthMode.RECOVER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clickable(enabled = !busy) { agreed = !agreed },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = agreed, onCheckedChange = { agreed = it }, enabled = !busy)
                    Spacer(Modifier.width(4.dp))
                    Text("我已阅读并同意")
                    TextButton(onClick = { agreementVisible = true }, enabled = !busy) { Text("用户协议") }
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        errorMessage = null
                        val result = when (mode) {
                            AccountAuthMode.LOGIN -> {
                                if (!agreed) AccountAuthResult("请先勾选用户协议") else onLogin(username, password)
                            }
                            AccountAuthMode.REGISTER -> {
                                when {
                                    !agreed -> AccountAuthResult("请先勾选用户协议")
                                    password != passwordConfirmation -> AccountAuthResult("两次输入的密码不一致")
                                    else -> onRegister(username, displayName, password)
                                }
                            }
                            AccountAuthMode.RECOVER -> {
                                if (password != passwordConfirmation) {
                                    AccountAuthResult("两次输入的密码不一致")
                                } else {
                                    val error = onRecover(username, recoveryCode, password)
                                    AccountAuthResult(error)
                                }
                            }
                        }
                        if (result.error != null) {
                            errorMessage = result.error
                        } else {
                            result.recoveryCode?.let(onRecoveryCode)
                            if (mode == AccountAuthMode.RECOVER) {
                                mode = AccountAuthMode.LOGIN
                                password = ""
                                passwordConfirmation = ""
                                recoveryCode = ""
                            }
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && username.isNotBlank() && password.isNotBlank(),
            ) {
                if (busy) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(
                    when (mode) {
                        AccountAuthMode.LOGIN -> "登录"
                        AccountAuthMode.REGISTER -> "注册并登录"
                        AccountAuthMode.RECOVER -> "重置密码"
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                when (mode) {
                    AccountAuthMode.LOGIN -> {
                        TextButton(enabled = !busy, onClick = { mode = AccountAuthMode.REGISTER; errorMessage = null }) { Text("注册账号") }
                        TextButton(enabled = !busy, onClick = { mode = AccountAuthMode.RECOVER; errorMessage = null }) { Text("忘记密码") }
                    }
                    else -> TextButton(enabled = !busy, onClick = { mode = AccountAuthMode.LOGIN; errorMessage = null }) { Text("返回登录") }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "需要从 Debug 版迁移数据时，可以在登录前导出或导入本机迁移包。迁移包不包含账号令牌、AI Key 或局域网密钥。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onExportMigration,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text("导出本机数据") }
                OutlinedButton(
                    onClick = onImportMigration,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text("导入迁移包") }
            }
        }
    }

    if (agreementVisible) {
        AlertDialog(
            onDismissRequest = { agreementVisible = false },
            title = { Text("PaykiTodo 用户协议") },
            text = {
                Text(
                    text = "PaykiTodo 是一个开源、以本地数据为主的个人待办工具。登录账号用于识别你的云端数据空间；手机仍保留本地数据，云端仅保存加密快照。我们不会把你的密码、AI API Key 或局域网连接密钥写入迁移包或公开仓库。\n\n你应妥善保管账号密码和恢复码。云同步采用整份快照和版本冲突保护，不承诺替代独立备份。项目源代码按仓库公开许可证发布，具体权利和责任以仓库中的 LICENSE、PRIVACY 和 SECURITY 文件为准。",
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                )
            },
            confirmButton = { TextButton(onClick = { agreementVisible = false }) { Text("我知道了") } },
        )
    }
}
