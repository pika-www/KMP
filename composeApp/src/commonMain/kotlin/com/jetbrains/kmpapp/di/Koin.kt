package com.jetbrains.kmpapp.di

import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    // 在这里添加您的依赖注入配置
}

fun initKoin() {
    startKoin {
        modules(appModule)
    }
}
