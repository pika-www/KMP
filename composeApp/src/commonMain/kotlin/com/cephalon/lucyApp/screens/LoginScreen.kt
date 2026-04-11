package com.cephalon.lucyApp.screens

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.login_bg
import androidios.composeapp.generated.resources.logo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.api.AuthInput
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.LoginRequest
import com.cephalon.lucyApp.components.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject


private enum class SheetPage {
    Login,
    Forgot,
    Register
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
    var confirmPassword by remember { mutableStateOf("") }
    var verifyCode by remember { mutableStateOf("") }
    val toastState = rememberToastState()
    var isLoading by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    var loginSheetVisible by remember { mutableStateOf(false) }
    var preferEmailLogin by remember { mutableStateOf(false) }
    var sheetPage by remember { mutableStateOf(SheetPage.Login) }
    var needsRegister by remember { mutableStateOf(false) }
    var isAccountEmail by remember { mutableStateOf(false) }
    var normalizedAccount by remember { mutableStateOf("") }
    var sheetTitle by remember { mutableStateOf("Welcome to Lucy") }


    // 3. 登录逻辑封装：接通拦截器与接口请求
    val performLogin: () -> Unit = {
        if (username.isBlank() || password.isBlank()) {
            toastState.show("请输入用户名和密码")
        } else {
            keyboardController?.hide()
            isLoading = true

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

                val isEmail = normalizedEmail != null
                val account = normalizedEmail ?: normalizedPhone
                if (account == null) {
                    isLoading = false
                    toastState.show("请输入正确的手机号(+86 11位)或邮箱(.com)")
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
                    authRepository.getUserInfo()
                    loginSheetVisible = false
                    onLoginSuccess()
                } else {
                    toastState.show(response.msg)
                }
            }
        }
    }

    val resetSheetState: () -> Unit = {
        username = ""
        password = ""
        confirmPassword = ""
        verifyCode = ""
        isLoading = false
        needsRegister = false
        isAccountEmail = false
        normalizedAccount = ""
        sheetTitle = "Welcome to Lucy"
    }

    val validateAccount: () -> Unit = {
        val input = username.trim()
        if (input.isNotBlank()) {
            val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.com$")
            val normalizedEmail = if (emailPattern.matches(input)) input else null
            val normalizedPhone = input.replace(" ", "")
                .let { v -> if (v.startsWith("+86")) v.removePrefix("+86") else v }
                .let { v -> if (v.startsWith("86") && v.length > 11) v.removePrefix("86") else v }
                .takeIf { Regex("^1\\d{10}$").matches(it) }

            val isEmail = normalizedEmail != null
            val account = normalizedEmail ?: normalizedPhone
            if (account == null) {
                toastState.show("请输入正确的手机号(+86 11位)或邮箱(.com)")
            } else {
                isAccountEmail = isEmail
                normalizedAccount = account
                scope.launch {
                    isLoading = true
                    val response = if (isEmail) {
                        authRepository.isEmailExist(account)
                    } else {
                        authRepository.isPhoneExist(account)
                    }
                    isLoading = false
                    if (response.code == 20000) {
                        val exists = response.data?.isExist ?: false
                        if (exists) {
                            needsRegister = false
                            sheetTitle = "Welcome to Lucy"
                        } else {
                            needsRegister = true
                            val typeLabel = if (isEmail) "邮箱" else "手机号"
                            sheetTitle = "此${typeLabel}还未注册"
                        }
                    } else {
                        toastState.show(response.msg)
                    }
                }
            }
        }
    }

    DesignScaleProvider(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        val ds = LocalDesignScale.current

        Box(modifier = Modifier.fillMaxSize()) {
            // 背景图 — 原尺寸，底部对齐
            Image(
                painter = painterResource(Res.drawable.login_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.BottomCenter
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = ds.sw(26.dp)),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(ds.sh(88.dp)))

                // Logo — 无背景无边框, 64px
                Icon(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(ds.sm(64.dp))
                )

                Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                // 标题
                Text(
                    text = "欢迎使用 Lucy",
                    color = Color.Black.copy(alpha = 0.90f),
                    fontSize = ds.sp(28f),
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(ds.sh(8.dp)))

                // 副标题
                Text(
                    text = "AI 驱动的个人数据操作系统",
                    color = Color.Black.copy(alpha = 0.60f),
                    fontSize = ds.sp(16f),
                    fontWeight = FontWeight.Light,
                )

                Spacer(modifier = Modifier.weight(1f))

                // 验证码登录按钮
                LoginGlassButton(
                    text = "通过验证码登录",
                    icon = { Icon(Icons.Default.Shield, null, tint = Color.Black, modifier = Modifier.size(ds.sm(20.dp))) },
                    backgroundColor = Color.White.copy(alpha = 0.90f),
                    textColor = Color.Black,
                    onClick = {
                        resetSheetState()
                        preferEmailLogin = false
                        sheetPage = SheetPage.Login
                        loginSheetVisible = true
                    }
                )

                Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                // 密码登录按钮
                LoginGlassButton(
                    text = "通过密码登录",
                    icon = { Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(ds.sm(20.dp))) },
                    backgroundColor = Color.White.copy(alpha = 0.20f),
                    textColor = Color.White,
                    onClick = {
                        resetSheetState()
                        preferEmailLogin = true
                        sheetPage = SheetPage.Login
                        loginSheetVisible = true
                    }
                )

                Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                // 底部注册提示
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.40f))) {
                            append("没有账号的可以用手机号或邮箱注册 ")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color.White,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("点击注册")
                        }
                    },
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        resetSheetState()
                        sheetPage = SheetPage.Register
                        sheetTitle = "注册账号"
                        loginSheetVisible = true
                    },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(ds.sh(32.dp)))
            }
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
            containerShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = Color(0xFFF5F5F5),
            topPadding = 60.dp,
            contentPadding = PaddingValues(0.dp)
        ) {
            AnimatedContent(
                targetState = sheetPage,
                transitionSpec = {
                    val goingForward = (initialState == SheetPage.Login && targetState != SheetPage.Login) ||
                        (initialState == SheetPage.Register && targetState == SheetPage.Forgot)
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
                val ds = LocalDesignScale.current
                val canSubmit = username.isNotBlank() && password.isNotBlank() && !isLoading &&
                    (!needsRegister || (confirmPassword.isNotBlank() && (preferEmailLogin || verifyCode.isNotBlank()))) &&
                    (preferEmailLogin || !needsRegister || verifyCode.isNotBlank())

                when (page) {
                    SheetPage.Login -> {
                        LoginSheetContent(
                            ds = ds,
                            sheetTitle = sheetTitle,
                            preferEmailLogin = preferEmailLogin,
                            needsRegister = needsRegister,
                            username = username,
                            onUsernameChange = { username = it; if (needsRegister) { needsRegister = false; sheetTitle = "Welcome to Lucy" } },
                            password = password,
                            onPasswordChange = { password = it },
                            confirmPassword = confirmPassword,
                            onConfirmPasswordChange = { confirmPassword = it },
                            verifyCode = verifyCode,
                            onVerifyCodeChange = { verifyCode = it },
                            isLoading = isLoading,
                            canSubmit = canSubmit,
                            normalizedAccount = normalizedAccount,
                            isAccountEmail = isAccountEmail,
                            onBackClick = { loginSheetVisible = false },
                            onFocusLostValidate = validateAccount,
                            onForgotClick = { sheetPage = SheetPage.Forgot },
                            onSubmit = performLogin,
                            onSendCode = { startTimer ->
                                if (normalizedAccount.isBlank()) {
                                    toastState.show("请先输入正确的手机号或邮箱")
                                    return@LoginSheetContent
                                }
                                scope.launch {
                                    isLoading = true
                                    val response = if (isAccountEmail) {
                                        authRepository.getCode(email = normalizedAccount, actionType = "login", appType = "lucy")
                                    } else {
                                        authRepository.getCode(phone = normalizedAccount, actionType = "login", appType = "lucy")
                                    }
                                    isLoading = false
                                    if (response.code == 20000) {
                                        startTimer()
                                    } else {
                                        toastState.show(response.msg)
                                    }
                                }
                            },
                            toastState = toastState
                        )
                    }

                    SheetPage.Forgot -> {
                        SheetPageContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = ds.sw(20.dp))
                            ) {
                                Spacer(modifier = Modifier.height(ds.sh(20.dp)))
                                SheetBackButton(onClick = { sheetPage = SheetPage.Login })
                                Spacer(modifier = Modifier.height(ds.sh(52.dp)))
                                ForgotPasswordForm(
                                    onResetSuccess = { loginSheetVisible = false },
                                    onShowToast = { toastState.show(it) }
                                )
                            }
                        }
                    }

                    SheetPage.Register -> {
                        LoginSheetContent(
                            ds = ds,
                            sheetTitle = "注册账号",
                            preferEmailLogin = false,
                            needsRegister = true,
                            isRegisterPage = true,
                            username = username,
                            onUsernameChange = { username = it },
                            password = password,
                            onPasswordChange = { password = it },
                            confirmPassword = confirmPassword,
                            onConfirmPasswordChange = { confirmPassword = it },
                            verifyCode = verifyCode,
                            onVerifyCodeChange = { verifyCode = it },
                            isLoading = isLoading,
                            canSubmit = username.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && verifyCode.isNotBlank() && !isLoading,
                            normalizedAccount = normalizedAccount,
                            isAccountEmail = isAccountEmail,
                            onBackClick = { loginSheetVisible = false },
                            onFocusLostValidate = validateAccount,
                            onForgotClick = null,
                            onSubmit = performLogin,
                            onSendCode = { startTimer ->
                                if (normalizedAccount.isBlank()) {
                                    toastState.show("请先输入正确的手机号或邮箱")
                                    return@LoginSheetContent
                                }
                                scope.launch {
                                    isLoading = true
                                    val response = if (isAccountEmail) {
                                        authRepository.getCode(email = normalizedAccount, actionType = "register", appType = "lucy")
                                    } else {
                                        authRepository.getCode(phone = normalizedAccount, actionType = "register", appType = "lucy")
                                    }
                                    isLoading = false
                                    if (response.code == 20000) {
                                        startTimer()
                                    } else {
                                        toastState.show(response.msg)
                                    }
                                }
                            },
                            toastState = toastState
                        )
                    }
                }
            }
        }

        ToastHost(state = toastState)
    }
}

@Composable
private fun LoginGlassButton(
    text: String,
    icon: @Composable () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val ds = LocalDesignScale.current
    val shape = RoundedCornerShape(100.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ds.sh(48.dp))
            .clip(shape)
            .background(backgroundColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.width(ds.sw(8.dp)))
            Text(
                text = text,
                color = textColor,
                fontSize = ds.sp(16f),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
