package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.api.LucyDevice
import com.cephalon.lucyApp.components.HalfModalBottomSheet
import com.cephalon.lucyApp.scan.rememberOpenWifiSettings

@Composable
internal fun WifiConfigSheet(
    isVisible: Boolean,
    device: LucyDevice?,
    onDismiss: () -> Unit,
) {
    val openWifiSettings = rememberOpenWifiSettings()

    HalfModalBottomSheet(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        onDismissed = {},
        showBackButton = true,
        onBack = onDismiss,
        showCloseButton = false,
        showTopBar = true,
        containerShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Color(0xFFF5F5F7),
        topPadding = 120.dp,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        if (device == null) return@HalfModalBottomSheet

        // ── 标题 ──
        Text(
            text = "配置WI-FI",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF111111)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 设备信息卡片 ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE6E6E6),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (device.name.firstOrNull() ?: 'D').uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF555555)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name.ifBlank { "设备" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF111111),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = device.serialNumber.ifBlank { device.id },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (device.status == "online" || device.status == "free") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "已连接",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF34C759),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 打开系统WiFi设置按钮 ──
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    openWifiSettings()
                    onDismiss()
                },
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1F2535)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "前往系统设置连接WIFI",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "点击后将跳转到系统WiFi设置页面，请在系统设置中完成WiFi连接后返回",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF999999)
        )
    }
}
