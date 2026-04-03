package com.cephalon.lucyApp.ws

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull

/**
 * WebSocket 业务入口，屏蔽具体 ws 路径和连接细节。
 */
class WsRepository(private val wsApi: WsApi) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { encodeDefaults = true }

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    private var session: DefaultClientWebSocketSession? = null
    private var receiveJob: Job? = null
    private var heartbeatJob: Job? = null
    private var isAwaitingAuthResult = false
    private var isAuthenticated = false

    suspend fun connect(cornId: String): DefaultClientWebSocketSession {
        close(silent = true)
        val newSession = wsApi.connect(cornId)
        session = newSession
        isAwaitingAuthResult = false
        isAuthenticated = false
        appendMessage("WS 已连接: $cornId")
        receiveJob = scope.launch {
            try {
                for (frame in newSession.incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            appendMessage("接收: $text")
                            handleAuthSuccessIfNeeded(text)
                        }
                        is Frame.Binary -> appendMessage("接收二进制消息: ${frame.data.size} bytes")
                        is Frame.Close -> appendMessage("服务端关闭连接")
                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                appendMessage("WS 异常: ${e.message ?: "unknown error"}")
            }
        }
        return newSession
    }

    suspend fun sendJwt(token: String) {
        val currentSession = session
        if (currentSession == null) {
            appendMessage("发送失败: WS 尚未连接")
            return
        }

        val payload = WsAuthPayload(
            data = WsAuthData(jwt = token)
        )
        val text = json.encodeToString(WsAuthPayload.serializer(), payload)
        isAwaitingAuthResult = true
        currentSession.send(Frame.Text(text))
        appendMessage("发送: $text")
    }

    suspend fun sendEvent(eventId: Int) {
        val currentSession = session
        if (currentSession == null) {
            appendMessage("发送失败: WS 尚未连接")
            return
        }

        val payload = WsEventPayload(eventId = eventId)
        val text = json.encodeToString(WsEventPayload.serializer(), payload)
        currentSession.send(Frame.Text(text))
        appendMessage("发送: $text")
    }

    suspend fun close() {
        close(silent = false)
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    private suspend fun close(silent: Boolean) {
        receiveJob?.cancelAndJoin()
        receiveJob = null
        heartbeatJob?.cancelAndJoin()
        heartbeatJob = null

        session?.close(
            CloseReason(
                CloseReason.Codes.NORMAL,
                "Closed by user"
            )
        )
        session = null
        isAwaitingAuthResult = false
        isAuthenticated = false

        if (!silent) {
            appendMessage("WS 已关闭")
        }
    }

    private fun handleAuthSuccessIfNeeded(message: String) {
        if (!isAwaitingAuthResult || isAuthenticated) return

        if (isJwtAuthSuccess(message)) {
            isAwaitingAuthResult = false
            isAuthenticated = true
            appendMessage("JWT 验证成功，开始发送心跳")
            startHeartbeat()
        }
    }

    private fun startHeartbeat() {
        val currentSession = session ?: return
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(15_000)
                val payload = WsEventPayload(eventId = 99)
                val text = json.encodeToString(WsEventPayload.serializer(), payload)
                currentSession.send(Frame.Text(text))
                appendMessage("发送心跳: $text")
            }
        }
    }

    private fun isJwtAuthSuccess(message: String): Boolean {
        val root = runCatching { json.parseToJsonElement(message).jsonObject }.getOrNull() ?: return false

        val code = root.intValue("code")
        val messageText = root.stringValue("message")
        return code == 0 && messageText == "success"
    }

    private fun JsonObject.intValue(key: String): Int? {
        return (this[key] as? JsonPrimitive)?.intOrNull
    }

    private fun JsonObject.stringValue(key: String): String? {
        return (this[key] as? JsonPrimitive)?.contentOrNull
    }

    private fun appendMessage(message: String) {
        _messages.value = _messages.value + message
    }
}

@Serializable
private data class WsAuthPayload(
    @SerialName("event_id")
    val eventId: Int = 7,
    val data: WsAuthData
)

@Serializable
private data class WsAuthData(
    val jwt: String
)

@Serializable
private data class WsEventPayload(
    @SerialName("event_id")
    val eventId: Int
)
