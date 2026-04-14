package com.cephalon.lucyApp.deviceaccess.gatt

import com.cephalon.lucyApp.deviceaccess.BleScanDevice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

enum class ProvisionFlowStage {
    Idle,
    Scanning,
    Connecting,
    Discovering,
    ReadingPairingInfo,
    RequestingOtp,
    ScanningWifi,
    ConfiguringWifi,
    WaitingForNetwork,
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

data class ProvisionResult(
    val device: BleScanDevice,
    val deviceInfo: DeviceInfoPayload,
    val pairingInfo: LucyPairingInfoPayload,
    val wifiNetworks: List<GattWifiNetwork>,
    val finalNetworkStatus: NetworkStatusPayload,
)

class ProvisionManager(
    private val connectionManager: GattConnectionManager,
    private val router: GattRouter,
) {
    private val _state = MutableStateFlow(ProvisionFlowState())
    val state: StateFlow<ProvisionFlowState> = _state.asStateFlow()
    val scanState: StateFlow<BleScanSnapshot> = connectionManager.scanState

    fun openBluetoothSettings() {
        connectionManager.openBluetoothSettings()
    }

    fun stopScan() {
        connectionManager.stopScan()
    }

    suspend fun startScan(): Result<Unit> {
        _state.value = ProvisionFlowState(stage = ProvisionFlowStage.Scanning)
        val ready = connectionManager.ensureBluetoothReady()
        if (!ready) {
            val error = IllegalStateException("蓝牙权限未授权或蓝牙未开启")
            _state.value = ProvisionFlowState(stage = ProvisionFlowStage.Failed, errorMessage = error.message)
            return Result.failure(error)
        }
        connectionManager.startScan(BrainBoxGattProtocol.SERVICE_UUID)
        return Result.success(Unit)
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
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Scanning,
                selectedDevice = found,
                errorMessage = null,
            )
            Result.success(found)
        } else {
            val error = IllegalStateException("扫描超时，未发现目标 BLE 设备")
            _state.value = _state.value.copy(stage = ProvisionFlowStage.Failed, errorMessage = error.message)
            Result.failure(error)
        }
    }

    suspend fun connectDevice(device: BleScanDevice): Result<LucyPairingInfoPayload> {
        return runCatching {
            currentCoroutineContext().ensureActive()
            val currentConnection = connectionManager.currentConnection()
            val isSameDevice = currentConnection?.device?.id == device.id

            // 如果是不同设备，或同一设备但连接已断开，重新连接
            if (!isSameDevice || !connectionManager.isConnected()) {
                if (currentConnection != null) {
                    println("[BrainBox] 断开旧连接 (sameDevice=$isSameDevice, connected=${connectionManager.isConnected()})")
                    connectionManager.disconnect()
                }
                _state.value = _state.value.copy(
                    stage = ProvisionFlowStage.Connecting,
                    selectedDevice = device,
                    errorMessage = null,
                )
                connectionManager.stopScan()
                println("[BrainBox] 正在连接设备: ${device.name} (${device.id})")
                connectionManager.connect(device).getOrThrow()
            } else {
                println("[BrainBox] 复用已有连接: ${device.name} (${device.id}), ready=${connectionManager.isReady()}")
                _state.value = _state.value.copy(
                    stage = ProvisionFlowStage.Connecting,
                    selectedDevice = device,
                    errorMessage = null,
                )
                connectionManager.stopScan()
            }

            // 只在尚未 Ready 时才重新发现服务，避免重复 discover 导致特性句柄失效
            if (!connectionManager.isReady()) {
                _state.value = _state.value.copy(stage = ProvisionFlowStage.Discovering)
                println("[BrainBox] 发现 GATT 服务...")
                router.discoverCharacteristics().getOrThrow()
            } else {
                println("[BrainBox] GATT 服务已就绪，跳过 discover")
            }

            val deviceInfo = router.readDeviceInfo().getOrThrow()
            println("[BrainBox] device_info: hostname=${deviceInfo.hostname}, model=${deviceInfo.model}, serial=${deviceInfo.serial}, interface=${deviceInfo.networkInterface}, ip=${deviceInfo.ip}, ssid=${deviceInfo.ssid}, state=${deviceInfo.state}, error=${deviceInfo.error}")
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.ReadingPairingInfo,
                deviceInfo = deviceInfo,
            )

            val pairingInfo = router.readLucyPairingInfo().getOrThrow()
            println("[BrainBox] pairing_info: version=${pairingInfo.version}, channel=${pairingInfo.channel}, state=${pairingInfo.state}, cdi=${pairingInfo.channelDeviceId}, bindingStatus=${pairingInfo.bindingStatus}, otp=${pairingInfo.otp}, createdAtMs=${pairingInfo.createdAtMs}")
            if (pairingInfo.isUnavailable) {
                throw IllegalStateException("设备配对信息暂不可用")
            }

            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.ReadingPairingInfo,
                pairingInfo = pairingInfo,
                channelDeviceId = pairingInfo.channelDeviceId.takeIf { it.isNotBlank() },
                otp = pairingInfo.otp.takeIf { it.isNotBlank() },
                errorMessage = null,
            )
            pairingInfo
        }.onFailure { error ->
            if (error is CancellationException) throw error
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Failed,
                selectedDevice = device,
                errorMessage = error.message ?: "连接设备失败",
            )
        }
    }

    suspend fun refreshWifiNetworks(
        device: BleScanDevice? = _state.value.selectedDevice,
    ): Result<List<GattWifiNetwork>> {
        val targetDevice = device
            ?: return Result.failure(IllegalStateException("请先选择蓝牙设备"))
        connectDevice(targetDevice).getOrElse { return Result.failure(it) }
        return runCatching {
            currentCoroutineContext().ensureActive()
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.ScanningWifi,
                selectedDevice = targetDevice,
                errorMessage = null,
            )
            val wifiNetworks = try {
                router.scanWifi().getOrThrow()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Wi-Fi 扫描失败（常见于设备扫描 WiFi 时 BLE 射频冲突断连），强制重连重试
                println("[BrainBox] Wi-Fi 扫描失败: ${e.message}，强制重连重试...")
                connectionManager.disconnect()
                delay(500) // 等待设备侧 WiFi 扫描完成
                connectDevice(targetDevice).getOrThrow()
                router.scanWifi().getOrThrow()
            }
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.ScanningWifi,
                wifiNetworks = wifiNetworks,
                errorMessage = null,
            )
            wifiNetworks
        }.onFailure { error ->
            if (error is CancellationException) throw error
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Failed,
                selectedDevice = targetDevice,
                errorMessage = error.message ?: "读取设备 Wi‑Fi 列表失败",
            )
        }
    }

    suspend fun configureWifi(
        ssid: String,
        password: String,
        hidden: Boolean = false,
        timeoutMillis: Long = DEFAULT_NETWORK_TIMEOUT_MS,
    ): Result<NetworkStatusPayload> {
        val targetDevice = _state.value.selectedDevice
            ?: return Result.failure(IllegalStateException("请先选择蓝牙设备"))
        val normalizedSsid = ssid.trim()
        if (normalizedSsid.isBlank()) {
            return Result.failure(IllegalArgumentException("Wi‑Fi 名称不能为空"))
        }
        connectDevice(targetDevice).getOrElse { return Result.failure(it) }
        return runCatching {
            currentCoroutineContext().ensureActive()
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.ConfiguringWifi,
                selectedDevice = targetDevice,
                errorMessage = null,
            )
            println("[BrainBox] configureWifi: 写入 WiFi 配置 ssid=$normalizedSsid hidden=$hidden")
            router.writeWifiConfig(
                ssid = normalizedSsid,
                password = password,
                hidden = hidden,
            ).getOrThrow()
            println("[BrainBox] configureWifi: WiFi 配置写入成功，开始等待联网...")

            _state.value = _state.value.copy(stage = ProvisionFlowStage.WaitingForNetwork)

            // 设备连接 WiFi 时 BLE 射频冲突可能导致断连，轮询 network_status 失败后强制重连重试
            val finalStatus = try {
                router.enableNetworkStatusNotify().getOrThrow()
                println("[BrainBox] configureWifi: notify 已启用，轮询 network_status...")
                awaitConnectedNetworkStatus(timeoutMillis)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("[BrainBox] configureWifi: 等待联网失败: ${e.message}，强制重连后重新轮询...")
                connectionManager.disconnect()
                delay(2_000) // 给设备时间完成 WiFi 连接
                connectDevice(targetDevice).getOrThrow()
                println("[BrainBox] configureWifi: 重连成功，继续轮询 network_status...")
                awaitConnectedNetworkStatus(timeoutMillis)
            }
            println("[BrainBox] configureWifi: 联网成功! state=${finalStatus.state}, ssid=${finalStatus.ssid}, ip=${finalStatus.ip}")

            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Completed,
                networkStatus = finalStatus,
                errorMessage = null,
            )
            finalStatus
        }.onFailure { error ->
            if (error is CancellationException) throw error
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Failed,
                selectedDevice = targetDevice,
                errorMessage = error.message ?: "下发 Wi‑Fi 配置失败",
            )
        }
    }

    /**
     * WiFi 连接成功后单独请求 OTP。
     * 向特性 7f0c0006 写入 {"request_id":"<timestamp>"}，等 2.5s 后读取 pairing_info 拿到 otp。
     */
    suspend fun requestOtpAfterWifi(): Result<LucyPairingInfoPayload> {
        val targetDevice = _state.value.selectedDevice
            ?: return Result.failure(IllegalStateException("请先选择蓝牙设备"))
        return runCatching {
            currentCoroutineContext().ensureActive()
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.RequestingOtp,
                errorMessage = null,
            )

            // 确保连接可用
            if (!connectionManager.isConnected()) {
                println("[BrainBox] requestOtp: 连接已断开，重新连接...")
                connectionManager.disconnect()
                connectDevice(targetDevice).getOrThrow()
            }

            val pairingInfo = try {
                router.requestOtpAndAwaitPairingInfo().getOrThrow()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("[BrainBox] requestOtp: 失败: ${e.message}，强制重连重试...")
                connectionManager.disconnect()
                delay(500)
                connectDevice(targetDevice).getOrThrow()
                router.requestOtpAndAwaitPairingInfo().getOrThrow()
            }
            println("[BrainBox] OTP 获取成功: otp=${pairingInfo.otp}, expiresAtMs=${pairingInfo.otpExpiresAtMs}, cdi=${pairingInfo.channelDeviceId}")

            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Completed,
                pairingInfo = pairingInfo,
                channelDeviceId = pairingInfo.channelDeviceId.takeIf { it.isNotBlank() },
                otp = pairingInfo.otp.takeIf { it.isNotBlank() },
                errorMessage = null,
            )
            pairingInfo
        }.onFailure { error ->
            if (error is CancellationException) throw error
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Failed,
                selectedDevice = targetDevice,
                errorMessage = error.message ?: "请求 OTP 失败",
            )
        }
    }

    suspend fun provision(
        device: BleScanDevice,
        ssid: String,
        password: String,
        timeoutMillis: Long = DEFAULT_NETWORK_TIMEOUT_MS,
    ): Result<ProvisionResult> {
        val normalizedSsid = ssid.trim()
        if (normalizedSsid.isBlank()) {
            val error = IllegalArgumentException("Wi‑Fi 名称不能为空")
            _state.value = ProvisionFlowState(stage = ProvisionFlowStage.Failed, errorMessage = error.message)
            return Result.failure(error)
        }

        return runCatching {
            currentCoroutineContext().ensureActive()
            val pairingInfo = connectDevice(device).getOrThrow()
            val deviceInfo = _state.value.deviceInfo ?: router.readDeviceInfo().getOrThrow()
            val wifiNetworks = refreshWifiNetworks(device).getOrThrow()
            val finalStatus = configureWifi(
                ssid = normalizedSsid,
                password = password,
                hidden = false,
                timeoutMillis = timeoutMillis,
            ).getOrThrow()

            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Completed,
                deviceInfo = deviceInfo,
                pairingInfo = pairingInfo,
                networkStatus = finalStatus,
                channelDeviceId = pairingInfo.channelDeviceId.takeIf { it.isNotBlank() },
                otp = pairingInfo.otp.takeIf { it.isNotBlank() },
                errorMessage = null,
            )
            ProvisionResult(
                device = device,
                deviceInfo = deviceInfo,
                pairingInfo = pairingInfo,
                wifiNetworks = wifiNetworks,
                finalNetworkStatus = finalStatus,
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.Failed,
                selectedDevice = device,
                errorMessage = error.message ?: "BLE 配网失败",
            )
        }
    }

    suspend fun cancel() {
        connectionManager.disconnect()
        _state.value = ProvisionFlowState(stage = ProvisionFlowStage.Idle)
    }

    private suspend fun awaitConnectedNetworkStatus(timeoutMillis: Long): NetworkStatusPayload {
        return withTimeoutOrNull(timeoutMillis) {
            pollNetworkStatusUntilConnected()
        } ?: throw IllegalStateException("等待设备联网超时")
    }

    private suspend fun pollNetworkStatusUntilConnected(): NetworkStatusPayload {
        while (true) {
            currentCoroutineContext().ensureActive()
            val current = router.readNetworkStatus().getOrThrow()
            println("[BrainBox] network_status: state=${current.state}, ssid=${current.ssid}, ip=${current.ip}, error=${current.error}, updatedAt=${current.updatedAt}")
            _state.value = _state.value.copy(networkStatus = current)
            if (current.isConnected) {
                return current
            }
            if (current.isFailed) {
                throw IllegalStateException(current.error.ifBlank { "设备联网失败" })
            }
            delay(NETWORK_STATUS_POLL_INTERVAL_MS)
        }
    }

    companion object {
        const val DEFAULT_NETWORK_TIMEOUT_MS = 90_000L
        private const val NETWORK_STATUS_POLL_INTERVAL_MS = 500L
    }
}
