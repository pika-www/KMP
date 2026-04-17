package com.cephalon.lucyApp.deviceaccess.gatt

import com.cephalon.lucyApp.deviceaccess.BleScanDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
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
 * 6. 写入 wifi_config (7f0c0003) → 等待 5s → 读取 network_status 验证
 * 7. 写入 lucy_pairing_request (7f0c0006) → 等待 3s → 读取 pairing_info 获取 OTP
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
     * 协议步骤 6：写入 wifi_config → 等待 5s → 读取 network_status 验证。
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
            router.writeWifiConfig(ssid = normalizedSsid, password = password, hidden = hidden).getOrThrow()

            _state.value = _state.value.copy(stage = ProvisionFlowStage.VerifyingNetwork)
            println("[BrainBox] wifi_config: 等待 ${GattRouter.WIFI_CONFIG_WAIT_MS}ms 后读取 network_status...")
            kotlinx.coroutines.delay(GattRouter.WIFI_CONFIG_WAIT_MS)

            val status = router.readNetworkStatus().getOrThrow()
            println("[BrainBox] network_status after wifi_config: state=${status.state}, ssid=${status.ssid}, ip=${status.ip}, error=${status.error}")
            _state.value = _state.value.copy(networkStatus = status)

            if (!status.isConnected) {
                throw IllegalStateException(status.error.ifBlank { "设备联网失败（state=${status.state}）" })
            }

            _state.value = _state.value.copy(stage = ProvisionFlowStage.Completed)
            status
        }.onFailure { error ->
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Failed,
                selectedDevice = targetDevice,
                errorMessage = error.message ?: "Wi‑Fi 配置失败",
            )
        }
    }

    /**
     * 协议步骤 7：写入 lucy_pairing_request → 等待 3s → 读取 pairing_info 获取 OTP。
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
        disconnectWatcherJob?.cancel()
        disconnectWatcherJob = null
        reconnectAttemptCount = 0
        connectionManager.disconnect()
        _state.value = ProvisionFlowState(stage = ProvisionFlowStage.Idle)
    }

    fun dispose() {
        watcherScope.cancel()
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
     * 2. 等 2s 让设备 BLE 栈恢复
     * 3. 重新扫描同一 device id（最多 15s）
     * 4. 重新 connect + discoverCharacteristics
     * 5. 重读 pairing_info。任一步失败 → 退避 3s 后从步骤 2 重试。
     * 6. 成功后重新挂载断开监听，发出 reconnectedEvents 让 UI 重新写入 request_id 并读 OTP。
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
        // 单轮重连失败后的退避时间，避免紧密循环。
        private const val RECONNECT_RETRY_BACKOFF_MS = 3_000L
        // 服务端绑定成功后，验证 pairing_info 的最大尝试次数（设备需要短暂时间把状态刷新为 bound）。
        private const val BIND_VERIFY_MAX_ATTEMPTS = 3
        private const val BIND_VERIFY_RETRY_DELAY_MS = 2_000L
    }
}
