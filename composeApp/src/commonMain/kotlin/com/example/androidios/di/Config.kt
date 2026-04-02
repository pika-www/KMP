package com.example.androidios.di

import com.example.androidios.network.NetworkConfig

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
