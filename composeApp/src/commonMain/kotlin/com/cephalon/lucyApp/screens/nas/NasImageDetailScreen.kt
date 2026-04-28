package com.cephalon.lucyApp.screens.nas

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_download
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.cephalon.lucyApp.sdk.SdkSessionManager
import org.koin.compose.koinInject
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.components.BlobImage
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.media.PlatformImageThumbnail
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

@Composable
internal fun NasImageDetailScreen(
    images: List<NasImageItem>,
    initialImageId: String,
    targetCdi: String,
    onBack: () -> Unit,
    onShare: (NasImageItem) -> Unit,
    onDownload: (NasImageItem) -> Unit,
    onDelete: (NasImageItem) -> Unit,
    isChatMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) return

    val ds = LocalDesignScale.current
    val density = LocalDensity.current
    val swipeStartEdgePx = with(density) { 28.dp.toPx() }
    val swipeBackThresholdPx = with(density) { 72.dp.toPx() }
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val fullImageBlobRefs = remember { mutableStateMapOf<Long, String?>() }
    val fullImageLoading = remember { mutableStateMapOf<Long, Boolean>() }
    var showMenu by remember { mutableStateOf(false) }
    val backgroundColor = Color(0xFFFAFAFC)
    val foregroundColor = Color(0xFF111111)

    val initialPage = images.indexOfFirst { it.id == initialImageId }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { images.size }
    val currentImage = images[pagerState.currentPage]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(onBack, swipeStartEdgePx, swipeBackThresholdPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    if (down.position.x > swipeStartEdgePx) return@awaitEachGesture

                    val pointerId = down.id
                    var totalDx = 0f
                    var totalAbsDy = 0f

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break

                        val delta = change.position - change.previousPosition
                        totalDx += delta.x
                        totalAbsDy += abs(delta.y)

                        if (totalDx > swipeBackThresholdPx && totalDx > totalAbsDy * 1.2f) {
                            onBack()
                            break
                        }
                    }
                }
            }
    ) {
        // 全屏图片翻页
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = ds.sm(12.dp),
            beyondViewportPageCount = 1
        ) { page ->
            val pageImage = images[page]
            val fid = pageImage.fileId
            val resolvedBlobRef = fid?.let { fullImageBlobRefs[it] }?.takeIf { it.isNotBlank() }
            val displayPath = resolvedBlobRef ?: pageImage.path.takeIf { it.isNotBlank() }

            LaunchedEffect(fid) {
                if (fid == null || fid in fullImageBlobRefs || fullImageLoading[fid] == true) return@LaunchedEffect
                fullImageLoading[fid] = true
                try {
                    val response = sdkSessionManager.getFileFromNas(
                        targetCdi = targetCdi,
                        fileId = fid,
                    ).getOrThrow()
                    fullImageBlobRefs[fid] = response.item?.blobRef?.trim()?.takeIf { it.isNotEmpty() }
                } catch (_: Throwable) {
                    fullImageBlobRefs[fid] = null
                } finally {
                    fullImageLoading[fid] = false
                }
            }

            when {
                displayPath != null && displayPath.isLocalAttachmentSource() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFAFAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        PlatformImageThumbnail(
                            uri = displayPath,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                displayPath != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFAFAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        BlobImage(
                            blobRef = displayPath,
                            contentDescription = pageImage.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            errorContent = {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFC))
                                )
                            }
                        )
                    }
                }
                fullImageLoading[fid] == true -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFAFAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = foregroundColor.copy(alpha = 0.7f),
                            strokeWidth = 3.dp,
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFAFAFC))
                    )
                }
            }
        }

        // 顶部浮层：返回 | 时间地点 | 更多
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = ds.sm(16.dp), vertical = ds.sm(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NasDetailGlassCircleButton(
                size = ds.sm(32.dp),
                onClick = onBack
            ) {
                Icon(
                    imageVector = com.cephalon.lucyApp.screens.agentmodel.BackIcon,
                    contentDescription = "返回",
                    tint = Color.Black.copy(alpha = 0.60f),
                    modifier = Modifier.size(
                        width = ds.sw(11.dp),
                        height = ds.sh(17.dp),
                    ),
                )
            }

            if (isChatMode) {
                Text(
                    text = "${pagerState.currentPage + 1}/${images.size}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = ds.sp(12f),
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = foregroundColor,
                )
                NasDetailGlassCircleButton(
                    size = ds.sm(32.dp),
                    onClick = { onDownload(currentImage) }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_download),
                        contentDescription = "下载",
                        tint = foregroundColor,
                        modifier = Modifier.size(ds.sm(16.dp))
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6E6E6))
                ) {
                    Text(
                        text = buildString {
                            append(currentImage.time)
                            currentImage.location?.let { append("  $it") }
                        },
                        modifier = Modifier.padding(horizontal = ds.sm(16.dp), vertical = ds.sm(9.dp)),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = ds.sp(12f),
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF111111)
                    )
                }

                Box {
                    NasDetailGlassCircleButton(
                        size = ds.sm(32.dp),
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = Color.Black.copy(alpha = 0.40f),
                            modifier = Modifier.size(ds.sm(18.dp))
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("发送脑花", color = Color(0xFF111111)) },
                            onClick = {
                                showMenu = false
                                onShare(currentImage)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("下载", color = Color(0xFF111111)) },
                            onClick = {
                                showMenu = false
                                onDownload(currentImage)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = Color(0xFFFF3B30)) },
                            onClick = {
                                showMenu = false
                                onDelete(currentImage)
                            }
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun NasDetailGlassCircleButton(
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.05f))
            .border(
                width = 0.5.dp,
                color = Color.Black.copy(alpha = 0.06f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun String.isLocalAttachmentSource(): Boolean {
    val value = trim()
    return value.startsWith("file://") ||
        value.startsWith("content://") ||
        value.startsWith("ph://") ||
        value.startsWith("assets-library://") ||
        value.startsWith("/")
}
