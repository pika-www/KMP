package com.cephalon.lucyApp.screens.brainbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.api.LucyDevice
import com.cephalon.lucyApp.brainbox.BrainBoxBleDevice
import com.cephalon.lucyApp.brainbox.BrainBoxProvisionController
import com.cephalon.lucyApp.brainbox.BrainBoxWifiMode
import com.cephalon.lucyApp.brainbox.BrainBoxWifiNetwork

@Composable
internal fun BrainBoxStepIndicator(currentIndex: Int) {
    val labels = listOf("扫描", "配网", "绑定")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            labels.forEachIndexed { index, _ ->
                BrainBoxStepDot(
                    isActive = index <= currentIndex,
                    modifier = Modifier.weight(1f),
                )
                if (index != labels.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                color = if (index < currentIndex) Color(0xFF4A4A4A) else Color(0xFFD6D6D6),
                                shape = RoundedCornerShape(999.dp),
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF555555),
                    )
                }
            }
        }
    }
}

@Composable
private fun BrainBoxStepDot(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isActive) Color(0xFF9E9E9E) else Color(0xFFE1E1E1),
                    shape = CircleShape,
                )
        )
    }
}

@Composable
internal fun BrainBoxScanStep(
    controller: BrainBoxProvisionController,
    devices: List<BrainBoxBleDevice>,
    isBleScanning: Boolean,
    selectedDevice: BrainBoxBleDevice?,
    onSelectDevice: (BrainBoxBleDevice) -> Unit,
    onRequestPermission: () -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!controller.bluetoothPermissionGranted || !controller.bluetoothEnabled) {
            BrainBoxActionCard(
                title = if (!controller.bluetoothPermissionGranted) "需要蓝牙权限" else "蓝牙未开启",
                body = if (!controller.bluetoothPermissionGranted) {
                    "请授权蓝牙权限以发现附近的脑花盒子设备。"
                } else {
                    "请先打开系统蓝牙后再开始扫描设备。"
                },
                primaryText = if (!controller.bluetoothPermissionGranted) "授权并扫描" else "打开蓝牙设置",
                onPrimary = {
                    if (!controller.bluetoothPermissionGranted) onRequestPermission() else controller.openBluetoothSettings()
                }
            )
        } else if (devices.isEmpty()) {
            BrainBoxActionCard(
                title = if (isBleScanning) "正在扫描附近设备" else "暂未发现设备",
                body = if (isBleScanning) {
                    "请将脑花盒子靠近手机并保持设备通电。"
                } else {
                    "可以重新发起扫描，或检查蓝牙与设备电源状态。"
                },
                primaryText = if (isBleScanning) "重新扫描" else "开始扫描",
                onPrimary = onRequestPermission,
                showLoading = isBleScanning,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(devices, key = { it.id }) { device ->
                    BrainBoxBleDeviceCard(
                        device = device,
                        selected = selectedDevice?.id == device.id,
                        onClick = { onSelectDevice(device) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onNext,
            enabled = selectedDevice != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2535)),
        ) {
            Text(text = "下一步：配网")
        }
    }
}

@Composable
internal fun BrainBoxWifiStep(
    wifiMode: BrainBoxWifiMode,
    wifiPermissionGranted: Boolean,
    openWifiSettings: () -> Unit,
    selectedDevice: BrainBoxBleDevice?,
    wifiNetworks: List<BrainBoxWifiNetwork>,
    isWifiLoading: Boolean,
    selectedWifiSsid: String,
    onSelectWifi: (String) -> Unit,
    manualSsid: String,
    onManualSsidChange: (String) -> Unit,
    wifiPassword: String,
    onWifiPasswordChange: (String) -> Unit,
    isConnectingWifi: Boolean,
    onRefreshWifi: () -> Unit,
    onConnectWifi: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        selectedDevice?.let {
            BrainBoxSelectedDevice(device = it)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (wifiMode == BrainBoxWifiMode.NearbyScan) {
            if (!wifiPermissionGranted) {
                BrainBoxActionCard(
                    title = "需要 Wi‑Fi 与定位权限",
                    body = "授权后才能查询附近 Wi‑Fi 并发起连接。",
                    primaryText = "授权并刷新",
                    onPrimary = onRefreshWifi,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "附近 Wi‑Fi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF111111),
                    )
                    TextButton(onClick = onRefreshWifi) {
                        Text(text = if (isWifiLoading) "刷新中" else "刷新")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (wifiNetworks.isEmpty()) {
                    BrainBoxActionCard(
                        title = if (isWifiLoading) "正在查询附近 Wi‑Fi" else "暂未获取到 Wi‑Fi 列表",
                        body = if (isWifiLoading) "请稍候…" else "你可以刷新重试，或直接去系统设置连接。",
                        primaryText = "打开系统 Wi‑Fi",
                        onPrimary = openWifiSettings,
                        showLoading = isWifiLoading,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(wifiNetworks, key = { it.ssid }) { network ->
                            BrainBoxWifiCard(
                                network = network,
                                selected = selectedWifiSsid == network.ssid,
                                onClick = { onSelectWifi(network.ssid) },
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "iOS 无法直接读取附近 Wi‑Fi，输入网络信息后会跳到系统设置完成连接。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                )
                OutlinedTextField(
                    value = manualSsid,
                    onValueChange = onManualSsidChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Wi‑Fi 名称") },
                    singleLine = true,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = wifiPassword,
            onValueChange = onWifiPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Wi‑Fi 密码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onConnectWifi,
            enabled = !isConnectingWifi,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2535)),
        ) {
            if (isConnectingWifi) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(text = "连接 Wi‑Fi")
            }
        }
    }
}

@Composable
internal fun BrainBoxBindStep(
    selectedDevice: BrainBoxBleDevice?,
    serverDevice: LucyDevice?,
    connectedWifi: String?,
    isLoadingDevices: Boolean,
    isBinding: Boolean,
    onBind: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoadingDevices) {
            BrainBoxActionCard(
                title = "正在同步设备信息",
                body = "请稍候，正在确认设备在线状态。",
                primaryText = "请稍候",
                onPrimary = {},
                enabled = false,
                showLoading = true,
            )
        } else {
            BrainBoxBindDeviceCard(
                selectedDevice = selectedDevice,
                serverDevice = serverDevice,
                connectedWifi = connectedWifi,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBind,
            enabled = connectedWifi != null && !isBinding && !isLoadingDevices,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2535)),
        ) {
            if (isBinding) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(text = "绑定并进入")
            }
        }
    }
}

