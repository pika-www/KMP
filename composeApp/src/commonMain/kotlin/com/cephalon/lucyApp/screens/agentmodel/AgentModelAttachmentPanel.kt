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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.media.PlatformImageThumbnail

@Composable
internal fun AgentModelAttachmentPanel(
    recentImages: List<String>,
    hasMoreRecentImages: Boolean,
    onLoadMoreRecentImages: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onImagesSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    var expanded by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()

    // 收起时显示前 10 张，展开时显示全部已加载的图片
    val visibleImages = if (expanded) recentImages else recentImages.take(7)

    // 展开模式下，滚动到底部自动加载更多
    if (expanded && hasMoreRecentImages) {
        LaunchedEffect(gridState, hasMoreRecentImages) {
            snapshotFlow {
                val layoutInfo = gridState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleIndex >= totalItems - 8 // 提前 8 个 item 触发加载
            }.distinctUntilChanged().collect { shouldLoad ->
                if (shouldLoad) onLoadMoreRecentImages()
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val parentHeight = maxHeight
        val contentHeight = remember { mutableStateOf(0.dp) }
        val sheetHeight by animateDpAsState(
            targetValue = if (expanded) parentHeight * 0.8f else {
                maxOf(contentHeight.value, ds.sh(340.dp))
            },
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
                .padding(start = ds.sw(8.dp), end = ds.sw(8.dp), bottom = ds.sh(0.dp))
                .height(sheetHeight)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
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
                    .padding(horizontal = ds.sw(16.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = ds.sw(1.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .background(Color.White.copy(alpha = 0.60f))
//                    .padding(vertical = ds.sh(17.dp))
                ,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "上传照片",
                    color = Color(0xFF717580),
                    fontSize = ds.sp(14f),
                    fontWeight = FontWeight.Normal,
                    lineHeight = ds.sp(20f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "收起全部" else "查看全部",
                    color = Color(0xFF717580),
                    fontSize = ds.sp(14f),
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { expanded = !expanded }
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(12.dp)))

            // ── 图片网格 ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = ds.sw(16.dp),
                        end = ds.sw(16.dp),
                        bottom = ds.sh(0.dp) // Adjust to 0dp to control spacing manually
                    ),
                    horizontalArrangement = Arrangement.spacedBy(ds.sw(6.dp)),
                    verticalArrangement = Arrangement.spacedBy(ds.sh(6.dp))
                ) {
                    // 拍照按钮
                    item {
                        ActionTile(
                            icon = {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "拍照",
                                    tint = Color(0xFF3C3C3C),
                                    modifier = Modifier.size(ds.sm(24.dp))
                                )
                            },
                            label = "相机",
                            ds = ds,
                            onClick = onOpenCamera
                        )
                    }

                    // 图片列表
                    itemsIndexed(visibleImages) { _, uri ->
                        val isSelected = uri == selectedUri
                        ImageTile(
                            uri = uri,
                            isSelected = isSelected,
                            ds = ds,
                            onClick = {
                                if (isSelected) {
                                    // If already selected, deselect it
                                    selectedUri = ""
                                } else {
                                    // Deselect all others, select only this one and trigger upload
                                    selectedUri = uri
                                    onImagesSelected(listOf(uri))
                                }
                            }
                        )
                    }

                    // 加载中提示
                    if (expanded && hasMoreRecentImages) {
                        item(span = { GridItemSpan(4) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = ds.sh(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "加载更多...",
                                    color = Color(0xFF999999),
                                    fontSize = ds.sp(13f)
                                )
                            }
                        }
                    }
                }
            }
            // 确保图片网格底部到分割线总间距为24dp
            Spacer(modifier = Modifier.height(ds.sh(24.dp)))
            // 分割线
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ds.sw(16.dp))
                    .padding(top = ds.sh(0.dp), bottom = ds.sh(8.dp))
                    .border(width = 0.dp, color = Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onOpenFilePicker() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ds.sh(1.dp))
                        .background(Color(0xFFF0F0F0))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onOpenFilePicker() }
                    .padding(horizontal = ds.sw(16.dp), vertical = ds.sh(12.dp)).padding(bottom = ds.sh(40.dp)),

                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "上传系统文件",
                    color = Color(0xFF717580),
                    fontSize = ds.sp(14f),
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "上传系统文件",
                    tint = Color(0xFF717580),
                    modifier = Modifier.size(ds.sm(16.dp))
                )
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
