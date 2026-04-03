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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController

@Composable
fun LocalDeployTestScreen(onBack: () -> Unit) {
    val logs = remember {
        mutableStateListOf(
            "点击下面的按钮测试系统能力。",
            "相机会先申请系统权限，图库和文件会通过系统选择器打开。",
            "录音会先申请麦克风权限，结束后可在下方播放。"
        )
    }
    val mediaAccessController = rememberPlatformMediaAccessController { message ->
        logs.add(0, message)
    }

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
                    text = "本地部署测试",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "这里可以测试相机、相册、文件选择和录音播放。",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF444444)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = mediaAccessController::openCamera,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("打开相机")
                }

                Button(
                    onClick = mediaAccessController::openGallery,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("选择图片")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = mediaAccessController::openFilePicker,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("选择系统文件")
                }

                Button(
                    onClick = mediaAccessController::toggleRecording,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (mediaAccessController.isRecording) "停止录音" else "开始录音")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = logs::clear,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清空记录")
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
                    if (mediaAccessController.recordings.isNotEmpty()) {
                        Text(
                            text = "录音结果",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = mediaAccessController.recordings,
                                key = { it.id }
                            ) { recording ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = recording.name,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                            )
                                            Text(
                                                text = recording.path,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF666666)
                                            )
                                        }

                                        OutlinedButton(onClick = { mediaAccessController.playRecording(recording) }) {
                                            Text("播放")
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF7F7F7), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (logs.isEmpty()) "暂无记录" else logs.joinToString("\n\n"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF222222)
                        )
                    }
                }
            }
        }
    }
}
