package com.cephalon.lucyApp.screens

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.login_bg
import androidios.composeapp.generated.resources.logo_img
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidios.composeapp.generated.resources.ic_lock
import androidios.composeapp.generated.resources.ic_shield_check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.ui.platform.LocalUriHandler
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.LoginRequest
import com.cephalon.lucyApp.components.*
import kotlinx.coroutines.delay
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
    var showCodeLoginPage by remember { mutableStateOf(false) }
    var showPasswordLoginPage by remember { mutableStateOf(false) }
    var showRegisterPage by remember { mutableStateOf(false) }
    var preferEmailLogin by remember { mutableStateOf(false) }
    var sheetPage by remember { mutableStateOf(SheetPage.Login) }
    var needsRegister by remember { mutableStateOf(false) }
    var sheetTitle by remember { mutableStateOf("Welcome to Lucy") }
    // 注册页：账号已注册命中时弹出弹窗
    var accountRegistered by remember { mutableStateOf(false) }
    var showAccountRegisteredDialog by remember { mutableStateOf(false) }
    // 是否已完成「是否已注册」校验；输入变化时重置为 false，校验成功返回后置为 true。
    // 用于门控「获取验证码」按钮——必须校验通过且条件满足后才亮起。
    var accountCheckPassed by remember { mutableStateOf(false) }


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
                            appType = "lucy",
                            way = "phone_code"
                        )
                    } else {
                        LoginRequest(
                            phone = phone,
                            code = verifyCode,
                            appType = "lucy",
                            trackId = "kmp",
                            way = "phone_code"
                        )
                    }
                    val response = authRepository.login(request)
                    if (response.code == 20000 && response.data != null) {
                        onLoginSuccess()
                    } else {
                        isLoading = false
                        toastState.show(response.msg)
                    }
                }
            }
        } else if (sheetPage == SheetPage.Register) {
            // ===== 注册 =====
            val phone = username.trim()
            if (phone.length != 11 || !Regex("^1\\d{10}$").matches(phone)) {
                toastState.show("请输入正确的11位手机号")
            } else if (password.isBlank() || confirmPassword.isBlank() || verifyCode.isBlank()) {
                toastState.show("请填写所有必填项")
            } else if (password != confirmPassword) {
                toastState.show("两次密码不一致")
            } else {
                isLoading = true
                scope.launch {
                    val request = LoginRequest(
                        phone = phone,
                        pwd = password,
                        confirmPwd = confirmPassword,
                        code = verifyCode,
                        trackId = "kmp",
                        appType = "lucy",
                        way = "phone_pwd"
                    )
                    val response = authRepository.login(request)
                    if (response.code == 20000 && response.data != null) {
                        onLoginSuccess()
                    } else {
                        isLoading = false
                        toastState.show(response.msg)
                    }
                }
            }
        } else {
            // ===== 密码登录 =====
            if (username.isBlank() || password.isBlank()) {
                toastState.show("请输入手机号和密码")
            } else {
                isLoading = true
                scope.launch {
                    val phone = username.trim()
                        .replace(" ", "")
                        .let { v -> if (v.startsWith("+86")) v.removePrefix("+86") else v }
                        .let { v -> if (v.startsWith("86") && v.length > 11) v.removePrefix("86") else v }
                    if (!Regex("^1\\d{10}$").matches(phone)) {
                        isLoading = false
                        toastState.show("请输入正确的11位手机号")
                        return@launch
                    }

                    val request = if (needsRegister) {
                        LoginRequest(
                            phone = phone,
                            pwd = password,
                            confirmPwd = confirmPassword,
                            code = verifyCode,
                            trackId = "kmp",
                            appType = "lucy",
                            way = "phone_pwd"
                        )
                    } else {
                        LoginRequest(
                            phone = phone,
                            pwd = password,
                            trackId = "kmp",
                            appType = "lucy",
                            way = "phone_pwd"
                        )
                    }

                    val response = authRepository.login(request)
                    if (response.code == 20000 && response.data != null) {
                        onLoginSuccess()
                    } else {
                        isLoading = false
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
        isLoading = false
        needsRegister = false
        sheetTitle = "Welcome to Lucy"
        accountRegistered = false
        accountCheckPassed = false
    }

    // 实时归一化当前 username 为 11 位手机号；非法/空则返回 null。
    val normalizeCurrentAccount: () -> String? = normalize@{
        val input = username.trim().replace(" ", "")
        val withoutPrefix = when {
            input.startsWith("+86") -> input.removePrefix("+86")
            input.startsWith("86") && input.length > 11 -> input.removePrefix("86")
            else -> input
        }
        return@normalize withoutPrefix.trim().takeIf { Regex("^1\\d{10}$").matches(it) }
    }

    val validateAccount: () -> Unit = {
        // 仅触发 LaunchedEffect 中的账号存在性检查，不弹 toast。
        // toast 由 performLogin 统一处理，避免焦点丢失 + 点击提交时重复弹出。
    }

    LaunchedEffect(username, sheetPage, loginSheetVisible, showCodeLoginPage, showPasswordLoginPage, showRegisterPage) {
        if (!loginSheetVisible && !showCodeLoginPage && !showPasswordLoginPage && !showRegisterPage) return@LaunchedEffect
        if (sheetPage == SheetPage.Forgot) return@LaunchedEffect
        val phone = normalizeCurrentAccount() ?: return@LaunchedEffect
        delay(500)
        val response = authRepository.isPhoneExist(phone)
        if (response.code == 20000) {
            val exists = response.data?.isExist ?: false
            if (sheetPage == SheetPage.Register) {
                accountRegistered = exists
                if (exists) showAccountRegisteredDialog = true
            } else {
                accountRegistered = false
                if (exists) {
                    needsRegister = false
                    sheetTitle = "Welcome to Lucy"
                } else {
                    needsRegister = true
                    sheetTitle = "此手机号还未注册"
                }
            }
            accountCheckPassed = true
        }
    }

    DesignScaleProvider(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
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
                Spacer(modifier = Modifier.height(ds.sh(133.dp)))

                // Logo — 无背景无边框, 64px
                Image(
                    painter = painterResource(Res.drawable.logo_img),
                    contentDescription = null,
                    modifier = Modifier
                        .size(ds.sm(64.dp))
                        .clip(RoundedCornerShape(ds.sm(12.dp)))
                )

                Spacer(modifier = Modifier.height(ds.sh(32.dp)))

                // 标题
                Text(
                    text = "欢迎使用脑花",
                    color = Color.Black.copy(alpha = 0.90f),
                    fontSize = ds.sp(24f),
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
                        showCodeLoginPage = true
                    }
                )

                Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                // 密码登录按钮
                LoginGlassButton(
                    text = "通过密码登录",
                    icon = { Icon(painterResource(Res.drawable.ic_lock), null, tint = Color.Black, modifier = Modifier.size(ds.sm(20.dp))) },
                    backgroundColor = Color.White.copy(alpha = 0.90f),
                    textColor = Color.Black,
                    onClick = {
                        resetSheetState()
                        preferEmailLogin = true
                        sheetPage = SheetPage.Login
                        showPasswordLoginPage = true
                    }
                )

                Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                // 底部注册提示
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.40f))) {
                            append("没有账号的可以用手机号注册 ")
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
                        showRegisterPage = true
                    },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(ds.sh(40.dp)))
            }
        }

        // ── 密码登录全页面 ──
        AnimatedVisibility(
            visible = showPasswordLoginPage,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(160)),
        ) {
            PasswordLoginPage(
                username = username,
                onUsernameChange = { if (it != username) { username = it; accountCheckPassed = false; if (needsRegister) { needsRegister = false; sheetTitle = "Welcome to Lucy" } } },
                password = password,
                onPasswordChange = { password = it },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it },
                verifyCode = verifyCode,
                onVerifyCodeChange = { verifyCode = it },
                isLoading = isLoading,
                needsRegister = needsRegister,
                accountCheckPassed = accountCheckPassed,
                onFocusLostValidate = validateAccount,
                onSubmit = performLogin,
                onSendCode = { startTimer ->
                    val phone = normalizeCurrentAccount()
                    if (phone == null) {
                        toastState.show("请先输入正确的11位手机号")
                        return@PasswordLoginPage
                    }
                    val actionType = if (needsRegister) "register" else "login"
                    scope.launch {
                        isLoading = true
                        val response = authRepository.getCode(phone = phone, actionType = actionType, appType = "lucy")
                        isLoading = false
                        if (response.code == 20000) startTimer() else toastState.show(response.msg)
                    }
                },
                onDismiss = {
                    showPasswordLoginPage = false
                    resetSheetState()
                },
                onForgotPasswordSuccess = {
                    showPasswordLoginPage = false
                    resetSheetState()
                },
                toastState = toastState,
            )
        }

        // ── 注册全页面 ──
        AnimatedVisibility(
            visible = showRegisterPage,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(160)),
        ) {
            RegisterPage(
                username = username,
                onUsernameChange = { username = it; accountRegistered = false; accountCheckPassed = false },
                password = password,
                onPasswordChange = { password = it },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it },
                verifyCode = verifyCode,
                onVerifyCodeChange = { verifyCode = it },
                isLoading = isLoading,
                accountCheckPassed = accountCheckPassed,
                accountRegistered = accountRegistered,
                onFocusLostValidate = validateAccount,
                onSubmit = performLogin,
                onSendCode = { startTimer ->
                    val phone = normalizeCurrentAccount()
                    if (phone == null) {
                        toastState.show("请先输入正确的11位手机号")
                        return@RegisterPage
                    }
                    scope.launch {
                        isLoading = true
                        val response = authRepository.getCode(phone = phone, actionType = "register", appType = "lucy")
                        isLoading = false
                        if (response.code == 20000) startTimer() else toastState.show(response.msg)
                    }
                },
                onDismiss = {
                    showRegisterPage = false
                    resetSheetState()
                },
                onGotoLogin = {
                    showRegisterPage = false
                    resetSheetState()
                    preferEmailLogin = false
                    sheetPage = SheetPage.Login
                    showCodeLoginPage = true
                },
                toastState = toastState,
            )
        }

        // ── 账号已注册弹窗 ──
        AnimatedVisibility(
            visible = showAccountRegisteredDialog,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showAccountRegisteredDialog = false },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = showAccountRegisteredDialog,
                    enter = scaleIn(initialScale = 0.85f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                    exit = scaleOut(targetScale = 0.85f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
                ) {
                    AccountRegisteredDialog(
                        onDismiss = { showAccountRegisteredDialog = false },
                        onGotoLogin = {
                            showAccountRegisteredDialog = false
                            resetSheetState()
                            preferEmailLogin = false
                            sheetPage = SheetPage.Login
                        },
                    )
                }
            }
        }

        // ── 验证码登录全页面 ──
        AnimatedVisibility(
            visible = showCodeLoginPage,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(160)),
        ) {
            val ds = LocalDesignScale.current
            val uriHandler = LocalUriHandler.current

            val canSendCode = Regex("^1\\d{10}$").matches(username.trim()) && accountCheckPassed
            val isSettingPassword = needsRegister
            val passwordRuleOk = !isSettingPassword ||
                (validatePasswordRule(password) == null && password.isNotEmpty() && password == confirmPassword)
            val canSubmit = username.isNotBlank() && verifyCode.isNotBlank() && !isLoading &&
                (!needsRegister || (password.isNotBlank() && confirmPassword.isNotBlank())) &&
                passwordRuleOk
            val passwordErr = if (isSettingPassword) validatePasswordRule(password) else null
            val confirmPwdErr = if (isSettingPassword && confirmPassword.isNotEmpty() && password != confirmPassword) "两次输入的密码不一致" else null

            var hadFocus by remember { mutableStateOf(false) }

            val codeInputShape = RoundedCornerShape(80.dp)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAFAFC))
                    .statusBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { focusManager.clearFocus() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .padding(horizontal = ds.sw(20.dp)),
                ) {
                    Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                    // ── 返回按钮 ──
                    Box(
                        modifier = Modifier
                            .size(ds.sm(32.dp))
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.05f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) {
                                showCodeLoginPage = false
                                resetSheetState()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = com.cephalon.lucyApp.screens.agentmodel.BackIcon,
                            contentDescription = "Back",
                            tint = Color.Black.copy(alpha = 0.60f),
                            modifier = Modifier.size(
                                width = ds.sw(11.dp),
                                height = ds.sh(17.dp),
                            ),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Logo 距返回 icon 44px
                        Spacer(modifier = Modifier.height(ds.sh(44.dp)))
                        Image(
                            painter = painterResource(Res.drawable.logo_img),
                            contentDescription = null,
                            modifier = Modifier
                                .size(ds.sm(64.dp))
                                .clip(RoundedCornerShape(ds.sm(12.dp))),
                        )

                        // 标题 距 logo 24px
                        Spacer(modifier = Modifier.height(ds.sh(24.dp)))
                        Text(
                            text = "欢迎使用脑花",
                            color = Color.Black.copy(alpha = 0.90f),
                            fontSize = ds.sp(16f),
                            fontWeight = FontWeight.Medium,
                        )

                        // 副标题 距标题 8px
                        Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                        Text(
                            text = "AI 驱动的个人数据操作系统",
                            color = Color.Black.copy(alpha = 0.60f),
                            fontSize = ds.sp(12f),
                            fontWeight = FontWeight.Normal,
                        )

                        // 输入框距副标题 24px
                        Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                        // 手机号输入
                        PhoneOnlyInput(
                            value = username,
                            onValueChange = { if (it != username) { username = it; accountCheckPassed = false; if (needsRegister) { needsRegister = false; sheetTitle = "Welcome to Lucy" } } },
                            label = "请输入您手机号",
                            enabled = !isLoading,
                            containerShape = codeInputShape,
                            containerShadowElevation = 20.dp,
                            placeholderFontSize = 12f,
                            placeholderColor = Color.Black.copy(alpha = 0.40f),
                            inputFontSize = 14f,
                            modifier = Modifier.onFocusChanged { focusState ->
                                if (hadFocus && !focusState.isFocused) validateAccount()
                                hadFocus = focusState.isFocused
                            },
                        )

                        // 输入框间隔 16px
                        Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                        // 验证码输入
                        CodeInput(
                            value = verifyCode,
                            onValueChange = { verifyCode = it },
                            enabled = !isLoading,
                            canSend = canSendCode,
                            containerShape = codeInputShape,
                            containerShadowElevation = 20.dp,
                            placeholderFontSize = 12f,
                            placeholderColor = Color.Black.copy(alpha = 0.40f),
                            inputFontSize = 14f,
                            onSendCode = { startTimer ->
                                val phone = normalizeCurrentAccount()
                                if (phone == null) {
                                    toastState.show("请先输入正确的11位手机号")
                                    return@CodeInput
                                }
                                val actionType = if (needsRegister) "register" else "login"
                                scope.launch {
                                    isLoading = true
                                    val response = authRepository.getCode(phone = phone, actionType = actionType, appType = "lucy")
                                    isLoading = false
                                    if (response.code == 20000) startTimer() else toastState.show(response.msg)
                                }
                            },
                        )

                        // 未注册时弹出密码设置
                        AnimatedVisibility(
                            visible = needsRegister,
                            enter = fadeIn() + androidx.compose.animation.expandVertically(),
                            exit = fadeOut() + androidx.compose.animation.shrinkVertically(),
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                                PasswordInput(
                                    value = password,
                                    onValueChange = { password = it },
                                    enabled = !isLoading,
                                    label = "设置密码",
                                    errorText = passwordErr,
                                )
                                Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                                PasswordInput(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    enabled = !isLoading,
                                    label = "再次输入密码",
                                    errorText = confirmPwdErr,
                                )
                            }
                        }

                        // 按钮距输入框 32px
                        Spacer(modifier = Modifier.height(ds.sh(32.dp)))

                        // 登录按钮
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ds.sh(40.dp))
                                .clip(RoundedCornerShape(80.dp))
                                .background(if (canSubmit) Color.Black else Color.Black.copy(alpha = 0.30f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = canSubmit,
                                ) { performLogin() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(ds.sm(24.dp)),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = if (needsRegister) "立即注册" else "登录",
                                    color = Color.White,
                                    fontSize = ds.sp(16f),
                                    fontWeight = FontWeight.Normal,
                                )
                            }
                        }

                        // 底部留白，确保键盘弹起时登录按钮可滚动到可见区域
                        Spacer(modifier = Modifier.height(ds.sh(40.dp)))
                    }

                    // ── 底部协议文本 距底部安全距离 10px ──
                    val termsText = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                            append("登录即表示同意我们的")
                        }
                        withLink(LinkAnnotation.Clickable(tag = "SERVICE") {
                            uriHandler.openUri("https://app.lucy.run/service.html")
                        }) {
                            withStyle(SpanStyle(
                                color = Color.Black,
                                textDecoration = TextDecoration.Underline,
                            )) {
                                append("《服务条款》")
                            }
                        }
                        withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                            append("和")
                        }
                        withLink(LinkAnnotation.Clickable(tag = "PRIVACY") {
                            uriHandler.openUri("https://app.lucy.run/privacy.html")
                        }) {
                            withStyle(SpanStyle(
                                color = Color.Black,
                                textDecoration = TextDecoration.Underline,
                            )) {
                                append("《隐私政策》")
                            }
                        }
                    }
                    Text(
                        text = termsText,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = ds.sp(10f),
                            fontWeight = FontWeight.Normal,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = ds.sh(10.dp)),
                    )
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
            .height(ds.sh(40.dp))
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

/* ───────── Password Login Page (full page) ───────── */

@Composable
private fun PasswordLoginPage(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    verifyCode: String,
    onVerifyCodeChange: (String) -> Unit,
    isLoading: Boolean,
    needsRegister: Boolean,
    accountCheckPassed: Boolean,
    onFocusLostValidate: () -> Unit,
    onSubmit: () -> Unit,
    onSendCode: (startTimer: () -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onForgotPasswordSuccess: () -> Unit,
    toastState: ToastState,
) {
    val ds = LocalDesignScale.current
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    var hadFocus by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }

    val codeInputShape = RoundedCornerShape(80.dp)

    val isSettingPassword = needsRegister
    val passwordErr = if (isSettingPassword) validatePasswordRule(password) else null
    val confirmPwdErr = if (isSettingPassword && confirmPassword.isNotEmpty() && password != confirmPassword) "两次输入的密码不一致" else null
    val passwordRuleOk = !isSettingPassword ||
        (validatePasswordRule(password) == null && password.isNotEmpty() && password == confirmPassword)

    val canSendCode = normalizePhone(username) != null && accountCheckPassed
    val canSubmit = username.isNotBlank() && password.isNotBlank() && !isLoading &&
        (!needsRegister || (confirmPassword.isNotBlank() && verifyCode.isNotBlank())) &&
        passwordRuleOk

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = ds.sw(20.dp)),
        ) {
            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            // ── 返回按钮 ──
            Box(
                modifier = Modifier
                    .size(ds.sm(32.dp))
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.05f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        if (showForgotPassword) {
                            showForgotPassword = false
                        } else {
                            onDismiss()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = com.cephalon.lucyApp.screens.agentmodel.BackIcon,
                    contentDescription = "Back",
                    tint = Color.Black.copy(alpha = 0.60f),
                    modifier = Modifier.size(
                        width = ds.sw(11.dp),
                        height = ds.sh(17.dp),
                    ),
                )
            }

            if (showForgotPassword) {
                // ForgotPasswordForm 需要有界高度才能正常使用 fillMaxSize/weight，
                // 所以放在 weight(1f) 容器中而非 verticalScroll 内部
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding()
                        .padding(bottom = ds.sh(10.dp)),
                ) {
                    ForgotPasswordForm(
                        onResetSuccess = onForgotPasswordSuccess,
                        onShowToast = { toastState.show(it) },
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(ds.sh(44.dp)))
                    Image(
                        painter = painterResource(Res.drawable.logo_img),
                        contentDescription = null,
                        modifier = Modifier
                            .size(ds.sm(64.dp))
                            .clip(RoundedCornerShape(ds.sm(12.dp))),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(24.dp)))
                    Text(
                        text = "欢迎使用脑花",
                        color = Color.Black.copy(alpha = 0.90f),
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                    Text(
                        text = "AI 驱动的个人数据操作系统",
                        color = Color.Black.copy(alpha = 0.60f),
                        fontSize = ds.sp(12f),
                        fontWeight = FontWeight.Normal,
                    )
                    Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                    // ── 密码登录表单 ──
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PhoneOnlyInput(
                            value = username,
                            onValueChange = onUsernameChange,
                            label = "请输入您手机号",
                            enabled = !isLoading,
                            containerShape = codeInputShape,
                            containerShadowElevation = 20.dp,
                            placeholderFontSize = 12f,
                            placeholderColor = Color.Black.copy(alpha = 0.40f),
                            inputFontSize = 14f,
                            modifier = Modifier.onFocusChanged { focusState ->
                                if (hadFocus && !focusState.isFocused) onFocusLostValidate()
                                hadFocus = focusState.isFocused
                            },
                        )

                        // 未注册时弹出验证码输入
                        AnimatedVisibility(
                            visible = needsRegister,
                            enter = fadeIn() + androidx.compose.animation.expandVertically(),
                            exit = fadeOut() + androidx.compose.animation.shrinkVertically(),
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                                CodeInput(
                                    value = verifyCode,
                                    onValueChange = onVerifyCodeChange,
                                    enabled = !isLoading,
                                    canSend = canSendCode,
                                    containerShape = codeInputShape,
                                    containerShadowElevation = 20.dp,
                                    placeholderFontSize = 12f,
                                    placeholderColor = Color.Black.copy(alpha = 0.40f),
                                    inputFontSize = 14f,
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
                            errorText = passwordErr,
                            containerShape = codeInputShape,
                            containerShadowElevation = 20.dp,
                            placeholderFontSize = 12f,
                            placeholderColor = Color.Black.copy(alpha = 0.40f),
                            inputFontSize = 14f,
                        )

                        // 忘记密码
                        if (!needsRegister) {
                            Spacer(modifier = Modifier.height(ds.sh(4.dp)))
                            Text(
                                text = "忘记密码",
                                color = Color.Black.copy(alpha = 0.90f),
                                fontSize = ds.sp(12f),
                                fontWeight = FontWeight.Normal,
                                textDecoration = TextDecoration.Underline,
                                lineHeight = ds.sp(16f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { showForgotPassword = true },
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            )
                        }

                        // 未注册时弹出确认密码
                        AnimatedVisibility(
                            visible = needsRegister,
                            enter = fadeIn() + androidx.compose.animation.expandVertically(),
                            exit = fadeOut() + androidx.compose.animation.shrinkVertically(),
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                                PasswordInput(
                                    value = confirmPassword,
                                    onValueChange = onConfirmPasswordChange,
                                    enabled = !isLoading,
                                    label = "再次输入密码",
                                    errorText = confirmPwdErr,
                                    containerShape = codeInputShape,
                                    containerShadowElevation = 20.dp,
                                    placeholderFontSize = 12f,
                                    placeholderColor = Color.Black.copy(alpha = 0.40f),
                                    inputFontSize = 14f,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(ds.sh(12.dp)))

                        // 登录按钮
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ds.sh(40.dp))
                                .clip(RoundedCornerShape(80.dp))
                                .background(if (canSubmit) Color.Black else Color.Black.copy(alpha = 0.30f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = canSubmit,
                                ) { onSubmit() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(ds.sm(24.dp)),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = if (needsRegister) "立即注册" else "登录",
                                    color = Color.White,
                                    fontSize = ds.sp(16f),
                                    fontWeight = FontWeight.Normal,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(ds.sh(40.dp)))
                }

                // ── 底部协议 ──
                val termsText = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                        append("登录即表示同意我们的")
                    }
                    withLink(LinkAnnotation.Clickable(tag = "SERVICE") {
                        uriHandler.openUri("https://app.lucy.run/service.html")
                    }) {
                        withStyle(SpanStyle(
                            color = Color.Black,
                            textDecoration = TextDecoration.Underline,
                        )) {
                            append("《服务条款》")
                        }
                    }
                    withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                        append("和")
                    }
                    withLink(LinkAnnotation.Clickable(tag = "PRIVACY") {
                        uriHandler.openUri("https://app.lucy.run/privacy.html")
                    }) {
                        withStyle(SpanStyle(
                            color = Color.Black,
                            textDecoration = TextDecoration.Underline,
                        )) {
                            append("《隐私政策》")
                        }
                    }
                }
                Text(
                    text = termsText,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = ds.sp(10f),
                        fontWeight = FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = ds.sh(10.dp)),
                )
            }
        }
    }
}

/* ───────── Register Page (full page) ───────── */

@Composable
private fun RegisterPage(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    verifyCode: String,
    onVerifyCodeChange: (String) -> Unit,
    isLoading: Boolean,
    accountCheckPassed: Boolean,
    accountRegistered: Boolean,
    onFocusLostValidate: () -> Unit,
    onSubmit: () -> Unit,
    onSendCode: (startTimer: () -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onGotoLogin: () -> Unit,
    toastState: ToastState,
) {
    val ds = LocalDesignScale.current
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    var hadFocus by remember { mutableStateOf(false) }

    val codeInputShape = RoundedCornerShape(80.dp)

    val passwordErr = validatePasswordRule(password)
    val confirmPwdErr = if (confirmPassword.isNotEmpty() && password != confirmPassword) "两次输入的密码不一致" else null
    val passwordRuleOk = validatePasswordRule(password) == null && password.isNotEmpty() && password == confirmPassword

    val canSendCode = normalizePhone(username) != null && !accountRegistered && accountCheckPassed
    val canSubmit = !accountRegistered &&
        username.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() &&
        verifyCode.isNotBlank() && !isLoading && passwordRuleOk

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = ds.sw(20.dp)),
        ) {
            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            // ── 返回按钮 ──
            Box(
                modifier = Modifier
                    .size(ds.sm(32.dp))
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.05f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = com.cephalon.lucyApp.screens.agentmodel.BackIcon,
                    contentDescription = "Back",
                    tint = Color.Black.copy(alpha = 0.60f),
                    modifier = Modifier.size(
                        width = ds.sw(11.dp),
                        height = ds.sh(17.dp),
                    ),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(ds.sh(44.dp)))
                Image(
                    painter = painterResource(Res.drawable.logo_img),
                    contentDescription = null,
                    modifier = Modifier
                        .size(ds.sm(64.dp))
                        .clip(RoundedCornerShape(ds.sm(12.dp))),
                )
                Spacer(modifier = Modifier.height(ds.sh(24.dp)))
                Text(
                    text = "注册账号",
                    color = Color.Black.copy(alpha = 0.90f),
                    fontSize = ds.sp(16f),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                Text(
                    text = "AI 驱动的个人数据操作系统",
                    color = Color.Black.copy(alpha = 0.60f),
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.Normal,
                )
                Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                // 手机号输入
                PhoneOnlyInput(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = "请输入您手机号",
                    enabled = !isLoading,
                    containerShape = codeInputShape,
                    containerShadowElevation = 20.dp,
                    placeholderFontSize = 12f,
                    placeholderColor = Color.Black.copy(alpha = 0.40f),
                    inputFontSize = 14f,
                    modifier = Modifier.onFocusChanged { focusState ->
                        if (hadFocus && !focusState.isFocused) onFocusLostValidate()
                        hadFocus = focusState.isFocused
                    },
                )

                Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                // 验证码输入
                CodeInput(
                    value = verifyCode,
                    onValueChange = onVerifyCodeChange,
                    enabled = !isLoading,
                    canSend = canSendCode,
                    containerShape = codeInputShape,
                    containerShadowElevation = 20.dp,
                    placeholderFontSize = 12f,
                    placeholderColor = Color.Black.copy(alpha = 0.40f),
                    inputFontSize = 14f,
                    onSendCode = onSendCode,
                )

                Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                // 密码
                PasswordInput(
                    value = password,
                    onValueChange = onPasswordChange,
                    enabled = !isLoading,
                    label = "设置密码",
                    errorText = passwordErr,
                    containerShape = codeInputShape,
                    containerShadowElevation = 20.dp,
                    placeholderFontSize = 12f,
                    placeholderColor = Color.Black.copy(alpha = 0.40f),
                    inputFontSize = 14f,
                )

                Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                // 确认密码
                PasswordInput(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    enabled = !isLoading,
                    label = "再次输入密码",
                    errorText = confirmPwdErr,
                    containerShape = codeInputShape,
                    containerShadowElevation = 20.dp,
                    placeholderFontSize = 12f,
                    placeholderColor = Color.Black.copy(alpha = 0.40f),
                    inputFontSize = 14f,
                )

                // 账号已注册提示
                if (accountRegistered) {
                    Spacer(modifier = Modifier.height(ds.sh(12.dp)))
                    Text(
                        text = "该手机号已被注册，前往登录",
                        color = Color.Black.copy(alpha = 0.60f),
                        fontSize = ds.sp(12f),
                        fontWeight = FontWeight.Normal,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onGotoLogin() },
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(ds.sh(32.dp)))

                // 注册按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ds.sh(40.dp))
                        .clip(RoundedCornerShape(80.dp))
                        .background(if (canSubmit) Color.Black else Color.Black.copy(alpha = 0.30f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = canSubmit,
                        ) { onSubmit() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ds.sm(24.dp)),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "立即注册",
                            color = Color.White,
                            fontSize = ds.sp(16f),
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ds.sh(40.dp)))
            }

            // ── 底部协议 ──
            val termsText = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                    append("登录即表示同意我们的")
                }
                withLink(LinkAnnotation.Clickable(tag = "SERVICE") {
                    uriHandler.openUri("https://app.lucy.run/service.html")
                }) {
                    withStyle(SpanStyle(
                        color = Color.Black,
                        textDecoration = TextDecoration.Underline,
                    )) {
                        append("《服务条款》")
                    }
                }
                withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                    append("和")
                }
                withLink(LinkAnnotation.Clickable(tag = "PRIVACY") {
                    uriHandler.openUri("https://app.lucy.run/privacy.html")
                }) {
                    withStyle(SpanStyle(
                        color = Color.Black,
                        textDecoration = TextDecoration.Underline,
                    )) {
                        append("《隐私政策》")
                    }
                }
            }
            Text(
                text = termsText,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = ds.sp(10f),
                    fontWeight = FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = ds.sh(10.dp)),
            )
        }
    }
}

// 归一化手机号（提取为顶级函数供多个 Page 复用）
private fun normalizePhone(input: String): String? {
    val raw = input.trim().replace(" ", "")
    val withoutPrefix = when {
        raw.startsWith("+86") -> raw.removePrefix("+86")
        raw.startsWith("86") && raw.length > 11 -> raw.removePrefix("86")
        else -> raw
    }
    return withoutPrefix.trim().takeIf { Regex("^1\\d{10}$").matches(it) }
}

/* ───────── Account Registered Dialog ───────── */

@Composable
private fun AccountRegisteredDialog(
    onDismiss: () -> Unit,
    onGotoLogin: () -> Unit,
) {
    val ds = LocalDesignScale.current
    Surface(
        shape = RoundedCornerShape(ds.sm(20.dp)),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* consume click */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sw(20.dp), vertical = ds.sh(24.dp)),
        ) {
            Text(
                text = "该账号已被注册，请前往登录",
                fontSize = ds.sp(20f),
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2535),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(ds.sh(24.dp)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ds.sw(11.dp)),
            ) {
                // 取消按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onDismiss() }
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "取消",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1F2535),
                    )
                }

                // 去登录按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color(0xFF1F2535))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onGotoLogin() }
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "去登录",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
