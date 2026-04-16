package com.cephalon.lucyApp

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.Child
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.sdk.SdkSessionManager
import com.cephalon.lucyApp.sdk.createLocalNotificationSender
import com.cephalon.lucyApp.ws.BalanceWsManager
import com.cephalon.lucyApp.navigation.RootComponent
import com.cephalon.lucyApp.navigation.RootComponentImpl
import com.cephalon.lucyApp.navigation.createDefaultComponentContext
import com.cephalon.lucyApp.payment.IAPManager
import com.cephalon.lucyApp.screens.BrainBoxGuideScreen
import com.cephalon.lucyApp.screens.HomeScreen
import com.cephalon.lucyApp.screens.AgentModelScreen
import com.cephalon.lucyApp.screens.nas.NasScreen
import com.cephalon.lucyApp.screens.LoginScreen
import com.cephalon.lucyApp.screens.ScanBindChannelScreen
import com.cephalon.lucyApp.screens.SdkTestScreen
import com.cephalon.lucyApp.screens.WsTestScreen
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object LoginDestination

@Serializable
object HomeDestination

@Serializable
object WsTestDestination

@Composable
fun App(
    modifier: Modifier = Modifier,
    iapManager: IAPManager? = null
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize IAP if manager is provided (iOS platform)
    LaunchedEffect(Unit) {
        iapManager?.handleUnfinishedTransactions()
        iapManager?.loadProducts()
    }
    
    MaterialTheme(
        colorScheme = lightColorScheme()
    ) {
      CompositionLocalProvider(LocalIndication provides NoIndication) {
        // 确保 Surface 也使用 MaterialTheme 的背景色
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val authRepository = koinInject<AuthRepository>()
            val settings = koinInject<Settings>()
            val balanceWsManager = koinInject<BalanceWsManager>()
            val sdkSessionManager = koinInject<SdkSessionManager>()

            LaunchedEffect(Unit) {
                if (sdkSessionManager.localNotificationSender == null) {
                    sdkSessionManager.localNotificationSender = createLocalNotificationSender()
                }
            }

            // 冷启动时也尝试领取每日奖励
            LaunchedEffect(Unit) {
                if (authRepository.hasValidToken()) {
                    try {
                        authRepository.claimDailyRewardIfNeeded()
                    } catch (_: Exception) { }
                }
            }

            BindAppLifecycle(
                onForeground = {
                    sdkSessionManager.onForeground()
                    balanceWsManager.onForeground()
                    if (authRepository.hasValidToken()) {
                        CoroutineScope(Dispatchers.Default).launch {
                            try {
                                authRepository.claimDailyRewardIfNeeded()
                            } catch (_: Exception) { }
                        }
                    }
                },
                onBackground = {
                    sdkSessionManager.onBackground()
                    balanceWsManager.onBackground()
                },
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
                            showBack = instance.component.showBack,
                            onBack = instance.component::onBack,
                            onLogout = instance.component::onLogout,
                            onOpenSdkTest = instance.component::onOpenSdkTest,
                            onOpenWsTest = instance.component::onOpenWsTest,
                            onOpenBrainBoxGuide = instance.component::onOpenBrainBoxGuide,
                            onOpenBrainBoxLoginSuccess = instance.component::onOpenBrainBoxLoginSuccess,
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
                            onNavigateToHome = instance.component::onNavigateToHome,
                            onLogout = instance.component::onLogout,
                            initialTargetCdi = instance.component.targetCdi,
                        )
                    }

                    is RootComponent.Child.ScanBindChannel -> {
                        ScanBindChannelScreen(
                            onBack = instance.component::onBack,
                            onScanSuccess = instance.component::onScanSuccess,
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
}

private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return object : Modifier.Node() {}
    }

    override fun hashCode(): Int = -1

    override fun equals(other: Any?): Boolean = other === this
}