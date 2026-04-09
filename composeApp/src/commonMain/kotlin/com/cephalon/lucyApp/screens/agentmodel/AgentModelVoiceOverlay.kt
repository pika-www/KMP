package com.cephalon.lucyApp.screens.agentmodel

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.time.currentTimeMillis
import kotlinx.coroutines.delay

@Composable
internal fun AgentModelVoiceRecordingOverlay(
    startedAtMillis: Long,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var elapsedSeconds by remember(startedAtMillis) {
        mutableLongStateOf(((currentTimeMillis() - startedAtMillis) / 1_000L).coerceAtLeast(0L))
    }

    LaunchedEffect(startedAtMillis) {
        while (true) {
            elapsedSeconds = ((currentTimeMillis() - startedAtMillis) / 1_000L).coerceAtLeast(0L)
            delay(250L)
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0x00000000),
            Color(0x66006CFF),
            Color(0xCC006CFF),
            Color(0xFF006CFF)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "录音时长",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            Text(
                text = formatVoiceDuration(elapsedSeconds),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x33000000))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                val bars = listOf(6, 10, 14, 10, 6, 12, 18, 12, 6, 10, 14, 10)
                bars.forEach { h ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(h.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoiceActionButton(
                    icon = Icons.Default.Close,
                    label = "取消",
                    onClick = onCancel
                )

                Spacer(modifier = Modifier.width(16.dp))

                VoiceActionButton(
                    icon = Icons.Default.Check,
                    label = "确认",
                    onClick = onConfirm
                )
            }
        }
    }
}

@Composable
private fun VoiceActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x33000000))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.18f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
    }
}

private fun formatVoiceDuration(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0L)
    val minutes = safeSeconds / 60L
    val seconds = safeSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
