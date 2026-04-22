package com.cephalon.lucyApp.screens.nas

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_delete
import androidios.composeapp.generated.resources.ic_download
import androidios.composeapp.generated.resources.ic_share
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.media.PlatformDocumentPreview
import com.cephalon.lucyApp.media.platformSaveCacheFile
import com.cephalon.lucyApp.sdk.SdkSessionManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.math.abs

@Composable
internal fun NasDocumentDetailScreen(
    document: NasDocumentItem,
    targetCdi: String,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    isChatMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    val density = LocalDensity.current
    val swipeStartEdgePx = with(density) { 28.dp.toPx() }
    val swipeBackThresholdPx = with(density) { 72.dp.toPx() }
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = if (isChatMode) Color.White else Color.Black
    val foregroundColor = if (isChatMode) Color(0xFF111111) else Color.White
    val headerColor = if (isChatMode) Color.White else Color.Black

    var showMenu by remember { mutableStateOf(false) }
    var localFilePath by remember(document.id) { mutableStateOf<String?>(null) }
    var fileLoading by remember(document.id) { mutableStateOf(true) }
    var fileError by remember(document.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(document.fileId, targetCdi) {
        if (localFilePath != null) return@LaunchedEffect
        fileLoading = true
        fileError = null
        runCatching {
            val blobRef = when {
                document.fileId != null -> {
                    val getResponse = sdkSessionManager.getFileFromNas(
                        targetCdi = targetCdi,
                        fileId = document.fileId,
                    ).getOrThrow()
                    getResponse.item?.blobRef ?: throw IllegalStateException("文件详情缺少 blobRef")
                }
                document.path.isNotBlank() -> document.path
                else -> throw IllegalStateException("文件 ID 缺失")
            }
            val bytes = sdkSessionManager.fetchBlobBytes(blobRef).getOrThrow()
            platformSaveCacheFile(bytes, document.name)
        }.onSuccess { path ->
            localFilePath = path
            fileLoading = false
        }.onFailure { err ->
            fileError = err.message ?: "加载文档失败"
            fileLoading = false
        }
    }

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
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .statusBarsPadding()
                    .padding(horizontal = ds.sm(16.dp), vertical = ds.sm(12.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DocDetailGlassCircleButton(size = ds.sm(36.dp), isLight = isChatMode, onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = foregroundColor,
                        modifier = Modifier.size(ds.sm(16.dp))
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = ds.sm(10.dp)),
                    shape = RoundedCornerShape(999.dp),
                    color = if (isChatMode) Color(0xFFF5F5F7) else Color(0x1AFFFFFF),
                    border = BorderStroke(1.dp, if (isChatMode) Color(0xFFE6E6E6) else Color(0x0FFFFFFF))
                ) {
                    Text(
                        text = buildString {
                            append(document.name.removeSuffix(".${document.format}"))
                            append("...${document.format.uppercase()}")
                        },
                        modifier = Modifier.padding(
                            horizontal = ds.sm(16.dp),
                            vertical = ds.sm(9.dp)
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = ds.sp(12f),
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = foregroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isChatMode) {
                    DocDetailGlassCircleButton(size = ds.sm(36.dp), isLight = true, onClick = onDownload) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_download),
                            contentDescription = "下载",
                            tint = foregroundColor,
                            modifier = Modifier.size(ds.sm(16.dp))
                        )
                    }
                } else {
                    Box {
                        DocDetailGlassCircleButton(size = ds.sm(36.dp), onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多",
                                tint = Color.White,
                                modifier = Modifier.size(ds.sm(16.dp))
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(ds.sm(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("发送脑花", color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_share),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(ds.sm(18.dp))
                                    )
                                },
                                onClick = { showMenu = false; onShare() }
                            )
                            DropdownMenuItem(
                                text = { Text("下载", color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_download),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(ds.sm(18.dp))
                                    )
                                },
                                onClick = { showMenu = false; onDownload() }
                            )
                            DropdownMenuItem(
                                text = { Text("删除", color = Color(0xFFFF3B30)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_delete),
                                        contentDescription = null,
                                        tint = Color(0xFFFF3B30),
                                        modifier = Modifier.size(ds.sm(18.dp))
                                    )
                                },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    fileLoading -> {
                        CircularProgressIndicator(
                            color = foregroundColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(ds.sm(32.dp))
                        )
                    }
                    fileError != null -> {
                        Text(
                            text = fileError ?: "加载失败",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF8E8E93)
                        )
                    }
                    localFilePath != null -> {
                        PlatformDocumentPreview(
                            source = localFilePath!!,
                            fileName = document.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Text(
                            text = "准备加载文档…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }
        } // end Column

        if (!isChatMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = ds.sm(16.dp), bottom = ds.sm(20.dp))
            ) {
                DocDetailGlassCircleButton(size = ds.sm(44.dp), onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = Color.White,
                        modifier = Modifier.size(ds.sm(20.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun DocDetailGlassCircleButton(
    size: Dp,
    isLight: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = if (isLight) Color.White else Color(0x1AFFFFFF),
        border = BorderStroke(1.dp, if (isLight) Color(0xFFE6E6E6) else Color(0x0FFFFFFF))
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
