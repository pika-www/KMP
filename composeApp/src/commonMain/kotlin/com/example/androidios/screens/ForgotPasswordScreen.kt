package com.example.androidios.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.androidios.components.CustomTextField
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.text.input.VisualTransformation


@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current // 获取焦点管理器

    var account by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf(false) }

    val performReset: () -> Unit = {
        if (account.isBlank() || code.isBlank() || pwd.isBlank() || confirmPwd.isBlank()) {
            error = "请填写完整信息"
        } else if (pwd != confirmPwd) {
            error = "两次输入的密码不一致"
        } else {
            isLoading = true
            error = ""
            // 这里执行重置密码逻辑
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            // 点击背景收起键盘
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            },
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "重置密码",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- 手机号或邮箱 ---
                CustomTextField(
                    value = account,
                    onValueChange = {
                        account = it
                        error = ""
                    },
                    label = "手机号或邮箱",
                    leadingIcon = Icons.Default.Person,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(12.dp))

                CustomTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = "验证码",
                    leadingIcon = Icons.Default.LockOpen,
                    // --- 新增参数 ---
                    isVerificationCode = true,
                    onSendCode = {
                        // 在这里调用您的发送验证码接口
                        println("正在向 $account 发送验证码...")
                    },
                    // ----------------
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )


                Spacer(modifier = Modifier.height(12.dp))

                // --- 新密码 ---
                CustomTextField(
                    value = pwd,
                    onValueChange = {
                        pwd = it
                        error = ""
                    },
                    label = "新密码",
                    leadingIcon = Icons.Default.Lock,
                    enabled = !isLoading,
                    // --- 修复点：根据状态切换可见性 ---
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    // ----------------
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

// --- 确认新密码 ---
                CustomTextField(
                    value = confirmPwd,
                    onValueChange = {
                        confirmPwd = it
                        error = ""
                    },
                    label = "确认新密码",
                    leadingIcon = Icons.Default.Lock,
                    enabled = !isLoading,
                    // --- 修复点：同样应用可见性逻辑 ---
                    visualTransformation = if (confirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPassword = !confirmPassword }) {
                            Icon(
                                imageVector = if (confirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    // ----------------
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { performReset() })
                )

                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = performReset,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("重置并登录", style = MaterialTheme.typography.titleMedium)
                    }
                }

                TextButton(onClick = onBackToLogin, enabled = !isLoading) {
                    Text("返回登录")
                }
            }
        }
    }
}
