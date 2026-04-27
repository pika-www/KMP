package com.cephalon.lucyApp.screens.nas

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_delete
import androidios.composeapp.generated.resources.ic_download
import androidios.composeapp.generated.resources.ic_share
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.media.PlatformMediaAccessController
import com.cephalon.lucyApp.media.platformSaveCacheFile
import com.cephalon.lucyApp.sdk.SdkSessionManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
internal fun NasAudioDetailScreen(
    audio: NasAudioItem,
    targetCdi: String,
    mediaController: PlatformMediaAccessController,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    isChatMode: Boolean = false,
    resolveAudioFile: (suspend () -> String)? = null,
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

    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var isSeeking by remember(audio.id) { mutableStateOf(false) }
    var sliderPositionMillis by remember(audio.id) { mutableFloatStateOf(0f) }
    var localFilePath by remember(audio.id) { mutableStateOf<String?>(null) }
    var fileLoading by remember(audio.id) { mutableStateOf(false) }
    var fileError by remember(audio.id) { mutableStateOf<String?>(null) }
    var transcriptMarkdown by remember(audio.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(audio.id, targetCdi, resolveAudioFile) {
        if (localFilePath != null) return@LaunchedEffect
        fileLoading = true
        fileError = null
        coroutineScope.launch {
            runCatching {
                if (resolveAudioFile != null) {
                    resolveAudioFile()
                } else {
                    val blobRef = when {
                        audio.fileId != null -> {
                            val getResponse = sdkSessionManager.getFileFromNas(
                                targetCdi = targetCdi,
                                fileId = audio.fileId,
                            ).getOrThrow()
                            transcriptMarkdown = getResponse.item?.desc?.trim()?.ifBlank { null }
                            getResponse.item?.blobRef
                                ?: throw IllegalStateException("文件详情缺少 blobRef")
                        }
                        audio.path.isNotBlank() -> audio.path
                        else -> throw IllegalStateException("文件 ID 缺失")
                    }
                    val bytes = sdkSessionManager.fetchBlobBytes(blobRef).getOrThrow()
                    platformSaveCacheFile(bytes, audio.name)
                }
            }.onSuccess { path ->
                localFilePath = path
                fileLoading = false
            }.onFailure { err ->
                fileError = err.message ?: "加载音频失败"
                fileLoading = false
            }
        }
    }

    val playbackState = mediaController.audioPlaybackState
    val isCurrentAudio = playbackState.sourceId == audio.id
    val fallbackDurationMillis = audio.durationSec * 1000L
    val durationMillis = when {
        isCurrentAudio && playbackState.durationMillis > 0L -> playbackState.durationMillis
        else -> fallbackDurationMillis
    }.coerceAtLeast(1L)
    val currentPositionMillis = when {
        isCurrentAudio -> playbackState.currentPositionMillis.coerceIn(0L, durationMillis)
        else -> 0L
    }
    val isPlaying = isCurrentAudio && playbackState.isPlaying

    PlatformBackHandler(onBack = onBack)

    LaunchedEffect(audio.id, isCurrentAudio, currentPositionMillis, durationMillis, isSeeking) {
        if (!isSeeking) {
            sliderPositionMillis = currentPositionMillis.coerceIn(0L, durationMillis).toFloat()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 顶部栏：返回 | Tab | 更多
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ds.sm(16.dp), vertical = ds.sm(12.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AudioDetailGlassCircleButton(size = ds.sm(36.dp), isLight = isChatMode, onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = foregroundColor,
                        modifier = Modifier.size(ds.sm(16.dp))
                    )
                }

                // Tab 切换（对话模式隐藏）
                if (!isChatMode) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF1C1C1E))
                            .padding(ds.sm(4.dp))
                    ) {
                        listOf("音频", "文稿").forEachIndexed { index, title ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (selectedTab == index) Color(0xFF3A3A3C)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedTab = index }
                                    .padding(horizontal = ds.sm(20.dp), vertical = ds.sm(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = ds.sp(14f),
                                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = foregroundColor
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // 更多按钮 + 下拉菜单
                if (isChatMode) {
                    AudioDetailGlassCircleButton(size = ds.sm(36.dp), isLight = true, onClick = onDownload) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_download),
                            contentDescription = "下载",
                            tint = foregroundColor,
                            modifier = Modifier.size(ds.sm(16.dp))
                        )
                    }
                } else {
                    Box {
                        AudioDetailGlassCircleButton(size = ds.sm(36.dp), onClick = { showMenu = true }) {
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

            // 内容区域
            if (isChatMode) {
                AudioPlayerContent(
                    audio = audio,
                    isPlaying = isPlaying,
                    fileLoading = fileLoading,
                    fileError = fileError,
                    foregroundColor = foregroundColor,
                    currentPositionMillis = if (isSeeking) sliderPositionMillis.roundToLong() else currentPositionMillis,
                    durationMillis = durationMillis,
                    onPlayPauseClick = {
                        val path = localFilePath
                        if (path != null) {
                            mediaController.toggleAudioPlayback(
                                sourceId = audio.id,
                                name = audio.name,
                                source = path
                            )
                        }
                    },
                    onProgressChange = { value ->
                        isSeeking = true
                        sliderPositionMillis = value.coerceIn(0f, durationMillis.toFloat())
                    },
                    onProgressChangeFinished = {
                        mediaController.seekAudioPlaybackTo(sliderPositionMillis.roundToLong())
                        isSeeking = false
                    },
                    onSkipPreviousClick = { mediaController.skipAudioPlaybackBy(-10_000L) },
                    onSkipNextClick = { mediaController.skipAudioPlaybackBy(10_000L) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                when (selectedTab) {
                    0 -> AudioPlayerContent(
                        audio = audio,
                        isPlaying = isPlaying,
                        fileLoading = fileLoading,
                        fileError = fileError,
                        foregroundColor = foregroundColor,
                        currentPositionMillis = if (isSeeking) sliderPositionMillis.roundToLong() else currentPositionMillis,
                        durationMillis = durationMillis,
                        onPlayPauseClick = {
                            val path = localFilePath
                            if (path != null) {
                                mediaController.toggleAudioPlayback(
                                    sourceId = audio.id,
                                    name = audio.name,
                                    source = path
                                )
                            }
                        },
                        onProgressChange = { value ->
                            isSeeking = true
                            sliderPositionMillis = value.coerceIn(0f, durationMillis.toFloat())
                        },
                        onProgressChangeFinished = {
                            mediaController.seekAudioPlaybackTo(sliderPositionMillis.roundToLong())
                            isSeeking = false
                        },
                        onSkipPreviousClick = { mediaController.skipAudioPlaybackBy(-10_000L) },
                        onSkipNextClick = { mediaController.skipAudioPlaybackBy(10_000L) },
                        modifier = Modifier.weight(1f)
                    )
                    1 -> TranscriptContent(
                        transcript = transcriptMarkdown,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioPlayerContent(
    audio: NasAudioItem,
    isPlaying: Boolean,
    fileLoading: Boolean = false,
    fileError: String? = null,
    foregroundColor: Color = Color.White,
    currentPositionMillis: Long,
    durationMillis: Long,
    onPlayPauseClick: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    Column(modifier = modifier.fillMaxWidth()) {
        // 中间音频图标 / 加载状态 / 错误状态
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                fileLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = foregroundColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(ds.sm(32.dp))
                        )
                        Spacer(modifier = Modifier.height(ds.sm(12.dp)))
                        Text(
                            text = "加载音频中…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
                fileError != null -> {
                    Text(
                        text = fileError,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF8E8E93)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        modifier = Modifier.size(ds.sm(120.dp)),
                        tint = foregroundColor.copy(alpha = 0.15f)
                    )
                }
            }
        }

        // 音频标题
        Text(
            text = audio.name,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = ds.sm(16.dp),
                    top = ds.sm(16.dp),
                    end = ds.sm(16.dp)
                ),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = ds.sp(16f),
                fontWeight = FontWeight.Normal
            ),
            color = foregroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(ds.sm(8.dp)))

        // 自定义进度条
        AudioProgressBar(
            currentMs = currentPositionMillis,
            durationMs = durationMillis,
            trackColor = foregroundColor,
            onProgressChange = onProgressChange,
            onProgressChangeFinished = onProgressChangeFinished,
            modifier = Modifier.padding(horizontal = ds.sm(16.dp))
        )

        // 时间标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sm(20.dp)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTimeFromMillis(currentPositionMillis),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = ds.sp(12f)),
                color = Color(0xFF8E8E93)
            )
            Text(
                text = formatTimeFromMillis(durationMillis),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = ds.sp(12f)),
                color = Color(0xFF8E8E93)
            )
        }

        Spacer(modifier = Modifier.height(ds.sm(16.dp)))

        // 播放控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(ds.sm(48.dp))
                    .clip(CircleShape)
                    .clickable(onClick = onSkipPreviousClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "上一首",
                    modifier = Modifier.size(ds.sm(32.dp)),
                    tint = foregroundColor
                )
            }

            Spacer(modifier = Modifier.width(ds.sm(32.dp)))

            Box(
                modifier = Modifier
                    .size(ds.sm(56.dp))
                    .clip(CircleShape)
                    .clickable(onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(ds.sm(44.dp)),
                    tint = foregroundColor
                )
            }

            Spacer(modifier = Modifier.width(ds.sm(32.dp)))

            Box(
                modifier = Modifier
                    .size(ds.sm(48.dp))
                    .clip(CircleShape)
                    .clickable(onClick = onSkipNextClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "下一首",
                    modifier = Modifier.size(ds.sm(32.dp)),
                    tint = foregroundColor
                )
            }
        }

        Spacer(modifier = Modifier.height(ds.sm(32.dp)))
    }
}

@Composable
private fun AudioProgressBar(
    currentMs: Long,
    durationMs: Long,
    trackColor: Color = Color.White,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    val progress = (currentMs.toFloat() / durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ds.sm(20.dp))
            .pointerInput(durationMs, onProgressChange, onProgressChangeFinished) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val frac = (down.position.x / size.width).coerceIn(0f, 1f)
                    onProgressChange(frac * durationMs)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val dragFrac = (change.position.x / size.width).coerceIn(0f, 1f)
                        onProgressChange(dragFrac * durationMs)
                    } while (true)
                    onProgressChangeFinished()
                }
            }
    ) {
        val dotRadiusPx = ds.sm(5.dp).toPx() / 2f
        val trackY = center.y
        val trackH = ds.sm(1.dp).toPx()
        val startX = dotRadiusPx
        val endX = size.width - dotRadiusPx
        val trackLen = endX - startX
        val thumbX = startX + trackLen * progress
        val cornerR = CornerRadius(20.dp.toPx(), 20.dp.toPx())

        // 未播放轨道
        drawRoundRect(
            color = trackColor.copy(alpha = 0.40f),
            topLeft = Offset(startX, trackY - trackH / 2),
            size = Size(trackLen, trackH),
            cornerRadius = cornerR
        )
        // 已播放轨道
        if (thumbX > startX) {
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(startX, trackY - trackH / 2),
                size = Size(thumbX - startX, trackH),
                cornerRadius = cornerR
            )
        }
        // 起点小圆点
        drawCircle(color = trackColor, radius = dotRadiusPx, center = Offset(startX, trackY))
        // 当前位置小圆点（thumb）
        drawCircle(color = trackColor, radius = dotRadiusPx, center = Offset(thumbX, trackY))
        // 终点小圆点
        drawCircle(color = trackColor, radius = dotRadiusPx, center = Offset(endX, trackY))
    }
}

@Composable
private fun TranscriptContent
            (
    transcript: String?,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    val textColor = Color.White
    if (transcript.isNullOrBlank()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "文稿内容为空",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF8E8E93)
            )
        }
        return
    }

    SelectionContainer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ds.sm(20.dp), vertical = ds.sm(16.dp)),
            verticalArrangement = Arrangement.spacedBy(ds.sm(8.dp))
        ) {
            MarkdownTranscriptText(
                markdown = transcript,
                textColor = textColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MarkdownTranscriptText(
    markdown: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    val blocks = markdown.split("```")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ds.sh(6.dp))) {
        blocks.forEachIndexed { index, block ->
            if (index % 2 == 1) {
                val codeContent = block.lines().let { lines ->
                    if (lines.isNotEmpty() && lines.first().isNotBlank() && !lines.first().trimStart().contains(' ')) {
                        lines.drop(1).joinToString("\n").trim()
                    } else {
                        block.trim('\n')
                    }
                }
                Surface(
                    shape = RoundedCornerShape(ds.sm(10.dp)),
                    color = Color(0xFF1F1F1F)
                ) {
                    Text(
                        text = codeContent,
                        color = Color(0xFFEAEAEA),
                        fontSize = ds.sp(13f),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ds.sw(10.dp), vertical = ds.sh(8.dp))
                    )
                }
            } else {
                val lines = block.lines().map { it.trimEnd() }
                var i = 0
                while (i < lines.size) {
                    val line = lines[i]
                    if (line.isBlank()) {
                        i++
                        continue
                    }

                    if (line.trimStart().startsWith("|")) {
                        val tableLines = mutableListOf<String>()
                        while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                            tableLines.add(lines[i])
                            i++
                        }
                        MarkdownTranscriptTable(tableLines = tableLines, textColor = textColor)
                    } else {
                        when {
                            line.startsWith("### ") -> MarkdownTranscriptLine(
                                line = line.removePrefix("### "),
                                textColor = textColor,
                                fontSize = ds.sp(16f),
                                fontWeight = FontWeight.SemiBold
                            )

                            line.startsWith("## ") -> MarkdownTranscriptLine(
                                line = line.removePrefix("## "),
                                textColor = textColor,
                                fontSize = ds.sp(18f),
                                fontWeight = FontWeight.SemiBold
                            )

                            line.startsWith("# ") -> MarkdownTranscriptLine(
                                line = line.removePrefix("# "),
                                textColor = textColor,
                                fontSize = ds.sp(20f),
                                fontWeight = FontWeight.Bold
                            )

                            line.startsWith("- ") || line.startsWith("* ") -> MarkdownTranscriptLine(
                                line = "• ${line.drop(2)}",
                                textColor = textColor,
                                fontSize = ds.sp(14f),
                                fontWeight = FontWeight.Normal
                            )

                            else -> MarkdownTranscriptLine(
                                line = line,
                                textColor = textColor,
                                fontSize = ds.sp(14f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                        i++
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTranscriptTable(
    tableLines: List<String>,
    textColor: Color,
) {
    val ds = LocalDesignScale.current
    val borderColor = Color(0x33FFFFFF)
    val horizontalScrollState = rememberScrollState()

    val rows = tableLines.mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.matches(Regex("^\\|[\\s\\-:|]+\\|$"))) return@mapNotNull null
        trimmed.trim('|').split("|").map { it.trim() }
    }
    if (rows.isEmpty()) return

    val columnCount = rows.maxOf { it.size }
    val minColumnWidth = ds.sw(88.dp)
    val columnDividerWidth = 0.5.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val minTableWidth =
            (minColumnWidth * columnCount.toFloat()) +
                (columnDividerWidth * (columnCount - 1).coerceAtLeast(0).toFloat())
        val tableWidth = if (minTableWidth > maxWidth) minTableWidth else maxWidth

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Surface(
                modifier = Modifier.width(tableWidth),
                shape = RoundedCornerShape(ds.sm(8.dp)),
                border = BorderStroke(0.5.dp, borderColor),
                color = Color.Transparent
            ) {
                Column {
                    rows.forEachIndexed { rowIndex, cells ->
                        val isHeader = rowIndex == 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .then(
                                    if (isHeader) Modifier.background(Color(0x14FFFFFF))
                                    else Modifier
                                )
                        ) {
                            for (colIndex in 0 until columnCount) {
                                if (colIndex > 0) {
                                    Box(
                                        modifier = Modifier
                                            .width(columnDividerWidth)
                                            .fillMaxHeight()
                                            .background(borderColor)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .widthIn(min = minColumnWidth)
                                        .padding(
                                            horizontal = ds.sw(8.dp),
                                            vertical = ds.sh(6.dp)
                                        )
                                ) {
                                    Text(
                                        text = markdownTranscriptInlineAnnotatedString(
                                            text = cells.getOrElse(colIndex) { "" },
                                            textColor = textColor
                                        ),
                                        fontSize = ds.sp(13f),
                                        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                                        color = textColor
                                    )
                                }
                            }
                        }
                        if (rowIndex < rows.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(borderColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTranscriptLine(
    line: String,
    textColor: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
) {
    Text(
        text = markdownTranscriptInlineAnnotatedString(line, textColor),
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = textColor,
        textAlign = TextAlign.Start
    )
}

private fun markdownTranscriptInlineAnnotatedString(text: String, textColor: Color): AnnotatedString {
    val regex = Regex("(\\*\\*[^*]+\\*\\*)|(`[^`]+`)|(\\[[^\\]]+\\]\\([^)]*\\))")
    return buildAnnotatedString {
        var lastIndex = 0
        regex.findAll(text).forEach { match ->
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }
            val token = match.value
            when {
                token.startsWith("**") && token.endsWith("**") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor))
                    append(token.removePrefix("**").removeSuffix("**"))
                    pop()
                }

                token.startsWith("`") && token.endsWith("`") -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x14FFFFFF),
                            color = textColor
                        )
                    )
                    append(token.removePrefix("`").removeSuffix("`"))
                    pop()
                }

                token.startsWith("[") -> {
                    val label = token.substringAfter("[").substringBefore("]")
                    pushStyle(
                        SpanStyle(
                            color = Color(0xFF7DB3FF),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(label)
                    pop()
                }
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
private fun AudioDetailGlassCircleButton(
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

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

private fun formatTimeFromMillis(durationMillis: Long): String {
    return formatTime((durationMillis / 1000L).toInt())
}
