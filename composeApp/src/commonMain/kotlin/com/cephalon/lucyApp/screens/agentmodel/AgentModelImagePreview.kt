package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.components.BlobImage
import com.cephalon.lucyApp.components.LocalDesignScale
import kotlinx.coroutines.launch
import com.cephalon.lucyApp.media.PlatformImagePreview
import com.cephalon.lucyApp.media.PlatformImageThumbnail

@Composable
internal fun AgentModelImagePreview(
    previewState: ImagePreviewState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val images = previewState.images.filter { it.isNotBlank() }
    if (images.isEmpty()) return

    val initialPage = previewState.selectedIndex.coerceIn(0, images.lastIndex)
    val pagerState = rememberPagerState(initialPage = initialPage) { images.size }

    val currentIndex = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()
    val ds = LocalDesignScale.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部栏：关闭按钮 + 页码
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ds.sw(8.dp), vertical = ds.sh(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Box(
                        modifier = Modifier
                            .size(ds.sm(32.dp))
                            .background(Color.White.copy(alpha = 0.10f), CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close preview",
                            tint = Color(0xFF717580),
                            modifier = Modifier.size(ds.sm(18.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${currentIndex + 1}/${images.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(ds.sw(48.dp)))
            }

            // 主图区域 - HorizontalPager 支持左右滑动
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = ds.sw(16.dp),
                beyondViewportPageCount = 1
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val imageSource = images[page]
                    if (imageSource.isLocalAttachmentSource()) {
                        PlatformImagePreview(
                            uri = imageSource,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        BlobImage(
                            blobRef = imageSource,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            errorContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                )
                            }
                        )
                    }
                }
            }

            // 底部缩略图条
            if (images.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ds.sh(12.dp)),
                    horizontalArrangement = Arrangement.spacedBy(ds.sw(10.dp)),
                    contentPadding = PaddingValues(horizontal = ds.sw(16.dp))
                ) {
                    itemsIndexed(images) { index, uri ->
                        val isSelected = index == currentIndex
                        Card(
                            modifier = Modifier
                                .size(width = ds.sw(58.dp), height = ds.sh(72.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                            shape = RoundedCornerShape(ds.sm(14.dp)),
                            border = if (isSelected) {
                                BorderStroke(width = 2.dp, color = Color.White)
                            } else null,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF2A2A2A) else Color(0xFF161616)
                            )
                        ) {
                            if (uri.isLocalAttachmentSource()) {
                                PlatformImageThumbnail(
                                    uri = uri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                BlobImage(
                                    blobRef = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.isLocalAttachmentSource(): Boolean {
    val value = trim()
    return value.startsWith("file://") ||
        value.startsWith("content://") ||
        value.startsWith("ph://") ||
        value.startsWith("ios-phasset://") ||
        value.startsWith("assets-library://") ||
        value.startsWith("/")
}
