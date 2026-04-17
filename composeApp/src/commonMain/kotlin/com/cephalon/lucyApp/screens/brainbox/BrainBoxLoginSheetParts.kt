package com.cephalon.lucyApp.screens.brainbox

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.api.LucyDevice
import com.cephalon.lucyApp.brainbox.BrainBoxBleDevice
import com.cephalon.lucyApp.brainbox.BrainBoxProvisionController
import com.cephalon.lucyApp.brainbox.BrainBoxWifiMode
import com.cephalon.lucyApp.brainbox.BrainBoxWifiNetwork

/* ═══════════════ Color tokens ═══════════════ */
private val TextDefault = Color(0xFF12192B)
private val TextLinkGrey = Color(0xFF595E6B)
private val TextDisabledGrey = Color(0xFFA6ABB5)
private val BlueLink = Color(0xFF1A73E9)
private val StopRed = Color(0xFFE84026)
private val StepActiveColor = Color(0xFF12192B)
private val StepInactiveColor = Color(0xFFCCCCCC)
private val StepLineActive = Color(0xFF12192B)
private val StepLineInactive = Color(0xFFE0E0E0)

/* ═══════════════ Step Indicator with Icons ═══════════════ */

@Composable
internal fun BrainBoxStepIndicator(currentIndex: Int) {
    val labels = listOf("扫描", "配网", "绑定")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center,
    ) {
        labels.forEachIndexed { index, label ->
            val isActive = index <= currentIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val color = if (isActive) StepActiveColor else StepInactiveColor
                        when (index) {
                            0 -> drawScanIcon(color)
                            1 -> drawWifiIcon(color)
                            2 -> drawBindIcon(color)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = TextLinkGrey,
                )
            }
            if (index != labels.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .weight(1f)
                        .height(1.dp)
                        .background(if (index < currentIndex) StepLineActive else StepLineInactive)
                )
            }
        }
    }
}

private fun DrawScope.drawScanIcon(color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    // center dot
    drawCircle(color = color, radius = r * 0.15f, center = Offset(cx, cy))
    // arcs (simplified signal waves)
    val stroke = Stroke(width = r * 0.12f, cap = StrokeCap.Round)
    drawArc(color = color, startAngle = -60f, sweepAngle = 120f, useCenter = false,
        topLeft = Offset(cx - r * 0.45f, cy - r * 0.45f),
        size = androidx.compose.ui.geometry.Size(r * 0.9f, r * 0.9f), style = stroke)
    drawArc(color = color, startAngle = 120f, sweepAngle = 120f, useCenter = false,
        topLeft = Offset(cx - r * 0.45f, cy - r * 0.45f),
        size = androidx.compose.ui.geometry.Size(r * 0.9f, r * 0.9f), style = stroke)
    drawArc(color = color, startAngle = -60f, sweepAngle = 120f, useCenter = false,
        topLeft = Offset(cx - r * 0.75f, cy - r * 0.75f),
        size = androidx.compose.ui.geometry.Size(r * 1.5f, r * 1.5f), style = stroke)
    drawArc(color = color, startAngle = 120f, sweepAngle = 120f, useCenter = false,
        topLeft = Offset(cx - r * 0.75f, cy - r * 0.75f),
        size = androidx.compose.ui.geometry.Size(r * 1.5f, r * 1.5f), style = stroke)
}

private fun DrawScope.drawWifiIcon(color: Color) {
    val cx = size.width / 2f
    val bottom = size.height * 0.85f
    // bottom dot
    drawCircle(color = color, radius = size.minDimension * 0.08f, center = Offset(cx, bottom))
    // arcs
    val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
    val radii = listOf(0.25f, 0.45f, 0.65f)
    for (r in radii) {
        val arcR = size.minDimension * r
        drawArc(color = color, startAngle = -145f, sweepAngle = 110f, useCenter = false,
            topLeft = Offset(cx - arcR, bottom - arcR),
            size = androidx.compose.ui.geometry.Size(arcR * 2, arcR * 2), style = stroke)
    }
}

private fun DrawScope.drawBindIcon(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round)
    // top-right chain link
    val path1 = Path().apply {
        moveTo(w * 0.45f, h * 0.35f)
        lineTo(w * 0.7f, h * 0.15f)
        lineTo(w * 0.85f, h * 0.35f)
        lineTo(w * 0.6f, h * 0.55f)
    }
    drawPath(path1, color = color, style = stroke)
    // bottom-left chain link
    val path2 = Path().apply {
        moveTo(w * 0.55f, h * 0.65f)
        lineTo(w * 0.3f, h * 0.85f)
        lineTo(w * 0.15f, h * 0.65f)
        lineTo(w * 0.4f, h * 0.45f)
    }
    drawPath(path2, color = color, style = stroke)
}

/* ═══════════════ Bluetooth Status Bar ═══════════════ */

@Composable
internal fun BluetoothStatusBar(
    isBleScanning: Boolean,
    onStopScan: () -> Unit,
    onStartScan: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF2F3F5),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // bluetooth icon box
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.Black, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (isBleScanning) {
                    SpinningBluetoothIcon(size = 13.dp)
                } else {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawBluetoothRune(Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isBleScanning) "正在蓝牙扫描设备" else "蓝牙扫描已停止",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextDefault,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isBleScanning) "停止扫描" else "开始扫描",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = if (isBleScanning) StopRed else BlueLink,
                textAlign = TextAlign.End,
                lineHeight = 16.sp,
                modifier = Modifier.clickable {
                    if (isBleScanning) onStopScan() else onStartScan()
                },
            )
        }
    }
}

@Composable
private fun SpinningBluetoothIcon(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    Canvas(
        modifier = Modifier
            .size(size)
            .rotate(rotation),
    ) {
        drawLoadingSpinner()
    }
}

private fun DrawScope.drawLoadingSpinner() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    val stroke = Stroke(width = r * 0.25f, cap = StrokeCap.Round)
    // draw partial arc as spinner
    drawArc(
        brush = Brush.sweepGradient(listOf(Color.Transparent, Color.White)),
        startAngle = 0f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.7f, cy - r * 0.7f),
        size = androidx.compose.ui.geometry.Size(r * 1.4f, r * 1.4f),
        style = stroke,
    )
}

private fun DrawScope.drawBluetoothRune(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.3f, h * 0.2f)
        lineTo(w * 0.7f, h * 0.5f)
        lineTo(w * 0.3f, h * 0.8f)
        moveTo(w * 0.5f, h * 0f)
        lineTo(w * 0.5f, h * 1f)
        moveTo(w * 0.7f, h * 0.2f)
        lineTo(w * 0.3f, h * 0.5f)
        lineTo(w * 0.7f, h * 0.8f)
    }
    drawPath(path, color = color, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round))
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
    Column(modifier = Modifier.fillMaxSize()) {
        if (!controller.bluetoothPermissionGranted || !controller.bluetoothEnabled) {
            // permission / bluetooth off
            Text(
                text = "蓝牙状态",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextLinkGrey,
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextLinkGrey,
            )
            Spacer(modifier = Modifier.height(16.dp))
            BluetoothStatusBar(
                isBleScanning = isBleScanning,
                onStopScan = onStopScan,
                onStartScan = onRequestPermission,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // discovered devices
            Text(
                text = "已发现的设备",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextLinkGrey,
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (devices.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.3f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isBleScanning) "正在搜索附近设备…" else "暂未发现设备",
                            fontSize = 14.sp,
                            color = TextLinkGrey,
                        )
                    }
                }
            } else {
                // glassmorphism device list card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "RSSI${device.rssi ?: "--"}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = metaColor,
            )
        }
        when {
            isConnecting -> CircularProgressIndicator(
                color = BlueLink,
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(16.dp),
            )
            isProbing -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = TextLinkGrey,
                    strokeWidth = 1.2.dp,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "检测中…",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextLinkGrey,
                    lineHeight = 16.sp,
                )
            }
            isOccupied -> Text(
                text = "已占用",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = TextDisabledGrey,
                lineHeight = 16.sp,
                textAlign = TextAlign.End,
            )
            else -> Text(
                text = "连接",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = BlueLink,
                lineHeight = 16.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.clickable(onClick = onConnect),
            )
        }
    }
}

/* ═══════════════ WiFi Step (kept mostly same, refined) ═══════════════ */

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

