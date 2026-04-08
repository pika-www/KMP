package com.cephalon.lucyApp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.di.AppConfig
import kotlinx.coroutines.launch
import lucy.im.sdk.ConnectedSession
import lucy.im.sdk.LucyImAppClient
import lucy.im.sdk.LucyImAppConfig
import org.koin.compose.koinInject

private fun maskToken(token: String, keepStart: Int = 6, keepEnd: Int = 6): String {
    val t = token.trim()
    if (t.length <= keepStart + keepEnd + 3) return "***"
    return t.take(keepStart) + "***" + t.takeLast(keepEnd)
}

@Composable
fun SdkTestScreen(onBack: () -> Unit) {
    val tokenStore = koinInject<AuthTokenStore>()
    val scope = rememberCoroutineScope()
    val client = remember {
        LucyImAppClient(
            LucyImAppConfig(
                lucyServerBaseUrl = "${AppConfig.baseDomain}/aiden/lucy-server"
            )
        )
    }

    var session by remember { mutableStateOf<ConnectedSession?>(null) }
    var cdi by remember { mutableStateOf("test-device") }
    var payload by remember { mutableStateOf("""{"text":"hello from compose"}""") }
    var logText by remember { mutableStateOf("未连接") }
    var envLogText by remember { mutableStateOf("") }

    Scaffold(containerColor = Color(0xFFF1F1F1)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SDK 测试页面",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onBack) { Text("返回") }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "最小示例：读取登录 token -> connect -> publishToNpc",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "lucyServerBaseUrl: ${client.config.lucyServerBaseUrl}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (envLogText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = envLogText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF444444)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = cdi,
                onValueChange = { cdi = it },
                label = { Text("目标 CDI") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = payload,
                onValueChange = { payload = it },
                label = { Text("消息 JSON") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val token = tokenStore.getValidTokenOrNull()
                            if (token.isNullOrBlank()) {
                                logText = "连接失败：未找到有效登录 token"
                                envLogText =
                                    "token: <empty>\n" +
                                        "lucyServerBaseUrl: ${client.config.lucyServerBaseUrl}"
                                println("[SdkTest] lucyServerBaseUrl=${client.config.lucyServerBaseUrl}")
                                println("[SdkTest] token=<empty>")
                                return@launch
                            }

                            envLogText =
                                "token: ${maskToken(token)}\n" +
                                    "lucyServerBaseUrl: ${client.config.lucyServerBaseUrl}"
                            println("[SdkTest] lucyServerBaseUrl=${client.config.lucyServerBaseUrl}")
                            println("[SdkTest] token=${maskToken(token)}")

                            runCatching {
                                val newSession = client.connect(token)
                                session?.close()
                                session = newSession
                                logText = "连接成功，userId=${newSession.userId}"
                            }.onFailure { e ->
                                logText = "连接失败：${e.message ?: "unknown"}"
                            }
                        }
                    }
                ) { Text("连接 SDK") }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val activeSession = session
                            if (activeSession == null) {
                                logText = "请先连接 SDK"
                                return@launch
                            }
                            runCatching {
                                activeSession.publishToNpc(cdi = cdi, payload = payload)
                                logText = "发送成功 -> cdi=$cdi"
                            }.onFailure { e ->
                                logText = "发送失败：${e.message ?: "unknown"}"
                            }
                        }
                    }
                ) { Text("发送到 NPC") }

                OutlinedButton(
                    onClick = {
                        session?.close()
                        session = null
                        logText = "连接已关闭"
                    }
                ) { Text("关闭连接") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "状态：$logText",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
