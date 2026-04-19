package com.cephalon.lucyApp.brainbox

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

enum class BrainBoxWifiMode {
    NearbyScan,
    ManualOnly,
}

data class BrainBoxBleDevice(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val rssi: Int? = null,
)

data class BrainBoxWifiNetwork(
    val ssid: String,
    val strengthLevel: Int = 0,
    val isSecure: Boolean = true,
    /** 脑花盒子当前已连接的那个 Wi‑Fi 会带上这个标记，UI 上会展示"已连接"并允许直接沿用。 */
    val isCurrent: Boolean = false,
)

interface BrainBoxProvisionController {
    val bluetoothPermissionGranted: Boolean
    val bluetoothEnabled: Boolean
    val wifiPermissionGranted: Boolean
    val wifiMode: BrainBoxWifiMode
    val bleDevices: StateFlow<List<BrainBoxBleDevice>>
    val isBleScanning: StateFlow<Boolean>
    val wifiNetworks: StateFlow<List<BrainBoxWifiNetwork>>
    val isWifiLoading: StateFlow<Boolean>

    suspend fun requestBluetoothPermission(): Boolean
    suspend fun requestWifiPermission(): Boolean
    fun openBluetoothSettings()
    fun openWifiSettings()
    fun startBleScan()
    fun stopBleScan()
    suspend fun refreshWifiNetworks(): Result<List<BrainBoxWifiNetwork>>
    suspend fun connectToWifi(ssid: String, password: String): Result<String>
}

@Composable
expect fun rememberBrainBoxProvisionController(): BrainBoxProvisionController
