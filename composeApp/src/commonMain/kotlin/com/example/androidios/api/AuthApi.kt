package com.example.androidios.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

private object AuthApiRoutes {
    private const val AUTH_PREFIX = "/cephalon/user-center/v1"
    const val LOGIN = "$AUTH_PREFIX/login"
}

/**
 * 认证接口定义，集中管理登录模块的请求路径。
 */
class AuthApi(private val client: HttpClient) {

    suspend fun login(request: LoginRequest): BaseResponse<LoginData> {
        return client.post(AuthApiRoutes.LOGIN) {
            setBody(request)
        }.body()
    }
}
