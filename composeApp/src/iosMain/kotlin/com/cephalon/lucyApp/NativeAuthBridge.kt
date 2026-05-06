package com.cephalon.lucyApp

import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.LoginRequest
import com.cephalon.lucyApp.auth.AuthTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NativeAuthResult(
    val success: Boolean,
    val code: Int,
    val message: String,
)

class NativeAuthBridge : KoinComponent {
    private val authRepository: AuthRepository by inject()
    private val tokenStore: AuthTokenStore by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun hasValidToken(): Boolean = authRepository.hasValidToken()

    fun storedPhone(): String = tokenStore.getUserPhone().orEmpty()

    fun loginWithPassword(
        phone: String,
        password: String,
        completion: (NativeAuthResult) -> Unit,
    ) {
        val normalizedPhone = phone.trim()
        val normalizedPassword = password.trim()

        when {
            normalizedPhone.isBlank() || normalizedPassword.isBlank() -> {
                completion(NativeAuthResult(false, -1, "请输入手机号和密码"))
                return
            }

            !Regex("^1\\d{10}$").matches(normalizedPhone) -> {
                completion(NativeAuthResult(false, -1, "请输入正确的手机号"))
                return
            }
        }

        scope.launch {
            val result = runCatching {
                authRepository.login(
                    LoginRequest(
                        phone = normalizedPhone,
                        pwd = normalizedPassword,
                        trackId = "ios-native",
                        appType = "platform",
                        way = "phone_pwd",
                    ),
                )
            }.fold(
                onSuccess = { response ->
                    if (response.code == 20000 && response.data != null) {
                        NativeAuthResult(
                            success = true,
                            code = response.code,
                            message = response.msg.ifBlank { "登录成功" },
                        )
                    } else {
                        NativeAuthResult(
                            success = false,
                            code = response.code,
                            message = response.msg.ifBlank { "登录失败，请稍后重试" },
                        )
                    }
                },
                onFailure = { error ->
                    NativeAuthResult(
                        success = false,
                        code = -1,
                        message = error.message ?: "网络连接失败",
                    )
                },
            )
            completion(result)
        }
    }
}
