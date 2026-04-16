package com.cephalon.lucyApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.components.HalfModalBottomSheet
import com.cephalon.lucyApp.scan.playScanBeep
import com.cephalon.lucyApp.scan.QrScannerView
import com.cephalon.lucyApp.scan.rememberOpenAppSettings
import com.cephalon.lucyApp.scan.rememberCameraPermissionController
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private enum class ScanState {
    Idle,
    Loading,
    Success,
    Failure
}

/**
 * 从 QR 码内容中解析 lucy://bind?channel_device_id=...&otp=... 参数
 */
private fun parseLucyBindUrl(url: String): Pair<String, String>? {
    // 支持 lucy://bind?... 格式
    if (!url.startsWith("lucy://bind")) return null
    val queryStart = url.indexOf('?')
    if (queryStart < 0) return null
    val params = url.substring(queryStart + 1).split('&').associate { part ->
        val eqIdx = part.indexOf('=')
        if (eqIdx < 0) part to "" else part.substring(0, eqIdx) to part.substring(eqIdx + 1)
    }
    val cdi = params["channel_device_id"] ?: return null
    val otp = params["otp"] ?: return null
    if (cdi.isBlank() || otp.isBlank()) return null
    return cdi to otp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBindChannelScreen(
    onBack: () -> Unit,
    onScanSuccess: (cdi: String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    val cameraPermission = rememberCameraPermissionController()
    val openSettings = rememberOpenAppSettings()
    val authRepository: AuthRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()

    var scanState by remember { mutableStateOf(ScanState.Idle) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var bindErrorMsg by remember { mutableStateOf("") }
    var boundCdi by remember { mutableStateOf("") }
    val logs = remember {
        mutableStateListOf(
            "等待扫描二维码...",
            "二维码来源：OpenClaw 控制台生成（openclaw lucy auth-qrcode）。"
        )
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.hasPermission) {
            cameraPermission.requestPermission()
            kotlinx.coroutines.delay(500)
            if (!cameraPermission.hasPermission) {
                showPermissionDialog = true
            }
        }
    }

    // 绑定成功后自动跳转
    LaunchedEffect(scanState) {
        if (scanState == ScanState.Success && boundCdi.isNotBlank()) {
            delay(700)
            onScanSuccess(boundCdi)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F7)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "返回",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF111111)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "扫码绑定Channel",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111111)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E2E2))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "扫描自己的 OPEN CLAW 控制台生成的绑定二维码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "最小接入（按顺序执行）:\n" +
                            "openclaw plugins install @hzttt/lucy\n" +
                            "openclaw config set channels.lucy.enabled true\n" +
                            "openclaw config set channels.lucy.servers '[\\\"nats://chat.lucy.run:4222\\\"]' --strict-json\n\n" +
                            "生成绑定二维码:\n" +
                            "openclaw lucy auth-qrcode",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1F1F1F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(Color(0xFFDCDCDC), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                QrScannerView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(0.dp),
                    enabled = cameraPermission.hasPermission && (scanState == ScanState.Idle || scanState == ScanState.Failure),
                    onQrCodeScanned = { content ->
                        if (scanState != ScanState.Idle && scanState != ScanState.Failure) return@QrScannerView
                        playScanBeep()
                        logs.add(0, "扫码内容: $content")

                        val parsed = parseLucyBindUrl(content)
                        if (parsed == null) {
                            logs.add(0, "无法识别的二维码格式，需 lucy://bind?channel_device_id=...&otp=...")
                            bindErrorMsg = "无法识别的二维码"
                            scanState = ScanState.Failure
                            return@QrScannerView
                        }

                        val (cdi, otp) = parsed
                        logs.add(0, "解析成功: cdi=$cdi, otp=$otp")
                        scanState = ScanState.Loading

                        coroutineScope.launch {
                            logs.add(0, "正在调用绑定接口...")
                            val result = authRepository.bindDeviceWithOtp(otp)
                            result.fold(
                                onSuccess = { data ->
                                    val resultCdi = data.cdi.ifBlank { cdi }
                                    logs.add(0, "绑定成功! cdi=$resultCdi")
                                    boundCdi = resultCdi
                                    scanState = ScanState.Success
                                },
                                onFailure = { e ->
                                    logs.add(0, "绑定失败: ${e.message}")
                                    bindErrorMsg = e.message ?: "绑定失败"
                                    scanState = ScanState.Failure
                                }
                            )
                        }
                    }
                )

                if (!cameraPermission.hasPermission) {
                    Text(
                        text = "需要相机权限以扫码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7A7A7A)
                    )
                }

                when (scanState) {
                    ScanState.Idle -> {
                        Text(
                            text = "扫码区域",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7A7A7A)
                        )
                    }

                    ScanState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFF333333),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "绑定中...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF555555)
                            )
                        }
                    }

                    ScanState.Success -> {
                        Text(
                            text = "绑定成功",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1B5E20)
                        )
                    }

                    ScanState.Failure -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "绑定失败",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFB00020)
                            )
                            if (bindErrorMsg.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = bindErrorMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    bindErrorMsg = ""
                                    scanState = ScanState.Idle
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                            ) {
                                Text("重试")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "如何找到您的 OPEN CLAW 二维码",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF111111)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "https://github.com/HzTTT/lucy",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF111111),
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://github.com/HzTTT/lucy")
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "日志",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111111)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    logs.take(10).forEachIndexed { index, line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF222222)
                        )
                        if (index != logs.take(10).lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))
        }
    }

    if (showPermissionDialog && !cameraPermission.hasPermission) {
        HalfModalBottomSheet(
            isVisible = true,
            onDismissRequest = {
                showPermissionDialog = false
            },
            onDismissed = {
                showPermissionDialog = false
            },
            onBack = null,
            showBackButton = false,
            showCloseButton = false,
            showTopBar = false,
            containerShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = Color(0xFFF5F5F7),
            topPadding = 0.dp,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "需要相机权限",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111111)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "进入扫码绑定页需要使用相机进行二维码识别。请授权相机权限。",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("稍后")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        showPermissionDialog = false
                        openSettings()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("去授权")
                }
            }
        }
    }
}
