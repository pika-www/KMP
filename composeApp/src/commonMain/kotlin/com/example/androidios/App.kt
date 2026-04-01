package com.example.androidios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.androidios.screens.HomeScreen
import com.example.androidios.screens.LoginScreen
import kotlinx.serialization.Serializable

@Serializable
object LoginDestination

@Serializable
object HomeDestination

@Composable
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            var isLoggedIn by remember { mutableStateOf(false) }

            if (!isLoggedIn) {
                LoginScreen(onLoginSuccess = {
                    // 登录成功后清空回退栈，防止返回到登录页
                    isLoggedIn = true
                })
            } else {
                HomeScreen(onLogout = {
                    isLoggedIn = false
                })
            }
        }
    }
}
