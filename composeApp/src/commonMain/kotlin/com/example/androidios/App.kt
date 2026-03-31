package com.example.androidios

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.jetbrains.kmpapp.screens.LoginScreen
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import com.jetbrains.kmpapp.api.AuthRepository
import com.jetbrains.kmpapp.di.createHttpClient

// 定义 Koin 模块
val appModule = module {
    single { createHttpClient() } // 注入我们配置好的拦截器客户端
    single { AuthRepository(get()) } // get() 会自动获取上面的 HttpClient
}

@Composable
@Preview
fun App() {
    // 🔹 初始化 Koin
    KoinApplication(
        application = { modules(appModule) }
    ) {
        MaterialTheme {
            // 1. 定义登录状态
            var isLoggedIn by remember { mutableStateOf(false) }

            // 2. 根据状态显示页面
            if (!isLoggedIn) {
                LoginScreen(
                    onLoginSuccess = {
                        isLoggedIn = true
                        println("登录成功！状态已更新")
                    }
                )
            } else {
                Text("恭喜！你已成功进入主页 🎉")
            }
        }
    }
}