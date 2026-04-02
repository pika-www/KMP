package com.example.androidios.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidios.ws.WsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val TEST_CORN_ID = "1f07ccda-a297-6930-b363-9040bfec5880"
private const val TEST_JWT_TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJJbmZvIjoiMTkzODg0NDkyMjg0NTk0NTg1NiIsImV4cCI6MTc3NTYxOTEzOH0.1HTPOkw9-SsVVo8yZOZ_3dm3kUUicniU0eQtjjMB2Mk"

@Composable
fun WsTestScreen(onBack: () -> Unit) {
    val wsRepository = koinInject<WsRepository>()
    val messages by wsRepository.messages.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF1F1F1)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "WS 测试页面",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "连接 ID: $TEST_CORN_ID",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF444444)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        scope.launch {
                            wsRepository.connect(TEST_CORN_ID)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("连接 WS")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            wsRepository.sendJwt(TEST_JWT_TOKEN)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("发送消息")
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            wsRepository.close()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("关闭 WS")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "WS 返回结果",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        TextButton(onClick = wsRepository::clearMessages) {
                            Text("清空")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF7F7F7), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (messages.isEmpty()) "暂无消息" else messages.joinToString("\n\n"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF222222)
                        )
                    }
                }
            }
        }
    }
}
