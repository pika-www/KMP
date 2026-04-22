package com.cephalon.lucyApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.components.DesignScaleProvider
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.scan.playScanBeep
import com.cephalon.lucyApp.scan.QrScannerView
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

@Composable
fun ScanBindChannelScreen(
    onBack: () -> Unit,
    onScanSuccess: (cdi: String, onLoading: (Boolean) -> Unit) -> Unit,
    onOpenGuide: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val cameraPermission = rememberCameraPermissionController()
    val authRepository: AuthRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()

    var scanState by remember { mutableStateOf(ScanState.Idle) }
    var bindErrorMsg by remember { mutableStateOf("") }
    var boundCdi by remember { mutableStateOf("") }
    var isNavigating by remember { mutableStateOf(false) }
    val logs = remember {
        mutableStateListOf(
            "等待扫描二维码...",
            "二维码来源：OpenClaw 控制台生成（openclaw lucy auth-qrcode）。"
        )
    }

    // 绑定成功后自动跳转
    LaunchedEffect(scanState) {
        if (scanState == ScanState.Success && boundCdi.isNotBlank()) {
            delay(700)
            onScanSuccess(boundCdi) { isNavigating = it }
        }
    }

    DesignScaleProvider {
    val ds = LocalDesignScale.current

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        val horizontalGutter = ds.sw(18.dp)
        val subtleWhite = Color.White.copy(alpha = 0.60f)

        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
        ) {
            // ── 返回按钮（使用模态窗同款 icon，暗底版） ──
            Spacer(modifier = Modifier.height(ds.sh(12.dp)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ds.sw(12.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(ds.sm(40.dp))
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(ds.sm(22.dp)),
                    )
                }
            }

            // ── 标题，距离 icon 44dp ──
            Spacer(modifier = Modifier.height(ds.sh(44.dp)))
            Text(
                text = "扫码绑定 Channel",
                fontSize = ds.sp(24f),
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalGutter),
            )

            // ── 副标题，距离标题 4dp ──
            Spacer(modifier = Modifier.height(ds.sh(4.dp)))
            Text(
                text = "扫描自己的OPEN CLAW 控制台生成的链接二维码",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = subtleWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalGutter),
            )

            Spacer(modifier = Modifier.height(ds.sh(20.dp)))

            // ── 扫码区域（左右铺满屏幕） ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ds.sh(360.dp))
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

                when (scanState) {
                    ScanState.Idle -> Unit

                    ScanState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = ds.sm(2.dp),
                                modifier = Modifier.size(ds.sm(26.dp))
                            )
                            Spacer(modifier = Modifier.height(ds.sh(10.dp)))
                            Text(
                                text = "绑定中...",
                                fontSize = ds.sp(14f),
                                color = Color.White,
                            )
                        }
                    }

                    ScanState.Success -> {
                        Text(
                            text = "绑定成功",
                            fontSize = ds.sp(22f),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                        )
                    }

                    ScanState.Failure -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "绑定失败",
                                fontSize = ds.sp(22f),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE84026),
                            )
                            if (bindErrorMsg.isNotBlank()) {
                                Spacer(modifier = Modifier.height(ds.sh(4.dp)))
                                Text(
                                    text = bindErrorMsg,
                                    fontSize = ds.sp(12f),
                                    color = subtleWhite,
                                )
                            }
                            Spacer(modifier = Modifier.height(ds.sh(10.dp)))
                            Button(
                                onClick = {
                                    bindErrorMsg = ""
                                    scanState = ScanState.Idle
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f))
                            ) {
                                Text("重试", color = Color.White, fontSize = ds.sp(14f))
                            }
                        }
                    }
                }
            }

            // ── 寻找二维码帮助文案，距离扫码区 32dp ──
            Spacer(modifier = Modifier.height(ds.sh(32.dp)))
            Text(
                text = "如何找到您的OPEN CLAW 二维码",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = subtleWhite,
                modifier = Modifier.padding(horizontal = horizontalGutter),
            )

            // ── 点此查看 链接，距离上方 2dp ──
            Spacer(modifier = Modifier.height(ds.sh(2.dp)))
            Text(
                text = "点此查看",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF2191EE),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(horizontal = horizontalGutter)
                    .clickable { onOpenGuide() },
            )

            Spacer(modifier = Modifier.height(ds.sh(26.dp)))
        }

        // 绑定成功后跳转中 loading 遮罩
        if (isNavigating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { /* 拦截点击 */ },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        } // Box
    }
    } // DesignScaleProvider

}
