package com.example.androidios

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.jetbrains.kmpapp.screens.LoginScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        // 1. 定义一个状态来记录当前是否已登录
        var isLoggedIn by remember { mutableStateOf(false) }

        // 2. 根据登录状态显示不同的页面
        if (!isLoggedIn) {
            // 如果未登录，显示登录界面
            LoginScreen(
                onLoginSuccess = {
                    // 3. 登录成功时，修改状态为 true
                    isLoggedIn = true
                    println("登录成功！状态已更新")
                }
            )
        } else {
            // 4. 如果已登录，显示主页内容（这里暂时用简单的 Text 代替）
            // 你以后可以把这里换成你的主页组件，比如 HomeScreen()
            Text("恭喜！你已成功进入主页 🎉")
        }
    }
}
