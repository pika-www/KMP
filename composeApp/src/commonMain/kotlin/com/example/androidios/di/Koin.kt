package com.example.androidios.di

import com.example.androidios.api.AuthRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * 依赖注入模块配置
 */
val appModule = module {
    // 1. 注入 HttpClient，使用我们自定义的配置和拦截器
    single { createHttpClient() }

    // 2. 注入 AuthRepository，它依赖于 HttpClient
    single { AuthRepository(get()) }

    // 在这里添加您的其他依赖注入配置，例如 ViewModel
}

fun initKoin() {
    startKoin {
        modules(appModule)
    }
}