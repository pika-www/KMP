package com.cephalon.lucyApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = Color.Black
    ) { padding ->
        val horizontalGutter = 18.dp
        val subtleWhite = Color.White.copy(alpha = 0.60f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
        ) {
            // ── 返回按钮（使用模态窗同款 icon，暗底版） ──
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // ── 标题，距离 icon 44dp ──
            Spacer(modifier = Modifier.height(44.dp))
            Text(
                text = "扫码绑定 Channel",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalGutter),
            )

            // ── 副标题，距离标题 4dp ──
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "扫描自己的OPEN CLAW 控制台生成的链接二维码",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = subtleWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalGutter),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 扫码区域（左右铺满屏幕） ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center,
            ) {
                QrScannerView(
                    modifier = Modifier.fillMaxSize(),
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
                        fontSize = 14.sp,
                        color = subtleWhite,
                    )
                }

                when (scanState) {
                    ScanState.Idle -> Unit

                    ScanState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "绑定中...",
                                fontSize = 14.sp,
                                color = Color.White,
                            )
                        }
                    }

                    ScanState.Success -> {
                        Text(
                            text = "绑定成功",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4CAF50),
                        )
                    }

                    ScanState.Failure -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "绑定失败",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFE84026),
                            )
                            if (bindErrorMsg.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = bindErrorMsg,
                                    fontSize = 12.sp,
                                    color = subtleWhite,
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    bindErrorMsg = ""
                                    scanState = ScanState.Idle
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f))
                            ) {
                                Text("重试", color = Color.White)
                            }
                        }
                    }
                }
            }

            // ── 寻找二维码帮助文案，距离扫码区 32dp ──
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "如何找到您的OPEN CLAW 二维码",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = subtleWhite,
                modifier = Modifier.padding(horizontal = horizontalGutter),
            )

            // ── 点此查看 链接，距离上方 2dp ──
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "点此查看",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF2191EE),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(horizontal = horizontalGutter)
                    .clickable { uriHandler.openUri("https://github.com/HzTTT/lucy") },
            )

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
