package com.cephalon.lucyApp.di

import com.cephalon.lucyApp.AppEnvironment
import com.cephalon.lucyApp.appEnvironment
import com.cephalon.lucyApp.network.NetworkConfig

object AppConfig {
    val env: AppEnvironment get() = appEnvironment

    val baseDomain: String get() = when (env) {
        AppEnvironment.DEBUG   -> "https://test.unicorn.org.cn"
        AppEnvironment.TEST    -> "https://test.unicorn.org.cn"
        AppEnvironment.RELEASE -> "https://prod.unicorn.org.cn"
    }

    const val TIMEOUT_MILLIS = 20000L

    val networkConfig get() = NetworkConfig(
        baseUrl = baseDomain,
        timeoutMillis = TIMEOUT_MILLIS
    )
}
