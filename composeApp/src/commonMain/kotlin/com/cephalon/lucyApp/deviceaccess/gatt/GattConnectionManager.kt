package com.cephalon.lucyApp.deviceaccess.gatt

import com.cephalon.lucyApp.deviceaccess.BleScanDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class GattConnectionSnapshot(
    val device: BleScanDevice? = null,
    val state: BleGattConnectionState = BleGattConnectionState.Idle,
    val services: List<BleGattService> = emptyList(),
    val errorMessage: String? = null,
)

class GattConnectionManager(
    private val bleClient: BleClient,
) {
    private val _state = MutableStateFlow(GattConnectionSnapshot())
    val state: StateFlow<GattConnectionSnapshot> = _state.asStateFlow()
    val scanState: StateFlow<BleScanSnapshot> = bleClient.scanState

    private var activeConnection: BleGattConnection? = null

    suspend fun ensureBluetoothReady(): Boolean = bleClient.ensureBluetoothReady()

    fun openBluetoothSettings() = bleClient.openBluetoothSettings()

    fun startScan(serviceUuid: String? = BrainBoxGattProtocol.SERVICE_UUID) {
        bleClient.startScan(serviceUuid)
    }

    fun stopScan() {
        bleClient.stopScan()
    }

    suspend fun connect(device: BleScanDevice): Result<BleGattConnection> {
        _state.value = GattConnectionSnapshot(
            device = device,
            state = BleGattConnectionState.Connecting,
        )
        val connection = bleClient.connectGatt(device).getOrElse { error ->
            _state.value = GattConnectionSnapshot(
                device = device,
                state = BleGattConnectionState.Error,
                errorMessage = error.message ?: "GATT 连接失败",
            )
            return Result.failure(error)
        }
        activeConnection = connection
        _state.value = GattConnectionSnapshot(
            device = device,
            state = connection.state.value,
            services = connection.services.value,
        )
        return Result.success(connection)
    }

    suspend fun discoverProtocol(): Result<List<BleGattCharacteristic>> {
        val connection = activeConnection ?: return Result.failure(IllegalStateException("尚未建立 GATT 连接"))
        _state.value = _state.value.copy(state = BleGattConnectionState.Discovering, errorMessage = null)
        val services = connection.discoverServices().getOrElse { error ->
            _state.value = _state.value.copy(
                state = BleGattConnectionState.Error,
                errorMessage = error.message ?: "发现服务失败",
            )
            return Result.failure(error)
        }
        val protocolService = services.firstOrNull {
            it.uuid.equals(BrainBoxGattProtocol.SERVICE_UUID, ignoreCase = true)
        } ?: return Result.failure(IllegalStateException("未发现 Brain Box GATT Service"))
        _state.value = _state.value.copy(
            state = BleGattConnectionState.Ready,
            services = services,
            errorMessage = null,
        )
        return Result.success(protocolService.characteristics)
    }

    fun currentConnection(): BleGattConnection? = activeConnection

    fun isConnected(): Boolean {
        val conn = activeConnection ?: return false
        val s = conn.state.value
        return s == BleGattConnectionState.Connected || s == BleGattConnectionState.Ready
    }

    fun isReady(): Boolean {
        val conn = activeConnection ?: return false
        return conn.state.value == BleGattConnectionState.Ready
    }

    suspend fun disconnect() {
        activeConnection?.disconnect()
        activeConnection = null
        _state.value = GattConnectionSnapshot(state = BleGattConnectionState.Disconnected)
    }
}
