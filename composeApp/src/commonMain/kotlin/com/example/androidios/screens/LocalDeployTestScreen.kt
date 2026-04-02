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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidios.media.rememberPlatformMediaAccessController

@Composable
fun LocalDeployTestScreen(onBack: () -> Unit) {
    val logs = remember {
        mutableStateListOf(
            "点击下面的按钮测试系统能力。",
            "相机会先申请系统权限，图库和文件会通过系统选择器打开。"
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
                text = "这里可以测试相机、相册和系统文件选择。",
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

                OutlinedButton(
                    onClick = logs::clear,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("清空记录")
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
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
