package com.cephalon.lucyApp.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

val TitleColor = Color(0xFF1F2535)
val DisabledBtnColor = Color(0xFF717580)
val EnabledBtnColor = Color(0xFF1F2535)
val LinkColor = Color(0xFF0A59F7)

@Composable
fun SheetPageContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        content()
    }
}

@Composable
fun SheetBackButton(
    onClick: () -> Unit
) {
    val ds = LocalDesignScale.current
    Box(
        modifier = Modifier
            .size(ds.sm(32.dp))
            .clip(CircleShape)
            .background(Color(0x3D76768080.toInt()))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color(0xFF2D2D2D),
            modifier = Modifier.size(ds.sm(18.dp))
        )
    }
}

@Composable
fun LoginSheetContent(
    ds: DesignScale,
    sheetTitle: String,
    preferEmailLogin: Boolean,
    needsRegister: Boolean,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    verifyCode: String,
    onVerifyCodeChange: (String) -> Unit,
    isLoading: Boolean,
    canSubmit: Boolean,
    normalizedAccount: String,
    isAccountEmail: Boolean,
    onBackClick: () -> Unit,
    onFocusLostValidate: () -> Unit,
    onForgotClick: (() -> Unit)?,
    onSubmit: () -> Unit,
    onSendCode: (startTimer: () -> Unit) -> Unit,
    toastState: ToastState,
    isRegisterPage: Boolean = false,
    registerPhone: String = "",
    onRegisterPhoneChange: (String) -> Unit = {},
) {
    var hadFocus by remember { mutableStateOf(false) }

    SheetPageContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ds.sw(20.dp))
        ) {
            Spacer(modifier = Modifier.height(ds.sh(20.dp)))

            SheetBackButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(ds.sh(52.dp)))

            Text(
                text = sheetTitle,
                color = TitleColor,
                fontSize = ds.sp(28f),
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(ds.sh(24.dp)))

            if (!preferEmailLogin && !isRegisterPage) {
                // ===== 验证码登录模式：只输入手机号 + 验证码 =====
                PhoneOnlyInput(
                    value = username,
                    onValueChange = onUsernameChange,
                    enabled = !isLoading,
                    modifier = Modifier.onFocusChanged { focusState ->
                        if (hadFocus && !focusState.isFocused) {
                            onFocusLostValidate()
                        }
                        hadFocus = focusState.isFocused
                    },
                )

                Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                CodeInput(
                    value = verifyCode,
                    onValueChange = onVerifyCodeChange,
                    enabled = !isLoading,
                    onSendCode = onSendCode,
                )

                // 验证码登录 — 未注册时动画弹出密码+确认密码
                AnimatedVisibility(
                    visible = needsRegister,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                        PasswordInput(
                            value = password,
                            onValueChange = onPasswordChange,
                            enabled = !isLoading,
                            label = "设置密码",
                        )
                        Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                        PasswordInput(
                            value = confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            enabled = !isLoading,
                            label = "再次输入密码",
                        )
                    }
                }
            } else if (isRegisterPage) {
                // ===== 注册页 =====
                // 账号输入（手机号/邮箱）
                AccountInput(
                    value = username,
                    onValueChange = onUsernameChange,
                    enabled = !isLoading,
                    modifier = Modifier.onFocusChanged { focusState ->
                        if (hadFocus && !focusState.isFocused) {
                            onFocusLostValidate()
                        }
                        hadFocus = focusState.isFocused
                    },
                    onValidationError = { toastState.show(it) },
                )

                // 邮箱注册时，显示额外的手机号输入框
                AnimatedVisibility(
                    visible = isAccountEmail,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                        PhoneOnlyInput(
                            value = registerPhone,
                            onValueChange = onRegisterPhoneChange,
                            enabled = !isLoading,
                            label = "请输入手机号",
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                CodeInput(
                    value = verifyCode,
                    onValueChange = onVerifyCodeChange,
                    enabled = !isLoading,
                    onSendCode = onSendCode,
                )
                Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                PasswordInput(
                    value = password,
                    onValueChange = onPasswordChange,
                    enabled = !isLoading,
                    label = "设置密码",
                )
                Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                PasswordInput(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    enabled = !isLoading,
                    label = "再次输入密码",
                )
            } else {
                // ===== 密码登录模式 =====
                // 邮箱未注册时，在账号上方显示手机号输入框
                AnimatedVisibility(
                    visible = needsRegister && isAccountEmail,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        PhoneOnlyInput(
                            value = registerPhone,
                            onValueChange = onRegisterPhoneChange,
                            enabled = !isLoading,
                            label = "请输入手机号",
                        )
                        Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                    }
                }

                // 账号输入（手机号/邮箱）
                AccountInput(
                    value = username,
                    onValueChange = onUsernameChange,
                    enabled = !isLoading,
                    modifier = Modifier.onFocusChanged { focusState ->
                        if (hadFocus && !focusState.isFocused) {
                            onFocusLostValidate()
                        }
                        hadFocus = focusState.isFocused
                    },
                    onValidationError = { toastState.show(it) },
                )

                // 密码登录 — 未注册时动画弹出验证码（账号后面）
                AnimatedVisibility(
                    visible = needsRegister,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                        CodeInput(
                            value = verifyCode,
                            onValueChange = onVerifyCodeChange,
                            enabled = !isLoading,
                            onSendCode = onSendCode,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                PasswordInput(
                    value = password,
                    onValueChange = onPasswordChange,
                    enabled = !isLoading,
                    label = if (needsRegister) "设置密码" else "请输入密码",
                )

                // 忘记密码（右对齐）
                if (!needsRegister && onForgotClick != null) {
                    Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                    Text(
                        text = "忘记密码",
                        color = LinkColor,
                        fontSize = ds.sp(12f),
                        fontWeight = FontWeight.Normal,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onForgotClick
                            ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }

                // 密码登录 — 未注册时动画弹出确认密码（密码后面）
                AnimatedVisibility(
                    visible = needsRegister,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                        PasswordInput(
                            value = confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            enabled = !isLoading,
                            label = "再次输入密码",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f).defaultMinSize(minHeight = ds.sh(32.dp)))

            // 提交按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ds.sh(48.dp))
                    .clip(RoundedCornerShape(80.dp))
                    .background(if (canSubmit) EnabledBtnColor else DisabledBtnColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canSubmit
                    ) { onSubmit() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ds.sm(24.dp)),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (needsRegister || isRegisterPage) "立即注册" else "登录",
                        color = Color.White,
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                    )
                }
            }

            Spacer(modifier = Modifier.height(ds.sh(12.dp)))

            // 底部服务条款
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                        append("登录即表示同意我们的 ")
                    }
                    withStyle(SpanStyle(color = LinkColor)) {
                        append("《服务条款》")
                    }
                    withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                        append(" 和 ")
                    }
                    withStyle(SpanStyle(color = LinkColor)) {
                        append("《隐私政策》")
                    }
                },
                fontSize = ds.sp(10f),
                fontWeight = FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(ds.sh(24.dp)))
        }
    }
}
