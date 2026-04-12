package com.cephalon.lucyApp.screens

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
import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.ws.WsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val TEST_CORN_ID = "1f07ccda-a297-6930-b363-9040bfec5880"

@Composable
fun WsTestScreen(onBack: () -> Unit) {
    val wsRepository = koinInject<WsRepository>()
    val authTokenStore = koinInject<AuthTokenStore>()
    val messages by wsRepository.messages.collectAsState()
    val scope = rememberCoroutineScope()
    val currentToken = authTokenStore.getValidTokenOrNull()

    Scaffold(
        containerColor = Color(0xFFF5F5F7)
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

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (currentToken != null) "当前 Token: 已读取" else "当前 Token: 不存在或已过期",
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
                            val token = authTokenStore.getValidTokenOrNull()
                            if (token != null) {
                                wsRepository.sendJwt("Bearer $token")
                            }
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

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        scope.launch {
                            wsRepository.sendEvent(1)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("订阅钱包")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            wsRepository.sendEvent(2)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("订阅消息")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            wsRepository.sendEvent(3)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("订阅任务")
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
