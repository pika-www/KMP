package com.example.androidios.api

import com.example.androidios.auth.AuthTokenStore

/**
 * 登录相关接口
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore
) {

    /**
     * 登录请求
     * @param request 包含用户名和密码的请求体
     * @return 返回封装好的响应结果
     */
    suspend fun login(request: LoginRequest): BaseResponse<LoginData> {
        return try {
            val response = authApi.login(request)
            val token = response.data?.token
            if (response.code == 20000 && token != null) {
                tokenStore.saveToken(token)
            }
            response
        } catch (e: Exception) {
            // 网络异常或解析异常处理
            BaseResponse(code = -1, msg = e.message ?: "网络连接失败")
        }
    }

    suspend fun getCode(request: CodeRequest): BaseResponse<Unit> {
        return try {
            authApi.getCode(request)
        } catch (e: Exception) {
            BaseResponse(code = -1, msg = e.message ?: "网络连接失败")
        }
    }

    suspend fun isPhoneExist(phone: String): BaseResponse<Unit> {
        return try {
            authApi.isPhoneExist(phone)
        } catch (e: Exception) {
            BaseResponse(code = -1, msg = e.message ?: "网络连接失败")
        }
    }

    suspend fun forgetPassword(request: ForgetPasswordRequest): BaseResponse<Unit> {
        return try {
            authApi.forgetPassword(request)
        } catch (e: Exception) {
            BaseResponse(code = -1, msg = e.message ?: "网络连接失败")
        }
    }

    fun logout() {
        tokenStore.clear()
    }

    fun hasValidToken(): Boolean = tokenStore.getValidTokenOrNull() != null
}
