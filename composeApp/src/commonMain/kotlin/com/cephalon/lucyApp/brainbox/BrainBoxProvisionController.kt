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

/**
 * 手机本机 Wi‑Fi 的即时状态，用于 BLE 连上脑花盒子后"自动注入本机 Wi‑Fi"的流程。
 *
 * - [Disabled]: 手机 Wi‑Fi 开关关闭 → UI 提示用户打开 Wi‑Fi。
 * - [Unknown]: Wi‑Fi 可能开着，但没办法拿到 SSID（Android 缺 fine-location 授权；
 *   iOS 缺 "Access WiFi Information" entitlement / 定位权限；或此刻没连任何网络）。
 *   UI 端按"读不到"处理：回到 Wi‑Fi 手动步骤。
 * - [Connected]: 读到 SSID，可作为下发给盒子的目标 Wi‑Fi 名。
 */
sealed class PhoneWifiState {
    data object Disabled : PhoneWifiState()
    data object Unknown : PhoneWifiState()
    data class Connected(val ssid: String) : PhoneWifiState()
}

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

    /**
     * 读取手机当前 Wi‑Fi 状态与 SSID。仅用于自动把本机 Wi‑Fi 注入给脑花盒子：
     * - 返回 [PhoneWifiState.Connected] 时，UI 直接用 ssid 做 BLE `wifi_config` 的目标；
     * - 返回 [PhoneWifiState.Disabled] 时，UI toast 提示并引导用户开启 Wi‑Fi；
     * - 返回 [PhoneWifiState.Unknown] 时，UI 回退到手动输入 SSID/密码。
     *
     * Android：会按需请求 Wi‑Fi 相关权限（fine-location / nearby-wifi-devices）。
     * iOS：需要 App 开启 "Access WiFi Information" entitlement 且有定位授权，
     * 否则总是返回 Unknown。
     */
    suspend fun readCurrentPhoneWifi(): PhoneWifiState
}

@Composable
expect fun rememberBrainBoxProvisionController(): BrainBoxProvisionController
