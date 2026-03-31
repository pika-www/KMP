package com.jetbrains.kmpapp.di

/**
 * 项目环境配置
 */
object AppConfig {
    // 是否为生产环境 (可以通过 Gradle 编译参数动态控制，这里先手动定义)
    private const val IS_PROD = false

    val BASE_URL: String = if (IS_PROD) {
        "https://prod.unicorn.org.cn/cephalon/user-center/v1/"
    } else {
        "https://test.unicorn.org.cn/cephalon/user-center/v1/"
    }

    const val TIMEOUT_MILLIS = 20000L
}
