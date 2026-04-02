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
import com.example.androidios.screens.WsTestScreen
import kotlinx.serialization.Serializable

@Serializable
object LoginDestination

@Serializable
object HomeDestination

@Serializable
object WsTestDestination

@Composable
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            var isLoggedIn by remember { mutableStateOf(false) }
            var currentDestination by remember { mutableStateOf<Any>(LoginDestination) }

            if (!isLoggedIn) {
                LoginScreen(onLoginSuccess = {
                    isLoggedIn = true
                    currentDestination = HomeDestination
                })
            } else {
                when (currentDestination) {
                    HomeDestination -> HomeScreen(
                        onLogout = {
                            isLoggedIn = false
                            currentDestination = LoginDestination
                        },
                        onOpenWsTest = {
                            currentDestination = WsTestDestination
                        }
                    )

                    WsTestDestination -> WsTestScreen(
                        onBack = {
                            currentDestination = HomeDestination
                        }
                    )

                    else -> HomeScreen(
                        onLogout = {
                            isLoggedIn = false
                            currentDestination = LoginDestination
                        },
                        onOpenWsTest = {
                            currentDestination = WsTestDestination
                        }
                    )
                }
            }
        }
    }
}
