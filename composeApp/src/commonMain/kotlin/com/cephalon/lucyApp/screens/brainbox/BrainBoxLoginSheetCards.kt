package com.cephalon.lucyApp.screens.brainbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.cephalon.lucyApp.brainbox.BrainBoxBleDevice
import com.cephalon.lucyApp.brainbox.BrainBoxWifiNetwork

@Composable
internal fun BrainBoxActionCard(
    title: String,
    body: String,
    primaryText: String,
    onPrimary: () -> Unit,
    enabled: Boolean = true,
    showLoading: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFEDEDED),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111111),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onPrimary,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2535)),
            ) {
                if (showLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(text = primaryText)
                }
            }
        }
    }
}

@Composable
internal fun BrainBoxBleDeviceCard(
    device: BrainBoxBleDevice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) Color(0xFF1F2535) else Color(0xFFEDEDED),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = if (selected) Color.White.copy(alpha = 0.16f) else Color.White,
                        shape = RoundedCornerShape(16.dp),
                    )
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) Color.White else Color(0xFF111111),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = device.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) Color.White.copy(alpha = 0.72f) else Color(0xFF666666),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = device.rssi?.let { "${it}dBm" } ?: "--",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) Color.White else Color(0xFF666666),
            )
        }
    }
}

@Composable
internal fun BrainBoxSelectedDevice(device: BrainBoxBleDevice) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFEDEDED),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111111),
                )
                Text(
                    text = device.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF777777),
                )
            }
        }
    }
}

@Composable
internal fun BrainBoxWifiCard(
    network: BrainBoxWifiNetwork,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color(0xFF1F2535) else Color(0xFFEDEDED),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = network.ssid,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) Color.White else Color(0xFF111111),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (network.isSecure) "已加密网络" else "开放网络",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) Color.White.copy(alpha = 0.74f) else Color(0xFF777777),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${network.strengthLevel}/4",
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Color.White else Color(0xFF777777),
            )
        }
    }
}

@Composable
internal fun BrainBoxBindDeviceCard(
    selectedDevice: BrainBoxBleDevice?,
    serverDevice: LucyDevice?,
    connectedWifi: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFE9E9E9),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = serverDevice?.name?.ifBlank { selectedDevice?.name ?: "Lucy" }
                            ?: selectedDevice?.name
                            ?: "Lucy",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111111),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                    ) {
                        Text(
                            text = if (connectedWifi.isNullOrBlank()) "待联网" else "已联网",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF1F2535),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = connectedWifi ?: serverDevice?.serialNumber?.ifBlank { selectedDevice?.subtitle.orEmpty() }.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Device ID",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF444444),
                    )
                    Text(
                        text = serverDevice?.serialNumber?.ifBlank { serverDevice.id }
                            ?: selectedDevice?.id
                            ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF444444),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
