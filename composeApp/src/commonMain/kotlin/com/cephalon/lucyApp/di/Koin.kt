package com.cephalon.lucyApp.di

import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.api.AuthApi
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.network.NetworkUrlFactory
import com.cephalon.lucyApp.network.createNetworkClient
import com.cephalon.lucyApp.settings.createSettings
import com.cephalon.lucyApp.ws.WsApi
import com.cephalon.lucyApp.ws.BalanceWsManager
import com.cephalon.lucyApp.ws.WsRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * 依赖注入模块配置
 */
val appModule = module {
    // 1. 环境配置与 HttpClient
    single { AppConfig.networkConfig }
    single { createSettings() }
    single { AuthTokenStore(get()) }
    single { NetworkUrlFactory(get()) }
    single { createNetworkClient(get(), get()) }

    // 2. 业务 API 与 Repository
    single { AuthApi(get(), get()) }
    single { AuthRepository(get(), get()) }
    single { WsApi(get(), get()) }
    single { WsRepository(get()) }
    single { BalanceWsManager(get(), get(), get(), get()) }

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
