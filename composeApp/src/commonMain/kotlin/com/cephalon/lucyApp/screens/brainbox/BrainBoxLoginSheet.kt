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
import com.cephalon.lucyApp.brainbox.BrainBoxBleDevice
import com.cephalon.lucyApp.brainbox.BrainBoxWifiMode
import com.cephalon.lucyApp.brainbox.rememberBrainBoxProvisionController
import com.cephalon.lucyApp.components.HalfModalBottomSheet
import com.cephalon.lucyApp.components.ToastHost
import com.cephalon.lucyApp.components.rememberToastState
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
    onBindSuccess: () -> Unit,
) {
    val authRepository = koinInject<AuthRepository>()
    val controller = rememberBrainBoxProvisionController()
    val scope = rememberCoroutineScope()
    val toastState = rememberToastState()
    val bleDevices by controller.bleDevices.collectAsState()
    val isBleScanning by controller.isBleScanning.collectAsState()
    val wifiNetworks by controller.wifiNetworks.collectAsState()
    val isWifiLoading by controller.isWifiLoading.collectAsState()

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
        controller.stopBleScan()
    }

    fun goPrevious() {
        currentStep = when (currentStep) {
            BrainBoxStep.Scan -> BrainBoxStep.Scan
            BrainBoxStep.Wifi -> BrainBoxStep.Scan
            BrainBoxStep.Bind -> BrainBoxStep.Wifi
        }
    }

    val selectedWifiNetwork = wifiNetworks.firstOrNull { it.ssid == selectedWifiSsid }
    val selectedWifiName = when (controller.wifiMode) {
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
            controller.stopBleScan()
        }
    }

    LaunchedEffect(isVisible, currentStep, controller.bluetoothPermissionGranted, controller.bluetoothEnabled) {
        if (!isVisible) return@LaunchedEffect
        when (currentStep) {
            BrainBoxStep.Scan -> {
                val granted = controller.requestBluetoothPermission()
                if (granted && controller.bluetoothEnabled) {
                    controller.startBleScan()
                }
            }
            BrainBoxStep.Wifi -> {
                controller.stopBleScan()
                controller.requestWifiPermission()
                if (controller.wifiMode == BrainBoxWifiMode.NearbyScan && controller.wifiPermissionGranted) {
                    controller.refreshWifiNetworks().onFailure {
                        toastState.show(it.message ?: "查询附近 Wi‑Fi 失败")
                    }
                }
            }
            BrainBoxStep.Bind -> {
                controller.stopBleScan()
                isLoadingDevices = true
                serverDevices.clear()
                val devices = runCatching { authRepository.getDevices() }
                    .onFailure {
                        toastState.show(it.message ?: "获取设备列表失败")
                    }
                    .getOrDefault(emptyList())
                serverDevices.addAll(devices)
                isLoadingDevices = false
            }
        }
    }

    val displayDevice = remember(selectedBleDevice, serverDevices.toList(), wifiConnectedSsid) {
        serverDevices.firstOrNull { candidate ->
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
                controller.startBleScan()
            }
        }
    }

    fun refreshWifiAgain() {
        scope.launch {
            val granted = controller.requestWifiPermission()
            if (!granted) {
                toastState.show("请先授权 Wi‑Fi 与定位权限")
                return@launch
            }
            controller.refreshWifiNetworks().onFailure {
                toastState.show(it.message ?: "查询附近 Wi‑Fi 失败")
            }
        }
    }

    fun connectWifi() {
        val ssid = selectedWifiName
        val networkRequiresPassword = when (controller.wifiMode) {
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
            controller.connectToWifi(ssid = ssid, password = wifiPassword)
                .onSuccess {
                    wifiConnectedSsid = ssid
                    currentStep = BrainBoxStep.Bind
                    toastState.show(it)
                }
                .onFailure {
                    toastState.show(it.message ?: "连接 Wi‑Fi 失败")
                }
            isConnectingWifi = false
        }
    }

    fun bindAndContinue() {
        scope.launch {
            isBinding = true
            val success = runCatching { authRepository.setConnectionFlag() }
                .getOrDefault(false)
            isBinding = false
            if (success) {
                onBindSuccess()
            } else {
                toastState.show("绑定失败，请稍后重试")
            }
        }
    }

    HalfModalBottomSheet(
        isVisible = isVisible,
        onDismissRequest = {
            controller.stopBleScan()
            onDismiss()
        },
        onDismissed = {
            controller.stopBleScan()
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
                            controller.stopBleScan()
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
                            controller = controller,
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
                            onBind = ::bindAndContinue,
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

