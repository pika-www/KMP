package com.example.androidios.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onOpenWsTest: () -> Unit,
    onOpenBrainBoxGuide: () -> Unit,
    onOpenLocalDeployTest: () -> Unit
) {
    val scrollState = rememberScrollState()
    val options = listOf(
        AccessOption(
            title = "脑花盒子用户",
            desc1 = "我拥有AI NPC/龙虾Pai，",
            desc2 = "我要登录"
        ),
        AccessOption(
            title = "端脑云用户",
            desc1 = "我没有龙虾，我要领养一只"
        ),
        AccessOption(
            title = "本地部署用户",
            desc1 = "我自己有本地龙虾，我要接入"
        )
    )

    Scaffold(
        containerColor = Color(0xFFF1F1F1)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "选择脑花的接入方式",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF121212)
                )
                Text(
                    text = "不同的接入方式决定了您的数据存储位置\n和算力来源",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1C1C1C)
                )

                Spacer(modifier = Modifier.height(6.dp))

                options.forEach { option ->
                    AccessOptionCard(
                        option = option,
                        onClick = {
                            when (option.title) {
                                "脑花盒子用户" -> onOpenBrainBoxGuide()
                                "端脑云用户" -> onOpenWsTest()
                                "本地部署用户" -> onOpenLocalDeployTest()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("退出登录")
                }
            }
        }
    }
}

private data class AccessOption(
    val title: String,
    val desc1: String,
    val desc2: String = ""
)

@Composable
private fun AccessOptionCard(
    option: AccessOption,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 108.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFD9D9D9), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFD1D1D1), RoundedCornerShape(16.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF101010)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(option.desc1)
                        if (option.desc2.isNotEmpty()) append('\n').append(option.desc2)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF212121)
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "进入",
                tint = Color(0xFF222222),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
