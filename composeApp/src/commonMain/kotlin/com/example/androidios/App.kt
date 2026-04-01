package com.example.androidios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            val navController: NavHostController = rememberNavController()
            NavHost(navController = navController, startDestination = LoginDestination) {
                composable<LoginDestination> {
                    LoginScreen(onLoginSuccess = {
                        navController.navigate(HomeDestination) {
                            // 登录成功后清空回退栈，防止返回到登录页
                            popUpTo(LoginDestination) { inclusive = true }
                        }
                    })
                }
                composable<HomeDestination> {
                    HomeScreen(onLogout = {
                        navController.navigate(LoginDestination) {
                            popUpTo(HomeDestination) { inclusive = true }
                        }
                    })
                }
            }
        }
    }
}
