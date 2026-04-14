package com.cephalon.lucyApp.brainbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cephalon.lucyApp.logging.appLogD
import com.cephalon.lucyApp.scan.rememberOpenAppSettings
import com.cephalon.lucyApp.scan.rememberOpenWifiSettings
import kotlinx.coroutines.flow.MutableStateFlow
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerScanOptionAllowDuplicatesKey
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.CoreBluetooth.CBPeripheral
import platform.Foundation.NSNumber
import platform.darwin.NSObject

@Composable
actual fun rememberBrainBoxProvisionController(): BrainBoxProvisionController {
    val openAppSettingsAction = rememberOpenAppSettings()
    val openWifiSettingsAction = rememberOpenWifiSettings()
    val bleDevicesFlow = remember { MutableStateFlow<List<BrainBoxBleDevice>>(emptyList()) }
    val isBleScanningFlow = remember { MutableStateFlow(false) }
    val wifiNetworksFlow = remember { MutableStateFlow<List<BrainBoxWifiNetwork>>(emptyList()) }
    val isWifiLoadingFlow = remember { MutableStateFlow(false) }
    val discoveredDevices = remember { linkedMapOf<String, BrainBoxBleDevice>() }

    var bluetoothPermissionGrantedState by remember { mutableStateOf(false) }
    var bluetoothEnabledState by remember { mutableStateOf(false) }
    var centralManagerState by remember { mutableStateOf<CBCentralManager?>(null) }

    val delegate = remember {
        object : NSObject(), CBCentralManagerDelegateProtocol {
            override fun centralManagerDidUpdateState(central: CBCentralManager) {
                bluetoothEnabledState = central.state == CBManagerStatePoweredOn
                bluetoothPermissionGrantedState = central.state != CBManagerStateUnauthorized &&
                    central.state != CBManagerStateUnsupported
                if (central.state != CBManagerStatePoweredOn) {
                    isBleScanningFlow.value = false
                } else if (isBleScanningFlow.value) {
                    central.scanForPeripheralsWithServices(
                        serviceUUIDs = null,
                        options = mapOf(CBCentralManagerScanOptionAllowDuplicatesKey to false)
                    )
                }
            }

            override fun centralManager(
                central: CBCentralManager,
                didDiscoverPeripheral: CBPeripheral,
                advertisementData: Map<Any?, *>,
                RSSI: NSNumber,
            ) {
                val identifier = didDiscoverPeripheral.identifier.UUIDString
                if (identifier.isBlank()) return
                val localName = advertisementData[CBAdvertisementDataLocalNameKey] as? String
                val displayName = localName ?: didDiscoverPeripheral.name ?: "Lucy ${identifier.takeLast(4)}"
                discoveredDevices[identifier] = BrainBoxBleDevice(
                    id = identifier,
                    name = displayName,
                    subtitle = identifier,
                    rssi = RSSI.intValue,
                )
                bleDevicesFlow.value = discoveredDevices.values.sortedByDescending { it.rssi ?: Int.MIN_VALUE }
                val currentDevices = bleDevicesFlow.value
                val deviceText = currentDevices.joinToString(separator = " | ") { device ->
                    "name=${device.name}, id=${device.id}, subtitle=${device.subtitle}, rssi=${device.rssi}"
                }
                appLogD(BLE_SCAN_LOG_TAG, "Scanned BLE devices(${currentDevices.size}): $deviceText")
            }
        }
    }

    fun ensureCentralManager(): CBCentralManager {
        val current = centralManagerState
        if (current != null) return current
        val created = CBCentralManager(delegate = delegate, queue = null)
        centralManagerState = created
        return created
    }

    val controller = remember(delegate, openAppSettingsAction, openWifiSettingsAction) {
        object : BrainBoxProvisionController {
            override val bluetoothPermissionGranted: Boolean
                get() = bluetoothPermissionGrantedState

            override val bluetoothEnabled: Boolean
                get() = bluetoothEnabledState

            override val wifiPermissionGranted: Boolean
                get() = true

            override val wifiMode: BrainBoxWifiMode = BrainBoxWifiMode.ManualOnly

            override val bleDevices = bleDevicesFlow

            override val isBleScanning = isBleScanningFlow

            override val wifiNetworks = wifiNetworksFlow

            override val isWifiLoading = isWifiLoadingFlow

            override suspend fun requestBluetoothPermission(): Boolean {
                ensureCentralManager()
                return bluetoothPermissionGrantedState
            }

            override suspend fun requestWifiPermission(): Boolean {
                return true
            }

            override fun openBluetoothSettings() {
                openAppSettingsAction()
            }

            override fun openWifiSettings() {
                openWifiSettingsAction()
            }

            override fun startBleScan() {
                discoveredDevices.clear()
                bleDevicesFlow.value = emptyList()
                isBleScanningFlow.value = true
                val manager = ensureCentralManager()
                if (manager.state == CBManagerStatePoweredOn) {
                    manager.scanForPeripheralsWithServices(
                        serviceUUIDs = null,
                        options = mapOf(CBCentralManagerScanOptionAllowDuplicatesKey to false)
                    )
                } else {
                    isBleScanningFlow.value = false
                }
            }

            override fun stopBleScan() {
                centralManagerState?.stopScan()
                isBleScanningFlow.value = false
            }

            override suspend fun refreshWifiNetworks(): Result<List<BrainBoxWifiNetwork>> {
                wifiNetworksFlow.value = emptyList()
                return Result.success(emptyList())
            }

            override suspend fun connectToWifi(ssid: String, password: String): Result<String> {
                if (ssid.isBlank()) {
                    return Result.failure(IllegalArgumentException("请输入 Wi‑Fi 名称"))
                }
                openWifiSettingsAction()
                return Result.success("iOS 请在系统设置中完成连接到 $ssid")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            centralManagerState?.stopScan()
            isBleScanningFlow.value = false
        }
    }

    return controller
}

private const val BLE_SCAN_LOG_TAG = "BrainBoxBleScan"
