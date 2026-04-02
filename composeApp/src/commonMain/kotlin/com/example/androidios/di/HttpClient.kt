package com.example.androidios.di

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * 创建并配置 Ktor HttpClient，包含请求/响应拦截逻辑
 */
fun createHttpClient(config: NetworkConfig): HttpClient {
    return HttpClient {
        // 1. 设置基础域名和超时
        defaultRequest {
            url(config.baseUrl)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header("Lang", "zh")
        }

        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMillis
            connectTimeoutMillis = config.timeoutMillis
            socketTimeoutMillis = config.timeoutMillis
        }

        // 2. JSON 序列化配置
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
                encodeDefaults = true
            })
        }

        // 3. 日志拦截器 (类似 Axios 的请求/响应日志)
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }

        // 4. 请求拦截器 (类似 Axios 的 request interceptor)
        install(DefaultRequest) {
            // 这里可以添加全局 Token
            // val token = getToken()
            // if (token != null) header(HttpHeaders.Authorization, token)
        }

        // 5. 响应拦截器与错误处理 (类似 Axios 的 response interceptor)
        HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                // 模拟 Axios 的业务码处理逻辑 (如 40000 登录失效)
                // 这里可以根据实际业务返回的 JSON 结构进一步解析
                if (statusCode == 401 || statusCode == 40000) {
                    // 处理登录失效逻辑，例如跳转到登录页
                    println("登录凭证失效，请重新登录")
                }
            }

            handleResponseExceptionWithRequest { exception, _ ->
                // 网络异常处理
                println("网络异常: ${exception.message}")
            }
        }
    }
}
