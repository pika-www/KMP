package com.example.androidios.di

/**
 * 网络基础配置，只负责环境与域名，不承载业务路径。
 */
data class NetworkConfig(
    val baseUrl: String,
    val timeoutMillis: Long
)

object AppConfig {
    // 是否为生产环境（后续可以再切到 Gradle 或平台侧注入）
    private const val IS_PROD = false

    val baseDomain: String = if (IS_PROD) {
        "https://prod.unicorn.org.cn"
    } else {
        "https://test.unicorn.org.cn"
    }

    const val TIMEOUT_MILLIS = 20000L

    val networkConfig = NetworkConfig(
        baseUrl = baseDomain,
        timeoutMillis = TIMEOUT_MILLIS
    )
}
