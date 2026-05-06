package com.cephalon.lucyApp.screens.brainbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.cephalon.lucyApp.api.LucyDevice
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.components.PasswordInput
import com.cephalon.lucyApp.brainbox.BrainBoxBleDevice
import com.cephalon.lucyApp.brainbox.BrainBoxProvisionController

/* ═══════════════ Color tokens ═══════════════ */
private val TextDefault = Color(0xFF12192B)
private val TextLinkGrey = Color(0xFF595E6B)
private val TextDisabledGrey = Color(0xFFA6ABB5)
private val BlueLink = Color(0xFF1A73E9)
private val StopRed = Color(0xFFE84026)

/* ═══════════════ Step Indicator with Icons ═══════════════ */

@Composable
internal fun BrainBoxStepIndicator(
    currentIndex: Int,
    horizontalPadding: Dp = 28.dp,
) {
    val ds = LocalDesignScale.current
    val labels = listOf("扫描", "配网", "绑定")
    val iconFrameSize = ds.sm(36.dp)
    val iconSize = ds.sm(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center,
    ) {
        labels.forEachIndexed { index, label ->
            val isCurrent = index == currentIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // icon outer frame: 36×36, rounded circle, border + background
                Box(
                    modifier = Modifier
                        .size(iconFrameSize)
                        .clip(CircleShape)
                        .background(
                            if (isCurrent) Color(0xFF1E2434)
                            else Color.White.copy(alpha = 0.10f)
                        )
                        .border(
                            width = 0.75.dp,
                            color = Color.White.copy(alpha = 0.06f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val iconColor = if (isCurrent) Color.White else Color(0xFF1F2535)
                    val icon = when (index) {
                        0 -> ScanStepIcon
                        1 -> WifiStepIcon
                        else -> BindStepIcon
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = iconColor,
                    )
                }
                Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                Text(
                    text = label,
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.Light,
                    color = TextLinkGrey,
                )
            }
            if (index != labels.lastIndex) {
                // connector line: 12dp gap from icon, 0.5dp height, rounded, rgba(18,25,43,0.10)
                Spacer(modifier = Modifier.width(ds.sw(12.dp)))
                Box(
                    modifier = Modifier
                        .padding(top = iconFrameSize / 2)
                        .weight(1f)
                        .height(0.5.dp)
                        .clip(RoundedCornerShape(ds.sm(10.dp)))
                        .background(Color(0xFF12192B).copy(alpha = 0.10f))
                )
                Spacer(modifier = Modifier.width(ds.sw(12.dp)))
            }
        }
    }
}


/* ═══════════════ Bluetooth Status Bar ═══════════════ */

@Composable
internal fun BluetoothStatusBar(
    isBleScanning: Boolean,
    onStopScan: () -> Unit,
    onStartScan: () -> Unit,
) {
    val ds = LocalDesignScale.current
    val pillShape = RoundedCornerShape(ds.sm(99.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 15.dp,
                shape = pillShape,
                ambientColor = Color.Black.copy(alpha = 0.025f),
                spotColor = Color.Black.copy(alpha = 0.025f),
            )
            .clip(pillShape)
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sw(16.dp), vertical = ds.sh(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // bluetooth icon box
            val btBoxBg = if (isBleScanning) Color(0xFF1F2535) else Color(0xFF1F2535).copy(alpha = 0.08f)
            val btIconColor = if (isBleScanning) Color.White else Color(0xFF1F2535)
            Box(
                modifier = Modifier
                    .size(ds.sm(20.dp))
                    .background(btBoxBg, RoundedCornerShape(ds.sm(6.dp))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BtIcon,
                    contentDescription = null,
                    modifier = Modifier.size(ds.sm(10.dp)),
                    tint = btIconColor,
                )
            }

            Spacer(modifier = Modifier.width(ds.sw(8.dp)))

            Text(
                text = if (isBleScanning) "正在蓝牙扫描设备" else "蓝牙扫描已停止",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = TextDefault,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(ds.sw(8.dp)))

            Text(
                text = if (isBleScanning) "停止扫描" else "开始扫描",
                fontSize = ds.sp(12f),
                fontWeight = FontWeight.Normal,
                color = if (isBleScanning) StopRed else BlueLink,
                textAlign = TextAlign.End,
                lineHeight = ds.sp(16f),
                modifier = Modifier.clickable {
                    if (isBleScanning) onStopScan() else onStartScan()
                },
            )
        }
    }
}


/* ═══════════════ Scan Step (redesigned) ═══════════════ */

@Composable
internal fun BrainBoxScanStep(
    controller: BrainBoxProvisionController,
    devices: List<BrainBoxBleDevice>,
    isBleScanning: Boolean,
    selectedDevice: BrainBoxBleDevice?,
    onSelectDevice: (BrainBoxBleDevice) -> Unit,
    onRequestPermission: () -> Unit,
    onNext: () -> Unit,
    probeStates: Map<String, DeviceProbeState> = emptyMap(),
    connectingDeviceId: String? = null,
    onStopScan: () -> Unit = {},
) {
    val ds = LocalDesignScale.current
    Column(modifier = Modifier.fillMaxSize()) {
        if (!controller.bluetoothPermissionGranted || !controller.bluetoothEnabled) {
            // permission / bluetooth off
            Text(
                text = "蓝牙状态",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = TextLinkGrey,
            )
            Spacer(modifier = Modifier.height(ds.sh(16.dp)))
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
        } else {
            // bluetooth status
            Text(
                text = "蓝牙状态",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = TextLinkGrey,
            )
            Spacer(modifier = Modifier.height(ds.sh(16.dp)))
            BluetoothStatusBar(
                isBleScanning = isBleScanning,
                onStopScan = onStopScan,
                onStartScan = onRequestPermission,
            )

            Spacer(modifier = Modifier.height(ds.sh(24.dp)))

            // discovered devices
            Text(
                text = "已发现的设备",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = TextLinkGrey,
            )
            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            val cardShape = RoundedCornerShape(ds.sm(16.dp))
            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 15.dp,
                            shape = cardShape,
                            ambientColor = Color.Black.copy(alpha = 0.025f),
                            spotColor = Color.Black.copy(alpha = 0.025f),
                        )
                        .clip(cardShape)
                        .background(Color.White)
                        .padding(vertical = ds.sh(40.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isBleScanning) "正在搜索附近设备…" else "暂未发现设备",
                        fontSize = ds.sp(14f),
                        color = TextLinkGrey,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .shadow(
                            elevation = 15.dp,
                            shape = cardShape,
                            ambientColor = Color.Black.copy(alpha = 0.025f),
                            spotColor = Color.Black.copy(alpha = 0.025f),
                        )
                        .clip(cardShape)
                        .background(Color.White),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ds.sw(16.dp), vertical = ds.sh(20.dp)),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(devices, key = { it.id }) { device ->
                            BrainBoxBleDeviceRow(
                                device = device,
                                isConnecting = connectingDeviceId == device.id,
                                probeState = probeStates[device.id],
                                onConnect = {
                                    onSelectDevice(device)
                                    onNext()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ═══════════════ Single device row in scan list ═══════════════ */

@Composable
private fun BrainBoxBleDeviceRow(
    device: BrainBoxBleDevice,
    isConnecting: Boolean,
    probeState: DeviceProbeState?,
    onConnect: () -> Unit,
) {
    // 未放入 map（null）或 Probing：后台还在读 pairing_info
    val isProbing = probeState == null || probeState is DeviceProbeState.Probing
    val isOccupied = probeState is DeviceProbeState.Occupied
    val nameColor = if (isOccupied) TextDisabledGrey else TextDefault
    val metaColor = if (isOccupied) TextDisabledGrey else TextLinkGrey
    val ds = LocalDesignScale.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ds.sh(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Medium,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(ds.sh(4.dp)))
            Text(
                text = "RSSI${device.rssi ?: "--"}",
                fontSize = ds.sp(12f),
                fontWeight = FontWeight.Normal,
                color = metaColor,
            )
        }
        when {
            isConnecting -> CircularProgressIndicator(
                color = BlueLink,
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(ds.sm(16.dp)),
            )
            isProbing -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = TextLinkGrey,
                    strokeWidth = 1.2.dp,
                    modifier = Modifier.size(ds.sm(12.dp)),
                )
                Spacer(modifier = Modifier.width(ds.sw(6.dp)))
                Text(
                    text = "\u68c0\u6d4b\u4e2d\u2026",
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.Normal,
                    color = TextLinkGrey,
                    lineHeight = ds.sp(16f),
                )
            }
            isOccupied -> Text(
                text = "\u5df2\u5360\u7528",
                fontSize = ds.sp(12f),
                fontWeight = FontWeight.Normal,
                color = TextDisabledGrey,
                lineHeight = ds.sp(16f),
                textAlign = TextAlign.End,
            )
            else -> Text(
                text = "\u8fde\u63a5",
                fontSize = ds.sp(12f),
                fontWeight = FontWeight.Normal,
                color = BlueLink,
                lineHeight = ds.sp(16f),
                textAlign = TextAlign.End,
                modifier = Modifier.clickable(onClick = onConnect),
            )
        }
    }
}

/* ═══════════════ WiFi Step (redesigned) ═══════════════ */

@Composable
internal fun BrainBoxWifiStep(
    selectedDevice: BrainBoxBleDevice?,
    deviceSsid: String?,
    phoneSsid: String?,
    wifiPassword: String,
    onWifiPasswordChange: (String) -> Unit,
    isConnectingWifi: Boolean,
    onConnectWifi: () -> Unit,
    isSelectedCurrent: Boolean = false,
) {
    val ds = LocalDesignScale.current
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── 设备卡片 ──
        selectedDevice?.let { device ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 15.dp,
                        shape = RoundedCornerShape(ds.sm(16.dp)),
                        ambientColor = Color.Black.copy(alpha = 0.025f),
                        spotColor = Color.Black.copy(alpha = 0.025f),
                    )
                    .clip(RoundedCornerShape(ds.sm(16.dp)))
                    .background(Color.White)
                    .padding(ds.sm(16.dp)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(ds.sm(40.dp))
                            .clip(RoundedCornerShape(ds.sm(12.dp)))
                            .background(Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = DeviceBoxIcon20,
                            contentDescription = null,
                            modifier = Modifier.size(ds.sm(20.dp)),
                            tint = Color.Unspecified,
                        )
                    }
                    Spacer(modifier = Modifier.width(ds.sw(12.dp)))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name,
                            fontSize = ds.sp(16f),
                            fontWeight = FontWeight.Medium,
                            color = Color.Black.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(ds.sh(2.dp)))
                        Text(
                            text = if (deviceSsid != null) "设备 WIFI $deviceSsid"
                                   else "设备 WIFI 读取中",
                            fontSize = ds.sp(12f),
                            fontWeight = FontWeight.Normal,
                            color = Color.Black.copy(alpha = 0.60f),
                            lineHeight = ds.sp(16f),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(ds.sh(16.dp)))
        }

        // ── 当前手机 Wi‑Fi 卡片 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ds.sm(16.dp)))
                .background(Color.White)
                .padding(
                    start = ds.sw(16.dp),
                    top = ds.sh(16.dp),
                    end = ds.sw(16.dp),
                    bottom = ds.sh(24.dp),
                ),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(ds.sm(40.dp))
                            .shadow(
                                elevation = 15.dp,
                                shape = RoundedCornerShape(ds.sm(12.dp)),
                                ambientColor = Color.Black.copy(alpha = 0.025f),
                                spotColor = Color.Black.copy(alpha = 0.025f),
                            )
                            .clip(RoundedCornerShape(ds.sm(12.dp)))
                            .background(Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = WifiIcon20,
                            contentDescription = null,
                            modifier = Modifier.size(ds.sm(20.dp)),
                            tint = Color.Unspecified,
                        )
                    }
                    Spacer(modifier = Modifier.width(ds.sw(12.dp)))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "当前手机 Wi‑Fi",
                            fontSize = ds.sp(16f),
                            fontWeight = FontWeight.Medium,
                            color = Color.Black.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(ds.sh(2.dp)))
                        Text(
                            text = phoneSsid ?: "检测中",
                            fontSize = ds.sp(12f),
                            fontWeight = FontWeight.Normal,
                            color = Color.Black.copy(alpha = 0.60f),
                            lineHeight = ds.sp(16f),
                        )
                    }
                }

                if (!isSelectedCurrent) {
                    Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                    // ── 密码输入框 ──
                    BasicTextField(
                        value = wifiPassword,
                        onValueChange = onWifiPasswordChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = ds.sp(14f),
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF12192B),
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onConnectWifi() }),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(ds.sm(99.dp)))
                                    .background(Color.Black.copy(alpha = 0.05f))
                                    .padding(horizontal = ds.sw(16.dp), vertical = ds.sh(12.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (wifiPassword.isEmpty()) {
                                        Text(
                                            text = "输入 Wi-Fi 密码",
                                            fontSize = ds.sp(14f),
                                            fontWeight = FontWeight.Normal,
                                            color = Color.Black.copy(alpha = 0.30f),
                                        )
                                    }
                                    innerTextField()
                                }
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Black.copy(alpha = 0.30f),
                                    modifier = Modifier
                                        .size(ds.sm(20.dp))
                                        .clickable { passwordVisible = !passwordVisible },
                                )
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                // ── 配置网络按钮 ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ds.sh(40.dp))
                        .clip(RoundedCornerShape(ds.sm(80.dp)))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(ds.sm(80.dp)))
                        .background(Color.Black.copy(alpha = 0.90f))
                        .clickable(enabled = !isConnectingWifi) { onConnectWifi() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isConnectingWifi) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(ds.sm(18.dp)),
                        )
                    } else {
                        Text(
                            text = if (isSelectedCurrent) "使用当前 Wi‑Fi 并继续" else "配置网络",
                            fontSize = ds.sp(16f),
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 玻璃态卡片：padding 16, radius 16, 1px white border,
 * layered gradient + overlay background, inner/outer shadows.
 */
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val ds = LocalDesignScale.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ds.sm(16.dp)))
            .border(
                width = 1.dp,
                color = Color.White,
                shape = RoundedCornerShape(ds.sm(16.dp)),
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.10f),
                    )
                )
            ),
    ) {
        content()
    }
}

/* ═══════════════ Bind Step ═══════════════ */

@Composable
internal fun BrainBoxBindStep(
    selectedDevice: BrainBoxBleDevice?,
    serverDevice: LucyDevice?,
    connectedWifi: String?,
    isLoadingDevices: Boolean,
    isBinding: Boolean,
    onBind: () -> Unit,
) {
    val ds = LocalDesignScale.current
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
                .height(ds.sh(52.dp)),
            shape = RoundedCornerShape(ds.sm(18.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2535)),
        ) {
            if (isBinding) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(ds.sm(18.dp)),
                )
            } else {
                Text(text = "绑定")
            }
        }
    }
}

