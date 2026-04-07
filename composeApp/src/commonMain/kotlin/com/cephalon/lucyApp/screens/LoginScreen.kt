package com.cephalon.lucyApp.screens

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.logo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.api.AuthInput
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.LoginRequest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.cephalon.lucyApp.components.AccountInput
import com.cephalon.lucyApp.components.EmailInput
import com.cephalon.lucyApp.components.PasswordInput
import com.cephalon.lucyApp.components.ForgotPasswordForm
import com.cephalon.lucyApp.components.HalfModalBottomSheet


private enum class SheetPage {
    Login,
    Forgot
}

private val SheetPageShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
private fun SheetPageContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                shape = SheetPageShape
                clip = true
                shadowElevation = 0f
            }
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SheetTopBar(
    showBack: Boolean,
    onBack: (() -> Unit)?,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            IconButton(
                onClick = { onBack?.invoke() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6E6E6))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF2D2D2D),
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE6E6E6))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFF2D2D2D),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
) {
    val focusManager = LocalFocusManager.current // 获取焦点管理器

    // 1. 注入 AuthRepository (通过 Koin)
    val authRepository = koinInject<AuthRepository>()
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) } // 2. 登录加载状态

    val keyboardController = LocalSoftwareKeyboardController.current


    var loginSheetVisible by remember { mutableStateOf(false) }
    var preferEmailLogin by remember { mutableStateOf(false) }
    var sheetPage by remember { mutableStateOf(SheetPage.Login) }


    // 3. 登录逻辑封装：接通拦截器与接口请求
    val performLogin: () -> Unit = {
        if (username.isBlank() || password.isBlank()) {
            error = "请输入用户名和密码"
        } else {
            keyboardController?.hide()
            isLoading = true
            error = ""

            scope.launch {
                val rawAccount = AuthInput.normalizeAccount(username)
                val normalizedEmail = rawAccount.takeIf {
                    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.com$").matches(it)
                }
                val normalizedPhone = rawAccount
                    .replace(" ", "")
                    .let { v -> if (v.startsWith("+86")) v.removePrefix("+86") else v }
                    .let { v -> if (v.startsWith("86") && v.length > 11) v.removePrefix("86") else v }
                    .takeIf { Regex("^1\\d{10}$").matches(it) }

                val isEmail = preferEmailLogin
                val account = if (isEmail) normalizedEmail else normalizedPhone
                if (account == null) {
                    isLoading = false
                    error = if (isEmail) "请输入正确的邮箱(.com)" else "请输入正确的手机号(+86 11位)"
                    return@launch
                }

                val request = LoginRequest(
                    phone = if (isEmail) null else account,
                    email = if (isEmail) account else null,
                    pwd = password,
                    confirmPwd = password,
                    trackId = "kmp",
                    appType = "platform",
                    way = if (isEmail) "email_pwd" else "phone_pwd"
                )

                val response = authRepository.login(request)
                isLoading = false

                if (response.code == 20000 && response.data != null) {
                    loginSheetVisible = false
                    onLoginSuccess()
                } else {
                    error = response.msg
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(88.dp))

            Surface(
                color = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "欢迎使用 Lucy",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI 驱动的个人数据操作系统操作系统",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    preferEmailLogin = false
                    username = ""
                    password = ""
                    error = ""
                    isLoading = false
                    sheetPage = SheetPage.Login
                    loginSheetVisible = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "通过手机号登录", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    preferEmailLogin = true
                    username = ""
                    password = ""
                    error = ""
                    isLoading = false
                    sheetPage = SheetPage.Login
                    loginSheetVisible = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(imageVector = Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "通过邮箱登录", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "目前还没注册账号还没有账号 ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "点击注册",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        HalfModalBottomSheet(
            isVisible = loginSheetVisible,
            onDismissRequest = { loginSheetVisible = false },
            onDismissed = {
                sheetPage = SheetPage.Login
            },
            onBack = null,
            showBackButton = false,
            showCloseButton = false,
            showTopBar = false,
            containerShape = RoundedCornerShape(0.dp),
            containerColor = Color.Transparent,
            topPadding = 60.dp,
            contentPadding = PaddingValues(0.dp)
        ) {
            AnimatedContent(
                targetState = sheetPage,
                transitionSpec = {
                    val goingForward = initialState == SheetPage.Login && targetState == SheetPage.Forgot
                    val slideSpec = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)
                    if (goingForward) {
                        ContentTransform(
                            targetContentEnter = (slideInHorizontally(
                                animationSpec = slideSpec,
                                initialOffsetX = { it }
                            )),
                            initialContentExit = (slideOutHorizontally(
                                animationSpec = slideSpec,
                                targetOffsetX = { -it }
                            )),
                            targetContentZIndex = 1f,
                            sizeTransform = SizeTransform(clip = true)
                        )
                    } else {
                        ContentTransform(
                            targetContentEnter = (slideInHorizontally(
                                animationSpec = slideSpec,
                                initialOffsetX = { -it }
                            )),
                            initialContentExit = (slideOutHorizontally(
                                animationSpec = slideSpec,
                                targetOffsetX = { it }
                            )),
                            targetContentZIndex = 1f,
                            sizeTransform = SizeTransform(clip = true)
                        )
                    }
                },
                label = "LoginSheetPage"
            ) { page ->
                when (page) {
                    SheetPage.Login -> {
                        SheetPageContainer {
                            Spacer(modifier = Modifier.height(20.dp))
                            SheetTopBar(
                                showBack = false,
                                onBack = null,
                                onClose = { loginSheetVisible = false }
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                            ) {
                                Text(
                                    text = if (preferEmailLogin) "邮箱登录" else "手机号登录",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (preferEmailLogin) {
                                    EmailInput(
                                        value = username,
                                        onValueChange = {
                                            username = it
                                            error = ""
                                        },
                                        enabled = !isLoading,
                                        onValidationError = { msg ->
                                            error = msg
                                        },
                                        onValidated = { normalized ->
                                            scope.launch {
                                                isLoading = true
                                                val response = authRepository.isEmailExist(normalized)
                                                isLoading = false
                                                if (response.code == 20000) {
                                                    val isExit = response.data?.get("is_exit") as? Boolean ?: false
                                                    if (!isExit) {
                                                        error = response.msg
                                                    }
                                                } else {
                                                    error = response.msg
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    AccountInput(
                                        value = username,
                                        onValueChange = {
                                            username = it
                                            error = ""
                                        },
                                        enabled = !isLoading,
                                        onValidationError = { msg ->
                                            error = msg
                                        },
                                        onValidated = { normalized, isEmail ->
                                            if (isEmail) {
                                                error = "请输入正确的手机号(+86 11位)"
                                                return@AccountInput
                                            }
                                            scope.launch {
                                                isLoading = true
                                                val response = authRepository.isPhoneExist(normalized)
                                                isLoading = false
                                                if (response.code == 20000) {
                                                    val isExit = response.data?.get("is_exit") as? Boolean ?: false
                                                    if (!isExit) {
                                                        error = response.msg
                                                    }
                                                } else {
                                                    error = response.msg
                                                }
                                            }
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                PasswordInput(
                                    value = password,
                                    onValueChange = {
                                        password = it
                                        error = ""
                                    },
                                    enabled = !isLoading,
                                    label = "密码",
                                    onDone = { performLogin() }
                                )

                                if (error.isNotEmpty()) {
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(22.dp))

                                Button(
                                    onClick = performLogin,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = !isLoading
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            "登 录",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                TextButton(
                                    onClick = {
                                        sheetPage = SheetPage.Forgot
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        text = "忘记密码？",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    SheetPage.Forgot -> {
                        SheetPageContainer {
                            Spacer(modifier = Modifier.height(20.dp))
                            SheetTopBar(
                                showBack = true,
                                onBack = { sheetPage = SheetPage.Login },
                                onClose = { loginSheetVisible = false }
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                            ) {
                                ForgotPasswordForm(
                                    onResetSuccess = {
                                        loginSheetVisible = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
