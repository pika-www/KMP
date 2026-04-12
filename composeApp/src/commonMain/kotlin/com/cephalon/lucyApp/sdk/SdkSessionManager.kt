package com.cephalon.lucyApp.sdk

import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.di.AppConfig
import com.cephalon.lucyApp.logging.appLogD
import com.cephalon.lucyApp.time.currentTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
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
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        appLogD(TAG, "协程异常(已捕获): ${throwable.message}")
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
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
    private var observerRestartJob: Job? = null
    private var reconnectJob: Job? = null

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
        // 主动断开 NATS 连接，防止息屏后 socket 断开导致 natskt 内部协程
        // 抛出 ClosedByteChannelException (ENOTCONN) 未捕获异常闪退
        if (session != null) {
            appLogD(TAG, "应用进入后台，主动断开 NATS 连接")
            scope.launch {
                connectMutex.withLock {
                    resetSessionResources()
                    _connectionState.value = SdkConnectionState.DISCONNECTED
                    _connectionLog.value = "应用在后台，已主动断开"
                    appLogD(TAG, _connectionLog.value)
                }
            }
        }
    }

    fun connectIfTokenValid() {
        scope.launch {
            ensureConnectedIfTokenValid()
        }
    }

    suspend fun ensureConnectedIfTokenValid(): Result<Unit> {
        return connectMutex.withLock {
            if (session != null && _connectionState.value == SdkConnectionState.CONNECTED) {
                if (!areObserversActive()) {
                    appLogD(TAG, "检测到连接存在但监听未激活，重建监听")
                    _connectionLog.value = "连接正常，正在恢复监听..."
                    restartObservers(session = session!!, reason = "主动恢复监听")
                }
                return@withLock Result.success(Unit)
            }

            val token = tokenStore.getValidTokenOrNull()
            if (token.isNullOrBlank()) {
                _connectionState.value = SdkConnectionState.DISCONNECTED
                _connectionLog.value = "连接失败：未找到有效登录 token"
                appLogD(TAG, _connectionLog.value)
                return@withLock Result.failure(IllegalStateException("未找到有效登录 token"))
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
                if (isAuthorizationError(error)) {
                    tokenStore.clear()
                    resetSessionResources()
                    _connectionState.value = SdkConnectionState.DISCONNECTED
                    _connectionLog.value = "连接失败：鉴权失效，请重新登录"
                } else {
                    _connectionState.value = SdkConnectionState.ERROR
                    _connectionLog.value = "连接失败：${error.message ?: "unknown"}"
                }
                _consumerLog.value = "监听启动失败"
                _deviceLog.value = "设备监听启动失败"
                appLogD(TAG, _connectionLog.value)
                appLogD(TAG, _consumerLog.value)
                appLogD(TAG, _deviceLog.value)
            }.map { Unit }
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

    suspend fun publishTextToNpc(cdi: String, text: String): Result<String> {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) {
            return Result.failure(IllegalArgumentException("消息不能为空"))
        }
        val messageId = generateMessageId19()
        val payload =
            """{"version":2,"messageId":"$messageId","text":"${trimmedText.escapeForJson()}","timestamp":${currentTimeMillis()}}"""
        return publishToNpc(cdi = cdi, payload = payload).map { messageId }
    }

    private fun startObservers(newSession: ConnectedSession) {
        observerRestartJob?.cancel()
        observerRestartJob = null
        reconnectJob?.cancel()
        reconnectJob = null
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
                val machineEvent = parseMachineEvent(messageText)
                val incomingSourceMessageId = machineEvent?.sourceMessageId ?: extractSourceMessageId(messageText)
                val currentRequestMessageId = _currentRequestMessageId.value
                val sourceMatched = incomingSourceMessageId == currentRequestMessageId
                val shouldAcceptAssistantEventWithDifferentSource =
                    machineEvent != null &&
                        machineEvent.type.startsWith("assistant.") &&
                        !currentRequestMessageId.isNullOrBlank()
                val shouldAcceptMachineEventWithoutSource =
                    machineEvent != null &&
                        incomingSourceMessageId.isNullOrBlank() &&
                        !currentRequestMessageId.isNullOrBlank()
                if (
                    currentRequestMessageId.isNullOrBlank() ||
                    (!sourceMatched &&
                        !shouldAcceptMachineEventWithoutSource &&
                        !shouldAcceptAssistantEventWithDifferentSource)
                ) {
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
                handleMachineEvent(machineEvent)
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
            handleObserverFailure(
                throwable = throwable,
                logLabel = "监听",
                logUpdater = { _consumerLog.value = it },
            )
        }

        deviceObserverJob?.invokeOnCompletion { throwable ->
            if (throwable == null || throwable is CancellationException) return@invokeOnCompletion
            handleObserverFailure(
                throwable = throwable,
                logLabel = "设备监听",
                logUpdater = { _deviceLog.value = it },
            )
        }

        _consumerLog.value = "监听已启动"
        _deviceLog.value = "设备监听已启动"
        appLogD(TAG, _consumerLog.value)
        appLogD(TAG, _deviceLog.value)
    }

    private fun resetSessionResources() {
        observerRestartJob?.cancel()
        observerRestartJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        consumerJob?.cancel()
        deviceObserverJob?.cancel()
        deviceObserver?.stop()
        consumerJob = null
        deviceObserverJob = null
        deviceObserver = null
        try {
            session?.close()
        } catch (e: Throwable) {
            appLogD(TAG, "session.close() 异常: ${e.message}")
        }
        session = null
        _onlineDevices.value = emptyList()
        _onlineDeviceCdis.value = emptyList()
        _lastReplyMessageId.value = null
        _pendingReplyMessageIds.value = emptySet()
        _currentRequestMessageId.value = null
        _assistantReplyText.value = ""
        _assistantReplyStreaming.value = false
    }

    private fun handleObserverFailure(
        throwable: Throwable,
        logLabel: String,
        logUpdater: (String) -> Unit,
    ) {
        val message = throwable.message ?: "unknown"
        if (isAuthorizationError(throwable)) {
            scope.launch {
                connectMutex.withLock {
                    tokenStore.clear()
                    resetSessionResources()
                    _connectionState.value = SdkConnectionState.DISCONNECTED
                    _connectionLog.value = "鉴权失效，请重新登录"
                    logUpdater("${logLabel}异常：$message")
                    appLogD(TAG, _connectionLog.value)
                    appLogD(TAG, if (logLabel == "监听") _consumerLog.value else _deviceLog.value)
                }
            }
            return
        }

        logUpdater("${logLabel}异常：$message")
        appLogD(TAG, if (logLabel == "监听") _consumerLog.value else _deviceLog.value)

        if (isConnectionClosedError(throwable)) {
            appLogD(TAG, "${logLabel}检测到连接已关闭，准备重连")
            scheduleReconnect("${logLabel}连接已关闭")
            return
        }

        if (isSubscribeTimeoutError(throwable)) {
            appLogD(TAG, "${logLabel}订阅超时，准备重建监听")
            scheduleObserverRestart(logLabel)
            return
        }

        _connectionState.value = SdkConnectionState.ERROR
    }

    private fun isAuthorizationError(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: return false
        return message.contains("authorization violation") ||
            message.contains("auth violation") ||
            message.contains("permission denied")
    }

    private fun isSubscribeTimeoutError(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: return false
        return message.contains("subscribe") && message.contains("timed out")
    }

    private fun isConnectionClosedError(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: return false
        return message.contains("no connection open") ||
            message.contains("connection closed") ||
            message.contains("cannot send with no connection open")
    }

    private fun areObserversActive(): Boolean {
        return consumerJob?.isActive == true && deviceObserverJob?.isActive == true
    }

    private fun scheduleObserverRestart(logLabel: String) {
        if (observerRestartJob?.isActive == true) return
        observerRestartJob =
            scope.launch {
                delay(OBSERVER_RESTART_DELAY_MS)
                connectMutex.withLock {
                    val activeSession = session
                    if (activeSession == null || _connectionState.value != SdkConnectionState.CONNECTED) {
                        appLogD(TAG, "${logLabel}重建监听跳过：当前无可用连接")
                        return@withLock
                    }
                    restartObservers(session = activeSession, reason = "${logLabel}订阅超时")
                }
            }
    }

    private fun scheduleReconnect(reason: String) {
        if (reconnectJob?.isActive == true) return
        reconnectJob =
            scope.launch {
                delay(RECONNECT_DELAY_MS)
                connectMutex.withLock {
                    _connectionState.value = SdkConnectionState.DISCONNECTED
                    _connectionLog.value = "$reason，正在重连..."
                    appLogD(TAG, _connectionLog.value)
                    resetSessionResources()
                }
                ensureConnectedIfTokenValid()
            }
    }

    private fun restartObservers(session: ConnectedSession, reason: String) {
        appLogD(TAG, "$reason，开始重建监听")
        consumerJob?.cancel()
        deviceObserverJob?.cancel()
        deviceObserver?.stop()
        consumerJob = null
        deviceObserverJob = null
        deviceObserver = null
        _consumerLog.value = "监听重建中..."
        _deviceLog.value = "设备监听重建中..."
        startObservers(session)
    }

    private fun handleMachineEvent(event: NpcMachineEvent?) {
        event ?: return
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
        val candidates = buildList {
            add(root)
            root["event"]?.let { runCatching { it.jsonObject }.getOrNull()?.let(::add) }
            root["data"]?.let { runCatching { it.jsonObject }.getOrNull()?.let(::add) }
            root["payload"]?.let { runCatching { it.jsonObject }.getOrNull()?.let(::add) }
            root["events"]?.let { events ->
                runCatching { events.jsonArray }.getOrNull()?.forEach { item ->
                    runCatching { item.jsonObject }.getOrNull()?.let(::add)
                }
            }
        }

        candidates.forEach { candidate ->
            val event = parseMachineEventObject(candidate)
            if (event != null) return event
        }
        return null
    }

    private fun parseMachineEventObject(root: JsonObject): NpcMachineEvent? {
        val type =
            root["type"]?.jsonPrimitive?.contentOrNull
                ?: root["event_type"]?.jsonPrimitive?.contentOrNull
                ?: return null

        val text = extractTextFromEventObject(root)

        val sourceMessageId =
            root["source_message_id"]?.jsonPrimitive?.contentOrNull
                ?: root["sourceMessageId"]?.jsonPrimitive?.contentOrNull
                ?: root["request_id"]?.jsonPrimitive?.contentOrNull
                ?: root["messageId"]?.jsonPrimitive?.contentOrNull

        return NpcMachineEvent(type = type, text = text, sourceMessageId = sourceMessageId)
    }

    private fun extractTextFromEventObject(root: JsonObject): String? {
        val directKeys = listOf("text", "content", "delta", "message", "answer")
        directKeys.forEach { key ->
            val value = root[key] ?: return@forEach
            extractTextValue(value)?.let { return it }
        }

        val nestedKeys = listOf("data", "payload", "event", "result")
        nestedKeys.forEach { key ->
            val nested = runCatching { root[key]?.jsonObject }.getOrNull() ?: return@forEach
            extractTextFromEventObject(nested)?.let { return it }
        }
        return null
    }

    private fun extractTextValue(value: JsonElement): String? {
        runCatching { value.jsonPrimitive.contentOrNull }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val asObject = runCatching { value.jsonObject }.getOrNull() ?: return null
        return extractTextFromEventObject(asObject)
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
        private const val OBSERVER_RESTART_DELAY_MS = 800L
        private const val RECONNECT_DELAY_MS = 800L
        const val DEFAULT_TARGET_CDI = "2042541809425543168"
    }
}

private fun generateMessageId19(): String {
    val now = currentTimeMillis().coerceAtLeast(0L)
    val millisPart = (now % 1_000_000_000_000L).toString().padStart(12, '0')
    val nanoPart =
        (kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeNanoseconds and Long.MAX_VALUE) %
            10_000_000L
    val randomPart = (nanoPart % 10_000_000L).toString().padStart(7, '0')
    return millisPart + randomPart
}

private fun String.escapeForJson(): String {
    return this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
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
