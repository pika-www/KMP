package com.cephalon.lucyApp.navigation

import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.sdk.SdkSessionManager
import com.cephalon.lucyApp.ws.BalanceWsManager
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import com.arkivanov.decompose.DelicateDecomposeApi // 确保导入这个
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@OptIn(DelicateDecomposeApi::class) // 添加这一行

class RootComponentImpl(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val settings: Settings,
    private val balanceWsManager: BalanceWsManager,
    private val sdkSessionManager: SdkSessionManager,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private fun isOnboardingSeen(): Boolean = settings.getBoolean(KEY_ONBOARDING_SEEN, false)

    private fun markOnboardingSeen() {
        settings.putBoolean(KEY_ONBOARDING_SEEN, true)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        // 已有有效 token 时，启动时自动连接 WS 并拉取用户信息
        if (authRepository.hasValidToken()) {
            balanceWsManager.start()
            sdkSessionManager.reconnectOnAppStartIfTokenExists()
            scope.launch { authRepository.getUserInfo() }
        }
    }

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = if (authRepository.hasValidToken()) {
            Config.Home
        } else if (isOnboardingSeen()) {
            Config.Login
        } else {
            Config.BrainBoxGuide(source = BrainBoxGuideSource.Initial)
        },
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(
        config: Config,
        childContext: ComponentContext
    ): RootComponent.Child {
        return when (config) {
            Config.Login -> RootComponent.Child.Login(
                component = object : LoginComponent {
                    override fun onLoginSuccess() {
                        balanceWsManager.start()
                        sdkSessionManager.connectAfterLogin()
                        navigation.replaceAll(Config.Home)
                    }

//                    override fun onForgotPassword() {
//                        navigation.push(Config.ForgotPassword)
//                    }
                }
            )

//            Config.ForgotPassword -> RootComponent.Child.ForgotPassword(
//                component = object : ForgotPasswordComponent {
//                    override fun onBack() {
//                        navigation.pop()
//                    }
//                }
//            )

            Config.Home -> RootComponent.Child.Home(
                component = object : HomeComponent {
                    override fun onLogout() {
                        balanceWsManager.stop()
                        sdkSessionManager.disconnect()
                        authRepository.logout()
                        navigation.replaceAll(Config.Login)
                    }

                    override fun onOpenSdkTest() {
                        navigation.push(Config.SdkTest)
                    }

                    override fun onOpenWsTest() {
                        navigation.push(Config.WsTest)
                    }

                    override fun onOpenBrainBoxGuide() {
                        navigation.push(Config.BrainBoxGuide(source = BrainBoxGuideSource.FromHome))
                    }

                    override fun onOpenAgentModel() {
                        navigation.push(Config.AgentModel)
                    }

                    override fun onOpenScanBindChannel() {
                        navigation.push(Config.ScanBindChannel)
                    }

                    override fun onOpenNas() {
                        navigation.push(Config.Nas)
                    }
                }
            )

            is Config.BrainBoxGuide -> RootComponent.Child.BrainBoxGuide(
                component = object : BrainBoxGuideComponent {
                    override fun onBack() {
                        when (config.source) {
                            BrainBoxGuideSource.Initial -> {
                                markOnboardingSeen()
                                navigation.replaceAll(Config.Login)
                            }
                            BrainBoxGuideSource.FromHome -> navigation.pop()
                        }
                    }

                    override fun onFinish() {
                        when (config.source) {
                            BrainBoxGuideSource.Initial -> {
                                markOnboardingSeen()
                                navigation.replaceAll(Config.Login)
                            }
                            BrainBoxGuideSource.FromHome -> navigation.pop()
                        }
                    }
                }
            )

            Config.SdkTest -> RootComponent.Child.SdkTest(
                component = object : SdkTestComponent {
                    override fun onBack() {
                        navigation.pop()
                    }
                }
            )

            Config.WsTest -> RootComponent.Child.WsTest(
                component = object : WsTestComponent {
                    override fun onBack() {
                        navigation.pop()
                    }
                }
            )

            Config.AgentModel -> RootComponent.Child.AgentModel(
                component = object : AgentModelComponent {
                    override fun onBack() {
                        navigation.pop()
                    }
                    override fun onNavigateToNas() {
                        navigation.push(Config.Nas)
                    }
                    override fun onLogout() {
                        balanceWsManager.stop()
                        sdkSessionManager.disconnect()
                        authRepository.logout()
                        navigation.replaceAll(Config.Login)
                    }
                }
            )

            Config.ScanBindChannel -> RootComponent.Child.ScanBindChannel(
                component = object : ScanBindChannelComponent {
                    override fun onBack() {
                        navigation.pop()
                    }
                }
            )

            Config.Nas -> RootComponent.Child.Nas(
                component = object : NasComponent {
                    override fun onBack() {
                        navigation.pop()
                    }
                }
            )
        }
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Login : Config

//        @Serializable
//        data object ForgotPassword : Config

        @Serializable
        data object Home : Config

        @Serializable
        data class BrainBoxGuide(val source: BrainBoxGuideSource) : Config

        @Serializable
        data object SdkTest : Config

        @Serializable
        data object WsTest : Config

        @Serializable
        data object AgentModel : Config

        @Serializable
        data object ScanBindChannel : Config

        @Serializable
        data object Nas : Config
    }

    @Serializable
    private enum class BrainBoxGuideSource {
        Initial,
        FromHome
    }

    private companion object {
        private const val KEY_ONBOARDING_SEEN = "onboarding.seen"
    }
}
