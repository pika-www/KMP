package com.cephalon.lucyApp.navigation

import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.auth.SessionExpiredNotifier
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

    // 防止快速点击导致重复 push 闪退
    private var isNavigating = false
    private fun safePush(config: Config) {
        if (isNavigating) return
        isNavigating = true
        navigation.push(config) {
            isNavigating = false
        }
    }

    private fun isOnboardingSeen(): Boolean = settings.getBoolean(KEY_ONBOARDING_SEEN, false)

    private fun markOnboardingSeen() {
        settings.putBoolean(KEY_ONBOARDING_SEEN, true)
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main +
        kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
            println("RootComponent: 协程异常(已捕获): ${throwable.message}")
        }
    )

    init {
        // 已有有效 token 时，启动时自动连接 WS 并拉取用户信息
        if (authRepository.hasValidToken()) {
            balanceWsManager.start()
            sdkSessionManager.reconnectOnAppStartIfTokenExists()
            scope.launch { authRepository.getUserInfo() }
            // 本地缓存未命中时，异步查询接口并跳转
            if (!authRepository.isConnectionFlagCached()) {
                scope.launch {
                    val connected = authRepository.checkConnectionFlag()
                    if (connected) {
                        navigation.replaceAll(Config.AgentModel)
                    }
                }
            }
        }

        // 监听接口返回 40000（未登录），自动退出到登录页
        scope.launch {
            SessionExpiredNotifier.expired.collect {
                println("RootComponent: 收到会话过期通知，执行退出")
                balanceWsManager.stopAndClear()
                sdkSessionManager.disconnect()
                authRepository.logout()
                navigation.replaceAll(Config.Login)
            }
        }
    }

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = if (authRepository.hasValidToken()) {
            if (authRepository.isConnectionFlagCached()) Config.AgentModel else Config.Home()
        } else if (isOnboardingSeen()) {
            Config.Home()
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
                        scope.launch {
                            authRepository.getUserInfo()
                            val connected = authRepository.checkConnectionFlag()
                            if (connected) {
                                navigation.replaceAll(Config.AgentModel)
                            } else {
                                navigation.replaceAll(Config.Home())
                            }
                        }
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

            is Config.Home -> RootComponent.Child.Home(
                component = object : HomeComponent {
                    override fun onLogout() {
                        balanceWsManager.stopAndClear()
                        sdkSessionManager.disconnect()
                        authRepository.logout()
                        navigation.replaceAll(Config.Login)
                    }

                    override fun onOpenSdkTest() {
                        safePush(Config.SdkTest)
                    }

                    override fun onOpenWsTest() {
                        safePush(Config.WsTest)
                    }

                    override fun onOpenBrainBoxGuide() {
                        safePush(Config.BrainBoxGuide(source = BrainBoxGuideSource.FromHome))
                    }

                    override fun onOpenBrainBoxLoginSuccess() {
                        navigation.replaceAll(Config.AgentModel)
                    }

                    override fun onOpenAgentModel() {
                        // 调用 connect 接口 → SDK 初始化绑定设备 → 跳转对话页
                        scope.launch {
                            println("RootComponent: 点击端脑云用户，开始调用 connectLucyApp()")
                            val connectResult = authRepository.connectLucyApp()
                            println("RootComponent: connectLucyApp 返回: isSuccess=${connectResult.isSuccess}, error=${connectResult.exceptionOrNull()?.message}")
                            if (connectResult.isFailure) {
                                println("RootComponent: 端脑云接入失败，不跳转")
                                return@launch
                            }
                            println("RootComponent: 接入成功，开始 SDK connectAfterLogin()")
                            sdkSessionManager.connectAfterLogin()
                            println("RootComponent: SDK 初始化完成，跳转 AgentModel")
                            navigation.replaceAll(Config.AgentModel)
                        }
                    }

                    override fun onOpenScanBindChannel() {
                        safePush(Config.ScanBindChannel)
                    }

                    override fun onOpenNas() {
                        safePush(Config.Nas)
                    }

                    override val showBack: Boolean get() = config.showBack

                    override fun onBack() {
                        navigation.replaceAll(Config.AgentModel)
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
                        navigation.replaceAll(Config.Home())
                    }
                    override fun onNavigateToNas() {
                        safePush(Config.Nas)
                    }
                    override fun onNavigateToHome() {
                        navigation.replaceAll(Config.Home(showBack = true))
                    }
                    override fun onLogout() {
                        balanceWsManager.stopAndClear()
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
                    override fun onScanSuccess() {
                        scope.launch {
                            authRepository.setConnectionFlag()
                            navigation.replaceAll(Config.AgentModel)
                        }
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
        data class Home(val showBack: Boolean = false) : Config

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
