package com.cephalon.lucyApp.ws

import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.BalanceData
import com.cephalon.lucyApp.auth.AuthTokenStore
import com.russhwolf.settings.Settings
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cephalon.lucyApp.time.currentTimeMillis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * 余额 WebSocket 管理器
 *
 * WS 事件协议:
 *   发送: Login(7) → SubscribeWallet(1) → Ping(99)
 *   接收: LoginSuccess(22) → ImmediateWallet(15) / ListenWallet(16) /
 *          BalanceAlarm(17) / Pong(100) / InvalidAuthToken(21)
 *
 * 职责:
 * 1. 通过 HTTP 获取初始余额兜底
 * 2. 通过 WS 订阅钱包，实时监听余额变化
 * 3. 心跳保活（每 15 秒 Ping=99，期待 Pong=100）
 * 4. 断线自动重连（指数退避 2s→4s→8s→…→30s）
 * 5. App 前后台切换时断开/重连
 */
class BalanceWsManager(
    private val authRepository: AuthRepository,
    private val wsApi: WsApi,
    private val tokenStore: AuthTokenStore,
    private val settings: Settings,
) {
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        println("$TAG: 协程异常(已捕获): ${throwable.message}")
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ─────────── 事件 ID 常量 ───────────
    private object EventId {
        const val SUBSCRIBE_WALLET = 1
        const val SUBSCRIBE_MESSAGE = 2
        const val SUBSCRIBE_MISSION = 3
        const val UNSUBSCRIBE_WALLET = 4
        const val LOGIN = 7
        const val IMMEDIATE_WALLET = 15
        const val LISTEN_WALLET = 16
        const val BALANCE_ALARM = 17
        const val INVALID_AUTH_TOKEN = 21
        const val LOGIN_SUCCESS = 22
        const val PING = 99
        const val PONG = 100
    }

    /** 余额状态：key="1"充值脑力值, key="4"免费脑力值 */
    private val _balance = MutableStateFlow(BalanceData())
    val balance: StateFlow<BalanceData> = _balance.asStateFlow()

    /** 连接状态 */
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var managerJob: Job? = null
    private var heartbeatJob: Job? = null
    private var session: DefaultClientWebSocketSession? = null
    private var isActive = false
    private var lastPongTime = 0L

    companion object {
        private const val WS_UUID_KEY = "ws.uuid"
        private const val INITIAL_RETRY_DELAY_MS = 2_000L
        private const val MAX_RETRY_DELAY_MS = 30_000L
        private const val HEARTBEAT_INTERVAL_MS = 15_000L
        private const val PONG_TIMEOUT_MS = 30_000L
        private const val TAG = "BalanceWsManager"
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
    }

    /**
     * 启动余额监听（进入充值页面 / 登录后调用）
     */
    fun start() {
        if (isActive) return
        isActive = true
        managerJob = scope.launch {
            fetchBalanceFromHttp()
            connectWithRetry()
        }
    }

    /**
     * 停止余额监听（离开充值页面 / 登出时调用）
     */
    fun stop() {
        isActive = false
        managerJob?.cancel()
        managerJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        scope.launch { closeSession() }
    }

    /**
     * 停止并清除所有内存状态（登出时调用）
     */
    fun stopAndClear() {
        stop()
        _balance.value = BalanceData()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * App 从后台回到前台
     */
    fun onForeground() {
        if (tokenStore.getValidTokenOrNull() == null) return
        start()
    }

    /**
     * App 进入后台
     */
    fun onBackground() {
        stop()
    }

    /**
     * 主动刷新余额（如充值成功后）
     */
    fun refreshBalance() {
        scope.launch { fetchBalanceFromHttp() }
    }

    // ─────────── HTTP 兜底 ───────────

    private suspend fun fetchBalanceFromHttp() {
        try {
            val resp = authRepository.getBalance()
            if (resp.code == 20000 && resp.data != null) {
                _balance.value = resp.data
                println("$TAG: HTTP 余额获取成功 ${resp.data.balances}")
            }
        } catch (e: Exception) {
            println("$TAG: HTTP 余额获取失败 ${e.message}")
        }
    }

    // ─────────── WS 核心流程 ───────────

    private suspend fun connectWithRetry() {
        var retryDelay = INITIAL_RETRY_DELAY_MS

        while (isActive) {
            val token = tokenStore.getValidTokenOrNull()
            if (token == null) {
                println("$TAG: 无有效 token，停止连接")
                _connectionState.value = ConnectionState.DISCONNECTED
                return
            }

            try {
                _connectionState.value = if (retryDelay > INITIAL_RETRY_DELAY_MS) {
                    ConnectionState.RECONNECTING
                } else {
                    ConnectionState.CONNECTING
                }

                // 使用块式 API，Ktor 内部 reader/writer 异常会正确传播到此 try-catch
                val wsUuid = getWsUuid()
                wsApi.withConnection(wsUuid) {
                    session = this
                    println("$TAG: WS 已连接")

                    // 发送 Login (event_id=7)
                    sendLogin(this, token)

                    // 接收消息循环
                    lastPongTime = currentTimeMs()
                    for (frame in incoming) {
                        if (!isActive) break
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                handleMessage(this@withConnection, text)
                            }
                            is Frame.Close -> {
                                println("$TAG: 服务端关闭连接")
                                break
                            }
                            else -> Unit
                        }
                    }
                }
            } catch (e: Throwable) {
                println("$TAG: 连接异常 ${e.message}")
            }

            // 清理当前会话
            heartbeatJob?.cancel()
            heartbeatJob = null
            closeSession()

            if (!isActive) return

            _connectionState.value = ConnectionState.RECONNECTING
            println("$TAG: ${retryDelay}ms 后重连…")
            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY_MS)

            // 重连前 HTTP 兜底
            fetchBalanceFromHttp()
        }
    }

    /**
     * 处理收到的 WS 消息
     *
     * 服务端响应格式:
     *   {"code":0,"message":"success","data":{"event_id":22}}
     *   {"code":0,"message":"success","data":{"event_id":15,"data":{"user_id":"...","wallets":[...]}}}
     *   {"code":0,"message":"成功","data":{"event_id":100,"data":"pong"}}
     */
    private suspend fun handleMessage(session: DefaultClientWebSocketSession, text: String) {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        if (root == null) {
            println("$TAG: 无法解析消息 $text")
            return
        }

        // event_id 在 data 内部: root.data.event_id
        val dataObj = root["data"]?.jsonObject ?: return
        val eventId = dataObj.intValue("event_id")

        when (eventId) {
            EventId.LOGIN_SUCCESS -> {
                // 登录成功 → 订阅钱包、消息、任务
                println("$TAG: 登录成功，订阅钱包/消息/任务")
                _connectionState.value = ConnectionState.CONNECTED
                sendEvent(session, EventId.SUBSCRIBE_WALLET)
                sendEvent(session, EventId.SUBSCRIBE_MESSAGE)
                sendEvent(session, EventId.SUBSCRIBE_MISSION)
                startHeartbeat(session)
            }

            EventId.IMMEDIATE_WALLET -> {
                // 订阅后立即返回当前余额
                println("$TAG: 收到即时余额推送")
                parseAndUpdateBalance(dataObj)
            }

            EventId.LISTEN_WALLET -> {
                // 余额变动推送
                println("$TAG: 收到余额变动推送")
                parseAndUpdateBalance(dataObj)
            }

            EventId.BALANCE_ALARM -> {
                // 余额告警
                println("$TAG: 收到余额告警")
                parseAndUpdateBalance(dataObj)
            }

            EventId.PONG -> {
                // 心跳回复
                lastPongTime = currentTimeMs()
            }

            EventId.INVALID_AUTH_TOKEN -> {
                // Token 失效，需要重新登录
                println("$TAG: Token 失效，断开重连")
                tokenStore.clear()
                closeSession()
            }

            else -> {
                println("$TAG: 收到未处理事件 event_id=$eventId")
            }
        }
    }

    /**
     * 从 WS 消息中解析余额数据并更新 StateFlow
     *
     * 响应中 data 层结构:
     *   {"event_id":15,"data":{"user_id":"...","wallets":[{"symbol_id":1,"amount":20000},...]}}
     *
     * 也兼容 balances map 格式:
     *   {"event_id":15,"data":{"balances":{"1":20000,"4":0}}}
     */
    private fun parseAndUpdateBalance(dataObj: JsonObject) {
        val innerData = dataObj["data"]?.jsonObject ?: return

        val newBalances = mutableMapOf<String, Long>()

        // 格式 1: wallets 数组 [{"symbol_id":1,"amount":20000}, ...]
        val walletsArray = innerData["wallets"]
        if (walletsArray != null) {
            runCatching {
                val wallets = json.decodeFromJsonElement(
                    kotlinx.serialization.builtins.ListSerializer(WalletItem.serializer()),
                    walletsArray
                )
                for (w in wallets) {
                    newBalances[w.symbolId.toString()] = w.amount
                }
            }
        }

        // 格式 2: balances map {"1":20000,"4":0}
        val balancesObj = innerData["balances"]?.jsonObject
        if (balancesObj != null) {
            for ((key, value) in balancesObj) {
                val prim = value as? JsonPrimitive ?: continue
                val longVal = prim.longOrNull ?: prim.doubleOrNull?.toLong()
                if (longVal != null) {
                    newBalances[key] = longVal
                }
            }
        }

        if (newBalances.isNotEmpty()) {
            val current = _balance.value.balances.toMutableMap()
            current.putAll(newBalances)
            _balance.value = BalanceData(balances = current)
            println("$TAG: 余额更新 $current")
        }
    }

    // ─────────── 发送方法 ───────────

    private suspend fun sendLogin(session: DefaultClientWebSocketSession, token: String) {
        try {
            val payload = WsLoginPayload(
                data = WsLoginData(jwt = "Bearer $token")
            )
            val text = json.encodeToString(WsLoginPayload.serializer(), payload)
            session.send(Frame.Text(text))
            println("$TAG: 发送 Login")
        } catch (e: Throwable) {
            println("$TAG: 发送 Login 失败 ${e.message}")
        }
    }

    private suspend fun sendEvent(session: DefaultClientWebSocketSession, eventId: Int) {
        try {
            val payload = WsSimpleEvent(eventId = eventId)
            val text = json.encodeToString(WsSimpleEvent.serializer(), payload)
            session.send(Frame.Text(text))
            println("$TAG: 发送 event_id=$eventId")
        } catch (e: Throwable) {
            println("$TAG: 发送 event_id=$eventId 失败 ${e.message}")
        }
    }

    // ─────────── 心跳 ───────────

    private fun startHeartbeat(session: DefaultClientWebSocketSession) {
        heartbeatJob?.cancel()
        lastPongTime = currentTimeMs()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)

                // 检查 pong 超时（连续 2 个周期没收到 pong 则认为断线）
                if (currentTimeMs() - lastPongTime > PONG_TIMEOUT_MS) {
                    println("$TAG: Pong 超时，断开重连")
                    try { session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Pong timeout")) } catch (_: Throwable) {}
                    break
                }

                try {
                    sendEvent(session, EventId.PING)
                } catch (e: Throwable) {
                    println("$TAG: 心跳发送失败 ${e.message}")
                    break
                }
            }
        }
    }

    // ─────────── 工具方法 ───────────

    private suspend fun closeSession() {
        try {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Closed by client"))
        } catch (_: Throwable) {}
        session = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * 获取或生成持久化 WS UUID（对标 JS 端 getWsUuid）
     */
    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    private fun getWsUuid(): String {
        val existing = settings.getStringOrNull(WS_UUID_KEY)
        if (!existing.isNullOrBlank()) return existing
        val newUuid = kotlin.uuid.Uuid.random().toString()
        settings.putString(WS_UUID_KEY, newUuid)
        return newUuid
    }

    private fun currentTimeMs(): Long = currentTimeMillis()

    private fun JsonObject.intValue(key: String): Int? {
        return (this[key] as? JsonPrimitive)?.intOrNull
    }
}

// ─────────── WS 消息数据类 ───────────

@Serializable
private data class WsLoginPayload(
    @SerialName("event_id")
    val eventId: Int = 7,
    val data: WsLoginData
)

@Serializable
private data class WsLoginData(
    val jwt: String
)

@Serializable
private data class WsSimpleEvent(
    @SerialName("event_id")
    val eventId: Int
)

@Serializable
private data class WalletItem(
    @SerialName("symbol_id")
    val symbolId: Int,
    val amount: Long
)
