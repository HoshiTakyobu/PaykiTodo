package com.example.todoalarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.todoalarm.R
import com.example.todoalarm.sync.AccountAuthResult
import kotlinx.coroutines.launch

private enum class AccountAuthMode {
    LOGIN,
    REGISTER,
    RECOVER
}

@Composable
fun AccountAuthScreen(
    onLogin: suspend (String, String, String?) -> AccountAuthResult,
    onRegister: suspend (String, String, String, String?) -> AccountAuthResult,
    onRecover: suspend (String, String, String, String?) -> String?,
    onRecoveryCode: (String) -> Unit,
    turnstileEnabled: Boolean = false,
    turnstileChallengeUrl: String = "",
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
    var turnstileToken by remember { mutableStateOf<String?>(null) }
    var turnstileReloadKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val darkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(
                id = if (darkMode) R.drawable.dashboard_bg_dark else R.drawable.dashboard_bg_light
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (darkMode) Color(0x990A151D) else Color(0x44FFFFFF),
                            if (darkMode) Color(0xCC0A151D) else Color(0x66FFFFFF),
                            if (darkMode) Color(0xEE0A151D) else Color(0x99FFFFFF),
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_art_transparent),
                        contentDescription = "PaykiTodo 图标",
                        modifier = Modifier
                            .size(82.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("PaykiTodo", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (mode) {
                            AccountAuthMode.LOGIN -> "登录后继续使用你的待办和日程"
                            AccountAuthMode.REGISTER -> "创建 PaykiTodo 账号"
                            AccountAuthMode.RECOVER -> "使用恢复码重置密码"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))

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

            if (turnstileEnabled && turnstileChallengeUrl.isNotBlank()) {
                Text(
                    text = "请完成人机验证",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                TurnstileChallenge(
                    challengeUrl = turnstileChallengeUrl,
                    reloadKey = turnstileReloadKey,
                    onToken = { turnstileToken = it; errorMessage = null },
                    onError = {
                        turnstileToken = null
                        errorMessage = "人机验证加载失败，请检查网络后重试。"
                    },
                )
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
                                if (!agreed) AccountAuthResult("请先勾选用户协议") else onLogin(username, password, turnstileToken)
                            }
                            AccountAuthMode.REGISTER -> {
                                when {
                                    !agreed -> AccountAuthResult("请先勾选用户协议")
                                    password != passwordConfirmation -> AccountAuthResult("两次输入的密码不一致")
                                    else -> onRegister(username, displayName, password, turnstileToken)
                                }
                            }
                            AccountAuthMode.RECOVER -> {
                                if (password != passwordConfirmation) {
                                    AccountAuthResult("两次输入的密码不一致")
                                } else {
                                    val error = onRecover(username, recoveryCode, password, turnstileToken)
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
                        turnstileToken = null
                        if (turnstileEnabled) turnstileReloadKey += 1
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && username.isNotBlank() && password.isNotBlank() &&
                    (!turnstileEnabled || !turnstileToken.isNullOrBlank()),
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
                        TextButton(enabled = !busy, onClick = {
                            mode = AccountAuthMode.REGISTER
                            errorMessage = null
                            turnstileToken = null
                            turnstileReloadKey += 1
                        }) { Text("注册账号") }
                        TextButton(enabled = !busy, onClick = {
                            mode = AccountAuthMode.RECOVER
                            errorMessage = null
                            turnstileToken = null
                            turnstileReloadKey += 1
                        }) { Text("忘记密码") }
                    }
                    else -> TextButton(enabled = !busy, onClick = {
                        mode = AccountAuthMode.LOGIN
                        errorMessage = null
                        turnstileToken = null
                        turnstileReloadKey += 1
                    }) { Text("返回登录") }
                }
            }
                }
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
