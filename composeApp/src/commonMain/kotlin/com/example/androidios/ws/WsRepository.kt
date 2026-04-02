package com.example.androidios.ws

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

    suspend fun connect(cornId: String): DefaultClientWebSocketSession {
        close(silent = true)
        val newSession = wsApi.connect(cornId)
        session = newSession
        appendMessage("WS 已连接: $cornId")
        receiveJob = scope.launch {
            try {
                for (frame in newSession.incoming) {
                    when (frame) {
                        is Frame.Text -> appendMessage("接收: ${frame.readText()}")
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

        session?.close(
            CloseReason(
                CloseReason.Codes.NORMAL,
                "Closed by user"
            )
        )
        session = null

        if (!silent) {
            appendMessage("WS 已关闭")
        }
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
