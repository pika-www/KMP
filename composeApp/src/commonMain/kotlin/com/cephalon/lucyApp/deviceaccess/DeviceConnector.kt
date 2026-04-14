package com.cephalon.lucyApp.deviceaccess

import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.LucyDevice
import com.cephalon.lucyApp.deviceaccess.gatt.ProvisionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import lucy.im.sdk.OnlineDevice

enum class DeviceConnectStage {
    Idle,
    Scanning,
    Binding,
    Provisioning,
    WaitingOnline,
    ConnectingChat,
    Completed,
    Failed,
}

data class DeviceConnectState(
    val stage: DeviceConnectStage = DeviceConnectStage.Idle,
    val channelDeviceId: String? = null,
    val selectedBleDevice: BleScanDevice? = null,
    val boundDevice: LucyDevice? = null,
    val onlineDevice: OnlineDevice? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class ConnectedDeviceSession(
    val channelDeviceId: String,
    val selectedBleDevice: BleScanDevice,
    val boundDevice: LucyDevice,
    val onlineDevice: OnlineDevice,
    val initialMessageId: String? = null,
)

class DeviceConnector(
    private val authRepository: AuthRepository,
    private val provisionUseCase: ProvisionUseCase,
    private val deviceChatManager: DeviceChatManager,
) {
    private val _state = MutableStateFlow(DeviceConnectState())
    val state: StateFlow<DeviceConnectState> = _state.asStateFlow()

    suspend fun startScan(bleManager: BleManager): Result<Unit> {
        _state.value = _state.value.copy(
            stage = DeviceConnectStage.Scanning,
            statusMessage = "正在扫描附近设备",
            errorMessage = null,
        )
        val ready = bleManager.ensureBluetoothReady()
        if (!ready) {
            return failUnit("蓝牙权限未授权或蓝牙未开启")
        }
        bleManager.startScan()
        return Result.success(Unit)
    }

    suspend fun awaitScannedDevice(
        bleManager: BleManager,
        timeoutMillis: Long = DEFAULT_SCAN_TIMEOUT_MS,
        matcher: (BleScanDevice) -> Boolean,
    ): Result<BleScanDevice> {
        val device = withTimeoutOrNull(timeoutMillis) {
            bleManager.state
                .map { state -> state.bleDevices.firstOrNull(matcher) }
                .filter { it != null }
                .first()
        }
        return if (device != null) {
            onBleDeviceSelected(device)
            Result.success(device)
        } else {
            failBle("扫描超时，未找到目标蓝牙设备")
        }
    }

    fun onBleDeviceSelected(device: BleScanDevice) {
        _state.value = _state.value.copy(
            stage = DeviceConnectStage.Scanning,
            selectedBleDevice = device,
            statusMessage = "已选择蓝牙设备 ${device.name}",
            errorMessage = null,
        )
    }

    suspend fun connectAndStartChat(
        provisionManager: ProvisionManager,
        selectedBleDevice: BleScanDevice,
        ssid: String,
        password: String,
        openingMessage: String? = null,
    ): Result<ConnectedDeviceSession> {
        _state.value = DeviceConnectState(
            stage = DeviceConnectStage.Provisioning,
            selectedBleDevice = selectedBleDevice,
            statusMessage = "正在通过 BLE GATT 连接设备并下发网络配置",
        )

        val provisionResult = provisionManager.provision(
            device = selectedBleDevice,
            ssid = ssid,
            password = password,
        ).getOrElse { error ->
            return fail(error.message ?: "BLE 配网失败")
        }

        val normalizedCdi = provisionResult.pairingInfo.channelDeviceId.trim()
        if (normalizedCdi.isBlank()) {
            return fail("设备未返回有效 channel_device_id")
        }

        _state.value = _state.value.copy(
            stage = DeviceConnectStage.Binding,
            channelDeviceId = normalizedCdi,
            statusMessage = "设备已完成配网，正在确认绑定信息",
            errorMessage = null,
        )

        val boundDevice = authRepository.requireDeviceByChannelDeviceId(normalizedCdi)
            .getOrElse { error ->
                return fail(error.message ?: "未找到已绑定设备")
            }

        _state.value = _state.value.copy(
            stage = DeviceConnectStage.WaitingOnline,
            channelDeviceId = normalizedCdi,
            boundDevice = boundDevice,
            statusMessage = "已拿到 CDI，正在等待设备上线",
            errorMessage = null,
        )

        deviceChatManager.ensureImConnected().getOrElse { error ->
            return fail(error.message ?: "IM 连接失败")
        }

        val onlineDevice = deviceChatManager.awaitDeviceOnline(normalizedCdi).getOrElse { error ->
            return fail(error.message ?: "设备未上线")
        }

        _state.value = _state.value.copy(
            stage = DeviceConnectStage.ConnectingChat,
            onlineDevice = onlineDevice,
            statusMessage = "设备已上线，正在建立通信",
            errorMessage = null,
        )

        val initialMessageId = if (!openingMessage.isNullOrBlank()) {
            deviceChatManager.sendText(normalizedCdi, openingMessage)
                .getOrElse { error ->
                    return fail(error.message ?: "建立通信失败")
                }
        } else {
            null
        }

        authRepository.setConnectionFlag()

        val result = ConnectedDeviceSession(
            channelDeviceId = normalizedCdi,
            selectedBleDevice = selectedBleDevice,
            boundDevice = boundDevice,
            onlineDevice = onlineDevice,
            initialMessageId = initialMessageId,
        )

        _state.value = _state.value.copy(
            stage = DeviceConnectStage.Completed,
            channelDeviceId = normalizedCdi,
            boundDevice = boundDevice,
            onlineDevice = onlineDevice,
            statusMessage = if (initialMessageId != null) {
                "设备接入完成，通信已建立"
            } else {
                "设备接入完成，等待发送首条消息"
            },
            errorMessage = null,
        )

        return Result.success(result)
    }

    suspend fun connectAndStartChat(
        bleManager: BleManager,
        selectedBleDevice: BleScanDevice,
        channelDeviceId: String,
        ssid: String,
        password: String,
        openingMessage: String? = null,
    ): Result<ConnectedDeviceSession> {
        val normalizedCdi = channelDeviceId.trim()
        if (normalizedCdi.isBlank()) {
            return fail("channel_device_id 不能为空")
        }

        _state.value = DeviceConnectState(
            stage = DeviceConnectStage.Binding,
            channelDeviceId = normalizedCdi,
            selectedBleDevice = selectedBleDevice,
            statusMessage = "正在绑定设备",
        )

        val provisioned = provisionUseCase.provisionDevice(
            bleManager = bleManager,
            channelDeviceId = normalizedCdi,
            ssid = ssid,
            password = password,
        ).getOrElse { error ->
            return fail(error.message ?: "配网失败")
        }

        _state.value = _state.value.copy(
            stage = DeviceConnectStage.WaitingOnline,
            boundDevice = provisioned.device,
            statusMessage = "正在等待设备上线",
        )

        deviceChatManager.ensureImConnected().getOrElse { error ->
            return fail(error.message ?: "IM 连接失败")
        }

        val onlineDevice = deviceChatManager.awaitDeviceOnline(normalizedCdi).getOrElse { error ->
            return fail(error.message ?: "设备未上线")
        }

        _state.value = _state.value.copy(
            stage = DeviceConnectStage.ConnectingChat,
            onlineDevice = onlineDevice,
            statusMessage = "设备已上线，正在建立通信",
        )

        val initialMessageId = if (!openingMessage.isNullOrBlank()) {
            deviceChatManager.sendText(normalizedCdi, openingMessage)
                .getOrElse { error ->
                    return fail(error.message ?: "建立通信失败")
                }
        } else {
            null
        }

        authRepository.setConnectionFlag()

        val result = ConnectedDeviceSession(
            channelDeviceId = normalizedCdi,
            selectedBleDevice = selectedBleDevice,
            boundDevice = provisioned.device,
            onlineDevice = onlineDevice,
            initialMessageId = initialMessageId,
        )

        _state.value = _state.value.copy(
            stage = DeviceConnectStage.Completed,
            boundDevice = provisioned.device,
            onlineDevice = onlineDevice,
            statusMessage = if (initialMessageId != null) {
                "设备接入完成，通信已建立"
            } else {
                "设备接入完成，等待发送首条消息"
            },
            errorMessage = null,
        )

        return Result.success(result)
    }

    private fun fail(message: String): Result<ConnectedDeviceSession> {
        _state.value = _state.value.copy(
            stage = DeviceConnectStage.Failed,
            statusMessage = null,
            errorMessage = message,
        )
        return Result.failure(IllegalStateException(message))
    }

    private fun failUnit(message: String): Result<Unit> {
        _state.value = _state.value.copy(
            stage = DeviceConnectStage.Failed,
            statusMessage = null,
            errorMessage = message,
        )
        return Result.failure(IllegalStateException(message))
    }

    private fun failBle(message: String): Result<BleScanDevice> {
        _state.value = _state.value.copy(
            stage = DeviceConnectStage.Failed,
            statusMessage = null,
            errorMessage = message,
        )
        return Result.failure(IllegalStateException(message))
    }

    companion object {
        private const val DEFAULT_SCAN_TIMEOUT_MS = 15_000L
    }
}
