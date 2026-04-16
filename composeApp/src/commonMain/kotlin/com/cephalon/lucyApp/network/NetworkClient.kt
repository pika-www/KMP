package com.cephalon.lucyApp.network

import com.cephalon.lucyApp.auth.AuthTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * 统一的网络客户端，HTTP 与 WebSocket 共用一套配置和拦截能力。
 */
fun createNetworkClient(
    config: NetworkConfig,
    tokenStore: AuthTokenStore
): HttpClient {
    return HttpClient {
        defaultRequest {
            url(config.baseUrl)
            header("Lang", "zh")
            val token = tokenStore.getTokenOrNull()
            println("[NetworkClient] defaultRequest: token=${if (token != null) "${token.take(20)}..." else "null"}, url=${this.url.buildString()}")
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMillis
            connectTimeoutMillis = config.timeoutMillis
            socketTimeoutMillis = config.timeoutMillis
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
                encodeDefaults = true
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }

        install(WebSockets)

        HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                if (statusCode == 401 || statusCode == 40000) {                         
                    println("登录凭证失效，请重新登录")
                    tokenStore.clear()
                }
            }

            handleResponseExceptionWithRequest { exception, _ ->
                println("网络异常: ${exception.message}")
            }
        }
    }
}