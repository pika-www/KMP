package com.example.androidios.api

/**
 * 认证相关的仓库类，负责调用登录接口
 */
class AuthRepository(private val authApi: AuthApi) {

    /**
     * 登录请求
     * @param request 包含用户名和密码的请求体
     * @return 返回封装好的响应结果
     */
    suspend fun login(request: LoginRequest): BaseResponse<LoginData> {
        return try {
            authApi.login(request)
        } catch (e: Exception) {
            // 网络异常或解析异常处理
            BaseResponse(code = -1, msg = e.message ?: "网络连接失败")
        }
    }
}
