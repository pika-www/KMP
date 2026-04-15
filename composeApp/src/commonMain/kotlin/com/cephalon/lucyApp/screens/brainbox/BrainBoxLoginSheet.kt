package com.cephalon.lucyApp.screens.brainbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.LucyDevice
import com.cephalon.lucyApp.api.channelDeviceId
import com.cephalon.lucyApp.brainbox.BrainBoxBleDevice
import com.cephalon.lucyApp.brainbox.BrainBoxWifiMode
import com.cephalon.lucyApp.brainbox.BrainBoxWifiNetwork
import com.cephalon.lucyApp.brainbox.rememberBrainBoxProvisionController
import com.cephalon.lucyApp.components.HalfModalBottomSheet
import com.cephalon.lucyApp.components.ToastHost
import com.cephalon.lucyApp.components.rememberToastState
import com.cephalon.lucyApp.deviceaccess.BleScanDevice
import com.cephalon.lucyApp.deviceaccess.gatt.GattWifiNetwork
import com.cephalon.lucyApp.deviceaccess.gatt.ProvisionFlowStage
import com.cephalon.lucyApp.deviceaccess.gatt.rememberProvisionManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private enum class BrainBoxStep(
    val title: String,
    val subtitle: String,
) {
    Scan(
        title = "扫描设备",
        subtitle = "打开蓝牙权限并扫描附近的脑花盒子设备",
    ),
    Wifi(
        title = "连接Wi‑Fi",
        subtitle = "为选中的设备完成当前网络配置",
    ),
    Bind(
        title = "绑定Channel",
        subtitle = "将此设备绑定到你的脑花账号",
    ),
}

@Composable
fun BrainBoxLoginSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onBindSuccess: (cdi: String) -> Unit,
) {
    val authRepository = koinInject<AuthRepository>()
    val controller = rememberBrainBoxProvisionController()
    val provisionManager = rememberProvisionManager()
    val scope = rememberCoroutineScope()
    val toastState = rememberToastState()
    val bleScanState by provisionManager.scanState.collectAsState()
    val provisionState by provisionManager.state.collectAsState()
    val bleDevices = bleScanState.devices.map { it.toBrainBoxBleDevice() }
    val isBleScanning = bleScanState.isScanning
    val wifiNetworks = provisionState.wifiNetworks.map { it.toBrainBoxWifiNetwork() }
    val isWifiLoading = provisionState.stage == ProvisionFlowStage.Connecting ||
        provisionState.stage == ProvisionFlowStage.Discovering ||
        provisionState.stage == ProvisionFlowStage.ReadingPairingInfo ||
        provisionState.stage == ProvisionFlowStage.RequestingOtp ||
        provisionState.stage == ProvisionFlowStage.ScanningWifi

    var currentStep by remember { mutableStateOf(BrainBoxStep.Scan) }
    var selectedBleDevice by remember { mutableStateOf<BrainBoxBleDevice?>(null) }
    var selectedWifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var manualSsid by remember { mutableStateOf("") }
    var wifiConnectedSsid by remember { mutableStateOf<String?>(null) }
    var isConnectingWifi by remember { mutableStateOf(false) }
    var isBinding by remember { mutableStateOf(false) }
    var isLoadingDevices by remember { mutableStateOf(false) }
    val serverDevices = remember { mutableStateListOf<LucyDevice>() }

    fun resetState() {
        currentStep = BrainBoxStep.Scan
        selectedBleDevice = null
        selectedWifiSsid = ""
        wifiPassword = ""
        manualSsid = ""
        wifiConnectedSsid = null
        isConnectingWifi = false
        isBinding = false
        isLoadingDevices = false
        serverDevices.clear()
        provisionManager.stopScan()
        scope.launch { provisionManager.cancel() }
    }

    fun goPrevious() {
        currentStep = when (currentStep) {
            BrainBoxStep.Scan -> BrainBoxStep.Scan
            BrainBoxStep.Wifi -> BrainBoxStep.Scan
            BrainBoxStep.Bind -> BrainBoxStep.Wifi
        }
    }

    val wifiMode = BrainBoxWifiMode.NearbyScan
    val selectedWifiNetwork = wifiNetworks.firstOrNull { it.ssid == selectedWifiSsid }
    val selectedWifiName = when (wifiMode) {
        BrainBoxWifiMode.NearbyScan -> selectedWifiSsid.trim()
        BrainBoxWifiMode.ManualOnly -> manualSsid.trim()
    }
    val currentSheetStepIndex = when (currentStep) {
        BrainBoxStep.Scan -> 0
        BrainBoxStep.Wifi -> 1
        BrainBoxStep.Bind -> 2
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            resetState()
        } else {
            provisionManager.stopScan()
            provisionManager.cancel()
        }
    }

    // 蓝牙状态变化只影响扫描步骤
    LaunchedEffect(isVisible, currentStep, controller.bluetoothPermissionGranted, controller.bluetoothEnabled) {
        if (!isVisible) return@LaunchedEffect
        if (currentStep == BrainBoxStep.Scan) {
            val granted = controller.requestBluetoothPermission()
            if (granted && controller.bluetoothEnabled) {
                provisionManager.startScan().onFailure {
                    toastState.show(it.message ?: "开始扫描失败")
                }
            }
        }
    }

    fun requestOtpAndBind() {
        if (isBinding) return
        isBinding = true
        scope.launch {
            println("[BrainBox] 开始请求 OTP 并绑定...")
            val otpResult = provisionManager.requestOtpAfterWifi()
            val otp = otpResult.getOrNull()?.otp
            if (otpResult.isFailure || otp.isNullOrBlank()) {
                toastState.show(otpResult.exceptionOrNull()?.message ?: "获取 OTP 失败，请重试")
                isBinding = false
                return@launch
            }
            println("[BrainBox] OTP 获取成功: otp=$otp, 开始调用绑定接口...")
            authRepository.bindDeviceWithOtp(otp)
                .onSuccess { bindingData ->
                    println("[BrainBox] UI: 绑定成功 cdi=${bindingData.cdi}, status=${bindingData.status}, msg=${bindingData.serverMsg}")
                    toastState.show(bindingData.serverMsg.ifBlank { "绑定成功" })
                    onBindSuccess(bindingData.cdi)
                }
                .onFailure { error ->
                    toastState.show(error.message ?: "绑定失败，请稍后重试")
                }
            isBinding = false
        }
    }

    // WiFi / 绑定步骤只在 step 切换时执行一次，不受蓝牙状态重触发
    LaunchedEffect(isVisible, currentStep) {
        if (!isVisible) return@LaunchedEffect
        when (currentStep) {
            BrainBoxStep.Scan -> { /* 由上面的 LaunchedEffect 处理 */ }
            BrainBoxStep.Wifi -> {
                provisionManager.stopScan()
                val device = selectedBleDevice?.toBleScanDevice()
                if (device != null) {
                    // 先连接设备读取 device_info，判断是否已联网
                    provisionManager.connectDevice(device).onSuccess {
                        val deviceInfo = provisionManager.state.value.deviceInfo
                        println("[BrainBox] WiFi步骤检查: isConnected=${deviceInfo?.isConnected}, ip=${deviceInfo?.ip}, ssid=${deviceInfo?.ssid}, state=${deviceInfo?.state}")
                        if (deviceInfo != null && deviceInfo.isConnected && deviceInfo.ip.isNotBlank() && deviceInfo.ssid.isNotBlank()) {
                            println("[BrainBox] 设备已联网 (ssid=${deviceInfo.ssid}, ip=${deviceInfo.ip})，跳过配网，请求 OTP 并绑定...")
                            wifiConnectedSsid = deviceInfo.ssid
                            currentStep = BrainBoxStep.Bind
                            requestOtpAndBind()
                        } else {
                            // 设备未联网，正常扫描 WiFi
                            provisionManager.refreshWifiNetworks(device).onFailure {
                                toastState.show(it.message ?: "查询附近 Wi‑Fi 失败")
                            }
                        }
                    }.onFailure {
                        toastState.show(it.message ?: "连接设备失败")
                    }
                }
            }
            BrainBoxStep.Bind -> {
                provisionManager.stopScan()
            }
        }
    }

    val displayDevice = remember(selectedBleDevice, serverDevices.toList(), wifiConnectedSsid, provisionState.channelDeviceId) {
        serverDevices.firstOrNull { candidate ->
            candidate.channelDeviceId == provisionState.channelDeviceId
        } ?: serverDevices.firstOrNull { candidate ->
            val currentName = selectedBleDevice?.name.orEmpty()
            currentName.isNotBlank() && candidate.name.contains(currentName, ignoreCase = true)
        } ?: serverDevices.firstOrNull()
    }

    fun requestBluetoothAgain() {
        scope.launch {
            val granted = controller.requestBluetoothPermission()
            if (!granted) {
                toastState.show("请先授权蓝牙权限")
            } else if (!controller.bluetoothEnabled) {
                toastState.show("请先打开系统蓝牙")
            } else {
                provisionManager.startScan().onFailure {
                    toastState.show(it.message ?: "开始扫描失败")
                }
            }
        }
    }

    fun refreshWifiAgain() {
        scope.launch {
            val device = selectedBleDevice?.toBleScanDevice()
            if (device == null) {
                toastState.show("请先选择蓝牙设备")
                return@launch
            }
            provisionManager.refreshWifiNetworks(device).onFailure {
                toastState.show(it.message ?: "查询附近 Wi‑Fi 失败")
            }
        }
    }

    fun connectWifi() {
        val ssid = selectedWifiName
        val networkRequiresPassword = when (wifiMode) {
            BrainBoxWifiMode.NearbyScan -> selectedWifiNetwork?.isSecure == true
            BrainBoxWifiMode.ManualOnly -> true
        }
        if (ssid.isBlank()) {
            toastState.show("请选择要连接的 Wi‑Fi")
            return
        }
        if (networkRequiresPassword && wifiPassword.isBlank()) {
            toastState.show("请输入 Wi‑Fi 密码")
            return
        }
        scope.launch {
            isConnectingWifi = true
            provisionManager.configureWifi(ssid = ssid, password = wifiPassword)
                .onSuccess { networkStatus ->
                    wifiConnectedSsid = ssid
                    toastState.show("设备已连接到 ${networkStatus.ssid.ifBlank { ssid }}")
                    currentStep = BrainBoxStep.Bind
                    // WiFi 连接成功后请求 OTP 并绑定
                    requestOtpAndBind()
                }
                .onFailure {
                    toastState.show(it.message ?: "连接 Wi‑Fi 失败")
                }
            isConnectingWifi = false
        }
    }

    HalfModalBottomSheet(
        isVisible = isVisible,
        onDismissRequest = {
            provisionManager.stopScan()
            scope.launch { provisionManager.cancel() }
            onDismiss()
        },
        onDismissed = {
            provisionManager.stopScan()
        },
        showBackButton = false,
        showCloseButton = false,
        showTopBar = false,
        containerShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        containerColor = Color(0xFFF8F8F8),
        topPadding = 12.dp,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF333333),
                        modifier = Modifier.clickable {
                            provisionManager.stopScan()
                            scope.launch { provisionManager.cancel() }
                            onDismiss()
                        }
                    )
                    if (currentStep != BrainBoxStep.Scan) {
                        Text(
                            text = "上一步",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF333333),
                            modifier = Modifier.clickable { goPrevious() }
                        )
                    } else {
                        Spacer(modifier = Modifier.width(44.dp))
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                BrainBoxStepIndicator(currentIndex = currentSheetStepIndex)

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111111),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = currentStep.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF777777),
                )

                Spacer(modifier = Modifier.height(22.dp))

                when (currentStep) {
                    BrainBoxStep.Scan -> {
                        BrainBoxScanStep(
                            controller = controller,
                            devices = bleDevices,
                            isBleScanning = isBleScanning,
                            selectedDevice = selectedBleDevice,
                            onSelectDevice = { selectedBleDevice = it },
                            onRequestPermission = ::requestBluetoothAgain,
                            onNext = { currentStep = BrainBoxStep.Wifi },
                        )
                    }
                    BrainBoxStep.Wifi -> {
                        BrainBoxWifiStep(
                            wifiMode = wifiMode,
                            wifiPermissionGranted = true,
                            openWifiSettings = controller::openWifiSettings,
                            selectedDevice = selectedBleDevice,
                            wifiNetworks = wifiNetworks,
                            isWifiLoading = isWifiLoading,
                            selectedWifiSsid = selectedWifiSsid,
                            onSelectWifi = { selectedWifiSsid = it },
                            manualSsid = manualSsid,
                            onManualSsidChange = { manualSsid = it },
                            wifiPassword = wifiPassword,
                            onWifiPasswordChange = { wifiPassword = it },
                            isConnectingWifi = isConnectingWifi,
                            onRefreshWifi = ::refreshWifiAgain,
                            onConnectWifi = ::connectWifi,
                        )
                    }
                    BrainBoxStep.Bind -> {
                        BrainBoxBindStep(
                            selectedDevice = selectedBleDevice,
                            serverDevice = displayDevice,
                            connectedWifi = wifiConnectedSsid,
                            isLoadingDevices = isLoadingDevices,
                            isBinding = isBinding,
                            onBind = ::requestOtpAndBind,
                        )
                    }
                }
            }

            ToastHost(
                state = toastState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun BleScanDevice.toBrainBoxBleDevice(): BrainBoxBleDevice {
    return BrainBoxBleDevice(
        id = id,
        name = name,
        subtitle = subtitle,
        rssi = rssi,
    )
}

private fun BrainBoxBleDevice.toBleScanDevice(): BleScanDevice {
    return BleScanDevice(
        id = id,
        name = name,
        subtitle = subtitle,
        rssi = rssi,
    )
}

private fun GattWifiNetwork.toBrainBoxWifiNetwork(): BrainBoxWifiNetwork {
    return BrainBoxWifiNetwork(
        ssid = ssid,
        strengthLevel = when (val currentSignal = signal ?: 0) {
            in 80..100 -> 4
            in 60..79 -> 3
            in 40..59 -> 2
            in 20..39 -> 1
            else -> 0
        },
        isSecure = isSecure,
    )
}
