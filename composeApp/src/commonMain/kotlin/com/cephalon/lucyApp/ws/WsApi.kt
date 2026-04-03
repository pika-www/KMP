package com.cephalon.lucyApp.ws

import com.cephalon.lucyApp.network.NetworkPaths
import com.cephalon.lucyApp.network.NetworkUrlFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
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
}
