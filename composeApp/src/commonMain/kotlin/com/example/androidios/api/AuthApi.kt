package com.example.androidios.api

import com.example.androidios.network.NetworkPaths
import com.example.androidios.network.NetworkUrlFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private object AuthApiRoutes {
    private const val AUTH_PREFIX = NetworkPaths.API_PREFIX
    const val LOGIN = "$AUTH_PREFIX/login"
}

/**
 * 认证接口定义，集中管理登录模块的请求路径。
 */
class AuthApi(
    private val client: HttpClient,
    private val urlFactory: NetworkUrlFactory
) {

    suspend fun login(request: LoginRequest): BaseResponse<LoginData> {
        return client.post(urlFactory.http(AuthApiRoutes.LOGIN)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
