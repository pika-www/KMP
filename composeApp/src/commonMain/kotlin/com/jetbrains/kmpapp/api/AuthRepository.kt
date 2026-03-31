package com.jetbrains.kmpapp.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * 认证相关的仓库类，负责调用登录接口
 */
class AuthRepository(private val client: HttpClient) {

    /**
     * 登录请求
     * @param request 包含用户名和密码的请求体
     * @return 返回封装好的响应结果
     */
    suspend fun login(request: LoginRequest): BaseResponse<LoginData> {
        return try {
            // 这里的请求会自动经过 HttpClient.kt 中配置的拦截器：
            // 1. 自动拼接 BaseURL (测试/生产)
            // 2. 自动添加 Content-Type: application/json
            // 3. 自动打印请求和响应日志
            // 4. 自动处理 401/40000 等业务错误码
            val response = client.post("login") {
                setBody(request)
            }

            // 将响应体反序列化为 BaseResponse<LoginData>
            response.body()
        } catch (e: Exception) {
            // 网络异常或解析异常处理
            BaseResponse(code = -1, msg = e.message ?: "网络连接失败")
        }
    }
}