package com.example.androidios.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
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
    // --- 修复点：保留并支持外部传入 trailingIcon ---
    trailingIcon: @Composable (() -> Unit)? = null,
    isVerificationCode: Boolean = false,
    onSendCode: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    var timeLeft by remember { mutableStateOf(0) }
    val isCountingDown = timeLeft > 0

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
        // --- 修复点：逻辑判断，优先显示验证码按钮，否则显示外部传入的图标 ---
        trailingIcon = {
            if (isVerificationCode) {
                TextButton(
                    onClick = {
                        if (!isCountingDown) {
                            onSendCode?.invoke()
                            timeLeft = 60
                        }
                    },
                    enabled = !isCountingDown && enabled,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = if (isCountingDown) "${timeLeft}s" else "发送验证码",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
                    )
                }
            } else {
                // 如果不是验证码模式，则显示外部传入的 trailingIcon（如密码切换图标）
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
