package com.example.androidios.di

import com.example.androidios.api.AuthApi
import com.example.androidios.api.AuthRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * 依赖注入模块配置
 */
val appModule = module {
    // 1. 环境配置与 HttpClient
    single { AppConfig.networkConfig }
    single { createHttpClient(get()) }

    // 2. 业务 API 与 Repository
    single { AuthApi(get()) }
    single { AuthRepository(get()) }

    // 在这里添加您的其他依赖注入配置，例如 ViewModel
}

fun initKoin() {
    // 使用 startKoin 的变体或手动检查，防止重复启动
    try {
        startKoin {
            modules(appModule)
        }
    } catch (e: Exception) {
        println("Koin 已经启动或初始化失败: ${e.message}")
    }
}
