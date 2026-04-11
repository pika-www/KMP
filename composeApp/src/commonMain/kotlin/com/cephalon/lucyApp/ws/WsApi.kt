package com.cephalon.lucyApp.ws

import com.cephalon.lucyApp.network.NetworkPaths
import com.cephalon.lucyApp.network.NetworkUrlFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.webSocketSession


private object WsRoutes {
    private const val WS_PREFIX = "${NetworkPaths.API_PREFIX}/ws"

    fun connectionPath(cornId: String): String = "$WS_PREFIX/$cornId"
}

/**
 * WebSocket 接口定义，集中管理 ws 路径。
 */
class WsApi(
    private val client: HttpClient,
    private val urlFactory: NetworkUrlFactory
) {

    suspend fun connect(cornId: String): DefaultClientWebSocketSession {
        return client.webSocketSession(urlString = urlFactory.webSocket(WsRoutes.connectionPath(cornId)))
    }

    /**
     * 块式 WebSocket 连接：内部 reader/writer 协程的异常会正确传播到调用方，
     * 而不会成为未捕获异常导致闪退。
     */
    suspend fun withConnection(
        cornId: String,
        block: suspend DefaultClientWebSocketSession.() -> Unit
    ) {
        client.webSocket(
            urlString = urlFactory.webSocket(WsRoutes.connectionPath(cornId))
        ) {
            block()
        }
    }
}
