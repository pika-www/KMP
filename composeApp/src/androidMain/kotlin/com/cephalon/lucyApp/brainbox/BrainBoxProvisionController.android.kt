package com.cephalon.lucyApp.brainbox

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult as WifiScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.cephalon.lucyApp.logging.appLogD
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun bluetoothPermissionList(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

private fun wifiPermissionList(): Array<String> {
    val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.NEARBY_WIFI_DEVICES
    }
    return permissions.toTypedArray()
}

private fun hasBluetoothPermissions(context: Context): Boolean {
    return bluetoothPermissionList().all(context::hasPermission)
}

private fun hasWifiPermissions(context: Context): Boolean {
    return wifiPermissionList().all(context::hasPermission)
}

private fun wifiSignalLevel(level: Int): Int {
    return when {
        level >= -55 -> 4
        level >= -65 -> 3
        level >= -75 -> 2
        level >= -85 -> 1
        else -> 0
    }
}

/**
 * 规范化 Android WifiInfo.getSSID()：去掉两端双引号，并把 "<unknown ssid>" / 空串 视作未知。
 *
 * - API 28 及以下：`WifiManager.connectionInfo.ssid` 直接是 `"foo"` 这种带引号的字符串；
 * - API 29+：同样是带引号，但若 App 没有 ACCESS_FINE_LOCATION，会得到占位符 "<unknown ssid>"；
 * - API 30+ 推荐从 `ConnectivityManager.getNetworkCapabilities(...).transportInfo as WifiInfo`
 *   读，对权限的依赖和上面一致。
 */
private fun normalizeWifiSsid(rawSsid: String?): String? {
    if (rawSsid.isNullOrBlank()) return null
    val trimmed = rawSsid.trim()
    val unquoted = if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
        trimmed.substring(1, trimmed.length - 1)
    } else {
        trimmed
    }
    if (unquoted.isBlank()) return null
    if (unquoted.equals(WifiManager.UNKNOWN_SSID, ignoreCase = true)) return null
    // API 33 以下常量 WifiManager.UNKNOWN_SSID = "<unknown ssid>"，这里额外兼容文字写法。
    if (unquoted == "<unknown ssid>") return null
    return unquoted
}

/**
 * 当前连接中的 Wi‑Fi 信息（若有）。
 *
 * 优先从 API 31+ 的 `NetworkCapabilities.transportInfo` 取 WifiInfo，
 * 老版本退回 `WifiManager.connectionInfo`。两条路径都需要 ACCESS_FINE_LOCATION（API 28+）。
 */
private fun readActiveWifiInfo(
    connectivityManager: ConnectivityManager,
    wifiManager: WifiManager,
): WifiInfo? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val active = connectivityManager.activeNetwork ?: return null
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return null
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        val info = caps.transportInfo
        return info as? WifiInfo
    }
    @Suppress("DEPRECATION")
    return wifiManager.connectionInfo
}

private fun mapWifiNetworks(results: List<WifiScanResult>): List<BrainBoxWifiNetwork> {
    return results
        .asSequence()
        .mapNotNull { result ->
            val ssid = result.SSID?.trim().orEmpty()
            if (ssid.isBlank()) {
                null
            } else {
                BrainBoxWifiNetwork(
                    ssid = ssid,
                    strengthLevel = wifiSignalLevel(result.level),
                    isSecure = result.capabilities?.contains("WEP", ignoreCase = true) == true ||
                        result.capabilities?.contains("WPA", ignoreCase = true) == true,
                )
            }
        }
        .groupBy { it.ssid }
        .mapNotNull { (_, items) -> items.maxByOrNull { it.strengthLevel } }
        .sortedByDescending { it.strengthLevel }
        .toList()
}

@Composable
actual fun rememberBrainBoxProvisionController(): BrainBoxProvisionController {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val bluetoothManager = remember(appContext) {
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    val wifiManager = remember(appContext) {
        appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    val connectivityManager = remember(appContext) {
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val bleDevicesState = remember { MutableStateFlow<List<BrainBoxBleDevice>>(emptyList()) }
    val isBleScanningState = remember { MutableStateFlow(false) }
    val wifiNetworksState = remember { MutableStateFlow<List<BrainBoxWifiNetwork>>(emptyList()) }
    val isWifiLoadingState = remember { MutableStateFlow(false) }
    val discoveredDevices = remember { linkedMapOf<String, BrainBoxBleDevice>() }

    var bluetoothPermissionGrantedState by remember { mutableStateOf(hasBluetoothPermissions(appContext)) }
    var bluetoothEnabledState by remember { mutableStateOf(bluetoothManager.adapter?.isEnabled == true) }
    var wifiPermissionGrantedState by remember { mutableStateOf(hasWifiPermissions(appContext)) }
    var pendingBluetoothPermission by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    var pendingWifiPermission by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    var activeWifiCallback by remember { mutableStateOf<ConnectivityManager.NetworkCallback?>(null) }

    val refreshState = {
        bluetoothPermissionGrantedState = hasBluetoothPermissions(appContext)
        bluetoothEnabledState = bluetoothManager.adapter?.isEnabled == true
        wifiPermissionGrantedState = hasWifiPermissions(appContext)
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            refreshState()
            pendingBluetoothPermission?.complete(bluetoothPermissionGrantedState)
            pendingBluetoothPermission = null
        }
    )

    val wifiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            refreshState()
            pendingWifiPermission?.complete(wifiPermissionGrantedState)
            pendingWifiPermission = null
        }
    )

    fun clearActiveWifiRequest() {
        val callback = activeWifiCallback ?: return
        runCatching {
            connectivityManager.unregisterNetworkCallback(callback)
        }
        activeWifiCallback = null
    }

    fun handleBleResult(result: ScanResult) {
        val address = runCatching { result.device.address }.getOrNull().orEmpty()
        if (address.isBlank()) return
        val deviceName = runCatching { result.device.name }.getOrNull().takeIf { !it.isNullOrBlank() }
            ?: result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
            ?: "Lucy ${address.takeLast(4)}"
        discoveredDevices[address] = BrainBoxBleDevice(
            id = address,
            name = deviceName,
            subtitle = address,
            rssi = result.rssi,
        )
        bleDevicesState.value = discoveredDevices.values.sortedByDescending { it.rssi ?: Int.MIN_VALUE }
        val currentDevices = bleDevicesState.value
        val deviceText = currentDevices.joinToString(separator = " | ") { device ->
            "name=${device.name}, id=${device.id}, subtitle=${device.subtitle}, rssi=${device.rssi}"
        }
        appLogD(BLE_SCAN_LOG_TAG, "Scanned BLE devices(${currentDevices.size}): $deviceText")
    }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleBleResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::handleBleResult)
            }

            override fun onScanFailed(errorCode: Int) {
                isBleScanningState.value = false
            }
        }
    }

    val stopBleScanRunnable = remember(scanCallback) {
        Runnable {
            runCatching {
                bluetoothManager.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
            isBleScanningState.value = false
        }
    }

    val controller = remember(appContext, bluetoothManager, wifiManager, connectivityManager, scanCallback) {
        object : BrainBoxProvisionController {
            override val bluetoothPermissionGranted: Boolean
                get() = bluetoothPermissionGrantedState

            override val bluetoothEnabled: Boolean
                get() = bluetoothEnabledState

            override val wifiPermissionGranted: Boolean
                get() = wifiPermissionGrantedState

            override val wifiMode: BrainBoxWifiMode = BrainBoxWifiMode.NearbyScan

            override val bleDevices = bleDevicesState

            override val isBleScanning = isBleScanningState

            override val wifiNetworks = wifiNetworksState

            override val isWifiLoading = isWifiLoadingState

            override suspend fun requestBluetoothPermission(): Boolean {
                refreshState()
                if (bluetoothPermissionGrantedState) return true
                pendingBluetoothPermission?.let { return it.await() }
                val deferred = CompletableDeferred<Boolean>()
                pendingBluetoothPermission = deferred
                bluetoothPermissionLauncher.launch(bluetoothPermissionList())
                return deferred.await()
            }

            override suspend fun requestWifiPermission(): Boolean {
                refreshState()
                if (wifiPermissionGrantedState) return true
                pendingWifiPermission?.let { return it.await() }
                val deferred = CompletableDeferred<Boolean>()
                pendingWifiPermission = deferred
                wifiPermissionLauncher.launch(wifiPermissionList())
                return deferred.await()
            }

            override fun openBluetoothSettings() {
                refreshState()
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(intent)
            }

            override fun openWifiSettings() {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(intent)
            }

            override fun startBleScan() {
                refreshState()
                if (!bluetoothPermissionGrantedState || !bluetoothEnabledState) {
                    isBleScanningState.value = false
                    return
                }
                discoveredDevices.clear()
                bleDevicesState.value = emptyList()
                mainHandler.removeCallbacks(stopBleScanRunnable)
                runCatching {
                    bluetoothManager.adapter?.bluetoothLeScanner?.startScan(scanCallback)
                }.onSuccess {
                    isBleScanningState.value = true
                    mainHandler.postDelayed(stopBleScanRunnable, 12_000L)
                }.onFailure {
                    isBleScanningState.value = false
                }
            }

            override fun stopBleScan() {
                mainHandler.removeCallbacks(stopBleScanRunnable)
                runCatching {
                    bluetoothManager.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
                }
                isBleScanningState.value = false
            }

            override suspend fun readCurrentPhoneWifi(): PhoneWifiState {
                refreshState()
                // 1) 不管权限怎样，WifiManager.isWifiEnabled 都能读。Wi‑Fi 被关 → 直接告诉 UI
                //    提示用户打开。
                val wifiEnabled = runCatching { wifiManager.isWifiEnabled }.getOrDefault(false)
                if (!wifiEnabled) {
                    println("[BrainBox] readCurrentPhoneWifi: wifi disabled")
                    return PhoneWifiState.Disabled
                }
                // 2) 要读到 SSID，Android 9+ 必须同时持有 ACCESS_FINE_LOCATION。先尝试授权；
                //    用户拒绝时仍然能落到下面的 `Unknown` 分支，UI 会回退到手动输入。
                val granted = if (!wifiPermissionGrantedState) {
                    requestWifiPermission()
                } else {
                    true
                }
                if (!granted) {
                    println("[BrainBox] readCurrentPhoneWifi: wifi permission denied -> Unknown")
                    return PhoneWifiState.Unknown
                }
                val info = readActiveWifiInfo(connectivityManager, wifiManager)
                val ssid = normalizeWifiSsid(info?.ssid)
                return if (ssid != null) {
                    println("[BrainBox] readCurrentPhoneWifi: connected ssid=$ssid")
                    PhoneWifiState.Connected(ssid)
                } else {
                    println("[BrainBox] readCurrentPhoneWifi: wifi on but ssid unreadable -> Unknown")
                    PhoneWifiState.Unknown
                }
            }

            override suspend fun refreshWifiNetworks(): Result<List<BrainBoxWifiNetwork>> {
                refreshState()
                if (!wifiPermissionGrantedState) {
                    return Result.failure(IllegalStateException("请先授权 Wi‑Fi 与定位权限"))
                }
                isWifiLoadingState.value = true
                return try {
                    @Suppress("DEPRECATION")
                    runCatching { wifiManager.startScan() }
                    delay(1200L)
                    val networks = mapWifiNetworks(wifiManager.scanResults.orEmpty())
                    wifiNetworksState.value = networks
                    Result.success(networks)
                } catch (error: Throwable) {
                    Result.failure(error)
                } finally {
                    isWifiLoadingState.value = false
                }
            }

            override suspend fun connectToWifi(ssid: String, password: String): Result<String> {
                refreshState()
                if (ssid.isBlank()) {
                    return Result.failure(IllegalArgumentException("请选择要连接的 Wi‑Fi"))
                }
                if (!wifiPermissionGrantedState) {
                    return Result.failure(IllegalStateException("请先授权 Wi‑Fi 与定位权限"))
                }
                return try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        suspendCancellableCoroutine { continuation ->
                            clearActiveWifiRequest()
                            val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(ssid)
                            if (password.isNotBlank()) {
                                specifierBuilder.setWpa2Passphrase(password)
                            }
                            val request = NetworkRequest.Builder()
                                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                                .setNetworkSpecifier(specifierBuilder.build())
                                .build()
                            val timeoutRunnable = Runnable {
                                if (!continuation.isActive) return@Runnable
                                clearActiveWifiRequest()
                                continuation.resume(Result.failure(IllegalStateException("连接 Wi‑Fi 超时，请重试")))
                            }
                            val callback = object : ConnectivityManager.NetworkCallback() {
                                override fun onAvailable(network: Network) {
                                    mainHandler.removeCallbacks(timeoutRunnable)
                                    activeWifiCallback = this
                                    if (continuation.isActive) {
                                        continuation.resume(Result.success("已连接到 $ssid"))
                                    }
                                }

                                override fun onUnavailable() {
                                    mainHandler.removeCallbacks(timeoutRunnable)
                                    clearActiveWifiRequest()
                                    if (continuation.isActive) {
                                        continuation.resume(Result.failure(IllegalStateException("系统未能连接到 $ssid")))
                                    }
                                }

                                override fun onLost(network: Network) {
                                    if (activeWifiCallback === this) {
                                        activeWifiCallback = null
                                    }
                                }
                            }
                            activeWifiCallback = callback
                            runCatching {
                                connectivityManager.requestNetwork(request, callback)
                            }.onSuccess {
                                mainHandler.postDelayed(timeoutRunnable, 15_000L)
                            }.onFailure {
                                clearActiveWifiRequest()
                                mainHandler.removeCallbacks(timeoutRunnable)
                                continuation.resume(Result.failure(it))
                            }
                            continuation.invokeOnCancellation {
                                mainHandler.removeCallbacks(timeoutRunnable)
                                if (activeWifiCallback === callback) {
                                    clearActiveWifiRequest()
                                }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val config = WifiConfiguration().apply {
                            SSID = "\"$ssid\""
                            if (password.isBlank()) {
                                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                            } else {
                                preSharedKey = "\"$password\""
                            }
                        }
                        @Suppress("DEPRECATION")
                        var networkId = wifiManager.addNetwork(config)
                        if (networkId == -1) {
                            @Suppress("DEPRECATION")
                            networkId = wifiManager.configuredNetworks
                                ?.firstOrNull { it.SSID == "\"$ssid\"" }
                                ?.networkId
                                ?: -1
                        }
                        if (networkId == -1) {
                            Result.failure(IllegalStateException("保存 Wi‑Fi 配置失败"))
                        } else {
                            @Suppress("DEPRECATION")
                            val enabled = wifiManager.enableNetwork(networkId, true)
                            @Suppress("DEPRECATION")
                            wifiManager.disconnect()
                            @Suppress("DEPRECATION")
                            val reconnected = wifiManager.reconnect()
                            if (enabled || reconnected) {
                                Result.success("已发起连接到 $ssid")
                            } else {
                                Result.failure(IllegalStateException("系统未能连接到 $ssid"))
                            }
                        }
                    }
                } catch (error: Throwable) {
                    Result.failure(error)
                }
            }
        }
    }

    DisposableEffect(appContext) {
        onDispose {
            mainHandler.removeCallbacks(stopBleScanRunnable)
            runCatching {
                bluetoothManager.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
            clearActiveWifiRequest()
            pendingBluetoothPermission?.complete(false)
            pendingWifiPermission?.complete(false)
        }
    }

    return controller
}

private const val BLE_SCAN_LOG_TAG = "BrainBoxBleScan"
