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
import androidx.compose.runtime.DisposableEffect
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
import com.cephalon.lucyApp.logging.appLogD
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import lucy.im.sdk.ConnectedSession
import lucy.im.sdk.LucyImAppClient
import lucy.im.sdk.LucyImAppConfig
import lucy.im.sdk.OnlineDevice
import lucy.im.sdk.OnlineNpcDeviceHandle
import lucy.im.sdk.collectDevices
import org.koin.compose.koinInject

private fun logd(message: String) {
    appLogD("SdkTestScreen", message)
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
    var payload by remember { mutableStateOf("""{"text":"hi"}""") }
    var logText by remember { mutableStateOf("未连接") }
    var consumerJob by remember { mutableStateOf<Job?>(null) }
    var consumerLogText by remember { mutableStateOf("未启动") }
    var deviceObserver by remember { mutableStateOf<OnlineNpcDeviceHandle?>(null) }
    var deviceObserverJob by remember { mutableStateOf<Job?>(null) }
    var onlineDevices by remember { mutableStateOf<List<OnlineDevice>>(emptyList()) }
    var deviceLogText by remember { mutableStateOf("未启动") }
    var receivedMessages by remember { mutableStateOf(listOf<String>()) }
    val consumerScope =
        remember {
            CoroutineScope(
                SupervisorJob() +
                    Dispatchers.Default +
                    CoroutineExceptionHandler { _, throwable ->
                        scope.launch {
                            val message = throwable.message ?: "unknown"
                            logd("consumer exception: $message")
                            consumerLogText = "监听异常：$message"
                            logText = "接收失败：$message"
                        }
                    }
            )
        }

    DisposableEffect(Unit) {
        onDispose {
            consumerJob?.cancel()
            deviceObserverJob?.cancel()
            deviceObserver?.stop()
            consumerScope.cancel()
            session?.close()
        }
    }

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
                                logd("token=<empty>")
                                return@launch
                            }

                            logd("token=$token")

                            runCatching {
                                val newSession = client.connect(token)
                                consumerJob?.cancel()
                                deviceObserverJob?.cancel()
                                deviceObserver?.stop()
                                session?.close()
                                session = newSession
                                val newObserver = newSession.startOnlineNpcDeviceObserver(scope = scope)
                                deviceObserver = newObserver
                                deviceObserverJob =
                                    newObserver.collectDevices(scope) { devices ->
                                        onlineDevices = devices
                                        deviceLogText = "已获取 ${devices.size} 台在线设备"
                                    }
                                consumerJob =
                                    newSession.startUserChannelConsumer(consumerScope) { subject, messagePayload ->
                                        val messageText =
                                            runCatching { messagePayload.decodeToString() }.getOrDefault(
                                                "<binary payload ${messagePayload.size} bytes>"
                                            )
                                        scope.launch {
                                            receivedMessages =
                                                listOf("subject=$subject\n$messageText") + receivedMessages
                                            consumerLogText =
                                                "监听中，累计接收 ${receivedMessages.size} 条"
                                        }
                                    }
                                consumerJob?.invokeOnCompletion { throwable ->
                                    if (throwable == null || throwable is CancellationException) return@invokeOnCompletion
                                    scope.launch {
                                        val message = throwable.message ?: "unknown"
                                        consumerLogText = "监听异常：$message"
                                        logText = "接收失败：$message"
                                    }
                                }
                                deviceObserverJob?.invokeOnCompletion { throwable ->
                                    if (throwable == null || throwable is CancellationException) return@invokeOnCompletion
                                    scope.launch {
                                        val message = throwable.message ?: "unknown"
                                        deviceLogText = "设备监听异常：$message"
                                    }
                                }
                                consumerLogText = "监听已启动"
                                deviceLogText = "设备监听已启动"
                                logText = "连接成功，userId=${newSession.userId}"
                            }.onFailure { e ->
                                logText = "连接失败：${e.message ?: "unknown"}"
                                consumerLogText = "监听启动失败"
                                deviceLogText = "设备监听启动失败"
                                logd("connect failed: ${e.message ?: "unknown"}")
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
                                logd("publish failed: ${e.message ?: "unknown"}")
                            }
                        }
                    }
                ) { Text("发送到 NPC") }

                OutlinedButton(
                    onClick = {
                        consumerJob?.cancel()
                        deviceObserverJob?.cancel()
                        deviceObserver?.stop()
                        consumerJob = null
                        deviceObserverJob = null
                        deviceObserver = null
                        onlineDevices = emptyList()
                        session?.close()
                        session = null
                        logText = "连接已关闭"
                        consumerLogText = "监听已停止"
                        deviceLogText = "设备监听已停止"
                    }
                ) { Text("关闭连接") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "状态：$logText",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "接收监听：$consumerLogText",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "设备监听：$deviceLogText",
                style = MaterialTheme.typography.bodyMedium
            )

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

            if (receivedMessages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "最近回复：",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = receivedMessages.take(20).joinToString("\n\n"),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
