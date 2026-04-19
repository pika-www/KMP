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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
            scope.launch { authRepository.getModelConfig() }

            // 路由复核：不论 initialConfiguration 取的是 AgentModel 还是 Home，
            // 这里都按 "当前账号是否真有绑定设备" 做一次权威判定：
            // - 有 token + 有设备（且接入过）→ AgentModel；
            // - 有 token 但设备列表为空 → 回 Home 让用户选择接入方式，
            //   无论本地 KEY_CONNECTION_FLAG 是否缓存为 true 都不该直接进对话页。
            scope.launch {
                val connected = authRepository.checkConnectionFlag()
                val hasDevice = connected && authRepository.getDevices().isNotEmpty()
                println("RootComponent: init route-check connected=$connected hasDevice=$hasDevice")
                val currentConfig = runCatching { stack.value.active.configuration }.getOrNull()
                when {
                    connected && hasDevice -> {
                        if (currentConfig !is Config.AgentModel) {
                            navigation.replaceAll(Config.AgentModel())
                        }
                    }
                    else -> {
                        if (currentConfig !is Config.Home) {
                            // 本地 flag 可能是旧账号残留，顺手失效一下
                            authRepository.invalidateConnectionFlagCache()
                            navigation.replaceAll(Config.Home())
                        }
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
            if (authRepository.isConnectionFlagCached()) Config.AgentModel() else Config.Home()
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
                        scope.launch {
                            authRepository.getUserInfo()
                            authRepository.getModelConfig()
                            // 关键 1：先失效上一轮账号在本地残留的 connection_flag 缓存，
                            //        强制 checkConnectionFlag() 走一次网络，避免被旧值误判。
                            authRepository.invalidateConnectionFlagCache()

                            // 关键 2：路由从 "flag" 改为 "当前账号是否真有绑定设备"。
                            //        connection_flag 只代表"历史上接入过"，解绑光了设备也可能为 true。
                            //        真正决定该进 AgentModel 还是回 Home（选择接入方式）的，
                            //        是账号当下是否还有设备可用。
                            val connected = authRepository.checkConnectionFlag()
                            val hasDevice = connected && authRepository.getDevices().isNotEmpty()
                            println("RootComponent: onLoginSuccess connected=$connected hasDevice=$hasDevice")
                            if (connected && hasDevice) {
                                navigation.replaceAll(Config.AgentModel())
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
                        sdkSessionManager.clearSelectedDeviceCache()
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

                    override fun onOpenBrainBoxLoginSuccess(cdi: String) {
                        scope.launch {
                            println("[BrainBox] 绑定成功 cdi=$cdi，设置连接标记...")
                            authRepository.setConnectionFlag()
                            sdkSessionManager.selectDevice(cdi)

                            // 绑定成功后的新流程（与旧的 "先跳转再后台连接" 不同）：
                            // 1. 先把 SDK 连好（下发 provision_model 需要 NATS publish；
                            //    同时 OnlineNpcDeviceObserver 才会起来跑 runPingAndEmit）。
                            // 2. 下发一次 provision_model 给刚拿到的 cdi。
                            // 3. 记录这个 cdi，等它出现在 sdkSessionManager.onlineDevices 里
                            //    （即 runPingAndEmit 发现了它）再跳转对话页。
                            println("[BrainBox] 绑定后连接 SDK...")
                            val connectResult = sdkSessionManager.ensureConnectedIfTokenValid()
                            println("[BrainBox] SDK 连接结果: ${connectResult.isSuccess}")

                            if (connectResult.isSuccess) {
                                pushProvisionModelToNewDevice(cdi)
                                println("[BrainBox] 等待 cdi=$cdi 出现在 runPingAndEmit 结果中...")
                                val onlineResult = awaitCdiOnline(cdi)
                                if (onlineResult.isSuccess) {
                                    println("[BrainBox] cdi=$cdi 已上线，跳转对话页")
                                } else {
                                    println("[BrainBox] 等待 cdi=$cdi 上线超时：${onlineResult.exceptionOrNull()?.message}，仍然跳转对话页由 AgentModel 兜底等待")
                                }
                            } else {
                                println("[BrainBox] SDK 未连上，跳过 provision_model 与 await-online，直接跳转对话页由 AgentModel 兜底")
                            }

                            println("[BrainBox] 跳转对话页 targetCdi=$cdi")
                            navigation.replaceAll(Config.AgentModel(targetCdi = cdi))
                        }
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
                            println("RootComponent: 接入成功，开始 SDK ensureConnectedIfTokenValid()")
                            val sdkResult = sdkSessionManager.ensureConnectedIfTokenValid()
                            println("RootComponent: SDK 连接结果: ${sdkResult.isSuccess}，跳转 AgentModel")
                            navigation.replaceAll(Config.AgentModel())
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
                        navigation.replaceAll(Config.AgentModel())
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

            is Config.AgentModel -> RootComponent.Child.AgentModel(
                component = object : AgentModelComponent {
                    override val targetCdi: String? get() = config.targetCdi
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
                        sdkSessionManager.clearSelectedDeviceCache()
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
                    override fun onScanSuccess(cdi: String) {
                        scope.launch {
                            authRepository.setConnectionFlag()
                            sdkSessionManager.selectDevice(cdi)

                            // 扫码绑定 = BLE/OTP 绑定的另一个入口，流程对齐 onOpenBrainBoxLoginSuccess：
                            // 先连 SDK → 下发 provision_model → 等 cdi 上线 → 再跳转。
                            println("[ScanBind] 绑定后连接 SDK...")
                            val connectResult = sdkSessionManager.ensureConnectedIfTokenValid()
                            println("[ScanBind] SDK 连接结果: ${connectResult.isSuccess}")

                            if (connectResult.isSuccess) {
                                pushProvisionModelToNewDevice(cdi)
                                println("[ScanBind] 等待 cdi=$cdi 出现在 runPingAndEmit 结果中...")
                                val onlineResult = awaitCdiOnline(cdi)
                                if (onlineResult.isSuccess) {
                                    println("[ScanBind] cdi=$cdi 已上线，跳转对话页")
                                } else {
                                    println("[ScanBind] 等待 cdi=$cdi 上线超时：${onlineResult.exceptionOrNull()?.message}，仍然跳转对话页由 AgentModel 兜底等待")
                                }
                            } else {
                                println("[ScanBind] SDK 未连上，跳过 provision_model 与 await-online，直接跳转对话页由 AgentModel 兜底")
                            }

                            navigation.replaceAll(Config.AgentModel(targetCdi = cdi))
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

    /**
     * 订阅 [SdkSessionManager.onlineDevices]，等到目标 [cdi] 出现在列表里再返回成功。
     *
     * 列表的数据源是 [lucy.im.sdk.internal.OnlineNpcDeviceObserver]：
     * - `runPingAndEmit` 在 SDK 启动和重连时主动 ping 一次 discover subject
     * - discover subject 的订阅会持续把增量设备列表推上来
     *
     * 所以一旦新绑定的 cdi 被云侧 discover 发现（或增量上报），本方法就会解除阻塞。
     *
     * @param timeoutMillis 最多等这么久；超时返回 failure，调用方自行决定兜底（通常是"仍然跳转，由对话页兜底再等"）。
     */
    private suspend fun awaitCdiOnline(
        cdi: String,
        timeoutMillis: Long = CDI_ONLINE_WAIT_TIMEOUT_MS,
    ): Result<Unit> {
        val normalized = cdi.trim()
        if (normalized.isBlank()) {
            return Result.failure(IllegalArgumentException("cdi 不能为空"))
        }
        // 先检查一次当前快照，避免"已在线"场景还要无谓地 flow.first 一下。
        if (sdkSessionManager.onlineDevices.value.any { it.cdi == normalized }) {
            return Result.success(Unit)
        }
        val hit = withTimeoutOrNull(timeoutMillis) {
            sdkSessionManager.onlineDevices
                .map { devices -> devices.any { it.cdi == normalized } }
                .filter { it }
                .first()
        }
        return if (hit == true) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("等待 cdi=$normalized 上线超时 (${timeoutMillis}ms)"))
        }
    }

    /**
     * 绑定一台新设备（BLE/OTP 或扫码）成功后调用：拉最新模型配置 → 下发给新 cdi。
     *
     * 行为约定：
     * - 网络/字段/SDK 任意一环失败，都只打日志不抛，保证绑定-跳转主路径不被打断。
     * - `modelId` 固定取 `models[0].id`（与协议约定一致：接口返回的模型列表首项即默认激活项）。
     * - `switchDefaultModel=true` + `restartRequested=true`，让新设备切到本账号默认模型并重启应用。
     */
    private suspend fun pushProvisionModelToNewDevice(cdi: String) {
        val response = authRepository.getModelConfig()
        val config = response.data
        if (response.code != 20000 || config == null) {
            println("[BrainBox] provision_model 跳过：model-config 拉取失败 code=${response.code} msg=${response.msg}")
            return
        }
        val firstModelId = config.models.firstOrNull()?.id?.trim().orEmpty()
        if (firstModelId.isBlank()) {
            println("[BrainBox] provision_model 跳过：model-config models 为空或首项 id 为空")
            return
        }
        val providerId = config.providerId.trim()
        if (providerId.isBlank()) {
            println("[BrainBox] provision_model 跳过：model-config providerId 为空")
            return
        }
        val pushResult = sdkSessionManager.publishProvisionModelToNpc(
            cdi = cdi,
            providerId = providerId,
            modelId = firstModelId,
            apiKey = config.apiKey,
            baseUrl = config.baseUrl,
            switchDefaultModel = true,
            restartRequested = true,
        )
        if (pushResult.isSuccess) {
            println("[BrainBox] provision_model 推送成功 cdi=$cdi providerId=$providerId modelId=$firstModelId")
        } else {
            println("[BrainBox] provision_model 推送失败 cdi=$cdi: ${pushResult.exceptionOrNull()?.message}")
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
        data class AgentModel(val targetCdi: String? = null) : Config

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

        // 绑定后等 cdi 出现在 sdkSessionManager.onlineDevices 里的最长时间：
        // - discover ping 默认 pingTimeoutMs 是 SDK 内部设置，通常几秒
        // - 设备绑定后可能正在启动/重启（provision_model.restartRequested=true 也会触发重启），
        //   留足够余量避免"首次绑完刚好赶上重启窗口"把用户卡在首页
        // 与 DeviceChatManager.DEFAULT_ONLINE_TIMEOUT_MS (30s) 对齐。
        private const val CDI_ONLINE_WAIT_TIMEOUT_MS = 30_000L
    }
}
