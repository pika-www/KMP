package com.cephalon.lucyApp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.Child
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.sdk.SdkSessionManager
import com.cephalon.lucyApp.ws.BalanceWsManager
import com.cephalon.lucyApp.navigation.RootComponent
import com.cephalon.lucyApp.navigation.RootComponentImpl
import com.cephalon.lucyApp.navigation.createDefaultComponentContext
import com.cephalon.lucyApp.screens.BrainBoxGuideScreen
import com.cephalon.lucyApp.screens.HomeScreen
import com.cephalon.lucyApp.screens.AgentModelScreen
import com.cephalon.lucyApp.screens.nas.NasScreen
import com.cephalon.lucyApp.screens.LoginScreen
import com.cephalon.lucyApp.screens.ScanBindChannelScreen
import com.cephalon.lucyApp.screens.SdkTestScreen
import com.cephalon.lucyApp.screens.WsTestScreen
import com.russhwolf.settings.Settings
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
        colorScheme = lightColorScheme()
    ) {
        // 确保 Surface 也使用 MaterialTheme 的背景色
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val authRepository = koinInject<AuthRepository>()
            val settings = koinInject<Settings>()
            val balanceWsManager = koinInject<BalanceWsManager>()
            val sdkSessionManager = koinInject<SdkSessionManager>()

            BindAppLifecycle(
                onForeground = { sdkSessionManager.onForeground() },
                onBackground = { sdkSessionManager.onBackground() },
            )

            val root: RootComponent = remember {
                RootComponentImpl(
                    createDefaultComponentContext(),
                    authRepository,
                    settings,
                    balanceWsManager,
                    sdkSessionManager,
                )
            }

            Children(stack = root.stack) { child: Child.Created<*, RootComponent.Child> ->
                when (val instance = child.instance) {
                    is RootComponent.Child.Login -> {
                        LoginScreen(
                            onLoginSuccess = instance.component::onLoginSuccess,
                        )
                    }


                    is RootComponent.Child.Home -> {
                        HomeScreen(
                            onLogout = instance.component::onLogout,
                            onOpenSdkTest = instance.component::onOpenSdkTest,
                            onOpenWsTest = instance.component::onOpenWsTest,
                            onOpenBrainBoxGuide = instance.component::onOpenBrainBoxGuide,
                            onOpenAgentModel = instance.component::onOpenAgentModel,
                            onOpenScanBindChannel = instance.component::onOpenScanBindChannel,

                        )
                    }

                    is RootComponent.Child.SdkTest -> {
                        SdkTestScreen(
                            onBack = instance.component::onBack
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

                    is RootComponent.Child.AgentModel -> {
                        AgentModelScreen(
                            onBack = instance.component::onBack,
                            onNavigateToNas = instance.component::onNavigateToNas,
                            onLogout = instance.component::onLogout
                        )
                    }

                    is RootComponent.Child.ScanBindChannel -> {
                        ScanBindChannelScreen(
                            onBack = instance.component::onBack
                        )
                    }

                    is RootComponent.Child.Nas -> {
                        NasScreen(
                            onBack = instance.component::onBack
                        )
                    }
                }
            }
        }
    }
}