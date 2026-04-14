package com.cephalon.lucyApp.deviceaccess.gatt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.cephalon.lucyApp.deviceaccess.BleScanDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


data class BleScanSnapshot(
    val bluetoothPermissionGranted: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val devices: List<BleScanDevice> = emptyList(),
    val errorMessage: String? = null,
)

enum class BleGattConnectionState {
    Idle,
    Connecting,
    Connected,
    Discovering,
    Ready,
    Disconnected,
    Error,
}

data class BleGattCharacteristic(
    val serviceUuid: String,
    val uuid: String,
    val properties: Int = 0,
)

data class BleGattService(
    val uuid: String,
    val characteristics: List<BleGattCharacteristic> = emptyList(),
)

interface BleGattConnection {
    val device: BleScanDevice
    val state: StateFlow<BleGattConnectionState>
    val services: StateFlow<List<BleGattService>>

    suspend fun discoverServices(): Result<List<BleGattService>>
    suspend fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Result<ByteArray>
    suspend fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, payload: ByteArray): Result<Unit>
    suspend fun setCharacteristicNotification(
        serviceUuid: String,
        characteristicUuid: String,
        enabled: Boolean,
    ): Result<Unit>
    fun observeCharacteristic(serviceUuid: String, characteristicUuid: String): Flow<ByteArray>
    suspend fun disconnect()
}

interface BleClient {
    val scanState: StateFlow<BleScanSnapshot>

    suspend fun ensureBluetoothReady(): Boolean
    fun openBluetoothSettings()
    fun startScan(serviceUuid: String? = null)
    fun stopScan()
    suspend fun connectGatt(device: BleScanDevice): Result<BleGattConnection>
}

@Composable
expect fun rememberBleClient(): BleClient

@Composable
fun rememberProvisionManager(): ProvisionManager {
    val bleClient = rememberBleClient()
    val dispatcher = remember { JsonCommandDispatcher() }
    val connectionManager = remember(bleClient) { GattConnectionManager(bleClient) }
    val router = remember(connectionManager, dispatcher) { GattRouter(connectionManager, dispatcher) }
    return remember(connectionManager, router) { ProvisionManager(connectionManager, router) }
}
