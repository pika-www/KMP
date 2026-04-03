package com.cephalon.lucyApp.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.ForgetPasswordRequest
import com.cephalon.lucyApp.components.AccountInput
import com.cephalon.lucyApp.components.CodeInput
import com.cephalon.lucyApp.components.PasswordInput
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun ForgotPasswordBottomSheet(
    onDismiss: () -> Unit,
    onResetSuccess: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }

    // 统一的退出逻辑，先触发退场动画，动画结束后 onDismissed 会回调 onDismiss
    val requestCloseSheet = {
        isVisible = false
    }

    // 调用重构后的自定义组件
    HalfModalBottomSheet(
        onDismissRequest = { requestCloseSheet() },
        onDismissed = onDismiss,
        isVisible = isVisible,
        // 关键点 2: 将返回按钮与关闭逻辑绑定
        onBack = { requestCloseSheet() },
        showBackButton = true,
        showCloseButton = true
    ) {
        ForgotPasswordForm(
            onResetSuccess = {
                onResetSuccess()
                requestCloseSheet()
            }
        )
    }
}

@Composable
fun ForgotPasswordForm(
    onResetSuccess: () -> Unit,
) {
    val authRepository = koinInject<AuthRepository>()
    val scope = rememberCoroutineScope()

    // 状态管理
    var account by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val performReset: () -> Unit = performReset@{
        if (account.isBlank() || code.isBlank() || pwd.isBlank() || confirmPwd.isBlank()) {
            error = "请填写完整信息"
        } else if (pwd != confirmPwd) {
            error = "两次输入的密码不一致"
        } else {
            isLoading = true
            error = ""
            val input = account.trim()
            val normalizedEmail = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.com$").matchEntire(input)?.value
            val normalizedPhone = input.replace(" ", "").removePrefix("+86").let { v ->
                if (Regex("^1\\d{10}$").matches(v)) v else null
            }
            val isEmail = normalizedEmail != null
            val normalizedAccount = normalizedEmail ?: normalizedPhone
            if (normalizedAccount == null) {
                isLoading = false
                error = "请输入正确的手机号(+86 11位)或邮箱(.com)"
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
                        val isExit = existsResponse.data?.get("is_exit") as? Boolean ?: false
                        if (!isExit) {
                            isLoading = false
                            error = existsResponse.msg
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
                        error = response.msg
                    }
                } catch (e: Exception) {
                    isLoading = false
                    error = "网络异常，请稍后再试"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "重置密码",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        AccountInput(
            value = account,
            onValueChange = { account = it; error = "" },
            enabled = !isLoading,
            imeAction = ImeAction.Next,
            onValidationError = { msg -> error = msg }
        )

        Spacer(modifier = Modifier.height(12.dp))

        CodeInput(
            value = code,
            onValueChange = { code = it; error = "" },
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
                    error = "请输入正确的手机号(+86 11位)或邮箱(.com)"
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
                        val isExit = existsResponse.data?.get("is_exit") as? Boolean ?: false
                        if (!isExit) {
                            isLoading = false
                            error = existsResponse.msg
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
                        error = ""
                        startTimer()
                    } else {
                        error = response.msg
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PasswordInput(
            value = pwd,
            onValueChange = { pwd = it; error = "" },
            enabled = !isLoading,
            label = "新密码",
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(12.dp))

        PasswordInput(
            value = confirmPwd,
            onValueChange = { confirmPwd = it; error = "" },
            enabled = !isLoading,
            label = "确认新密码",
            imeAction = ImeAction.Done,
            onDone = { performReset() }
        )

        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = performReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    "重置并登录",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
