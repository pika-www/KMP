package com.cephalon.lucyApp.screens.agentmodel

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_brain
import androidios.composeapp.generated.resources.ic_nas_storage
import androidios.composeapp.generated.resources.ic_sparkle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.ws.BalanceWsManager
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
internal fun AgentModelTopBar(
    title: String,
    subtitle: String,
    onOpenProfile: () -> Unit,
    onCall: () -> Unit,
    onPillClick: () -> Unit = {},
    hasMessages: Boolean = false,
    isDeviceOnline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    val balanceWsManager: BalanceWsManager = koinInject()
    val balanceData by balanceWsManager.balance.collectAsState()
    val totalBalance = (balanceData.balances["1"] ?: 0L) + (balanceData.balances["4"] ?: 0L)

    val pillShape = RoundedCornerShape(80.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = ds.sw(20.dp), vertical = ds.sh(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ── 左侧：脑花图标 + 标题 ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(ds.sm(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.08f),
                            spotColor = Color.Black.copy(alpha = 0.12f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.60f)
                                )
                            )
                        )
                        .border(
                            width = 0.67.dp,
                            color = Color.White.copy(alpha = 0.50f),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOpenProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_brain),
                        contentDescription = "Profile",
                        tint = Color(0xFF1F2535),
                        modifier = Modifier.size(ds.sm(14.dp))
                    )
                }
                if (isDeviceOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(ds.sm(8.dp))
                            .clip(CircleShape)
                            .background(Color(0xFF67C168))
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(ds.sw(12.dp)))

            Text(
                text = title,
                color = Color(0xFF1F2535),
                fontSize = ds.sp(16f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.64.sp
            )
        }

        // ── 右侧：余额胶囊 ──
        Box(
            modifier = Modifier
                .height(ds.sh(32.dp))
                .shadow(
                    elevation = 6.dp,
                    shape = pillShape,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.60f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.50f), pillShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onPillClick() }
                .padding(horizontal = ds.sw(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (hasMessages) Res.drawable.ic_nas_storage
                        else Res.drawable.ic_sparkle
                    ),
                    contentDescription = null,
                    tint = Color(0xFF1F2535),
                    modifier = Modifier.size(ds.sm(16.dp))
                )
                Spacer(modifier = Modifier.width(ds.sw(4.dp)))
                Text(
                    text = if (hasMessages) "NAS" else "$totalBalance",
                    color = Color(0xFF1F2535),
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.48.sp
                )
            }
        }
    }
}
