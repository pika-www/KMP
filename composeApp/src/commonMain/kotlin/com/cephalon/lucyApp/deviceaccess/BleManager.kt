package com.cephalon.lucyApp.deviceaccess

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.cephalon.lucyApp.brainbox.BrainBoxBleDevice
import com.cephalon.lucyApp.brainbox.BrainBoxProvisionController
import com.cephalon.lucyApp.brainbox.BrainBoxWifiMode
import com.cephalon.lucyApp.brainbox.BrainBoxWifiNetwork
import com.cephalon.lucyApp.logging.appLogD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class BleWifiMode {
    NearbyScan,
    ManualOnly,
}

data class BleScanDevice(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val rssi: Int? = null,
)

data class BleWifiCandidate(
    val ssid: String,
    val strengthLevel: Int = 0,
    val isSecure: Boolean = true,
)

data class BleManagerState(
    val bluetoothPermissionGranted: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val wifiPermissionGranted: Boolean = false,
    val wifiMode: BleWifiMode = BleWifiMode.NearbyScan,
    val bleDevices: List<BleScanDevice> = emptyList(),
    val isScanning: Boolean = false,
    val wifiNetworks: List<BleWifiCandidate> = emptyList(),
    val isWifiLoading: Boolean = false,
)

interface BleManager {
    val state: StateFlow<BleManagerState>

    suspend fun ensureBluetoothReady(): Boolean
    suspend fun ensureWifiReady(): Boolean
    fun openBluetoothSettings()
    fun openWifiSettings()
    fun startScan()
    fun stopScan()
    suspend fun refreshWifiNetworks(): Result<List<BleWifiCandidate>>
    suspend fun connectPhoneToWifi(ssid: String, password: String): Result<String>
}

@Composable
expect fun rememberBleManager(): BleManager

@Composable
internal fun rememberDelegatingBleManager(
    controller: BrainBoxProvisionController,
): BleManager {
    val state = remember {
        MutableStateFlow(
            BleManagerState(
                bluetoothPermissionGranted = controller.bluetoothPermissionGranted,
                bluetoothEnabled = controller.bluetoothEnabled,
                wifiPermissionGranted = controller.wifiPermissionGranted,
                wifiMode = controller.wifiMode.toBleWifiMode(),
            )
        )
    }

    LaunchedEffect(controller) {
        snapshotFlow {
            AvailabilitySnapshot(
                bluetoothPermissionGranted = controller.bluetoothPermissionGranted,
                bluetoothEnabled = controller.bluetoothEnabled,
                wifiPermissionGranted = controller.wifiPermissionGranted,
                wifiMode = controller.wifiMode.toBleWifiMode(),
            )
        }.collect { snapshot ->
            state.update {
                it.copy(
                    bluetoothPermissionGranted = snapshot.bluetoothPermissionGranted,
                    bluetoothEnabled = snapshot.bluetoothEnabled,
                    wifiPermissionGranted = snapshot.wifiPermissionGranted,
                    wifiMode = snapshot.wifiMode,
                )
            }
        }
    }

    LaunchedEffect(controller) {
        controller.bleDevices.collect { devices ->
            val mappedDevices = devices.map { it.toBleScanDevice() }
            if (mappedDevices.isEmpty()) {
                appLogD(BLE_LOG_TAG, "Scanned BLE devices: []")
            } else {
                val deviceText = mappedDevices.joinToString(separator = " | ") { device ->
                    "name=${device.name}, id=${device.id}, subtitle=${device.subtitle}, rssi=${device.rssi}"
                }
                appLogD(BLE_LOG_TAG, "Scanned BLE devices(${mappedDevices.size}): $deviceText")
            }
            state.update { current ->
                current.copy(bleDevices = mappedDevices)
            }
        }
    }

    LaunchedEffect(controller) {
        controller.isBleScanning.collect { scanning ->
            state.update { current -> current.copy(isScanning = scanning) }
        }
    }

    LaunchedEffect(controller) {
        controller.wifiNetworks.collect { networks ->
            state.update { current ->
                current.copy(wifiNetworks = networks.map { it.toBleWifiCandidate() })
            }
        }
    }

    LaunchedEffect(controller) {
        controller.isWifiLoading.collect { loading ->
            state.update { current -> current.copy(isWifiLoading = loading) }
        }
    }

    return remember(controller, state) {
        object : BleManager {
            override val state: StateFlow<BleManagerState> = state.asStateFlow()

            override suspend fun ensureBluetoothReady(): Boolean {
                val granted = controller.requestBluetoothPermission()
                state.update {
                    it.copy(
                        bluetoothPermissionGranted = controller.bluetoothPermissionGranted,
                        bluetoothEnabled = controller.bluetoothEnabled,
                    )
                }
                return granted && controller.bluetoothEnabled
            }

            override suspend fun ensureWifiReady(): Boolean {
                val granted = controller.requestWifiPermission()
                state.update {
                    it.copy(
                        wifiPermissionGranted = controller.wifiPermissionGranted,
                    )
                }
                return granted
            }

            override fun openBluetoothSettings() {
                controller.openBluetoothSettings()
            }

            override fun openWifiSettings() {
                controller.openWifiSettings()
            }

            override fun startScan() {
                controller.startBleScan()
            }

            override fun stopScan() {
                controller.stopBleScan()
            }

            override suspend fun refreshWifiNetworks(): Result<List<BleWifiCandidate>> {
                return controller.refreshWifiNetworks().map { list ->
                    list.map { it.toBleWifiCandidate() }
                }
            }

            override suspend fun connectPhoneToWifi(ssid: String, password: String): Result<String> {
                return controller.connectToWifi(ssid = ssid, password = password)
            }
        }
    }
}

private data class AvailabilitySnapshot(
    val bluetoothPermissionGranted: Boolean,
    val bluetoothEnabled: Boolean,
    val wifiPermissionGranted: Boolean,
    val wifiMode: BleWifiMode,
)

private fun BrainBoxBleDevice.toBleScanDevice(): BleScanDevice {
    return BleScanDevice(
        id = id,
        name = name,
        subtitle = subtitle,
        rssi = rssi,
    )
}

private fun BrainBoxWifiNetwork.toBleWifiCandidate(): BleWifiCandidate {
    return BleWifiCandidate(
        ssid = ssid,
        strengthLevel = strengthLevel,
        isSecure = isSecure,
    )
}

private fun BrainBoxWifiMode.toBleWifiMode(): BleWifiMode {
    return when (this) {
        BrainBoxWifiMode.NearbyScan -> BleWifiMode.NearbyScan
        BrainBoxWifiMode.ManualOnly -> BleWifiMode.ManualOnly
    }
}

private const val BLE_LOG_TAG = "BleManager"
