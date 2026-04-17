package com.cephalon.lucyApp.screens.nas

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_delete
import androidios.composeapp.generated.resources.ic_download
import androidios.composeapp.generated.resources.img_demo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.cephalon.lucyApp.components.decodeImageBytes
import com.cephalon.lucyApp.sdk.SdkSessionManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.components.BlobImage
import com.cephalon.lucyApp.components.LocalDesignScale
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
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) return

    val ds = LocalDesignScale.current
    val density = LocalDensity.current
    val swipeStartEdgePx = with(density) { 28.dp.toPx() }
    val swipeBackThresholdPx = with(density) { 72.dp.toPx() }
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val coroutineScope = rememberCoroutineScope()
    val fullImageCache = remember { mutableStateMapOf<Long, ImageBitmap?>() }
    val fullImageLoading = remember { mutableStateMapOf<Long, Boolean>() }

    val initialPage = images.indexOfFirst { it.id == initialImageId }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { images.size }
    val currentImage = images[pagerState.currentPage]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
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
            val cachedBitmap = fid?.let { fullImageCache[it] }

            LaunchedEffect(fid) {
                if (fid == null || fid in fullImageCache || fullImageLoading[fid] == true) return@LaunchedEffect
                fullImageLoading[fid] = true
                coroutineScope.launch {
                    sdkSessionManager.getFileFromNas(
                        targetCdi = targetCdi,
                        fileId = fid,
                    ).onSuccess { response ->
                        val blobRef = response.item?.blobRef
                        if (!blobRef.isNullOrBlank()) {
                            sdkSessionManager.fetchBlobBytes(blobRef)
                                .onSuccess { bytes ->
                                    fullImageCache[fid] = decodeImageBytes(bytes)
                                }
                                .onFailure {
                                    fullImageCache[fid] = null
                                }
                        } else {
                            fullImageCache[fid] = null
                        }
                    }.onFailure {
                        fullImageCache[fid] = null
                    }
                    fullImageLoading[fid] = false
                }
            }

            when {
                cachedBitmap != null -> {
                    Image(
                        painter = BitmapPainter(cachedBitmap),
                        contentDescription = pageImage.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                fullImageLoading[fid] == true -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White.copy(alpha = 0.7f),
                            strokeWidth = 3.dp,
                        )
                    }
                }
                pageImage.path.isNotBlank() -> {
                    BlobImage(
                        blobRef = pageImage.path,
                        contentDescription = pageImage.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        errorContent = {
                            Image(
                                painter = painterResource(Res.drawable.img_demo),
                                contentDescription = pageImage.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    )
                }
                else -> {
                    Image(
                        painter = painterResource(Res.drawable.img_demo),
                        contentDescription = pageImage.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // 顶部浮层：返回 | 时间地点 | 删除
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
                size = ds.sm(36.dp),
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(ds.sm(16.dp))
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0x1AFFFFFF),
                border = BorderStroke(1.dp, Color(0x0FFFFFFF))
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
                    color = Color.White
                )
            }

            NasDetailGlassCircleButton(
                size = ds.sm(36.dp),
                onClick = { onDelete(currentImage) }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_delete),
                    contentDescription = "删除",
                    tint = Color.White,
                    modifier = Modifier.size(ds.sm(16.dp))
                )
            }
        }

        // 底部浮层：发送脑花 | 下载
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(horizontal = ds.sm(16.dp), vertical = ds.sm(16.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .width(ds.sm(140.dp))
                    .height(ds.sm(49.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onShare(currentImage) },
                shape = RoundedCornerShape(999.dp),
                color = Color(0x1AFFFFFF),
                border = BorderStroke(1.dp, Color(0x0FFFFFFF))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "发送脑花",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = ds.sp(18f),
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }

            NasDetailGlassCircleButton(
                size = ds.sm(48.dp),
                onClick = { onDownload(currentImage) }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_download),
                    contentDescription = "下载",
                    tint = Color.White,
                    modifier = Modifier.size(ds.sm(20.dp))
                )
            }
        }
    }
}

@Composable
private fun NasDetailGlassCircleButton(
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = Color(0x1AFFFFFF),
        border = BorderStroke(1.dp, Color(0x0FFFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
