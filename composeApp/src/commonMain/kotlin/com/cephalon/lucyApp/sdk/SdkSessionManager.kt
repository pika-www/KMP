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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    private val json = Json { ignoreUnknownKeys = true }

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

    private val _onlineDeviceCdis = MutableStateFlow<List<String>>(emptyList())
    val onlineDeviceCdis: StateFlow<List<String>> = _onlineDeviceCdis.asStateFlow()

    private val _lastOnlineCdi = MutableStateFlow<String?>(null)
    val lastOnlineCdi: StateFlow<String?> = _lastOnlineCdi.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<String>>(emptyList())
    val receivedMessages: StateFlow<List<String>> = _receivedMessages.asStateFlow()

    private val _lastReplyMessageId = MutableStateFlow<String?>(null)
    val lastReplyMessageId: StateFlow<String?> = _lastReplyMessageId.asStateFlow()

    private val _assistantReplyText = MutableStateFlow("")
    val assistantReplyText: StateFlow<String> = _assistantReplyText.asStateFlow()

    private val _assistantReplyStreaming = MutableStateFlow(false)
    val assistantReplyStreaming: StateFlow<Boolean> = _assistantReplyStreaming.asStateFlow()

    private val _currentRequestMessageId = MutableStateFlow<String?>(null)

    private val _pendingReplyMessageIds = MutableStateFlow<Set<String>>(emptySet())

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
                _lastOnlineCdi.value = null
                _lastReplyMessageId.value = null
                _pendingReplyMessageIds.value = emptySet()
                _currentRequestMessageId.value = null
                _assistantReplyText.value = ""
                _assistantReplyStreaming.value = false
                appLogD(TAG, _connectionLog.value)
                appLogD(TAG, _consumerLog.value)
                appLogD(TAG, _deviceLog.value)
            }
        }
    }

    suspend fun publishToNpc(cdi: String, payload: String): Result<Unit> {
        val activeSession = session
            ?: return Result.failure(IllegalStateException("请先连接 SDK"))

        val outgoingMessageId = extractMessageId(payload)
        if (outgoingMessageId != null) {
            _currentRequestMessageId.value = outgoingMessageId
            _pendingReplyMessageIds.value = setOf(outgoingMessageId)
            _assistantReplyText.value = ""
            _assistantReplyStreaming.value = false
            appLogD(TAG, "发送请求 messageId=$outgoingMessageId，等待回复")
        }
        appLogD(TAG, "发送消息 cdi=$cdi payload=$payload")
        return runCatching {
            activeSession.publishToNpc(cdi = cdi, payload = payload)
        }.onSuccess {
            appLogD(TAG, "发送成功 cdi=$cdi")
        }.onFailure { error ->
            if (outgoingMessageId != null) {
                _pendingReplyMessageIds.value = emptySet()
                _currentRequestMessageId.value = null
            }
            appLogD(TAG, "发送失败 cdi=$cdi error=${error.message ?: "unknown"}")
        }
    }

    private fun startObservers(newSession: ConnectedSession) {
        _receivedMessages.value = emptyList()

        val newObserver = newSession.startOnlineNpcDeviceObserver(scope = scope)
        deviceObserver = newObserver
        deviceObserverJob =
            newObserver.collectDevices(scope) { devices ->
                _onlineDevices.value = devices
                val cdis = devices.map { it.cdi }
                _onlineDeviceCdis.value = cdis
                if (cdis.isNotEmpty()) {
                    _lastOnlineCdi.value = cdis.first()
                }
                _deviceLog.value =
                    if (cdis.isEmpty()) {
                        "当前无在线设备（lastOnlineCdi=${_lastOnlineCdi.value ?: "none"}）"
                    } else {
                        "已获取 ${cdis.size} 台在线设备，cdi=${cdis.joinToString(",")}" 
                    }
                appLogD(TAG, _deviceLog.value)
            }

        consumerJob =
            newSession.startUserChannelConsumer(scope) { subject, messagePayload ->
                val messageText =
                    runCatching { messagePayload.decodeToString() }.getOrDefault(
                        "<binary payload ${messagePayload.size} bytes>",
                    )
                val incomingSourceMessageId = extractSourceMessageId(messageText)
                val currentRequestMessageId = _currentRequestMessageId.value
                if (currentRequestMessageId.isNullOrBlank() || incomingSourceMessageId != currentRequestMessageId) {
                    appLogD(
                        TAG,
                        "忽略消息 source_message_id=${incomingSourceMessageId ?: "none"}, currentMessageId=${currentRequestMessageId ?: "none"}",
                    )
                    return@startUserChannelConsumer
                }
                appLogD(TAG, "收到消息 subject=$subject payload=$messageText")
                _receivedMessages.update { current ->
                    (listOf("subject=$subject\n$messageText") + current).take(100)
                }
                handleMachineEvent(messageText)
                extractMessageId(messageText)?.let { messageId ->
                    _lastReplyMessageId.value = messageId
                    appLogD(TAG, "收到回复 messageId=$messageId")
                    var matched = false
                    _pendingReplyMessageIds.update { current ->
                        matched = current.contains(messageId)
                        if (matched) current - messageId else current
                    }
                    if (matched) {
                        appLogD(TAG, "已关联回复 messageId=$messageId")
                    } else {
                        appLogD(TAG, "收到未匹配回复 messageId=$messageId")
                    }
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
        _onlineDeviceCdis.value = emptyList()
        _lastReplyMessageId.value = null
        _pendingReplyMessageIds.value = emptySet()
        _currentRequestMessageId.value = null
        _assistantReplyText.value = ""
        _assistantReplyStreaming.value = false
    }

    private fun handleMachineEvent(payload: String) {
        val event = parseMachineEvent(payload) ?: return
        when (event.type) {
            "assistant.start" -> {
                _assistantReplyText.value = ""
                _assistantReplyStreaming.value = true
            }

            "assistant.partial" -> {
                event.text?.let { _assistantReplyText.value = it }
                _assistantReplyStreaming.value = true
            }

            "assistant.final" -> {
                event.text?.let { _assistantReplyText.value = it }
                _assistantReplyStreaming.value = false
                _currentRequestMessageId.value = null
            }

            "error" -> {
                _assistantReplyStreaming.value = false
                _currentRequestMessageId.value = null
            }
        }
    }

    private fun parseMachineEvent(payload: String): NpcMachineEvent? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null
        val type = root["type"]?.jsonPrimitive?.contentOrNull ?: return null
        val text = root["text"]?.jsonPrimitive?.contentOrNull
        val sourceMessageId =
            root["source_message_id"]?.jsonPrimitive?.contentOrNull
                ?: root["messageId"]?.jsonPrimitive?.contentOrNull
        return NpcMachineEvent(type = type, text = text, sourceMessageId = sourceMessageId)
    }

    private fun extractMessageId(payload: String): String? {
        parseMachineEvent(payload)?.sourceMessageId?.takeIf { it.isNotBlank() }?.let { return it }
        val regex = Regex("\\\"messageId\\\"\\s*:\\s*(?:\\\"(\\d{19})\\\"|(\\d{19}))")
        val match = regex.find(payload) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
            ?: match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
    }

    private fun extractSourceMessageId(payload: String): String? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null
        return root["source_message_id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "SdkSessionManager"
    }
}

private data class NpcMachineEvent(
    val type: String,
    val text: String?,
    val sourceMessageId: String?,
)

enum class SdkConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}
