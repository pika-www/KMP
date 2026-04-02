package com.example.androidios.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidios.api.AuthInput
import com.example.androidios.api.AuthRepository
import com.example.androidios.api.CodeRequest
import com.example.androidios.api.ForgetPasswordRequest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit
) {
    val authRepository = koinInject<AuthRepository>()
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("忘记密码") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = account,
                onValueChange = {
                    account = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("手机号或邮箱") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("验证码") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pwd,
                onValueChange = {
                    pwd = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("新密码") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPwd,
                onValueChange = {
                    confirmPwd = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("确认新密码") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (error.isNotBlank()) {
                Text(error)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val normalized = AuthInput.normalizeAccount(account)
                    if (normalized.isBlank()) {
                        error = "请输入手机号或邮箱"
                        return@Button
                    }
                    val isEmail = AuthInput.isEmail(normalized)

                    isLoading = true
                    error = ""
                    scope.launch {
                        val resp = authRepository.getCode(
                            CodeRequest(
                                actionType = "modify",
                                appType = if (isEmail) null else "platform",
                                email = if (isEmail) normalized else null,
                                phone = if (isEmail) null else normalized
                            )
                        )
                        isLoading = false
                        if (resp.code != 20000) {
                            error = resp.msg
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("获取验证码")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val normalized = AuthInput.normalizeAccount(account)
                    if (normalized.isBlank() || code.isBlank() || pwd.isBlank() || confirmPwd.isBlank()) {
                        error = "请填写完整信息"
                        return@Button
                    }
                    if (pwd != confirmPwd) {
                        error = "两次密码不一致"
                        return@Button
                    }

                    val isEmail = AuthInput.isEmail(normalized)
                    val type = if (isEmail) "email" else "phone"

                    isLoading = true
                    error = ""
                    scope.launch {
                        val resp = authRepository.forgetPassword(
                            ForgetPasswordRequest(
                                account = normalized,
                                code = code,
                                confirmPwd = confirmPwd,
                                pwd = pwd,
                                type = type
                            )
                        )
                        isLoading = false
                        if (resp.code == 20000) {
                            onBack()
                        } else {
                            error = resp.msg
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重置密码")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onBack,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回")
            }
        }
    }
}
