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

    val canSubmit = account.isNotBlank() && code.isNotBlank() && pwd.isNotBlank() && confirmPwd.isNotBlank() && !isLoading

    val performReset: () -> Unit = performReset@{
        if (!canSubmit) return@performReset
        if (pwd != confirmPwd) {
            showError("两次输入的密码不一致")
            return@performReset
        }
        isLoading = true
        val input = account.trim()
        val normalizedEmail = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.com$").matchEntire(input)?.value
        val normalizedPhone = input.replace(" ", "").removePrefix("+86").let { v ->
            if (Regex("^1\\d{10}$").matches(v)) v else null
        }
        val isEmail = normalizedEmail != null
        val normalizedAccount = normalizedEmail ?: normalizedPhone
        if (normalizedAccount == null) {
            isLoading = false
            showError("请输入正确的手机号(+86 11位)或邮箱(.com)")
            return@performReset
        }
        val accountType = if (isEmail) "email" else "phone"

        scope.launch {
            try {
                val existsResponse = if (isEmail) {
                    authRepository.isEmailExist(normalizedAccount)
                } else {
                    authRepository.isPhoneExist(normalizedAccount)
                }
                if (existsResponse.code == 20000) {
                    val exists = existsResponse.data?.isExist ?: false
                    if (!exists) {
                        isLoading = false
                        showError("该账号未注册")
                        return@launch
                    }
                }

                val response = authRepository.forgetPassword(
                    ForgetPasswordRequest(
                        account = normalizedAccount,
                        code = code,
                        pwd = pwd,
                        confirmPwd = confirmPwd,
                        type = accountType
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

            AccountInput(
                value = account,
                onValueChange = { account = it },
                enabled = !isLoading,
                imeAction = ImeAction.Next,
                onValidationError = { msg -> showError(msg) }
            )

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            CodeInput(
                value = code,
                onValueChange = { code = it },
                enabled = !isLoading,
                imeAction = ImeAction.Next,
                onSendCode = { startTimer ->
                    val input = account.trim()
                    val normalizedEmail = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.com$").matchEntire(input)?.value
                    val normalizedPhone = input.replace(" ", "").removePrefix("+86").let { v ->
                        if (Regex("^1\\d{10}$").matches(v)) v else null
                    }
                    val isEmail = normalizedEmail != null
                    val normalizedAccount = normalizedEmail ?: normalizedPhone
                    if (normalizedAccount == null) {
                        showError("请输入正确的手机号(+86 11位)或邮箱(.com)")
                        return@CodeInput
                    }

                    scope.launch {
                        isLoading = true
                        val existsResponse = if (isEmail) {
                            authRepository.isEmailExist(normalizedAccount)
                        } else {
                            authRepository.isPhoneExist(normalizedAccount)
                        }
                        if (existsResponse.code == 20000) {
                            val exists = existsResponse.data?.isExist ?: false
                            if (!exists) {
                                isLoading = false
                                showError("该账号未注册")
                                return@launch
                            }
                        }

                        val response = if (isEmail) {
                            authRepository.getCode(email = normalizedAccount, actionType = "modify", appType = "lucy")
                        } else {
                            authRepository.getCode(phone = normalizedAccount, actionType = "modify", appType = "lucy")
                        }
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
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            PasswordInput(
                value = confirmPwd,
                onValueChange = { confirmPwd = it },
                enabled = !isLoading,
                label = "再次输入密码",
                imeAction = ImeAction.Done,
                onDone = { performReset() }
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
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(ds.sh(24.dp)))
    }
}
