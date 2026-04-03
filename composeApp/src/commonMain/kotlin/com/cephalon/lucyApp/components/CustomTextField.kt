package com.cephalon.lucyApp.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    isVerificationCode: Boolean = false,
    // --- 修改点：onSendCode 现在接收一个回调函数，用来在异步请求成功后触发倒计时 ---
    onSendCode: ((startTimer: () -> Unit) -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    var timeLeft by remember { mutableIntStateOf(0) }
    val isCountingDown = timeLeft > 0
    var hasSentOnce by remember { mutableStateOf(false) }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft -= 1
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = {
            if (isVerificationCode) {
                TextButton(
                    onClick = {
                        if (!isCountingDown) {
                            // 调用 onSendCode，并传入一个启动倒计时的 lambda
                            onSendCode?.invoke {
                                timeLeft = 60
                                hasSentOnce = true
                            }
                        }
                    },
                    enabled = !isCountingDown && enabled,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = when {
                            isCountingDown -> "${timeLeft}s"
                            hasSentOnce -> "重新发送"
                            else -> "发送验证码"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
                    )
                }
            } else {
                trailingIcon?.invoke()
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        enabled = enabled,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

private fun normalizeChinaPhoneOrNull(input: String): String? {
    val raw = input.trim().replace(" ", "")
    val withoutPrefix = when {
        raw.startsWith("+86") -> raw.removePrefix("+86")
        raw.startsWith("86") && raw.length > 11 -> raw.removePrefix("86")
        else -> raw
    }
    val normalized = withoutPrefix.trim()
    return if (Regex("^1\\d{10}$").matches(normalized)) normalized else null
}

private fun normalizeComEmailOrNull(input: String): String? {
    val raw = input.trim()
    return if (Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.com$").matches(raw)) raw else null
}

@Composable
fun AccountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null,
    onValidationError: ((String) -> Unit)? = null,
    onValidated: ((normalized: String, isEmail: Boolean) -> Unit)? = null,
) {
    CustomTextField(
        value = value,
        onValueChange = onValueChange,
        label = "手机号或邮箱",
        leadingIcon = Icons.Default.Person,
        enabled = enabled,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                val input = value.trim()
                if (input.isBlank()) {
                    onValidationError?.invoke("请输入账号")
                    return@KeyboardActions
                }

                val normalizedEmail = normalizeComEmailOrNull(input)
                if (normalizedEmail != null) {
                    onValidated?.invoke(normalizedEmail, true)
                    onNext?.invoke()
                    return@KeyboardActions
                }

                val normalizedPhone = normalizeChinaPhoneOrNull(input)
                if (normalizedPhone != null) {
                    onValidated?.invoke(normalizedPhone, false)
                    onNext?.invoke()
                    return@KeyboardActions
                }

                onValidationError?.invoke("请输入正确的手机号(+86 11位)或邮箱(.com)")
            }
        )
    )
}

@Composable
fun EmailInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null,
    onValidationError: ((String) -> Unit)? = null,
    onValidated: ((normalized: String) -> Unit)? = null,
) {
    CustomTextField(
        value = value,
        onValueChange = onValueChange,
        label = "邮箱",
        leadingIcon = Icons.Default.Person,
        enabled = enabled,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                val normalized = normalizeComEmailOrNull(value)
                if (normalized == null) {
                    onValidationError?.invoke("请输入正确的邮箱(.com)")
                    return@KeyboardActions
                }
                onValidated?.invoke(normalized)
                onNext?.invoke()
            }
        )
    )
}

@Composable
fun CodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onSendCode: ((startTimer: () -> Unit) -> Unit)? = null,
) {
    CustomTextField(
        value = value,
        onValueChange = onValueChange,
        label = "验证码",
        leadingIcon = Icons.Default.LockOpen,
        enabled = enabled,
        modifier = modifier,
        isVerificationCode = true,
        onSendCode = onSendCode,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        )
    )
}

@Composable
fun PasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "密码",
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }

    CustomTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        leadingIcon = Icons.Default.Lock,
        enabled = enabled,
        modifier = modifier,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onDone?.invoke()
            }
        )
    )
}