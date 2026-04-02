package com.example.androidios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.Child
import com.example.androidios.api.AuthRepository
import com.example.androidios.navigation.RootComponent
import com.example.androidios.navigation.RootComponentImpl
import com.example.androidios.navigation.createDefaultComponentContext
import com.example.androidios.screens.ForgotPasswordScreen
import com.example.androidios.screens.BrainBoxGuideScreen
import com.example.androidios.screens.HomeScreen
import com.example.androidios.screens.LoginScreen
import com.example.androidios.screens.WsTestScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

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
            val authRepository = koinInject<AuthRepository>()

            val root: RootComponent = remember {
                RootComponentImpl(createDefaultComponentContext(), authRepository)
            }

            Children(stack = root.stack) { child: Child.Created<*, RootComponent.Child> ->
                when (val instance = child.instance) {
                    is RootComponent.Child.Login -> {
                        LoginScreen(
                            onLoginSuccess = instance.component::onLoginSuccess,
                            onForgotPassword = instance.component::onForgotPassword
                        )
                    }

                    is RootComponent.Child.ForgotPassword -> {
                        ForgotPasswordScreen(
                            onBack = instance.component::onBack
                        )
                    }

                    is RootComponent.Child.Home -> {
                        HomeScreen(
                            onLogout = instance.component::onLogout,
                            onOpenWsTest = instance.component::onOpenWsTest,
                            onOpenBrainBoxGuide = instance.component::onOpenBrainBoxGuide
                        )
                    }

                    is RootComponent.Child.BrainBoxGuide -> {
                        BrainBoxGuideScreen(
                            onBack = instance.component::onBack,
                            onFinish = instance.component::onFinish
                        )
                    }

                    is RootComponent.Child.WsTest -> {
                        WsTestScreen(
                            onBack = instance.component::onBack
                        )
                    }
                }
            }
        }
    }
}
