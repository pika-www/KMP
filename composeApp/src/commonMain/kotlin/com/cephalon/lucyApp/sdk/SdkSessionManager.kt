package com.cephalon.lucyApp.sdk

import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.di.AppConfig
import com.cephalon.lucyApp.logging.appLogD
import com.cephalon.lucyApp.time.currentTimeMillis
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import lucy.im.sdk.ConnectedSession
import lucy.im.sdk.LucyImAppClient
import lucy.im.sdk.LucyImAppConfig
import lucy.im.sdk.OnlineDevice
import lucy.im.sdk.OnlineNpcDeviceHandle
import lucy.im.sdk.blob.BlobPutResult
import lucy.im.sdk.collectDevices
import lucy.im.sdk.filetransfer.ProgressFrame
import lucy.im.sdk.filetransfer.SendFileOutcome
import lucy.im.sdk.filetransfer.SendItem
import lucy.im.sdk.filetransfer.SendOptions
import lucy.im.sdk.filetransfer.TransferTarget

enum class FileTransferDeviceKind {
    Npc,
    Nas,
}

data class TransferUploadItem(
    val entryId: String,
    val entryName: String,
    val bytes: ByteArray,
)

data class NasRegisterBlobItem(
    val blobRef: String,
    val entryId: String? = null,
    val fileName: String? = null,
    val kind: String? = null,
    val contentType: String? = null,
)

data class NasRegisterBlobResult(
    val blobRef: String,
    val ok: Boolean,
    val id: String?,
    val error: String?,
)

data class NasRegisterBlobsResponse(
    val cmd: String,
    val requestId: String?,
    val ok: Boolean?,
    val results: List<NasRegisterBlobResult>,
)

data class NasFileListItem(
    val id: Long?,
    val time: String?,
    val location: String?,
    val kind: String?,
    val contentType: String?,
    val size: Long?,
    val fileName: String?,
    val desc: String?,
    val thumbnailImgBlobRef: String?,
)

data class NasFileListResponse(
    val cmd: String,
    val requestId: String?,
    val kind: String?,
    val items: List<NasFileListItem>,
    val nextCursor: String?,
    val error: String?,
)

private data class PendingNasRegisterRequest(
    val requestId: String,
    val blobRefs: List<String>,
    val waiter: CompletableDeferred<NasRegisterBlobsResponse>,
)

private data class PendingNasFileListRequest(
    val requestId: String,
    val kind: String,
    val cursor: String?,
    val waiter: CompletableDeferred<NasFileListResponse>,
)

class SdkSessionManager(
    private val tokenStore: AuthTokenStore,
) {
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        appLogD(TAG, "协程异常(已捕获): ${throwable.message}")
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private val connectMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private val sdkDispatcher = Dispatchers.Default.limitedParallelism(4)
    private val sdkClient =
        LucyImAppClient(
            LucyImAppConfig(
                lucyServerBaseUrl = "${AppConfig.baseDomain}/aiden/lucy-server",
                dispatcher = sdkDispatcher,
            ),
        )

    private var session: ConnectedSession? = null
    private var consumerJob: Job? = null
    private var deviceObserver: OnlineNpcDeviceHandle? = null
    private var deviceObserverJob: Job? = null
    private var observerRestartJob: Job? = null
    private var reconnectJob: Job? = null
    private var tokenExpiryJob: Job? = null
    private var authReconnectAttempts = 0

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

    private val _streamingStatusText = MutableStateFlow<String?>(null)
    val streamingStatusText: StateFlow<String?> = _streamingStatusText.asStateFlow()

    private val _reasoningText = MutableStateFlow("")
    val reasoningText: StateFlow<String> = _reasoningText.asStateFlow()

    private val _activeRequestIds = MutableStateFlow<Set<String>>(emptySet())
    val activeRequestIds: StateFlow<Set<String>> = _activeRequestIds.asStateFlow()

    private val _replyStateMap = MutableStateFlow<Map<String, ReplyState>>(emptyMap())
    val replyStateMap: StateFlow<Map<String, ReplyState>> = _replyStateMap.asStateFlow()

    private val pendingNasRegisterRequests = MutableStateFlow<Map<String, PendingNasRegisterRequest>>(emptyMap())
    private val pendingNasFileListRequests = MutableStateFlow<Map<String, PendingNasFileListRequest>>(emptyMap())
    private val _activeFileTransferCount = MutableStateFlow(0)

    private var _latestRequestId: String? = null

    private val _isInBackground = MutableStateFlow(false)
    val isInBackground: StateFlow<Boolean> = _isInBackground.asStateFlow()
    private var deferredDisconnectJob: Job? = null

    var localNotificationSender: LocalNotificationSender? = null

    fun connectAfterLogin() {
        connectIfTokenValid()
    }

    fun reconnectOnAppStartIfTokenExists() {
        connectIfTokenValid()
    }

    fun onForeground() {
        _isInBackground.value = false
        deferredDisconnectJob?.cancel()
        deferredDisconnectJob = null
        if (tokenStore.getValidTokenOrNull().isNullOrBlank()) return
        connectIfTokenValid()
    }

    fun onBackground() {
        _isInBackground.value = true
        if (session == null) return

        if (hasActiveTransportWork()) {
            // 对话进行中（或已发送请求等待回复），不断开连接
            appLogD(
                TAG,
                "应用进入后台，存在进行中的连接工作(activeIds=${_activeRequestIds.value}, fileTransfers=${_activeFileTransferCount.value}, nasRegisters=${pendingNasRegisterRequests.value.size})，保持连接",
            )
            return
        }

        // 无对话进行，主动断开 NATS 连接，防止息屏后 socket 断开导致 natskt 内部协程
        // 抛出 ClosedByteChannelException (ENOTCONN) 未捕获异常闪退
        disconnectInBackground()
    }

    private fun disconnectInBackground() {
        appLogD(TAG, "应用在后台，主动断开 NATS 连接")
        scope.launch {
            connectMutex.withLock {
                resetSessionResources()
                _connectionState.value = SdkConnectionState.DISCONNECTED
                _connectionLog.value = "应用在后台，已主动断开"
                appLogD(TAG, _connectionLog.value)
            }
        }
    }

    /** 对话结束后，若仍在后台则延迟断开连接 */
    private fun scheduleBackgroundDisconnectIfNeeded() {
        if (!_isInBackground.value) return
        deferredDisconnectJob?.cancel()
        deferredDisconnectJob = scope.launch {
            delay(BACKGROUND_DISCONNECT_DELAY_MS)
            if (_isInBackground.value && !hasActiveTransportWork()) {
                disconnectInBackground()
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
                val observersActive = areObserversActive()
                appLogD(TAG, "SDK 已连接(复用), userId=${session!!.userId}, observersActive=$observersActive, onlineDeviceCdis=${_onlineDeviceCdis.value}, consumerJob=${consumerJob?.isActive}, deviceObserverJob=${deviceObserverJob?.isActive}")
                if (!observersActive) {
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
            appLogD(TAG, "connect: token=${token.take(20)}..., tokenLen=${token.length}")

            appLogD(TAG, "connect: 调用 sdkClient.connect() ...")
            val connectResult = runCatching { sdkClient.connect(token) }
            appLogD(TAG, "connect: sdkClient.connect() 返回 isSuccess=${connectResult.isSuccess}, error=${connectResult.exceptionOrNull()?.message}")

            connectResult.onSuccess { newSession ->
                appLogD(TAG, "connect: onSuccess 开始, userId=${newSession.userId}")
                resetSessionResources()
                session = newSession
                authReconnectAttempts = 0
                _connectionState.value = SdkConnectionState.CONNECTED
                _connectionLog.value = "连接成功，userId=${newSession.userId}"
                appLogD(TAG, _connectionLog.value)
                appLogD(TAG, "connect: 即将启动 observers...")
                startObservers(newSession)
                appLogD(TAG, "connect: observers 已启动")
                startTokenExpiryMonitor()
                appLogD(TAG, "connect: tokenExpiryMonitor 已启动")
            }.onFailure { error ->
                appLogD(TAG, "connect: onFailure error=${error::class.simpleName}: ${error.message}")
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
                _activeRequestIds.value = emptySet()
                _replyStateMap.value = emptyMap()
                _latestRequestId = null
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
            _activeRequestIds.update { it + outgoingMessageId }
            _replyStateMap.update { it + (outgoingMessageId to ReplyState()) }
            _latestRequestId = outgoingMessageId
            _assistantReplyText.value = ""
            _assistantReplyStreaming.value = false
            _streamingStatusText.value = null
            _reasoningText.value = ""
            appLogD(TAG, "发送请求 messageId=$outgoingMessageId，当前活跃: ${_activeRequestIds.value}")
        }
        val userId = activeSession.userId
        val natsSubject = "cephalon.im.npc.$userId.$cdi"
        appLogD(TAG, "发送消息 subject=$natsSubject, userId=$userId, cdi=$cdi, payload=$payload")
        return runCatching {
            activeSession.publishToNpc(cdi = cdi, payload = payload)
        }.onSuccess {
            appLogD(TAG, "发送成功 subject=$natsSubject")
        }.onFailure { error ->
            if (outgoingMessageId != null) {
                _activeRequestIds.update { it - outgoingMessageId }
                _replyStateMap.update { it - outgoingMessageId }
                if (_latestRequestId == outgoingMessageId) {
                    _latestRequestId = _activeRequestIds.value.lastOrNull()
                }
            }
            appLogD(TAG, "发送失败 cdi=$cdi error=${error.message ?: "unknown"}")
        }
    }

    suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<BlobPutResult> {
        appLogD(TAG, "开始上传图片 fileName=$fileName size=${imageBytes.size}")
        return runCatching {
            platformUploadBlob(imageBytes, fileName)
        }.onSuccess { result ->
            appLogD(TAG, "图片上传成功 blobRef=${result.blobRef} fileHash=${result.fileHash}")
        }.onFailure { error ->
            appLogD(TAG, "图片上传失败: ${error.message ?: "unknown"}")
        }
    }

    suspend fun sendFilesToDevice(
        targetCdi: String,
        items: List<TransferUploadItem>,
        deviceKind: FileTransferDeviceKind = FileTransferDeviceKind.Nas,
        retryCount: Int = 3,
        timeoutMs: Long = 60_000L,
        onProgress: (ProgressFrame) -> Unit = {},
    ): Result<SendFileOutcome> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("上传文件不能为空"))
        }

        ensureConnectedIfTokenValid()
            .exceptionOrNull()
            ?.let { error -> return Result.failure(error) }

        val activeSession = session
            ?: return Result.failure(IllegalStateException("请先连接 SDK"))

        val resolvedTargetCdi = targetCdi.trim()
        val target =
            when (deviceKind) {
                FileTransferDeviceKind.Npc -> TransferTarget.Npc(targetCdi = resolvedTargetCdi)
                FileTransferDeviceKind.Nas -> TransferTarget.Nas(targetCdi = resolvedTargetCdi)
            }

        appLogD(
            TAG,
            "开始批量传输文件 target=${deviceKind.name} cdi=$resolvedTargetCdi count=${items.size}",
        )

        _activeFileTransferCount.update { it + 1 }

        return runCatching {
            activeSession
                .blobTransfer(scope)
                .sendfileToDevice(
                    target = target,
                    items = items.map { item ->
                        SendItem(
                            entryId = item.entryId,
                            entryName = item.entryName,
                            bytes = item.bytes,
                        )
                    },
                    options = SendOptions(
                        retryCount = retryCount,
                        timeoutMs = timeoutMs,
                        onProgress = onProgress,
                    ),
                )
        }.onSuccess { outcome ->
            appLogD(
                TAG,
                "批量传输成功 target=${deviceKind.name} cdi=$resolvedTargetCdi status=${outcome.done.status} items=${outcome.done.items.size}",
            )
        }.onFailure { error ->
            appLogD(
                TAG,
                "批量传输失败 target=${deviceKind.name} cdi=$resolvedTargetCdi error=${error.message ?: "unknown"}",
            )
        }.also {
            _activeFileTransferCount.update { current -> (current - 1).coerceAtLeast(0) }
            if (_activeFileTransferCount.value == 0) {
                scheduleBackgroundDisconnectIfNeeded()
            }
        }
    }

    suspend fun registerBlobsToNas(
        targetCdi: String,
        items: List<NasRegisterBlobItem>,
        timeoutMs: Long = 60_000L,
    ): Result<NasRegisterBlobsResponse> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("登记文件不能为空"))
        }

        ensureConnectedIfTokenValid()
            .exceptionOrNull()
            ?.let { error -> return Result.failure(error) }

        val activeSession = session
            ?: return Result.failure(IllegalStateException("请先连接 SDK"))

        val requestId = generateMessageId19()
        val rspSubject = "cephalon.im.user.${activeSession.userId}"
        val waiter = CompletableDeferred<NasRegisterBlobsResponse>()
        val resolvedTargetCdi = targetCdi.trim()
        pendingNasRegisterRequests.update { current ->
            current + (requestId to PendingNasRegisterRequest(requestId, items.map { it.blobRef }, waiter))
        }
        _activeFileTransferCount.update { it + 1 }

        appLogD(
            TAG,
            "开始 NAS blob 登记 cdi=$resolvedTargetCdi requestId=$requestId count=${items.size}",
        )

        return runCatching {
            activeSession.publishToNas(
                cdi = resolvedTargetCdi,
                payload = buildNasRegisterBlobsPayload(
                    requestId = requestId,
                    rspSubject = rspSubject,
                    items = items,
                ),
            )
            withTimeoutOrNull(timeoutMs) { waiter.await() }
                ?: throw IllegalStateException("等待 NAS 登记响应超时")
        }.onSuccess { response ->
            appLogD(
                TAG,
                "NAS blob 登记成功 cdi=$resolvedTargetCdi requestId=$requestId ok=${response.ok} results=${response.results.size}",
            )
        }.onFailure { error ->
            appLogD(
                TAG,
                "NAS blob 登记失败 cdi=$resolvedTargetCdi requestId=$requestId error=${error.message ?: "unknown"}",
            )
            waiter.cancel(error as? CancellationException)
        }.also {
            pendingNasRegisterRequests.update { current -> current - requestId }
            _activeFileTransferCount.update { current -> (current - 1).coerceAtLeast(0) }
            if (_activeFileTransferCount.value == 0) {
                scheduleBackgroundDisconnectIfNeeded()
            }
        }
    }

    suspend fun listFilesFromNas(
        targetCdi: String,
        kind: String,
        pageSize: Int = 20,
        cursor: String? = null,
        timeoutMs: Long = 60_000L,
    ): Result<NasFileListResponse> {
        val normalizedKind = kind.trim()
        if (normalizedKind.isBlank()) {
            return Result.failure(IllegalArgumentException("kind 不能为空"))
        }
        if (pageSize <= 0) {
            return Result.failure(IllegalArgumentException("pageSize 必须大于 0"))
        }

        ensureConnectedIfTokenValid()
            .exceptionOrNull()
            ?.let { error -> return Result.failure(error) }

        val activeSession = session
            ?: return Result.failure(IllegalStateException("请先连接 SDK"))

        val requestId = generateMessageId19()
        val waiter = CompletableDeferred<NasFileListResponse>()
        val rspSubject = "cephalon.im.user.${activeSession.userId}"
        val resolvedTargetCdi = targetCdi.trim()
        pendingNasFileListRequests.update { current ->
            current + (requestId to PendingNasFileListRequest(requestId, normalizedKind, cursor, waiter))
        }

        appLogD(
            TAG,
            "开始获取 NAS 文件列表 cdi=$resolvedTargetCdi requestId=$requestId kind=$normalizedKind pageSize=$pageSize cursor=${cursor ?: "null"}",
        )

        return runCatching {
            activeSession.publishToNas(
                cdi = resolvedTargetCdi,
                payload = buildNasFileListPayload(
                    requestId = requestId,
                    rspSubject = rspSubject,
                    kind = normalizedKind,
                    pageSize = pageSize,
                    cursor = cursor,
                ),
            )
            withTimeoutOrNull(timeoutMs) { waiter.await() }
                ?: throw IllegalStateException("等待 NAS 文件列表响应超时")
        }.onSuccess { response ->
            appLogD(
                TAG,
                "获取 NAS 文件列表成功 cdi=$resolvedTargetCdi requestId=$requestId kind=$normalizedKind count=${response.items.size} nextCursor=${response.nextCursor ?: "null"} error=${response.error ?: "null"}",
            )
        }.onFailure { error ->
            appLogD(
                TAG,
                "获取 NAS 文件列表失败 cdi=$resolvedTargetCdi requestId=$requestId kind=$normalizedKind error=${error.message ?: "unknown"}",
            )
            waiter.cancel(error as? CancellationException)
        }.also {
            pendingNasFileListRequests.update { current -> current - requestId }
            scheduleBackgroundDisconnectIfNeeded()
        }
    }

    suspend fun publishTextWithImageToNpc(
        cdi: String,
        text: String,
        blobRef: String,
        contentType: String,
        size: Long,
        fileName: String,
    ): Result<String> {
        val messageId = generateMessageId19()
        val escapedText = text.trim().escapeForJson()
        val escapedBlobRef = blobRef.escapeForJson()
        val escapedFileName = fileName.escapeForJson()
        val payload =
            """${'{'}"version":2,"messageId":"$messageId","text":"$escapedText","media":{"transport":"iroh-blob","blob_ref":"$escapedBlobRef","kind":"image","contentType":"$contentType","size":$size,"fileName":"$escapedFileName"},"timestamp":${currentTimeMillis()}}"""
        appLogD(TAG, "发送图片消息 cdi=$cdi fileName=$fileName")
        return publishToNpc(cdi = cdi, payload = payload).map { messageId }
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

    // ── Token 过期监控 ──

    private fun startTokenExpiryMonitor() {
        tokenExpiryJob?.cancel()
        tokenExpiryJob = scope.launch {
            while (true) {
                val remaining = tokenStore.getTokenRemainingMillis()
                appLogD(TAG, "[TokenMonitor] token 剩余有效时间: ${remaining / 1000}s (${remaining / 60000}min)")

                if (remaining <= 0L) {
                    appLogD(TAG, "[TokenMonitor] token 已过期，执行重连")
                    reconnectWithFreshToken()
                    break
                }

                // 提前 60s 重连，留出缓冲
                val checkInterval = if (remaining <= TOKEN_REFRESH_BUFFER_MS) {
                    appLogD(TAG, "[TokenMonitor] token 即将过期(${remaining / 1000}s)，执行重连")
                    reconnectWithFreshToken()
                    break
                } else {
                    // 下次检查时间 = 剩余时间 - 缓冲，但至少 30s 检查一次
                    (remaining - TOKEN_REFRESH_BUFFER_MS).coerceIn(TOKEN_CHECK_MIN_INTERVAL_MS, TOKEN_CHECK_MAX_INTERVAL_MS)
                }

                delay(checkInterval)
            }
        }
    }

    private suspend fun reconnectWithFreshToken() {
        connectMutex.withLock {
            // 检查是否有新 token（可能已被刷新）
            val token = tokenStore.getValidTokenOrNull()
            if (token.isNullOrBlank()) {
                appLogD(TAG, "[TokenMonitor] 无有效 token，断开连接")
                resetSessionResources()
                _connectionState.value = SdkConnectionState.DISCONNECTED
                _connectionLog.value = "Token 过期，请重新登录"
                return
            }
            // token 仍有效或已被刷新，重建连接
            appLogD(TAG, "[TokenMonitor] 使用新 token 重连")
            resetSessionResources()
        }
        ensureConnectedIfTokenValid()
    }

    private fun startObservers(newSession: ConnectedSession) {
        observerRestartJob?.cancel()
        observerRestartJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        _receivedMessages.value = emptyList()

        appLogD(TAG, "[DeviceObserver] 启动在线设备订阅, userId=${newSession.userId}, subscribeChannel=cephalon.im.user.${newSession.userId}")
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
                        "[DeviceObserver] 当前无在线设备（userId=${newSession.userId}, lastOnlineCdi=${_lastOnlineCdi.value ?: "none"}）"
                    } else {
                        "[DeviceObserver] 在线设备更新: ${cdis.size} 台, cdis=[${cdis.joinToString(", ")}], userId=${newSession.userId}"
                    }
                appLogD(TAG, _deviceLog.value)
            }

        consumerJob =
            newSession.startUserChannelConsumer(scope) { subject, messagePayload ->
                val messageText =
                    runCatching { messagePayload.decodeToString() }.getOrDefault(
                        "<binary payload ${messagePayload.size} bytes>",
                    )
                appLogD(TAG, "[Consumer] 收到原始消息 subject=$subject len=${messageText.length}")
                val nasFileListResponse = parseNasFileListResponse(messageText)
                if (nasFileListResponse != null) {
                    val matchedRequestId = findMatchingNasFileListRequestId(nasFileListResponse)
                    if (matchedRequestId != null) {
                        appLogD(
                            TAG,
                            "[Consumer] 接受 NAS 文件列表响应 requestId=$matchedRequestId kind=${nasFileListResponse.kind ?: "unknown"} count=${nasFileListResponse.items.size}",
                        )
                        _receivedMessages.update { current ->
                            (listOf("subject=$subject\n$messageText") + current).take(100)
                        }
                        pendingNasFileListRequests.value[matchedRequestId]?.waiter?.complete(nasFileListResponse)
                        pendingNasFileListRequests.update { current -> current - matchedRequestId }
                        _consumerLog.value = "监听中，累计接收 ${_receivedMessages.value.size} 条"
                        appLogD(TAG, _consumerLog.value)
                    } else {
                        appLogD(
                            TAG,
                            "[Consumer] 收到未匹配的 NAS 文件列表响应 requestId=${nasFileListResponse.requestId ?: "none"} kind=${nasFileListResponse.kind ?: "unknown"} count=${nasFileListResponse.items.size}",
                        )
                    }
                    return@startUserChannelConsumer
                }
                val nasRegisterResponse = parseNasRegisterBlobsResponse(messageText)
                if (nasRegisterResponse != null) {
                    val matchedRequestId = findMatchingNasRegisterRequestId(nasRegisterResponse)
                    if (matchedRequestId != null) {
                        appLogD(
                            TAG,
                            "[Consumer] 接受 NAS 登记响应 requestId=$matchedRequestId ok=${nasRegisterResponse.ok} results=${nasRegisterResponse.results.size}",
                        )
                        _receivedMessages.update { current ->
                            (listOf("subject=$subject\n$messageText") + current).take(100)
                        }
                        pendingNasRegisterRequests.value[matchedRequestId]?.waiter?.complete(nasRegisterResponse)
                        pendingNasRegisterRequests.update { current -> current - matchedRequestId }
                        _consumerLog.value = "监听中，累计接收 ${_receivedMessages.value.size} 条"
                        appLogD(TAG, _consumerLog.value)
                    } else {
                        appLogD(
                            TAG,
                            "[Consumer] 收到未匹配的 NAS 登记响应 requestId=${nasRegisterResponse.requestId ?: "none"} results=${nasRegisterResponse.results.size}",
                        )
                    }
                    return@startUserChannelConsumer
                }
                val machineEvent = parseMachineEvent(messageText)
                if (machineEvent != null) {
                    appLogD(TAG, "[Consumer] 解析事件 type=${machineEvent.type}, text=${machineEvent.text?.take(80) ?: "null"}, sourceId=${machineEvent.sourceMessageId ?: "null"}")
                } else {
                    appLogD(TAG, "[Consumer] 未解析为事件，原始: ${messageText.take(200)}")
                }
                val incomingSourceMessageId = machineEvent?.sourceMessageId ?: extractSourceMessageId(messageText)
                val activeIds = _activeRequestIds.value
                val sourceMatched = !incomingSourceMessageId.isNullOrBlank() &&
                    incomingSourceMessageId in activeIds
                if (!sourceMatched) {
                    appLogD(
                        TAG,
                        "[Consumer] 过滤掉消息: sourceId=${incomingSourceMessageId ?: "none"}, activeIds=$activeIds, matched=false",
                    )
                    return@startUserChannelConsumer
                }
                appLogD(TAG, "[Consumer] 接受消息 subject=$subject sourceId=$incomingSourceMessageId")
                _receivedMessages.update { current ->
                    (listOf("subject=$subject\n$messageText") + current).take(100)
                }
                handleMachineEvent(machineEvent, incomingSourceMessageId)
                _lastReplyMessageId.value = incomingSourceMessageId
                _consumerLog.value = "监听中，累计接收 ${_receivedMessages.value.size} 条"
                appLogD(TAG, _consumerLog.value)
            }

        consumerJob?.invokeOnCompletion { throwable ->
            if (throwable is CancellationException) return@invokeOnCompletion
            if (throwable == null) {
                appLogD(TAG, "[Consumer] 消费者正常结束（不应发生），准备重建监听")
                _consumerLog.value = "监听意外结束，准备恢复..."
                scheduleObserverRestart("消费者正常结束")
                return@invokeOnCompletion
            }
            handleObserverFailure(
                throwable = throwable,
                logLabel = "监听",
                logUpdater = { _consumerLog.value = it },
            )
        }

        deviceObserverJob?.invokeOnCompletion { throwable ->
            if (throwable is CancellationException) return@invokeOnCompletion
            if (throwable == null) {
                appLogD(TAG, "[DeviceObserver] 设备监听正常结束（不应发生），准备重建")
                _deviceLog.value = "设备监听意外结束，准备恢复..."
                scheduleObserverRestart("设备监听正常结束")
                return@invokeOnCompletion
            }
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
        tokenExpiryJob?.cancel()
        tokenExpiryJob = null
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
        _activeRequestIds.value = emptySet()
        _replyStateMap.value = emptyMap()
        pendingNasRegisterRequests.value.values.forEach { pending ->
            pending.waiter.cancel()
        }
        pendingNasFileListRequests.value.values.forEach { pending ->
            pending.waiter.cancel()
        }
        pendingNasRegisterRequests.value = emptyMap()
        pendingNasFileListRequests.value = emptyMap()
        _activeFileTransferCount.value = 0
        _latestRequestId = null
        _assistantReplyText.value = ""
        _assistantReplyStreaming.value = false
        _streamingStatusText.value = null
        _reasoningText.value = ""
    }

    private fun handleObserverFailure(
        throwable: Throwable,
        logLabel: String,
        logUpdater: (String) -> Unit,
    ) {
        val message = throwable.message ?: "unknown"
        if (isAuthorizationError(throwable)) {
            authReconnectAttempts++
            if (authReconnectAttempts > MAX_AUTH_RECONNECT_ATTEMPTS) {
                // 多次重连仍失败，app token 可能也已失效
                appLogD(TAG, "${logLabel} NATS 鉴权重连已达上限($MAX_AUTH_RECONNECT_ATTEMPTS 次)，清除 token")
                scope.launch {
                    connectMutex.withLock {
                        tokenStore.clear()
                        resetSessionResources()
                        authReconnectAttempts = 0
                        _connectionState.value = SdkConnectionState.DISCONNECTED
                        _connectionLog.value = "鉴权失效，请重新登录"
                        logUpdater("${logLabel}：鉴权多次重连失败")
                        appLogD(TAG, _connectionLog.value)
                    }
                }
                return
            }
            // NATS token 过期，用 app token 重新 connect() 换取新 NATS token
            appLogD(TAG, "${logLabel} NATS 鉴权失败(token 可能过期)，尝试重连 (第${authReconnectAttempts}次)")
            logUpdater("${logLabel}：NATS token 过期，正在重连...")
            scheduleReconnect("${logLabel} NATS token 过期")
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

    private fun handleMachineEvent(event: NpcMachineEvent?, sourceMessageId: String?) {
        event ?: return
        val msgId = sourceMessageId ?: return
        val isLatest = msgId == _latestRequestId
        appLogD(TAG, "[Event] 处理事件 type=${event.type}, msgId=$msgId, isLatest=$isLatest, textLen=${event.text?.length ?: 0}, tool=${event.toolName ?: "none"}")
        when (event.type) {
            "inbound.accepted" -> {
                updateReplyState(msgId) { it.copy(streaming = true, streamingStatusText = "消息已送达") }
                if (isLatest) {
                    _streamingStatusText.value = "消息已送达"
                    _assistantReplyStreaming.value = true
                }
                appLogD(TAG, "[Event] inbound.accepted → 已送达 msgId=$msgId")
            }

            "assistant.start" -> {
                updateReplyState(msgId) { state ->
                    state.copy(
                        text = if (state.text.isNotEmpty()) state.text else "",
                        streaming = true,
                        reasoningText = "",
                        streamingStatusText = "正在生成回复...",
                    )
                }
                if (isLatest) {
                    if (_assistantReplyText.value.isEmpty()) _assistantReplyText.value = ""
                    _reasoningText.value = ""
                    _streamingStatusText.value = "正在生成回复..."
                    _assistantReplyStreaming.value = true
                }
                appLogD(TAG, "[Event] assistant.start → streaming=true msgId=$msgId")
            }

            "reasoning.partial" -> {
                updateReplyState(msgId) { state ->
                    state.copy(
                        reasoningText = event.text ?: state.reasoningText,
                        streaming = true,
                        streamingStatusText = "正在思考...",
                    )
                }
                if (isLatest) {
                    if (event.text != null) _reasoningText.value = event.text
                    _streamingStatusText.value = "正在思考..."
                    _assistantReplyStreaming.value = true
                }
            }

            "reasoning.final" -> {
                updateReplyState(msgId) { state ->
                    state.copy(
                        reasoningText = event.text ?: state.reasoningText,
                        streaming = true,
                        streamingStatusText = "思考完成，正在组织回复...",
                    )
                }
                if (isLatest) {
                    if (event.text != null) _reasoningText.value = event.text
                    _streamingStatusText.value = "思考完成，正在组织回复..."
                    _assistantReplyStreaming.value = true
                }
            }

            "tool.start" -> {
                val toolLabel = event.toolName ?: event.text ?: "工具"
                updateReplyState(msgId) { it.copy(streaming = true, streamingStatusText = "正在使用 $toolLabel...") }
                if (isLatest) {
                    _streamingStatusText.value = "正在使用 $toolLabel..."
                    _assistantReplyStreaming.value = true
                }
                appLogD(TAG, "[Event] tool.start → 使用工具: $toolLabel msgId=$msgId")
            }

            "tool.end" -> {
                updateReplyState(msgId) { it.copy(streaming = true, streamingStatusText = "工具调用完成，正在生成回复...") }
                if (isLatest) {
                    _streamingStatusText.value = "工具调用完成，正在生成回复..."
                    _assistantReplyStreaming.value = true
                }
                appLogD(TAG, "[Event] tool.end → 工具调用完成 msgId=$msgId")
            }

            "assistant.partial" -> {
                if (event.text != null) {
                    updateReplyState(msgId) { it.copy(text = event.text, streaming = true, streamingStatusText = null) }
                    if (isLatest) {
                        _assistantReplyText.value = event.text
                        _streamingStatusText.value = null
                    }
                    appLogD(TAG, "[Event] assistant.partial → text更新 len=${event.text.length} msgId=$msgId")
                } else {
                    appLogD(TAG, "[Event] assistant.partial → text为null，未更新! msgId=$msgId")
                }
                updateReplyState(msgId) { it.copy(streaming = true) }
                if (isLatest) _assistantReplyStreaming.value = true
            }

            "assistant.final" -> {
                val finalText = event.text
                updateReplyState(msgId) { state ->
                    state.copy(
                        text = finalText ?: state.text,
                        streaming = false,
                        streamingStatusText = null,
                    )
                }
                _activeRequestIds.update { it - msgId }
                if (isLatest) {
                    if (finalText != null) _assistantReplyText.value = finalText
                    _streamingStatusText.value = null
                    _assistantReplyStreaming.value = false
                    _latestRequestId = _activeRequestIds.value.lastOrNull()
                }
                appLogD(TAG, "====== 对话结束 msgId=$msgId ====== 剩余活跃: ${_activeRequestIds.value}")
                notifyConversationCompleteIfInBackground(msgId)
                if (_activeRequestIds.value.isEmpty()) {
                    scheduleBackgroundDisconnectIfNeeded()
                }
            }

            "error" -> {
                updateReplyState(msgId) { it.copy(streaming = false, streamingStatusText = null) }
                _activeRequestIds.update { it - msgId }
                if (isLatest) {
                    _streamingStatusText.value = null
                    _assistantReplyStreaming.value = false
                    _latestRequestId = _activeRequestIds.value.lastOrNull()
                }
                appLogD(TAG, "====== 对话结束(异常) msgId=$msgId ====== error=${event.text ?: "unknown"}")
                notifyConversationCompleteIfInBackground(msgId)
                if (_activeRequestIds.value.isEmpty()) {
                    scheduleBackgroundDisconnectIfNeeded()
                }
            }

            else -> {
                appLogD(TAG, "[Event] 未处理的事件类型: ${event.type} msgId=$msgId")
            }
        }
    }

    private fun updateReplyState(messageId: String, transform: (ReplyState) -> ReplyState) {
        _replyStateMap.update { map ->
            val current = map[messageId] ?: ReplyState()
            map + (messageId to transform(current))
        }
    }

    private fun notifyConversationCompleteIfInBackground(messageId: String) {
        if (!_isInBackground.value) return
        val state = _replyStateMap.value[messageId]
        val replyPreview = (state?.text ?: _assistantReplyText.value).take(80).ifBlank { "对话已完成" }
        appLogD(TAG, "应用在后台，发送本地通知: $replyPreview")
        localNotificationSender?.sendNotification(
            title = "脑花",
            body = replyPreview,
        )
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

        val toolName =
            root["tool"]?.jsonPrimitive?.contentOrNull
                ?: root["tool_name"]?.jsonPrimitive?.contentOrNull
                ?: root["name"]?.jsonPrimitive?.contentOrNull

        return NpcMachineEvent(type = type, text = text, sourceMessageId = sourceMessageId, toolName = toolName)
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

    private fun hasActiveTransportWork(): Boolean {
        return _activeRequestIds.value.isNotEmpty() ||
            _activeFileTransferCount.value > 0 ||
            pendingNasRegisterRequests.value.isNotEmpty() ||
            pendingNasFileListRequests.value.isNotEmpty()
    }

    private fun parseNasFileListResponse(payload: String): NasFileListResponse? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null
        val cmd = root["cmd"]?.jsonPrimitive?.contentOrNull ?: return null
        if (cmd != "file_list_rsp") return null

        val body =
            runCatching { root["file_list_rsp"]?.jsonObject }.getOrNull()
                ?: root

        val items =
            runCatching { body["list"]?.jsonArray }.getOrNull()
                ?.mapNotNull { item ->
                    val itemObject = runCatching { item.jsonObject }.getOrNull() ?: return@mapNotNull null
                    NasFileListItem(
                        id = itemObject["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
                        time = itemObject["time"]?.jsonPrimitive?.contentOrNull,
                        location = itemObject["location"]?.jsonPrimitive?.contentOrNull,
                        kind = itemObject["kind"]?.jsonPrimitive?.contentOrNull,
                        contentType = itemObject["contentType"]?.jsonPrimitive?.contentOrNull,
                        size = itemObject["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
                        fileName = itemObject["fileName"]?.jsonPrimitive?.contentOrNull,
                        desc = itemObject["desc"]?.jsonPrimitive?.contentOrNull,
                        thumbnailImgBlobRef = itemObject["thumbnailImgBlobRef"]?.jsonPrimitive?.contentOrNull,
                    )
                }
                ?: emptyList()

        val kinds = items.mapNotNull { it.kind }.distinct()
        return NasFileListResponse(
            cmd = cmd,
            requestId =
                root["request_id"]?.jsonPrimitive?.contentOrNull
                    ?: body["request_id"]?.jsonPrimitive?.contentOrNull,
            kind = body["kind"]?.jsonPrimitive?.contentOrNull ?: kinds.singleOrNull(),
            items = items,
            nextCursor = body["next_cursor"]?.jsonPrimitive?.contentOrNull,
            error = body["error"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun parseNasRegisterBlobsResponse(payload: String): NasRegisterBlobsResponse? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null
        val cmd = root["cmd"]?.jsonPrimitive?.contentOrNull ?: return null
        if (cmd != "file_register_blobs_rsp") return null

        val body =
            runCatching { root["file_register_blobs_rsp"]?.jsonObject }.getOrNull()
                ?: root

        val results =
            runCatching {
                (body["results"] ?: root["results"])?.jsonArray
            }.getOrNull()
                ?.mapNotNull { item ->
                    val resultObject = runCatching { item.jsonObject }.getOrNull() ?: return@mapNotNull null
                    val blobRef =
                        resultObject["blobRef"]?.jsonPrimitive?.contentOrNull
                            ?: resultObject["blob_ref"]?.jsonPrimitive?.contentOrNull
                            ?: return@mapNotNull null
                    NasRegisterBlobResult(
                        blobRef = blobRef,
                        ok = resultObject["ok"]?.jsonPrimitive?.booleanOrNull ?: false,
                        id = resultObject["id"]?.jsonPrimitive?.contentOrNull,
                        error = resultObject["error"]?.jsonPrimitive?.contentOrNull,
                    )
                }
                ?: emptyList()

        return NasRegisterBlobsResponse(
            cmd = cmd,
            requestId =
                root["request_id"]?.jsonPrimitive?.contentOrNull
                    ?: body["request_id"]?.jsonPrimitive?.contentOrNull,
            ok = body["ok"]?.jsonPrimitive?.booleanOrNull ?: root["ok"]?.jsonPrimitive?.booleanOrNull,
            results = results,
        )
    }

    private fun findMatchingNasRegisterRequestId(response: NasRegisterBlobsResponse): String? {
        response.requestId
            ?.takeIf { it.isNotBlank() }
            ?.let { requestId ->
                if (requestId in pendingNasRegisterRequests.value) {
                    return requestId
                }
            }

        val responseBlobRefs = response.results.map { it.blobRef }
        if (responseBlobRefs.isNotEmpty()) {
            pendingNasRegisterRequests.value.values.firstOrNull { pending ->
                pending.blobRefs == responseBlobRefs
            }?.let { pending ->
                return pending.requestId
            }
        }

        return pendingNasRegisterRequests.value.keys.singleOrNull()
    }

    private fun findMatchingNasFileListRequestId(response: NasFileListResponse): String? {
        response.requestId
            ?.takeIf { it.isNotBlank() }
            ?.let { requestId ->
                if (requestId in pendingNasFileListRequests.value) {
                    return requestId
                }
            }

        response.kind
            ?.takeIf { it.isNotBlank() }
            ?.let { responseKind ->
                val kindMatched =
                    pendingNasFileListRequests.value.values.filter { pending ->
                        pending.kind == responseKind
                    }
                if (kindMatched.size == 1) {
                    return kindMatched.first().requestId
                }
            }

        return pendingNasFileListRequests.value.keys.singleOrNull()
    }

    companion object {
        private const val TAG = "SdkSessionManager"
        private const val OBSERVER_RESTART_DELAY_MS = 800L
        private const val RECONNECT_DELAY_MS = 800L
        private const val BACKGROUND_DISCONNECT_DELAY_MS = 3000L
        private const val TOKEN_REFRESH_BUFFER_MS = 60_000L   // 过期前 60s 触发重连
        private const val TOKEN_CHECK_MIN_INTERVAL_MS = 30_000L  // 最少 30s 检查一次
        private const val TOKEN_CHECK_MAX_INTERVAL_MS = 300_000L // 最多 5min 检查一次
        private const val MAX_AUTH_RECONNECT_ATTEMPTS = 3       // NATS 鉴权重连最多尝试次数
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

private fun buildNasRegisterBlobsPayload(
    requestId: String,
    rspSubject: String,
    items: List<NasRegisterBlobItem>,
): String {
    val encodedItems =
        items.joinToString(",") { item ->
            buildString {
                append("{")
                append("\"blobRef\":\"")
                append(item.blobRef.escapeForJson())
                append("\"")
                item.entryId?.takeIf { it.isNotBlank() }?.let {
                    append(",\"entry_id\":\"")
                    append(it.escapeForJson())
                    append("\"")
                }
                item.fileName?.takeIf { it.isNotBlank() }?.let {
                    append(",\"fileName\":\"")
                    append(it.escapeForJson())
                    append("\"")
                }
                item.kind?.takeIf { it.isNotBlank() }?.let {
                    append(",\"kind\":\"")
                    append(it.escapeForJson())
                    append("\"")
                }
                item.contentType?.takeIf { it.isNotBlank() }?.let {
                    append(",\"contentType\":\"")
                    append(it.escapeForJson())
                    append("\"")
                }
                append("}")
            }
        }
    return buildString {
        append("{")
        append("\"cmd\":\"file_register_blobs_req\",")
        append("\"request_id\":\"")
        append(requestId.escapeForJson())
        append("\",")
        append("\"rsp_subject\":\"")
        append(rspSubject.escapeForJson())
        append("\",")
        append("\"file_register_blobs_req\":{")
        append("\"items\":[")
        append(encodedItems)
        append("]")
        append("}")
        append("}")
    }
}

private fun buildNasFileListPayload(
    requestId: String,
    rspSubject: String,
    kind: String,
    pageSize: Int,
    cursor: String?,
): String {
    return buildString {
        append("{")
        append("\"cmd\":\"file_list_req\",")
        append("\"request_id\":\"")
        append(requestId.escapeForJson())
        append("\",")
        append("\"rsp_subject\":\"")
        append(rspSubject.escapeForJson())
        append("\",")
        append("\"file_list_req\":{")
        append("\"kind\":\"")
        append(kind.escapeForJson())
        append("\",")
        append("\"page_size\":")
        append(pageSize)
        append(",\"cursor\":")
        if (cursor.isNullOrBlank()) {
            append("null")
        } else {
            append("\"")
            append(cursor.escapeForJson())
            append("\"")
        }
        append("}")
        append("}")
    }
}

private data class NpcMachineEvent(
    val type: String,
    val text: String?,
    val sourceMessageId: String?,
    val toolName: String? = null,
)

data class ReplyState(
    val text: String = "",
    val streaming: Boolean = false,
    val reasoningText: String = "",
    val streamingStatusText: String? = null,
)

enum class SdkConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}
