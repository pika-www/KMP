package com.cephalon.lucyApp.screens.agentmodel

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_logo
import androidios.composeapp.generated.resources.ic_nas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.components.LocalDesignScale
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun AgentModelTopBar(
    title: String,
    subtitle: String,
    onOpenProfile: () -> Unit,
    onPillClick: () -> Unit = {},
    isDeviceOnline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    val statusText = subtitle.ifBlank { if (isDeviceOnline) "设备在线" else "设备离线" }
    val statusColor = if (isDeviceOnline) Color(0xFF5BB66C) else Color.Black.copy(alpha = 0.60f)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color.White),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ds.sh(80.dp))
                .padding(start = ds.sw(20.dp), end = ds.sw(20.dp), top = ds.sh(28.dp)),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(ds.sm(36.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOpenProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_logo),
                        contentDescription = "Profile",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(ds.sm(36.dp))
                    )
                }

                Spacer(modifier = Modifier.width(ds.sw(12.dp)))

                Column(
                    modifier = Modifier
                        .height(ds.sm(36.dp))
                        .align(Alignment.CenterVertically),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title,
                        color = Color.Black.copy(alpha = 0.9f),
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = ds.sp(20f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ds.sm(4.dp))
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(ds.sw(6.dp)))
                        Text(
                            text = statusText,
                            color = Color.Black.copy(alpha = 0.60f),
                            fontSize = ds.sp(12f),
                            fontWeight = FontWeight.Medium,
                            lineHeight = ds.sp(16f),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(ds.sm(36.dp))
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFFF5F5F5))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPillClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_nas),
                    contentDescription = "Open NAS",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(ds.sm(20.dp))
                )
            }
        }
    }
}
