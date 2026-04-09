package com.cephalon.lucyApp.screens

// TODO: 等 lucy-im-sdk-kotlin 子模块 SSH 权限就绪后恢复完整 SDK 测试页面

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SdkTestScreen(onBack: () -> Unit) {
    Scaffold(containerColor = Color(0xFFF1F1F1)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "lucy-im-sdk-kotlin 子模块尚未接入，\n请先配置 SSH 权限后执行:\ngit submodule update --init",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF888888),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
