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
        return runCatching {
            currentCoroutineContext().ensureActive()
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.ScanningWifi,
                selectedDevice = targetDevice,
                errorMessage = null,
            )
            // 在已有连接上直接扫描，断开时才重连
            val wifiNetworks = try {
                router.scanWifi().getOrThrow()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("[BrainBox] Wi-Fi 扫描失败: ${e.message}，重连后重试...")
                reconnectBle(targetDevice)
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
        return runCatching {
            currentCoroutineContext().ensureActive()
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.ConfiguringWifi,
                selectedDevice = targetDevice,
                errorMessage = null,
            )

            // 直接在已有连接上写入 WiFi 配置（连接由 connectDevice 在 Wifi 步骤入口时建立）
            println("[BrainBox] configureWifi: 写入 WiFi 配置 ssid=$normalizedSsid hidden=$hidden")
            router.writeWifiConfig(
                ssid = normalizedSsid,
                password = password,
                hidden = hidden,
            ).getOrThrow()
            println("[BrainBox] configureWifi: WiFi 配置写入成功，开始等待联网...")

            _state.value = _state.value.copy(stage = ProvisionFlowStage.WaitingForNetwork)

            // 轮询 network_status，BLE 射频冲突导致断连时重连后继续轮询
            val finalStatus = try {
                router.enableNetworkStatusNotify().getOrThrow()
                println("[BrainBox] configureWifi: notify 已启用，轮询 network_status...")
                awaitConnectedNetworkStatus(timeoutMillis)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("[BrainBox] configureWifi: 等待联网失败: ${e.message}，重连后重新轮询...")
                reconnectBle(targetDevice)
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
     * 轻量级 BLE 重连：只做 connect + discover，不读 device_info/pairing_info。
     * 带重试和递增等待，适合 WiFi 配网后 BLE 不稳定的场景。
     */
    private suspend fun reconnectBle(device: BleScanDevice, maxAttempts: Int = 3) {
        // 已就绪，直接返回
        if (connectionManager.isReady()) {
            println("[BrainBox] reconnectBle: BLE 已就绪，无需重连")
            return
        }
        // 已连接但未 discover，只需 discover
        if (connectionManager.isConnected()) {
            println("[BrainBox] reconnectBle: BLE 已连接，执行 discover...")
            try {
                router.discoverCharacteristics().getOrThrow()
                println("[BrainBox] reconnectBle: discover 成功")
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("[BrainBox] reconnectBle: discover 失败: ${e.message}，开始完整重连...")
            }
        }
        // 完整重连，带递增等待重试
        for (attempt in 1..maxAttempts) {
            try {
                val waitMs = 2_000L * attempt // 2s, 4s, 6s
                println("[BrainBox] reconnectBle: 等待 ${waitMs}ms 后尝试连接 (第${attempt}次/${maxAttempts})...")
                connectionManager.disconnect()
                delay(waitMs)
                connectionManager.connect(device).getOrThrow()
                if (!connectionManager.isReady()) {
                    router.discoverCharacteristics().getOrThrow()
                }
                println("[BrainBox] reconnectBle: BLE 连接就绪")
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("[BrainBox] reconnectBle: 第${attempt}次失败: ${e.message}")
                if (attempt == maxAttempts) {
                    // 所有重试失败，检测到设备断开则回退到重新扫描
                    if (isDeviceDisconnectedError(e)) {
                        println("[BrainBox] reconnectBle: 设备已断开，回退到重新扫描...")
                        rescanAndReconnect(device)
                        return
                    }
                    throw e
                }
            }
        }
    }

    private fun isDeviceDisconnectedError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: return false
        return msg.contains("disconnect") || msg.contains("disconnected")
    }

    /**
     * 完整恢复：重新扫描 BLE → 找到同名设备 → connectDevice（connect + discover + read device_info + read pairing_info）。
     * 用于特性句柄丢失、设备彻底断开等场景。
     */
    private suspend fun rescanAndReconnect(
        originalDevice: BleScanDevice,
        scanTimeoutMillis: Long = 15_000L,
    ): BleScanDevice {
        println("[BrainBox] rescanAndReconnect: 断开旧连接，开始重新扫描...")
        connectionManager.disconnect()
        delay(2_000) // 等待设备 BLE 栈恢复
        connectionManager.startScan(BrainBoxGattProtocol.SERVICE_UUID)

        val rediscovered = withTimeoutOrNull(scanTimeoutMillis) {
            connectionManager.scanState
                .map { snapshot ->
                    snapshot.devices.firstOrNull { it.id == originalDevice.id }
                        ?: snapshot.devices.firstOrNull { it.name == originalDevice.name }
                }
                .filter { it != null }
                .first()
        } ?: throw IllegalStateException("重新扫描超时，未找到设备 ${originalDevice.name}")

        println("[BrainBox] rescanAndReconnect: 重新发现设备 ${rediscovered.name} (${rediscovered.id})，开始连接...")
        connectDevice(rediscovered).getOrThrow()
        println("[BrainBox] rescanAndReconnect: 连接成功，BLE 就绪")
        _state.value = _state.value.copy(selectedDevice = rediscovered)
        return rediscovered
    }

    suspend fun requestOtpAfterWifi(): Result<LucyPairingInfoPayload> {
        val targetDevice = _state.value.selectedDevice
            ?: return Result.failure(IllegalStateException("请先选择蓝牙设备"))
        return runCatching {
            currentCoroutineContext().ensureActive()
            _state.value = _state.value.copy(
                stage = ProvisionFlowStage.RequestingOtp,
                errorMessage = null,
            )

            // 策略：先用现有连接直接尝试，失败后重新扫描设备并连接
            var pairingInfo = try {
                println("[BrainBox] requestOtp: 直接尝试请求 OTP（复用现有 BLE 连接）...")
                router.requestOtpAndAwaitPairingInfo().getOrThrow()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("[BrainBox] requestOtp: 直接请求失败: ${e.message}，重新扫描设备...")
                rescanAndReconnect(targetDevice)
                router.requestOtpAndAwaitPairingInfo().getOrThrow()
            }
            println("[BrainBox] OTP 获取结果: otp=${pairingInfo.otp}, expiresAtMs=${pairingInfo.otpExpiresAtMs}, isOtpExpired=${pairingInfo.isOtpExpired}, cdi=${pairingInfo.channelDeviceId}")

            if (pairingInfo.isOtpExpired) {
                println("[BrainBox] OTP 已过期，重新请求...")
                pairingInfo = router.requestOtpAndAwaitPairingInfo().getOrThrow()
                println("[BrainBox] OTP 重新获取结果: otp=${pairingInfo.otp}, expiresAtMs=${pairingInfo.otpExpiresAtMs}, isOtpExpired=${pairingInfo.isOtpExpired}")
                if (pairingInfo.isOtpExpired) {
                    throw IllegalStateException("OTP 已过期，请重新操作")
                }
            }
            if (!pairingInfo.isOtpValid) {
                throw IllegalStateException("未获取到有效 OTP")
            }
            println("[BrainBox] OTP 有效: otp=${pairingInfo.otp}, cdi=${pairingInfo.channelDeviceId}")

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
