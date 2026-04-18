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
import androidios.composeapp.generated.resources.ic_lock
import androidios.composeapp.generated.resources.ic_shield_check
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
    var registerPhone by remember { mutableStateOf("") }
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


    // 3. 登录逻辑封装
    val performLogin: () -> Unit = {
        keyboardController?.hide()

        if (!preferEmailLogin && sheetPage == SheetPage.Login) {
            // ===== 验证码登录 =====
            val phone = username.trim()
            if (phone.length != 11 || !Regex("^1\\d{10}$").matches(phone)) {
                toastState.show("请输入正确的11位手机号")
            } else if (verifyCode.isBlank()) {
                toastState.show("请输入验证码")
            } else if (needsRegister && (password.isBlank() || confirmPassword.isBlank())) {
                toastState.show("请设置密码")
            } else if (needsRegister && password != confirmPassword) {
                toastState.show("两次密码不一致")
            } else {
                isLoading = true
                scope.launch {
                    val request = if (needsRegister) {
                        LoginRequest(
                            phone = phone,
                            code = verifyCode,
                            pwd = password,
                            confirmPwd = confirmPassword,
                            trackId = "kmp",
                            appType = "platform",
                            way = "phone_code"
                        )
                    } else {
                        LoginRequest(
                            phone = phone,
                            code = verifyCode,
                            trackId = "kmp",
                            appType = "platform",
                            way = "phone_code"
                        )
                    }
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
        } else if (sheetPage == SheetPage.Register) {
            // ===== 注册 =====
            if (username.isBlank() || password.isBlank() || confirmPassword.isBlank() || verifyCode.isBlank()) {
                toastState.show("请填写所有必填项")
            } else if (password != confirmPassword) {
                toastState.show("两次密码不一致")
            } else if (isAccountEmail && (registerPhone.length != 11 || !Regex("^1\\d{10}$").matches(registerPhone))) {
                toastState.show("请输入正确的11位手机号")
            } else {
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
                        phone = if (isEmail) registerPhone else account,
                        email = if (isEmail) account else null,
                        pwd = password,
                        confirmPwd = confirmPassword,
                        code = verifyCode,
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
        } else {
            // ===== 密码登录 =====
            if (username.isBlank() || password.isBlank()) {
                toastState.show("请输入用户名和密码")
            } else if (needsRegister && isAccountEmail && (registerPhone.length != 11 || !Regex("^1\\d{10}$").matches(registerPhone))) {
                toastState.show("请输入正确的11位手机号")
            } else {
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

                    val request = if (needsRegister) {
                        // 密码登录发现未注册 → 注册
                        LoginRequest(
                            phone = if (isEmail) registerPhone else account,
                            email = if (isEmail) account else null,
                            pwd = password,
                            confirmPwd = confirmPassword,
                            code = verifyCode,
                            trackId = "kmp",
                            appType = "platform",
                            way = if (isEmail) "email_pwd" else "phone_pwd"
                        )
                    } else {
                        LoginRequest(
                            phone = if (isEmail) null else account,
                            email = if (isEmail) account else null,
                            pwd = password,
                            trackId = "kmp",
                            appType = "platform",
                            way = if (isEmail) "email_pwd" else "phone_pwd"
                        )
                    }

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
    }

    val resetSheetState: () -> Unit = {
        username = ""
        password = ""
        confirmPassword = ""
        verifyCode = ""
        registerPhone = ""
        isLoading = false
        needsRegister = false
        isAccountEmail = false
        normalizedAccount = ""
        sheetTitle = "Welcome to Lucy"
    }

    // 从当前 username 实时归一化为 (account, isEmail)；非法/空则返回 null。
    // 提取此 helper 的原因：原先只有 validateAccount（失焦时）才会更新 normalizedAccount，
    // 导致用户输入账号后不点别处、直接点「获取验证码」时 normalizedAccount 仍是空串，
    // onSendCode 误判为"未输入"。需要每次点击按钮都能实时拿到最新输入。
    val normalizeCurrentAccount: () -> Pair<String, Boolean>? = normalize@{
        val input = username.trim()
        if (input.isBlank()) return@normalize null
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.com$")
        val normalizedEmail = if (emailPattern.matches(input)) input else null
        val normalizedPhone = input.replace(" ", "")
            .let { v -> if (v.startsWith("+86")) v.removePrefix("+86") else v }
            .let { v -> if (v.startsWith("86") && v.length > 11) v.removePrefix("86") else v }
            .takeIf { Regex("^1\\d{10}$").matches(it) }
        val isEmail = normalizedEmail != null
        val account = normalizedEmail ?: normalizedPhone ?: return@normalize null
        account to isEmail
    }

    val validateAccount: () -> Unit = {
        val input = username.trim()
        if (input.isNotBlank()) {
            val normalized = normalizeCurrentAccount()
            if (normalized == null) {
                toastState.show("请输入正确的手机号(+86 11位)或邮箱(.com)")
            } else {
                val (account, isEmail) = normalized
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
                    text = "欢迎使用脑花",
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
                    icon = { Icon(painterResource(Res.drawable.ic_shield_check), null, tint = Color.Black, modifier = Modifier.size(ds.sm(20.dp))) },
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
                    icon = { Icon(painterResource(Res.drawable.ic_lock), null, tint = Color.White, modifier = Modifier.size(ds.sm(20.dp))) },
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
                val canSubmit = when {
                    // 验证码登录：手机号 + 验证码 (+未注册时需密码)
                    !preferEmailLogin && page == SheetPage.Login ->
                        username.isNotBlank() && verifyCode.isNotBlank() && !isLoading &&
                        (!needsRegister || (password.isNotBlank() && confirmPassword.isNotBlank()))
                    // 注册页：账号 + 验证码 + 密码 + 确认密码 (+邮箱时需手机号)
                    page == SheetPage.Register ->
                        username.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() &&
                        verifyCode.isNotBlank() && (!isAccountEmail || registerPhone.isNotBlank()) && !isLoading
                    // 密码登录：账号 + 密码 (+未注册时需确认密码和验证码)
                    else ->
                        username.isNotBlank() && password.isNotBlank() && !isLoading &&
                        (!needsRegister || (confirmPassword.isNotBlank() && verifyCode.isNotBlank()))
                }

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
                                if (!preferEmailLogin) {
                                    // 验证码登录：直接用 username 作为手机号
                                    val phone = username.trim()
                                    if (phone.length != 11 || !Regex("^1\\d{10}$").matches(phone)) {
                                        toastState.show("请先输入正确的11位手机号")
                                        return@LoginSheetContent
                                    }
                                    val actionType = if (needsRegister) "register" else "login"
                                    scope.launch {
                                        isLoading = true
                                        val response = authRepository.getCode(phone = phone, actionType = actionType, appType = "lucy")
                                        isLoading = false
                                        if (response.code == 20000) startTimer() else toastState.show(response.msg)
                                    }
                                } else {
                                    // 密码登录模式（未注册时需要验证码）
                                    // 与注册页同理：不依赖失焦写入的 normalizedAccount，每次点击实时归一化。
                                    val normalized = normalizeCurrentAccount()
                                    if (normalized == null) {
                                        toastState.show("请先输入正确的手机号或邮箱")
                                        return@LoginSheetContent
                                    }
                                    val (account, isEmail) = normalized
                                    normalizedAccount = account
                                    isAccountEmail = isEmail
                                    val pwdActionType = if (needsRegister) "register" else "login"
                                    scope.launch {
                                        isLoading = true
                                        val response = if (isEmail) {
                                            authRepository.getCode(email = account, actionType = pwdActionType, appType = "lucy")
                                        } else {
                                            authRepository.getCode(phone = account, actionType = pwdActionType, appType = "lucy")
                                        }
                                        isLoading = false
                                        if (response.code == 20000) startTimer() else toastState.show(response.msg)
                                    }
                                }
                            },
                            toastState = toastState,
                            registerPhone = registerPhone,
                            onRegisterPhoneChange = { registerPhone = it },
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
                            canSubmit = username.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() &&
                                verifyCode.isNotBlank() && (!isAccountEmail || registerPhone.isNotBlank()) && !isLoading,
                            normalizedAccount = normalizedAccount,
                            isAccountEmail = isAccountEmail,
                            onBackClick = { loginSheetVisible = false },
                            onFocusLostValidate = validateAccount,
                            onForgotClick = null,
                            onSubmit = performLogin,
                            registerPhone = registerPhone,
                            onRegisterPhoneChange = { registerPhone = it },
                            onSendCode = { startTimer ->
                                // 实时归一化当前输入，不再依赖失焦时刻写入的 normalizedAccount——
                                // 用户可能输完账号直接点「获取验证码」没有失焦过。
                                val normalized = normalizeCurrentAccount()
                                if (normalized == null) {
                                    toastState.show("请先输入正确的手机号或邮箱")
                                    return@LoginSheetContent
                                }
                                val (account, isEmail) = normalized
                                // 同步回本地 state，让后续 performLogin 能直接用到最新归一化结果。
                                normalizedAccount = account
                                isAccountEmail = isEmail
                                scope.launch {
                                    isLoading = true
                                    val response = if (isEmail) {
                                        authRepository.getCode(email = account, actionType = "register", appType = "lucy")
                                    } else {
                                        authRepository.getCode(phone = account, actionType = "register", appType = "lucy")
                                    }
                                    isLoading = false
                                    if (response.code == 20000) startTimer() else toastState.show(response.msg)
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
