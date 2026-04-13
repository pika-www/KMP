package com.cephalon.lucyApp.screens.agentmodel

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_skill_image
import androidios.composeapp.generated.resources.ic_skill_document
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.media.PlatformImageThumbnail

@Composable
internal fun AgentModelAttachmentPanel(
    recentImages: List<String>,
    onOpenCamera: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onImagesSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    var expanded by remember { mutableStateOf(false) }
    val selectedUris = remember { mutableStateListOf<String>() }

    // 默认显示 3 行 × 4 列 = 12 格，前 2 格是功能按钮，剩余 10 格放图片
    // expanded 时显示更多图片（80% 屏幕高度）
    val visibleImageCount = if (expanded) recentImages.size.coerceAtMost(50) else recentImages.size.coerceAtMost(10)
    val visibleImages = recentImages.take(visibleImageCount)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val parentHeight = maxHeight
        val sheetHeight by animateDpAsState(
            targetValue = if (expanded) parentHeight * 0.8f else parentHeight * 0.42f,
            animationSpec = tween(durationMillis = 300),
            label = "sheetHeight"
        )

        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )

        // 底部弹窗
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight)
                .clip(RoundedCornerShape(topStart = ds.sm(16.dp), topEnd = ds.sm(16.dp)))
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* 消费点击，阻止穿透到遮罩层 */ }
                .animateContentSize()
        ) {
            // ── 拖拽指示条 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ds.sh(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(ds.sw(36.dp))
                        .height(ds.sh(4.dp))
                        .clip(RoundedCornerShape(ds.sm(2.dp)))
                        .background(Color(0xFFDDDDDD))
                )
            }

            // ── 标题行：上传照片/文件 | 查看全部 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ds.sw(16.dp)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "上传照片/文件",
                    color = Color(0xFF1F2535),
                    fontSize = ds.sp(18f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "查看全部",
                    color = Color(0xFF2563EB),
                    fontSize = ds.sp(14f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = !expanded }
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(12.dp)))

            // ── 图片网格 ──
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = ds.sw(16.dp)),
                    horizontalArrangement = Arrangement.spacedBy(ds.sw(6.dp)),
                    verticalArrangement = Arrangement.spacedBy(ds.sh(6.dp))
                ) {
                    // 拍照按钮
                    item {
                        ActionTile(
                            icon = { Icon(Icons.Default.CameraAlt, contentDescription = "拍照", tint = Color(0xFF3C3C3C), modifier = Modifier.size(ds.sm(24.dp))) },
                            label = "相机",
                            ds = ds,
                            onClick = onOpenCamera
                        )
                    }
                    // 文件按钮
                    item {
                        ActionTile(
                            icon = { Icon(Icons.Default.FolderOpen, contentDescription = "文件", tint = Color(0xFF3C3C3C), modifier = Modifier.size(ds.sm(24.dp))) },
                            label = "文件",
                            ds = ds,
                            onClick = onOpenFilePicker
                        )
                    }
                    // 图片列表
                    itemsIndexed(visibleImages) { _, uri ->
                        val isSelected = uri in selectedUris
                        ImageTile(
                            uri = uri,
                            isSelected = isSelected,
                            ds = ds,
                            onClick = {
                                if (isSelected) selectedUris.remove(uri)
                                else selectedUris.add(uri)
                            }
                        )
                    }
                }
            }

            // ── 底部悬浮"添加照片"按钮（仅选中时显示）──
            if (selectedUris.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ds.sh(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val btnShape = RoundedCornerShape(100.dp)
                    Box(
                        modifier = Modifier
                            .width(ds.sw(271.dp))
                            .height(ds.sh(48.dp))
                            .clip(btnShape)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.06f),
                                shape = btnShape
                            )
                            .background(Color(0xFF010101).copy(alpha = 0.60f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onImagesSelected(selectedUris.toList())
                                selectedUris.clear()
                            }
                            .padding(horizontal = ds.sw(21.dp), vertical = ds.sh(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "添加照片",
                            color = Color.White,
                            fontSize = ds.sp(16f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    icon: @Composable () -> Unit,
    label: String,
    ds: com.cephalon.lucyApp.components.DesignScale,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(ds.sm(2.dp)))
            .background(Color(0xFFF2F2F2))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.height(ds.sh(4.dp)))
        Text(
            text = label,
            color = Color(0xFF3C3C3C),
            fontSize = ds.sp(11f),
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun ImageTile(
    uri: String,
    isSelected: Boolean,
    ds: com.cephalon.lucyApp.components.DesignScale,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(ds.sm(2.dp)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        PlatformImageThumbnail(
            uri = uri,
            modifier = Modifier.fillMaxSize()
        )
        // 选中后半透明遮罩
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }
        // 右下角勾选标记
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ds.sm(4.dp))
                    .size(ds.sm(20.dp))
                    .clip(CircleShape)
                    .background(Color(0xFF2192EF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(ds.sm(12.dp))
                )
            }
        }
    }
}
