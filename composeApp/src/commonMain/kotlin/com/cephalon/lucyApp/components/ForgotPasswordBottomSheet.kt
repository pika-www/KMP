package com.cephalon.lucyApp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalUriHandler
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.ForgetPasswordRequest
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordForm(
    onResetSuccess: () -> Unit,
    onShowToast: ((String) -> Unit)? = null,
) {
    val ds = LocalDesignScale.current
    val authRepository = koinInject<AuthRepository>()
    val scope = rememberCoroutineScope()

    // 状态管理
    var account by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val showError: (String) -> Unit = { msg -> onShowToast?.invoke(msg) }

    // 实时计算密码规则 & 两次一致性错误（用于输入框下方红字提示 + 提交按钮置灰）
    val pwdErr = validatePasswordRule(pwd)
    val confirmPwdErr = if (confirmPwd.isNotEmpty() && pwd != confirmPwd) "两次输入的密码不一致" else null

    val canSubmit = account.isNotBlank() && code.isNotBlank() && pwd.isNotBlank() && confirmPwd.isNotBlank() && !isLoading &&
        pwdErr == null && confirmPwdErr == null

    val normalizePhone: () -> String? = {
        val raw = account.trim().replace(" ", "")
        val withoutPrefix = when {
            raw.startsWith("+86") -> raw.removePrefix("+86")
            raw.startsWith("86") && raw.length > 11 -> raw.removePrefix("86")
            else -> raw
        }
        withoutPrefix.trim().takeIf { Regex("^1\\d{10}$").matches(it) }
    }

    val performReset: () -> Unit = performReset@{
        if (!canSubmit) return@performReset
        val phone = normalizePhone()
        if (phone == null) {
            showError("请输入正确的11位手机号")
            return@performReset
        }
        isLoading = true
        scope.launch {
            try {
                val existsResponse = authRepository.isPhoneExist(phone)
                if (existsResponse.code == 20000) {
                    val exists = existsResponse.data?.isExist ?: false
                    if (!exists) {
                        isLoading = false
                        showError("该手机号未注册")
                        return@launch
                    }
                }

                val response = authRepository.forgetPassword(
                    ForgetPasswordRequest(
                        account = phone,
                        code = code,
                        pwd = pwd,
                        confirmPwd = confirmPwd,
                        type = "phone"
                    )
                )
                isLoading = false
                if (response.code == 20000) {
                    onResetSuccess()
                } else {
                    showError(response.msg)
                }
            } catch (e: Exception) {
                isLoading = false
                showError("网络异常，请稍后再试")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 可滚动的输入区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "设置新密码",
                color = TitleColor,
                fontSize = ds.sp(28f),
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(ds.sh(24.dp)))

            PhoneOnlyInput(
                value = account,
                onValueChange = { account = it },
                enabled = !isLoading,
                imeAction = ImeAction.Next,
            )

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            CodeInput(
                value = code,
                onValueChange = { code = it },
                enabled = !isLoading,
                imeAction = ImeAction.Next,
                onSendCode = { startTimer ->
                    val phone = normalizePhone()
                    if (phone == null) {
                        showError("请输入正确的11位手机号")
                        return@CodeInput
                    }
                    scope.launch {
                        isLoading = true
                        val existsResponse = authRepository.isPhoneExist(phone)
                        if (existsResponse.code == 20000) {
                            val exists = existsResponse.data?.isExist ?: false
                            if (!exists) {
                                isLoading = false
                                showError("该手机号未注册")
                                return@launch
                            }
                        }
                        val response = authRepository.getCode(phone = phone, actionType = "modify", appType = "lucy")
                        isLoading = false
                        if (response.code == 20000) {
                            startTimer()
                        } else {
                            showError(response.msg)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            PasswordInput(
                value = pwd,
                onValueChange = { pwd = it },
                enabled = !isLoading,
                label = "设置密码",
                imeAction = ImeAction.Next,
                errorText = pwdErr,
            )

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            PasswordInput(
                value = confirmPwd,
                onValueChange = { confirmPwd = it },
                enabled = !isLoading,
                label = "再次输入密码",
                imeAction = ImeAction.Done,
                onDone = { performReset() },
                errorText = confirmPwdErr,
            )
        }

        // 固定在底部的按钮和条款
        Spacer(modifier = Modifier.height(ds.sh(16.dp)))

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
                ) { performReset() },
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
                    text = "重置并登录",
                    color = Color.White,
                    fontSize = ds.sp(16f),
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        Spacer(modifier = Modifier.height(ds.sh(12.dp)))

        val uriHandler = LocalUriHandler.current
        val termsText = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                append("登录即表示同意我们的 ")
            }
            pushStringAnnotation(tag = "URL", annotation = "https://app.lucy.run/service.html")
            withStyle(SpanStyle(color = LinkColor)) {
                append("《服务条款》")
            }
            pop()
            withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                append(" 和 ")
            }
            pushStringAnnotation(tag = "URL", annotation = "https://app.lucy.run/privacy.html")
            withStyle(SpanStyle(color = LinkColor)) {
                append("《隐私政策》")
            }
            pop()
        }
        ClickableText(
            text = termsText,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = ds.sp(10f),
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                termsText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { uriHandler.openUri(it.item) }
            },
        )

        Spacer(modifier = Modifier.height(ds.sh(24.dp)))
    }
}
