package com.cephalon.lucyApp.sdk

import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.di.AppConfig
import com.cephalon.lucyApp.logging.appLogD
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import lucy.im.sdk.ConnectedSession
import lucy.im.sdk.LucyImAppClient
import lucy.im.sdk.LucyImAppConfig
import lucy.im.sdk.OnlineDevice
import lucy.im.sdk.OnlineNpcDeviceHandle
import lucy.im.sdk.collectDevices

class SdkSessionManager(
    private val tokenStore: AuthTokenStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val connectMutex = Mutex()

    private val sdkClient =
        LucyImAppClient(
            LucyImAppConfig(
                lucyServerBaseUrl = "${AppConfig.baseDomain}/aiden/lucy-server",
            ),
        )

    private var session: ConnectedSession? = null
    private var consumerJob: Job? = null
    private var deviceObserver: OnlineNpcDeviceHandle? = null
    private var deviceObserverJob: Job? = null

    private val _connectionState = MutableStateFlow(SdkConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SdkConnectionState> = _connectionState.asStateFlow()

    private val _connectionLog = MutableStateFlow("未连接")
    val connectionLog: StateFlow<String> = _connectionLog.asStateFlow()

    private val _consumerLog = MutableStateFlow("未启动")
    val consumerLog: StateFlow<String> = _consumerLog.asStateFlow()

    private val _deviceLog = MutableStateFlow("未启动")
    val deviceLog: StateFlow<String> = _deviceLog.asStateFlow()

    private val _onlineDevices = MutableStateFlow<List<OnlineDevice>>(emptyList())
    val onlineDevices: StateFlow<List<OnlineDevice>> = _onlineDevices.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<String>>(emptyList())
    val receivedMessages: StateFlow<List<String>> = _receivedMessages.asStateFlow()

    fun connectAfterLogin() {
        connectIfTokenValid()
    }

    fun reconnectOnAppStartIfTokenExists() {
        connectIfTokenValid()
    }

    fun onForeground() {
        if (tokenStore.getValidTokenOrNull().isNullOrBlank()) return
        connectIfTokenValid()
    }

    fun onBackground() {
        if (session != null && _connectionState.value == SdkConnectionState.CONNECTED) {
            _connectionLog.value = "应用在后台，连接保持中"
        }
    }

    fun connectIfTokenValid() {
        scope.launch {
            connectMutex.withLock {
                if (session != null && _connectionState.value == SdkConnectionState.CONNECTED) return@withLock

                val token = tokenStore.getValidTokenOrNull()
                if (token.isNullOrBlank()) {
                    _connectionState.value = SdkConnectionState.DISCONNECTED
                    _connectionLog.value = "连接失败：未找到有效登录 token"
                    appLogD(TAG, _connectionLog.value)
                    return@withLock
                }

                _connectionState.value = SdkConnectionState.CONNECTING
                _connectionLog.value = "连接中..."
                appLogD(TAG, _connectionLog.value)

                runCatching {
                    sdkClient.connect(token)
                }.onSuccess { newSession ->
                    resetSessionResources()
                    session = newSession
                    _connectionState.value = SdkConnectionState.CONNECTED
                    _connectionLog.value = "连接成功，userId=${newSession.userId}"
                    appLogD(TAG, _connectionLog.value)
                    startObservers(newSession)
                }.onFailure { error ->
                    _connectionState.value = SdkConnectionState.ERROR
                    _connectionLog.value = "连接失败：${error.message ?: "unknown"}"
                    _consumerLog.value = "监听启动失败"
                    _deviceLog.value = "设备监听启动失败"
                    appLogD(TAG, _connectionLog.value)
                    appLogD(TAG, _consumerLog.value)
                    appLogD(TAG, _deviceLog.value)
                }
            }
        }
    }

    fun disconnect() {
        scope.launch {
            connectMutex.withLock {
                resetSessionResources()
                _connectionState.value = SdkConnectionState.DISCONNECTED
                _connectionLog.value = "连接已关闭"
                _consumerLog.value = "监听已停止"
                _deviceLog.value = "设备监听已停止"
                appLogD(TAG, _connectionLog.value)
                appLogD(TAG, _consumerLog.value)
                appLogD(TAG, _deviceLog.value)
            }
        }
    }

    suspend fun publishToNpc(cdi: String, payload: String): Result<Unit> {
        val activeSession = session
            ?: return Result.failure(IllegalStateException("请先连接 SDK"))

        return runCatching {
            activeSession.publishToNpc(cdi = cdi, payload = payload)
        }
    }

    private fun startObservers(newSession: ConnectedSession) {
        _receivedMessages.value = emptyList()

        val newObserver = newSession.startOnlineNpcDeviceObserver(scope = scope)
        deviceObserver = newObserver
        deviceObserverJob =
            newObserver.collectDevices(scope) { devices ->
                _onlineDevices.value = devices
                _deviceLog.value = "已获取 ${devices.size} 台在线设备"
                appLogD(TAG, _deviceLog.value)
            }

        consumerJob =
            newSession.startUserChannelConsumer(scope) { subject, messagePayload ->
                val messageText =
                    runCatching { messagePayload.decodeToString() }.getOrDefault(
                        "<binary payload ${messagePayload.size} bytes>",
                    )
                _receivedMessages.update { current ->
                    (listOf("subject=$subject\n$messageText") + current).take(100)
                }
                _consumerLog.value = "监听中，累计接收 ${_receivedMessages.value.size} 条"
                appLogD(TAG, _consumerLog.value)
            }

        consumerJob?.invokeOnCompletion { throwable ->
            if (throwable == null || throwable is CancellationException) return@invokeOnCompletion
            _connectionState.value = SdkConnectionState.ERROR
            _consumerLog.value = "监听异常：${throwable.message ?: "unknown"}"
            appLogD(TAG, _consumerLog.value)
        }

        deviceObserverJob?.invokeOnCompletion { throwable ->
            if (throwable == null || throwable is CancellationException) return@invokeOnCompletion
            _deviceLog.value = "设备监听异常：${throwable.message ?: "unknown"}"
            appLogD(TAG, _deviceLog.value)
        }

        _consumerLog.value = "监听已启动"
        _deviceLog.value = "设备监听已启动"
        appLogD(TAG, _consumerLog.value)
        appLogD(TAG, _deviceLog.value)
    }

    private fun resetSessionResources() {
        consumerJob?.cancel()
        deviceObserverJob?.cancel()
        deviceObserver?.stop()
        consumerJob = null
        deviceObserverJob = null
        deviceObserver = null
        session?.close()
        session = null
        _onlineDevices.value = emptyList()
    }

    companion object {
        private const val TAG = "SdkSessionManager"
    }
}

enum class SdkConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}
