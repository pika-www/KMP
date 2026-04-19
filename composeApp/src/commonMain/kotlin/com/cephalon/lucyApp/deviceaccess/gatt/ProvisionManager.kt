package com.cephalon.lucyApp.deviceaccess.gatt

import com.cephalon.lucyApp.deviceaccess.BleScanDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

enum class ProvisionFlowStage {
    Idle,
    Scanning,
    Connecting,
    Discovering,
    ReadingDeviceInfo,
    ReadingNetworkStatus,
    ReadingPairingInfo,
    ScanningWifi,
    ConfiguringWifi,
    VerifyingNetwork,
    RequestingOtp,
    Reconnecting,
    Completed,
    Failed,
}

data class ProvisionFlowState(
    val stage: ProvisionFlowStage = ProvisionFlowStage.Idle,
    val selectedDevice: BleScanDevice? = null,
    val deviceInfo: DeviceInfoPayload? = null,
    val pairingInfo: LucyPairingInfoPayload? = null,
    val channelDeviceId: String? = null,
    val otp: String? = null,
    val wifiNetworks: List<GattWifiNetwork> = emptyList(),
    val networkStatus: NetworkStatusPayload? = null,
    val errorMessage: String? = null,
)

/**
 * Brain Box 配网流程管理器（严格按照协议文档 7 步实现）。
 *
 * 1. BLE 扫描 Service UUID = 7f0c0000-...
 * 2. 连接选中的设备，读取 device_info (7f0c0001)
 * 3. 读取 network_status (7f0c0002)
 * 4. 读取 lucy_pairing_info (7f0c0005)，要求 state=ready 且 binding_status ∈ {pending, bound}
 * 5. 写入 wifi_scan (7f0c0004) → 等待 5s → 读取一次
 * 6. 预读 network_status；未连到目标 ssid 才写 wifi_config (7f0c0003)，随后订阅 network_status 通知等 connected（超时兜底再 read）
 * 7. 写入 lucy_pairing_request (7f0c0006, WRITE_WITH_RESPONSE) → write ACK 返回（OTP 已注入）→ 读取 pairing_info 获取 OTP
 *
 * 流程中的任何 BLE 断开都会触发自动重连：重新扫描该设备 → 连接 → 发现服务 → 重读
 * pairing_info。重连失败会按固定退避时间循环重试，直到成功或 [cancel] 被调用。
 * 每次重连成功后通过 [reconnectedEvents] 通知 UI 重新写入 lucy_pairing_request
 * （新的 request_id）并重新读取 OTP。
 */
class ProvisionManager(
    private val connectionManager: GattConnectionManager,
    private val router: GattRouter,
) {
    private val _state = MutableStateFlow(ProvisionFlowState())
    val state: StateFlow<ProvisionFlowState> = _state.asStateFlow()
    val scanState: StateFlow<BleScanSnapshot> = connectionManager.scanState

    /**
     * 配网流程所属会话结束时发出（例如会话中没有选中设备，或自动重连无法继续），
     * UI 侧收到后应回到扫描步骤。正常的中途断开不会发出此事件：会被 attemptReconnect 无限循环重试消化。
     */
    private val _disconnectEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val disconnectEvents: SharedFlow<String> = _disconnectEvents.asSharedFlow()

    /** 自动重连成功后发出，UI 侧收到后应重新执行步骤 7（请求 OTP）并继续绑定。 */
    private val _reconnectedEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val reconnectedEvents: SharedFlow<Unit> = _reconnectedEvents.asSharedFlow()

    // 串行化 BLE GATT 入口，防止 UI 并发触发 connect/refreshWifi/configureWifi/requestOtp。
    private val gattMutex = Mutex()

    private val watcherScope = CoroutineScope(Dispatchers.Default)
    private var disconnectWatcherJob: Job? = null

    // BLE 空闲期的 keep-alive 协程：connectDevice / attemptReconnect 成功后启动，
    // 在 stage=Completed 的"等用户输密码 / 等下一步"这段空闲里周期性读一次 network_status，
    // 让盒子固件的"等待 wifi_config idle 超时"计时器不断被刷新，规避 BLE 被主动掐断。
    private var keepAliveJob: Job? = null

    // 当前 connectDevice 会话中已经进行的自动重连次数（仅用于日志）。不做上限限制。
    private var reconnectAttemptCount = 0

    /** 进入 Brain Box 登录流程前调用，开始 BLE 扫描。 */
    suspend fun startScan(): Result<Unit> {
        return runCatching {
            connectionManager.startScan(BrainBoxGattProtocol.SERVICE_UUID)
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Scanning,
                errorMessage = null,
            )
        }
    }

    fun stopScan() {
        connectionManager.stopScan()
    }

    suspend fun awaitScannedDevice(
        timeoutMillis: Long,
        matcher: (BleScanDevice) -> Boolean,
    ): Result<BleScanDevice> {
        val found = withTimeoutOrNull(timeoutMillis) {
            connectionManager.scanState
                .map { it.devices.firstOrNull(matcher) }
                .filter { it != null }
                .first()
        }
        return if (found != null) {
            Result.success(found)
        } else {
            Result.failure(IllegalStateException("在 ${timeoutMillis}ms 内未扫描到目标设备"))
        }
    }

    /**
     * 扫描步骤里的一次性 GATT 探测：connect → discoverServices → read pairing_info → disconnect。
     *
     * 用途：在用户点"连接"前判断这台盒子的 binding_status，是未绑定 / 当前账号自己绑的 / 被他人占用。
     * 不修改主流程 [_state]，也不启动 [startDisconnectWatcher]，避免把"正常断开"误报成流程异常。
     *
     * 与 [connectDevice]/[refreshWifiNetworks] 等共用 [gattMutex]，天然串行；调用方可 cancel
     * 外层协程来中止探测（[withLock] 会在 finally 里释放 mutex）。
     */
    suspend fun probeDevice(device: BleScanDevice): Result<LucyPairingInfoPayload> = gattMutex.withLock {
        runCatching {
            println("[BrainBox] probe: connect ${device.name} (${device.id})")
            connectionManager.connect(device).getOrThrow()
            try {
                router.discoverCharacteristics().getOrThrow()
                val info = router.readLucyPairingInfo().getOrThrow()
                println("[BrainBox] probe ok: ${device.id} state=${info.state}, binding=${info.bindingStatus}, cdi=${info.channelDeviceId}")
                info
            } finally {
                runCatching { connectionManager.disconnect() }
            }
        }.onFailure { error ->
            println("[BrainBox] probe failed for ${device.id}: ${error.message}")
        }
    }

    /**
     * 协议步骤 2–4：连接设备，依次读取 device_info / network_status / pairing_info。
     *
     * 强校验：pairing_info.state 必须是 "ready"，binding_status 必须是 "pending" 或 "bound"。
     * 任何一步失败都会抛出并回到 Failed 状态；UI 应回到步骤 1 重新扫描。
     */
    suspend fun connectDevice(device: BleScanDevice): Result<LucyPairingInfoPayload> = gattMutex.withLock {
        // 新的连接会话：重置自动重连计数。
        reconnectAttemptCount = 0
        runCatching {
            // 1) GATT 连接 + 发现服务
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Connecting,
                selectedDevice = device,
                errorMessage = null,
            )
            connectionManager.connect(device).getOrThrow()

            _state.value = _state.value.copy(stage = ProvisionFlowStage.Discovering)
            router.discoverCharacteristics().getOrThrow()
            router.logCharacteristicProperties()

            // 启动断开监听：一旦 GATT 变 Disconnected/Error，立即向 UI 发事件并标记 Failed。
            startDisconnectWatcher()

            // 2) 读 device_info
            _state.value = _state.value.copy(stage = ProvisionFlowStage.ReadingDeviceInfo)
            val deviceInfo = router.readDeviceInfo().getOrThrow()
            println("[BrainBox] device_info: hostname=${deviceInfo.hostname}, ip=${deviceInfo.ip}, ssid=${deviceInfo.ssid}, state=${deviceInfo.state}")
            _state.value = _state.value.copy(deviceInfo = deviceInfo)

            // 3) 读 network_status
            _state.value = _state.value.copy(stage = ProvisionFlowStage.ReadingNetworkStatus)
            val networkStatus = router.readNetworkStatus().getOrThrow()
            println("[BrainBox] network_status: state=${networkStatus.state}, ssid=${networkStatus.ssid}, ip=${networkStatus.ip}")
            _state.value = _state.value.copy(networkStatus = networkStatus)

            // 4) 读 pairing_info 并严格校验 ready + pending/bound
            _state.value = _state.value.copy(stage = ProvisionFlowStage.ReadingPairingInfo)
            val pairingInfo = router.readLucyPairingInfo().getOrThrow()
            println("[BrainBox] pairing_info: state=${pairingInfo.state}, bindingStatus=${pairingInfo.bindingStatus}, cdi=${pairingInfo.channelDeviceId}")

            if (!pairingInfo.state.equals("ready", ignoreCase = true)) {
                throw IllegalStateException("设备未就绪（state=${pairingInfo.state}）")
            }
            val isPendingOrBound = pairingInfo.bindingStatus.equals("pending", ignoreCase = true) ||
                pairingInfo.bindingStatus.equals("bound", ignoreCase = true)
            if (!isPendingOrBound) {
                throw IllegalStateException("设备绑定状态异常（binding_status=${pairingInfo.bindingStatus}）")
            }

            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Completed,
                pairingInfo = pairingInfo,
                channelDeviceId = pairingInfo.channelDeviceId.takeIf { it.isNotBlank() },
                otp = pairingInfo.otp.takeIf { it.isNotBlank() },
                errorMessage = null,
            )
            // connectDevice 成功 → 进入"等用户配网 / 等下一步"的空闲期，挂上 keep-alive
            // 周期性读 network_status，避免盒子固件因 idle 超时把 BLE 掐掉。
            startIdleKeepAlive()
            pairingInfo
        }.onFailure { error ->
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Failed,
                selectedDevice = device,
                errorMessage = error.message ?: "GATT 连接或读取失败",
            )
        }
    }

    /**
     * 协议步骤 5：写入 wifi_scan → 等待 5s → 读取一次。
     */
    suspend fun refreshWifiNetworks(
        device: BleScanDevice? = _state.value.selectedDevice,
    ): Result<List<GattWifiNetwork>> = gattMutex.withLock {
        val targetDevice = device
            ?: return@withLock Result.failure(IllegalStateException("请先选择蓝牙设备"))
        runCatching {
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.ScanningWifi,
                selectedDevice = targetDevice,
                errorMessage = null,
            )
            val networks = router.scanWifi().getOrThrow()
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Completed,
                wifiNetworks = networks,
            )
            networks
        }.onFailure { error ->
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Failed,
                selectedDevice = targetDevice,
                errorMessage = error.message ?: "Wi‑Fi 扫描失败",
            )
        }
    }

    /**
     * 协议步骤 6：下发 Wi-Fi 配置 → 等设备联网 → 进入 Bind。
     *
     * 两项优化（替代老的 "write → 固定等 5s → 一次 read" 的逻辑）：
     *
     * 1. **快通道（先读再写）**：进入本步骤先读一次 `network_status`。如果设备
     *    已经连在用户刚选中的同一个 SSID 上，说明固件在之前某次下发中已经成功联网，
     *    直接返回，跳过本次 `wifi_config` 的 write，避免不必要的切网-重启。
     *
     * 2. **订阅通知 + 兜底读**：如果必须 write，就先 enable `network_status` 的 CCCD
     *    通知，并用 `CoroutineStart.UNDISPATCHED` 在同一线程抢占式起一个 observer
     *    （保证 observer 的 SharedFlow 订阅在 write 触发的通知到达之前就位），
     *    然后 write `wifi_config`，等第一个 `isConnected=true` 的事件：
     *    - 收到通知 → 立即完成，UI 上层随即进入 `Bind` 步；
     *    - [WIFI_CONNECT_NOTIFY_TIMEOUT_MS] 内没收到 → 兜底再做一次 read，
     *      防止固件不主动 push 的情况卡死；
     *    - 任何路径结束都 disable 通知 CCCD，避免后续 pairing 步骤被 network_status
     *      的残留通知打扰。
     */
    suspend fun configureWifi(
        ssid: String,
        password: String,
        hidden: Boolean = false,
    ): Result<NetworkStatusPayload> = gattMutex.withLock {
        val normalizedSsid = ssid.trim()
        if (normalizedSsid.isBlank()) {
            return@withLock Result.failure(IllegalArgumentException("Wi‑Fi 名称不能为空"))
        }
        val targetDevice = _state.value.selectedDevice
            ?: return@withLock Result.failure(IllegalStateException("请先选择蓝牙设备"))

        runCatching {
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.ConfiguringWifi,
                errorMessage = null,
            )

            // 1) 快通道：设备已经连接到目标 Wi-Fi → 直接跳过 write。
            val fastPath = runCatching { router.readNetworkStatus().getOrThrow() }
                .onFailure {
                    println("[BrainBox] wifi_config 前预读 network_status 失败: ${it.message}，继续走 write 分支")
                }
                .getOrNull()
            if (fastPath != null) {
                println(
                    "[BrainBox] wifi_config 前预读 network_status: " +
                        "state=${fastPath.state}, ssid=${fastPath.ssid}, ip=${fastPath.ip}",
                )
                if (fastPath.isConnected && fastPath.ssid.equals(normalizedSsid, ignoreCase = true)) {
                    println(
                        "[BrainBox] 设备已连接到目标 Wi-Fi (ssid=${fastPath.ssid})，" +
                            "跳过 wifi_config 下发，直接进入 Bind",
                    )
                    _state.value = _state.value.copy(
                        networkStatus = fastPath,
                        stage = ProvisionFlowStage.Completed,
                    )
                    return@runCatching fastPath
                }
            }

            // 2) 正常分支：enable notify → 订阅 → write → 等通知/超时兜底。
            val enableNotifyOk = router.enableNetworkStatusNotify()
                .onFailure { println("[BrainBox] enableNetworkStatusNotify 失败: ${it.message}，只能靠超时兜底 read") }
                .isSuccess

            val finalStatus = try {
                coroutineScope {
                    // UNDISPATCHED：observer 在本线程同步进入 collect，确保 write 之前
                    // SharedFlow 的订阅已经就位，不会漏掉任何紧接着 write 发出的通知。
                    val observer = async(start = CoroutineStart.UNDISPATCHED) {
                        router.observeNetworkStatus()
                            .filter { it.isConnected }
                            .first()
                    }
                    try {
                        router.writeWifiConfig(
                            ssid = normalizedSsid,
                            password = password,
                            hidden = hidden,
                        ).getOrThrow()
                        _state.value = _state.value.copy(stage = ProvisionFlowStage.VerifyingNetwork)
                        println(
                            "[BrainBox] wifi_config 已下发，等待 network_status connected 通知 " +
                                "(最长 ${WIFI_CONNECT_NOTIFY_TIMEOUT_MS}ms)...",
                        )

                        val viaNotify = withTimeoutOrNull(WIFI_CONNECT_NOTIFY_TIMEOUT_MS) { observer.await() }
                        viaNotify ?: run {
                            println(
                                "[BrainBox] ${WIFI_CONNECT_NOTIFY_TIMEOUT_MS}ms 内未收到 connected 通知，" +
                                    "兜底 readNetworkStatus",
                            )
                            router.readNetworkStatus().getOrThrow()
                        }
                    } finally {
                        observer.cancel()
                    }
                }
            } finally {
                if (enableNotifyOk) {
                    runCatching { router.disableNetworkStatusNotify() }
                }
            }

            println(
                "[BrainBox] network_status final: state=${finalStatus.state}, " +
                    "ssid=${finalStatus.ssid}, ip=${finalStatus.ip}, error=${finalStatus.error}",
            )
            _state.value = _state.value.copy(networkStatus = finalStatus)

            if (!finalStatus.isConnected) {
                throw IllegalStateException(finalStatus.error.ifBlank { "设备联网失败（state=${finalStatus.state}）" })
            }

            _state.value = _state.value.copy(stage = ProvisionFlowStage.Completed)
            finalStatus
        }.onFailure { error ->
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Failed,
                selectedDevice = targetDevice,
                errorMessage = error.message ?: "Wi‑Fi 配置失败",
            )
        }
    }

    /**
     * 协议步骤 7：写入 lucy_pairing_request（WRITE_WITH_RESPONSE）→ 固定等 5s 后
     * 读取 pairing_info → 按 `otp` 字段判定成功/失败。
     *
     * 不再信任 write ACK（实测固件会回 ATT code=128/14，但 Lucy IPC 仍可能异步
     * 完成注入 OTP），详见 [GattRouter.requestOtpAndReadPairingInfo] 的 KDoc。
     *
     * 如果 pairing_info.bindingStatus 已经是 "bound"，跳过写入直接返回当前信息（设备已绑定）。
     */
    suspend fun requestOtpAfterWifi(): Result<LucyPairingInfoPayload> = gattMutex.withLock {
        val targetDevice = _state.value.selectedDevice
            ?: return@withLock Result.failure(IllegalStateException("请先选择蓝牙设备"))
        runCatching {
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.RequestingOtp,
                errorMessage = null,
            )

            // 设备已绑定 → 固件不允许再次请求 OTP，直接返回现有 pairing_info。
            val current = _state.value.pairingInfo
            if (current != null && current.isBound && current.channelDeviceId.isNotBlank()) {
                println("[BrainBox] 设备已绑定 (bindingStatus=${current.bindingStatus}, cdi=${current.channelDeviceId})，跳过 OTP 请求")
                _state.value = _state.value.copy(
                    stage = ProvisionFlowStage.Completed,
                    channelDeviceId = current.channelDeviceId,
                )
                return@runCatching current
            }

            val pairingInfo = router.requestOtpAndReadPairingInfo().getOrThrow()
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Completed,
                pairingInfo = pairingInfo,
                channelDeviceId = pairingInfo.channelDeviceId.takeIf { it.isNotBlank() }
                    ?: _state.value.channelDeviceId,
                otp = pairingInfo.otp.takeIf { it.isNotBlank() },
            )
            pairingInfo
        }.onFailure { error ->
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Failed,
                selectedDevice = targetDevice,
                errorMessage = error.message ?: "请求 OTP 失败",
            )
        }
    }

    /**
     * 服务端 bindDeviceWithOtp 成功后调用：重新读取 lucy_pairing_info，校验 binding_status == "bound"
     * 且 channel_device_id 非空，才算真正完成绑定。
     *
     * 设备在服务端 bind 成功后需要短暂时间才把 pairing_info 刷成 bound，所以这里最多 [BIND_VERIFY_MAX_ATTEMPTS]
     * 次读取，每次失败后延迟 [BIND_VERIFY_RETRY_DELAY_MS]。如果 BLE 当前已断开，读取会抛出异常；
     * 自动重连链路会在后台重连后再触发 reconnectedEvents，UI 到时会再次进入绑定流程（requestOtpAndBind
     * 会命中 isBound 短路直接跳转）。
     */
    suspend fun verifyBindingStatus(): Result<LucyPairingInfoPayload> = gattMutex.withLock {
        runCatching {
            var lastInfo: LucyPairingInfoPayload? = null
            repeat(BIND_VERIFY_MAX_ATTEMPTS) { attempt ->
                val info = router.readLucyPairingInfo().getOrThrow()
                println("[BrainBox] verifyBindingStatus 第 ${attempt + 1}/$BIND_VERIFY_MAX_ATTEMPTS 次: state=${info.state}, bindingStatus=${info.bindingStatus}, cdi=${info.channelDeviceId}")
                _state.value = _state.value.copy(
                    pairingInfo = info,
                    channelDeviceId = info.channelDeviceId.takeIf { it.isNotBlank() }
                        ?: _state.value.channelDeviceId,
                )
                lastInfo = info
                if (info.isBound && info.channelDeviceId.isNotBlank()) {
                    return@runCatching info
                }
                if (attempt < BIND_VERIFY_MAX_ATTEMPTS - 1) {
                    println("[BrainBox] pairing_info 尚未 bound，${BIND_VERIFY_RETRY_DELAY_MS}ms 后重试")
                    kotlinx.coroutines.delay(BIND_VERIFY_RETRY_DELAY_MS)
                }
            }
            val snapshot = lastInfo
            when {
                snapshot == null -> throw IllegalStateException("读取 pairing_info 失败")
                !snapshot.isBound -> throw IllegalStateException("设备未完成绑定（binding_status=${snapshot.bindingStatus}）")
                snapshot.channelDeviceId.isBlank() -> throw IllegalStateException("设备未返回有效 channel_device_id")
                else -> snapshot
            }
        }
    }

    /** 关闭 GATT 连接，清空流程状态。 */
    suspend fun cancel() {
        // 先停 keep-alive，避免它在主动 disconnect 的过程中还在抢 mutex 做 read。
        stopIdleKeepAlive()
        disconnectWatcherJob?.cancel()
        disconnectWatcherJob = null
        reconnectAttemptCount = 0
        connectionManager.disconnect()
        _state.value = ProvisionFlowState(stage = ProvisionFlowStage.Idle)
    }

    /**
     * UI 主动触发一次完整重连：断开当前 BLE → 已挂起的 [startDisconnectWatcher] 立即触发 →
     * [attemptReconnect] 接管（无上限重试）→ 成功后发出 [reconnectedEvents]。
     *
     * 用于 UI 在 ATT/GATT 层错误时（典型：lucy_pairing_request 写入后固件 BLE 栈复位，写返回
     * "Unknown ATT error"）放弃 in-place 重试，直接走完整重连流程。重连成功后 UI 会自动再
     * 触发一次业务调用（如重新写 request_id 拿 OTP）。
     *
     * 不持有 [gattMutex]，避免和正在执行的 attemptReconnect 互锁；disconnect 本身幂等。
     */
    suspend fun forceReconnect() {
        println("[BrainBox] forceReconnect: 主动断开 BLE，让 disconnectWatcher 接管自动重连")
        runCatching { connectionManager.disconnect() }
    }

    fun dispose() {
        stopIdleKeepAlive()
        watcherScope.cancel()
    }

    /**
     * 启动 BLE idle keep-alive：每 [IDLE_KEEP_ALIVE_INTERVAL_MS] 毫秒在 gattMutex 保护下做一次
     * `readNetworkStatus`。目的是在 stage=Completed 的"等用户手输 SSID/密码"期间让盒子固件
     * 感知 BLE 还活着，抵消它的"等配网 idle 超时（典型 15~30s）"。
     *
     * 非侵入：
     *   - 只在 stage == Completed 时执行实际 read，其它阶段（Reconnecting / ConfiguringWifi /
     *     RequestingOtp 等）说明有 in-flight 的主流程操作，keep-alive 让路不插队。
     *   - 读到的 network_status 会回写 [_state.networkStatus]，等于顺便给 UI 推一个最新快照。
     *   - 任何读失败都只打日志不退出循环：很可能只是瞬时 ATT 错误，下轮 delay 后再试；
     *     真的 BLE 断了 [startDisconnectWatcher] 会来调 [stopIdleKeepAlive]。
     */
    private fun startIdleKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = watcherScope.launch {
            while (true) {
                kotlinx.coroutines.delay(IDLE_KEEP_ALIVE_INTERVAL_MS)
                // 进 lock 之前先快判，避免无意义的排队等锁。
                if (_state.value.stage != ProvisionFlowStage.Completed) continue
                val outcome: Result<Unit> = gattMutex.withLock {
                    // 进了 lock 再复查：等待期间别的 public API 可能刚接手流程。
                    if (_state.value.stage != ProvisionFlowStage.Completed) {
                        Result.success(Unit)
                    } else {
                        runCatching {
                            val ns = router.readNetworkStatus().getOrThrow()
                            _state.value = _state.value.copy(networkStatus = ns)
                        }
                    }
                }
                outcome.onFailure { error ->
                    println(
                        "[BrainBox] idle keep-alive read network_status 失败: ${error.message}" +
                            "（不致命，等 disconnectWatcher 接管）",
                    )
                }
            }
        }
    }

    private fun stopIdleKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }

    private fun startDisconnectWatcher() {
        disconnectWatcherJob?.cancel()
        val connection = connectionManager.currentConnection() ?: return
        disconnectWatcherJob = watcherScope.launch {
            // 等待连接进入断开/错误状态的第一个事件后退出。
            connection.state.first { s ->
                s == BleGattConnectionState.Disconnected || s == BleGattConnectionState.Error
            }
            val stage = _state.value.stage
            if (stage == ProvisionFlowStage.Idle || stage == ProvisionFlowStage.Scanning) {
                // 处于空闲或扫描阶段，断开与流程无关。
                disconnectWatcherJob = null
                return@launch
            }
            // 一旦检测到断开就先停 keep-alive，防止它继续在死连接上尝试 read，
            // 也让 attemptReconnect 里的 gattMutex.withLock 不用和 keep-alive 抢。
            // 重连成功会在分支末尾重新 startIdleKeepAlive。
            stopIdleKeepAlive()
            reconnectAttemptCount++
            println("[BrainBox] 检测到 BLE 断开 (stage=$stage)，开始第 $reconnectAttemptCount 次自动重连（无上限，失败会循环重试）")
            // 注意：不在这里清空 disconnectWatcherJob，保留对当前协程的引用，
            // 以便 cancel() 可以 cancel 正在运行的 attemptReconnect 循环。
            attemptReconnect()
        }
    }

    /**
     * BLE 断开时的自动重连流程，内部循环重试直到成功或 cancel() 被调用：
     * 1. 设置 stage = Reconnecting
     * 2. disconnect()：挂起到平台回调（Android STATE_DISCONNECTED / iOS didDisconnectPeripheral）
     *    真正确认掉线，相当于 "waitUntilDisconnected"。
     * 3. 等 2s（RECONNECT_RECOVERY_DELAY_MS）让设备 BLE 栈复位并重新广播。
     * 4. 重新扫描同一 device id（最多 15s）。
     * 5. connect + discoverCharacteristics + logCharacteristicProperties。
     * 6. enableLucyPairingInfoNotify()：写 CCCD 提前订阅；**失败仅告警不中断**，避免设备
     *    未声明 NOTIFY 时无限 backoff。后续 per-call 会在 requestOtpAndReadPairingInfo 里
     *    再尝试一次并在需要时走 read fallback。
     * 7. 若 enable 成功则等 300ms（POST_NOTIFY_ENABLE_DELAY_MS）让设备完成订阅状态内部设置；
     *    失败路径直接跳过 settle 进入下一步。
     * 8. 重读 pairing_info。任一步失败 → 退避 3s 后从步骤 2 重试。
     * 9. 成功后重新挂载断开监听，发出 reconnectedEvents 让 UI 写入 request_id 并读 OTP。
     *    （requestOtpAndReadPairingInfo 内仍会 per-call 重新 enable notify，CCCD 幂等，无副作用。）
     *
     * 注意：本函数运行在 watcherScope 的协程中，[cancel] 会 cancel() 该协程，从而终止循环。
     */
    private suspend fun attemptReconnect() {
        val selectedDevice = _state.value.selectedDevice
        if (selectedDevice == null) {
            _disconnectEvents.tryEmit("蓝牙连接已断开（未记录目标设备）")
            return
        }
        var retry = 0
        while (true) {
            retry++
            val outcome = gattMutex.withLock {
                runCatching {
                    _state.value = _state.value.copy(
                        stage = ProvisionFlowStage.Reconnecting,
                        errorMessage = null,
                    )
                    println("[BrainBox] 自动重连第 $retry 轮: ${selectedDevice.name} (${selectedDevice.id})")

                    // 确保旧连接状态清理干净，让设备 BLE 栈有时间恢复。
                    connectionManager.disconnect()
                    kotlinx.coroutines.delay(RECONNECT_RECOVERY_DELAY_MS)

                    // 重新扫描，找到同一 id 的设备（设备在切换 Wi‑Fi 后会重新广播）。
                    connectionManager.startScan(BrainBoxGattProtocol.SERVICE_UUID)
                    val rescanned = try {
                        awaitScannedDevice(RECONNECT_SCAN_TIMEOUT_MS) { candidate ->
                            candidate.id.equals(selectedDevice.id, ignoreCase = true)
                        }.getOrNull()
                    } finally {
                        connectionManager.stopScan()
                    }
                    val target = rescanned ?: selectedDevice // 兼容：未重新扫到时仍尝试直连
                    println("[BrainBox] 第 $retry 轮重新扫描: rescanned=${rescanned != null}, 尝试连接 ${target.name}")

                    connectionManager.connect(target).getOrThrow()
                    router.discoverCharacteristics().getOrThrow()
                    router.logCharacteristicProperties()

                    // 主动写 CCCD 启用 lucy_pairing_info 的 notify：目的是在 read pairing_info
                    // 之前把设备端订阅状态先建好。注意 **不 getOrThrow**：若设备根本没声明
                    // NOTIFY，或 CCCD 写偶发失败，我们不希望本轮重连无限 backoff——per-call 的
                    // requestOtpAndReadPairingInfo 已经有 "enable 失败 → 走 read fallback" 的
                    // 优雅降级，这里保持同样容忍度，只告警并继续。
                    val notifyEnableResult = router.enableLucyPairingInfoNotify()
                    if (notifyEnableResult.isSuccess) {
                        println("[BrainBox] 第 $retry 轮重连后 enableNotify 成功，settle ${POST_NOTIFY_ENABLE_DELAY_MS}ms 再读 pairing_info")
                        kotlinx.coroutines.delay(POST_NOTIFY_ENABLE_DELAY_MS)
                    } else {
                        println("[BrainBox] WARN 第 $retry 轮重连 enableNotify 失败: ${notifyEnableResult.exceptionOrNull()?.message}，跳过 settle，直接 read pairing_info（后续 per-call 会回退到 read 模式）")
                    }

                    // 重读 pairing_info，让 UI 拿到最新状态。
                    val pairingInfo = router.readLucyPairingInfo().getOrThrow()
                    println("[BrainBox] 第 $retry 轮重连后 pairing_info: state=${pairingInfo.state}, bindingStatus=${pairingInfo.bindingStatus}, cdi=${pairingInfo.channelDeviceId}")
                    _state.value = _state.value.copy(
                        stage = ProvisionFlowStage.Completed,
                        selectedDevice = target,
                        pairingInfo = pairingInfo,
                        channelDeviceId = pairingInfo.channelDeviceId.takeIf { it.isNotBlank() }
                            ?: _state.value.channelDeviceId,
                        errorMessage = null,
                    )
                }
            }
            if (outcome.isSuccess) {
                // 挂载新的断开监听到新连接，并通知 UI 重新请求 OTP（新的 request_id）。
                startDisconnectWatcher()
                // 重新启动 idle keep-alive，继续抵消固件的 idle 超时。
                startIdleKeepAlive()
                println("[BrainBox] 第 $retry 轮自动重连成功，发出 reconnectedEvents")
                _reconnectedEvents.tryEmit(Unit)
                return
            }
            // 失败 → 退避后重试。不发出 disconnectEvents，避免 UI 回到扫描步骤。
            connectionManager.stopScan()
            val error = outcome.exceptionOrNull()
            val message = error?.message ?: "蓝牙重连失败"
            println("[BrainBox] 第 $retry 轮自动重连失败: $message，${RECONNECT_RETRY_BACKOFF_MS}ms 后继续重试")
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Reconnecting,
                errorMessage = "蓝牙重连中…（第 $retry 轮失败: $message）",
            )
            kotlinx.coroutines.delay(RECONNECT_RETRY_BACKOFF_MS)
        }
    }

    companion object {
        private const val RECONNECT_RECOVERY_DELAY_MS = 2_000L
        private const val RECONNECT_SCAN_TIMEOUT_MS = 15_000L
        // 写入 wifi_config 之后等 network_status 通知达到 connected 的最长时间。
        // 选 15s：覆盖常见的 DHCP/Auth/关联握手耗时；真快路径（几百 ms~2s 内收到通知）会立即早退，
        // 所以这个超时只是兜底给"固件不主动 push"场景触发一次 fallback read。
        // 旧的固定等 5s 再 read 的逻辑对慢路由（尤其 5GHz DFS 信道）会假阴性。
        private const val WIFI_CONNECT_NOTIFY_TIMEOUT_MS = 15_000L
        // 单轮重连失败后的退避时间，避免紧密循环。
        private const val RECONNECT_RETRY_BACKOFF_MS = 3_000L
        // 重连后写 CCCD 成功与 read pairing_info 之间的短小 settle：给设备内部时间
        // 完成 "订阅状态上构" 的后续步骤。经验值 300ms。
        private const val POST_NOTIFY_ENABLE_DELAY_MS = 300L
        // 服务端绑定成功后，验证 pairing_info 的最大尝试次数（设备需要短暂时间把状态刷新为 bound）。
        private const val BIND_VERIFY_MAX_ATTEMPTS = 3
        private const val BIND_VERIFY_RETRY_DELAY_MS = 2_000L
        // BLE idle keep-alive 的轮询间隔。选 8s：显著短于盒子固件常见的 15~30s idle 超时，
        // 同时足够稀疏不挤占 GATT 通道（用户手点"连接 Wi‑Fi"时 configureWifi 几乎总能立刻拿到 mutex）。
        private const val IDLE_KEEP_ALIVE_INTERVAL_MS = 8_000L
    }
}
