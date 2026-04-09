package com.cephalon.lucyApp.screens.nas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun NasDocumentDetailScreen(
    document: NasDocumentItem,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 顶部栏：返回按钮和标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color(0xFF3A3A3A)
                    )
                }

                // 标题
                Text(
                    text = document.name,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF222222)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 文档内容区域
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // 文档信息
                    DocumentInfoRow(
                        label = "文件名",
                        value = document.name
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DocumentInfoRow(
                        label = "格式",
                        value = document.format.uppercase()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DocumentInfoRow(
                        label = "大小",
                        value = "${document.sizeKB} KB"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DocumentInfoRow(
                        label = "时间",
                        value = document.time
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 分割线
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE5E5EA))
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 文档内容预览
                    Text(
                        text = "文档内容",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF222222)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "文档内容加载中...\n\n暂无可预览内容，请使用对应应用打开此文档。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 底部操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 发送朋友按钮
                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF222222)
                    )
                ) {
                    Text(
                        text = "发送朋友",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // 删除按钮
                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFFF3B30)
                    )
                ) {
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E8E93)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF222222)
        )
    }
}
