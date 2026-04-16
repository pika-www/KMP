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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.logging.appLogD
import com.cephalon.lucyApp.sdk.SdkSessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SdkTestScreen(onBack: () -> Unit) {
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val scope = rememberCoroutineScope()
    var targetCdi by remember { mutableStateOf("") }
    var textMessage by remember { mutableStateOf("你好") }
    var actionText by remember { mutableStateOf("") }
    var typedAssistantReply by remember { mutableStateOf("") }

    val onlineDevices by sdkSessionManager.onlineDevices.collectAsState()
    val assistantReplyText by sdkSessionManager.assistantReplyText.collectAsState()
    val assistantReplyStreaming by sdkSessionManager.assistantReplyStreaming.collectAsState()

    // 自动设置第一个可用设备的 CDI
    LaunchedEffect(onlineDevices) {
        if (targetCdi.isBlank() && onlineDevices.isNotEmpty()) {
            targetCdi = onlineDevices.first().cdi
        }
    }

    LaunchedEffect(actionText) {
        if (actionText.isNotBlank()) {
            appLogD("SdkTestScreen", "发送结果：$actionText")
        }
    }

    LaunchedEffect(assistantReplyText, assistantReplyStreaming) {
        if (assistantReplyText.isBlank()) {
            typedAssistantReply = ""
            return@LaunchedEffect
        }

        val current = typedAssistantReply
        val target = assistantReplyText
        val shouldType = assistantReplyStreaming && target.length >= current.length && target.startsWith(current)

        if (!shouldType) {
            typedAssistantReply = target
            return@LaunchedEffect
        }

        for (index in (current.length + 1)..target.length) {
            typedAssistantReply = target.take(index)
            delay(TYPEWRITER_DELAY_MS)
        }
    }

    Scaffold(containerColor = Color(0xFFF5F5F7)) { padding ->
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
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = targetCdi,
                onValueChange = { targetCdi = it },
                label = { Text("目标 CDI") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = textMessage,
                onValueChange = { textMessage = it },
                label = { Text("消息文本") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        sdkSessionManager.connectIfTokenValid()
                    }
                ) { Text("连接 SDK") }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            sdkSessionManager.publishTextToNpc(
                                cdi = targetCdi,
                                text = textMessage,
                            )
                                .onSuccess {
                                    actionText =
                                        "发送成功 -> cdi=$targetCdi, messageId=$it"
                                }
                                .onFailure { error ->
                                    actionText = "发送失败：${error.message ?: "unknown"}"
                                }
                        }
                    }
                ) { Text("发送到 NPC") }

                OutlinedButton(
                    onClick = {
                        sdkSessionManager.disconnect()
                    }
                ) { Text("关闭连接") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (onlineDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "在线 NPC 设备：",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = onlineDevices.joinToString("\n") { it.cdi },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (typedAssistantReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (assistantReplyStreaming) "NPC 回复（流式）" else "NPC 回复",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = typedAssistantReply,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private const val TYPEWRITER_DELAY_MS = 20L
