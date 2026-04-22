package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.media.AudioRecording
import com.cephalon.lucyApp.media.PickedFile
import com.cephalon.lucyApp.media.PlatformImageThumbnail
import kotlinx.coroutines.delay
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.painterResource
import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_skill_image
import androidios.composeapp.generated.resources.ic_skill_voice
import androidios.composeapp.generated.resources.ic_skill_document
import androidios.composeapp.generated.resources.ic_skill_chat
import androidios.composeapp.generated.resources.ic_skill_knowledge
import androidios.composeapp.generated.resources.ic_download
import androidios.composeapp.generated.resources.ic_doc
import androidios.composeapp.generated.resources.ic_audio
import androidx.compose.foundation.layout.width
import com.cephalon.lucyApp.components.BlobImage
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.media.AudioPlaybackState
import com.cephalon.lucyApp.sdk.MediaAttachment
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


/** 根据文件扩展名推断 MIME content type，用于 contentType 为 null 时的兜底分类 */
private fun inferContentTypeFromFileName(fileName: String?): String? {
    if (fileName.isNullOrBlank()) return null
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        "mp3" -> "audio/mpeg"
        "m4a", "aac" -> "audio/aac"
        "wav" -> "audio/wav"
        "ogg", "oga" -> "audio/ogg"
        "flac" -> "audio/flac"
        "wma" -> "audio/x-ms-wma"
        "opus" -> "audio/opus"
        "amr" -> "audio/amr"
        else -> null
    }
}

/** 取附件的有效 content type：优先用文件扩展名推断（更可靠），兜底用服务器返回值 */
private fun MediaAttachment.effectiveContentType(): String? =
    inferContentTypeFromFileName(fileName) ?: contentType

@Composable
internal fun AgentModelMessageList(
    messages: List<ChatItem>,
    playingRecordingId: String?,
    onToggleRecordingPlayback: (AudioRecording) -> Unit,
    onRecordingOpen: (AudioRecording) -> Unit = {},
    onImageClick: (ImagePreviewState) -> Unit,
    onFileClick: (PickedFile) -> Unit,
    onAudioFileOpen: (AudioRecording) -> Unit = {},
    onTapMessageArea: () -> Unit,
    onSkillClick: (String) -> Unit = {},
    onAttachmentOpen: (MediaAttachment, List<MediaAttachment>) -> Unit = { _, _ -> },
    onAttachmentDownload: (MediaAttachment) -> Unit = {},
    audioPlaybackState: AudioPlaybackState = AudioPlaybackState(),
    loadingAudioBlobRefs: Set<String> = emptySet(),
    onToggleAudioAttachmentPlayback: (MediaAttachment) -> Unit = {},
    onCopySuccess: () -> Unit = {},
    streamingStatusText: String? = null,
    isStopMode: Boolean = false,
    activeThinkingMessageIds: Set<String> = emptySet(),
    hiddenStatusMessageIds: Set<String> = emptySet(),
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ds.sh(10.dp))
    ) {
        item(key = "__top_spacer") {
            Spacer(modifier = Modifier.height(ds.sh(12.dp)))
        }

        itemsIndexed(items = messages, key = { index, item ->
            when (item) {
                is ChatItem.Assistant -> item.assistantId
                is ChatItem.User -> "user_$index"
                is ChatItem.UserAttachments -> "attachments_$index"
                is ChatItem.System -> "system_$index"
                is ChatItem.RecordingItem -> "recording_${item.id}"
                is ChatItem.Error -> "error_$index"
                is ChatItem.SkillSuggestions -> "skills_$index"
            }
        }) { index, item ->
            when (item) {
                is ChatItem.Assistant -> {
                    val isThinkingActive = item.messageId != null && item.messageId in activeThinkingMessageIds
                    val shouldHideStatusBubble = item.messageId != null && item.messageId in hiddenStatusMessageIds
                    Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                        // ── 可折叠思考状态气泡 ──
                        if (!shouldHideStatusBubble && (isThinkingActive || item.streamEvents.isNotEmpty())) {
                            ThinkingBubble(
                                events = item.streamEvents,
                                isStreaming = isThinkingActive,
                                reasoningText = item.reasoningText,
                                streamingStatusText = streamingStatusText,
                                ds = ds,
                            )
                            Spacer(modifier = Modifier.height(ds.sh(6.dp)))
                        }

                        // ── 文本内容 ──
                        if (item.text.isNotBlank()) {
                            Bubble(
                                text = item.text,
                                background = Color.Transparent,
                                textColor = Color(0xFF111111),
                                alignEnd = false,
                                border = null,
                                isMarkdown = true,
                                onClick = onTapMessageArea,
                                onCopySuccess = onCopySuccess,
                            )
                        }

                        // ── 附件卡片 ──
                        if (item.attachments.isNotEmpty()) {
                            if (item.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(ds.sh(6.dp)))
                            }
                            AssistantAttachments(
                                attachments = item.attachments,
                                timestamp = item.timestamp,
                                audioPlaybackState = audioPlaybackState,
                                loadingAudioBlobRefs = loadingAudioBlobRefs,
                                onToggleAudioAttachmentPlayback = onToggleAudioAttachmentPlayback,
                                onAttachmentOpen = onAttachmentOpen,
                                onAttachmentDownload = onAttachmentDownload,
                            )
                        }
                    }
                }

                is ChatItem.User -> {
                    BubbleContainer(alignEnd = true) { bubbleMaxWidth ->
                        Surface(
                            // 右侧用户发送气泡统一 22dp 圆角（旧值 99dp 是胶囊形）
                            shape = RoundedCornerShape(ds.sm(22.dp)),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, Color(0xFF1F2535).copy(alpha = 0.20f)),
                            modifier = Modifier
                                .clickable { onTapMessageArea() }
                                .wrapContentWidth()
                                .widthIn(max = bubbleMaxWidth)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = item.text,
                                    color = Color(0xFF1F2535),
                                    fontSize = ds.sp(14f),
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = ds.sp(20f),
                                    modifier = Modifier.padding(horizontal = ds.sw(16.dp), vertical = ds.sh(8.dp))
                                )
                            }
                        }
                    }
                }

                is ChatItem.UserAttachments -> {
                    BubbleContainer(alignEnd = true) { bubbleMaxWidth ->
                        val imageCellSize = ((bubbleMaxWidth - ds.sw(28.dp) - ds.sw(8.dp)) / 2).coerceAtMost(ds.sm(132.dp))
                        val fileCellWidth = (bubbleMaxWidth - ds.sw(28.dp) - ds.sw(8.dp)) / 2
                        Surface(
                            // 右侧用户附件气泡同样 22dp，和文字气泡视觉一致
                            shape = RoundedCornerShape(ds.sm(22.dp)),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, Color(0xFF1F2535).copy(alpha = 0.20f)),
                            modifier = Modifier
                                .clickable { onTapMessageArea() }
                                .wrapContentWidth()
                                .widthIn(max = bubbleMaxWidth)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = ds.sw(14.dp), vertical = ds.sh(12.dp)),
                                verticalArrangement = Arrangement.spacedBy(ds.sh(10.dp))
                            ) {
                                val messageText = item.text
                                if (!messageText.isNullOrBlank()) {
                                    Text(
                                        text = messageText,
                                        fontSize = ds.sp(14f),
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = ds.sp(20f),
                                        color = Color(0xFF1F2535)
                                    )
                                }

                                val images = item.attachments.filter { it.type == DraftAttachmentType.Image }
                                val files = item.attachments.filter { it.type == DraftAttachmentType.File }
                                val audios = item.attachments.filter { it.type == DraftAttachmentType.Audio }

                                if (images.isNotEmpty()) {
                                    val imageUris = images.map { it.uri }
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(ds.sh(8.dp))
                                    ) {
                                        images.chunked(2).forEachIndexed { rowIndex, rowImages ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(ds.sw(8.dp))
                                            ) {
                                                ImageAttachmentCell(
                                                    attachment = rowImages.getOrNull(0),
                                                    onClick = {
                                                        onImageClick(
                                                            ImagePreviewState(
                                                                images = imageUris,
                                                                selectedIndex = rowIndex * 2
                                                            )
                                                        )
                                                    },
                                                    modifier = Modifier.size(imageCellSize)
                                                )
                                                rowImages.getOrNull(1)?.let { secondAttachment ->
                                                    ImageAttachmentCell(
                                                        attachment = secondAttachment,
                                                        onClick = {
                                                            onImageClick(
                                                                ImagePreviewState(
                                                                    images = imageUris,
                                                                    selectedIndex = rowIndex * 2 + 1
                                                                )
                                                            )
                                                        },
                                                        modifier = Modifier.size(imageCellSize)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                val allFiles = files + audios
                                if (allFiles.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(ds.sh(8.dp))) {
                                        allFiles.chunked(2).forEach { rowFiles ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(ds.sw(8.dp))
                                            ) {
                                                rowFiles.forEach { attachment ->
                                                    Surface(
                                                        shape = RoundedCornerShape(ds.sm(12.dp)),
                                                        color = Color(0xFFF5F5F5),
                                                        border = BorderStroke(1.dp, Color(0xFFE7E7E7)),
                                                        modifier = Modifier
                                                            .width(fileCellWidth)
                                                            .height(ds.sh(72.dp))
                                                            .clickable {
                                                                if (attachment.type == DraftAttachmentType.Audio) {
                                                                    onAudioFileOpen(attachment.asAudioRecording())
                                                                } else {
                                                                    onFileClick(attachment.asPickedFile())
                                                                }
                                                            }
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = ds.sw(10.dp), vertical = ds.sh(10.dp)),
                                                            verticalArrangement = Arrangement.spacedBy(ds.sh(6.dp))
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(999.dp),
                                                                color = Color(0xFF111111)
                                                            ) {
                                                                Text(
                                                                    text = attachment.fileExtensionLabel(),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color.White,
                                                                    modifier = Modifier.padding(horizontal = ds.sw(8.dp), vertical = ds.sh(3.dp))
                                                                )
                                                            }
                                                            Text(
                                                                text = attachment.displayName(),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color(0xFF111111),
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                if (rowFiles.size == 1) {
                                                    Spacer(modifier = Modifier.width(fileCellWidth))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is ChatItem.System -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTapMessageArea() },
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A8A8A),
                            modifier = Modifier
                                .background(Color.Transparent)
                                .padding(vertical = ds.sh(2.dp))
                        )
                    }
                }

                is ChatItem.RecordingItem -> {
                    BubbleContainer(alignEnd = false) { bubbleMaxWidth ->
                        Card(
                            shape = RoundedCornerShape(ds.sm(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE7E7E7)),
                            modifier = Modifier
                                .clickable {
                                    onTapMessageArea()
                                    onRecordingOpen(
                                        AudioRecording(
                                            id = item.id,
                                            name = item.name,
                                            path = item.path
                                        )
                                    )
                                }
                                .wrapContentWidth()
                                .widthIn(max = bubbleMaxWidth)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = ds.sw(14.dp), vertical = ds.sh(12.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ds.sw(12.dp))
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = Color(0xFF111111)
                                )
                                Column(modifier = Modifier.widthIn(max = bubbleMaxWidth - ds.sw(140.dp))) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF111111)
                                    )
                                    Text(
                                        text = item.path,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF777777),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        onRecordingOpen(
                                            AudioRecording(
                                                id = item.id,
                                                name = item.name,
                                                path = item.path
                                            )
                                        )
                                    }
                                ) {
                                    Text("详情")
                                }
                            }
                        }
                    }
                }

                is ChatItem.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ds.sw(16.dp), vertical = ds.sh(4.dp)),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(ds.sm(12.dp)),
                            color = Color(0xFFFFF0F0),
                            border = BorderStroke(0.5.dp, Color(0xFFE8BCBC)),
                        ) {
                            Text(
                                text = item.text,
                                color = Color(0xFFCC4444),
                                fontSize = ds.sp(13f),
                                lineHeight = ds.sp(18f),
                                modifier = Modifier.padding(horizontal = ds.sw(12.dp), vertical = ds.sh(6.dp))
                            )
                        }
                    }
                }

                is ChatItem.SkillSuggestions -> {
                    BubbleContainer(alignEnd = false) { _ ->
                        SkillSuggestionsBubble(
                            onSkillClick = onSkillClick
                        )
                    }
                }
            }
        }

        item(key = "__bottom_spacer") {
            Spacer(modifier = Modifier.height(ds.sh(12.dp)))
        }
    }
}

@Composable
private fun rememberThinkingStatusText(): String {
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(420L)
            step = (step + 1) % 3
        }
    }
    val dots = when (step) {
        0 -> "."
        1 -> ".."
        else -> "..."
    }
    return "思考中$dots"
}

@Composable
private fun SkillSuggestionsBubble(
    onSkillClick: (String) -> Unit,
) {
    val ds = LocalDesignScale.current
    val skillItems = listOf(
        Res.drawable.ic_skill_image to "脑花找图片，模糊的信息也能找",
        Res.drawable.ic_skill_voice to "脑花翻录音 记得一句就能翻出来",
        Res.drawable.ic_skill_document to "脑花调文档 文件名忘了也能调",
        Res.drawable.ic_skill_chat to "脑花搞内容 从想法到发出不断更",
        Res.drawable.ic_skill_knowledge to "脑花控手机 插上硬件听你使唤",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ds.sh(8.dp))
    ) {
        Text(
            text = "Hi，我是脑花",
            fontSize = ds.sp(18f),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF12192B)
        )
        Text(
            text = "试试输入以下 Skill 来帮助完成工作细节",
            fontSize = ds.sp(12f),
            color = Color(0xFF595E6B)
        )

        Spacer(modifier = Modifier.height(ds.sh(4.dp)))

        skillItems.forEach { (iconRes, text) ->
            Card(
                shape = RoundedCornerShape(ds.sm(99.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(0.5.dp, Color(0xFF1F2535).copy(alpha = 0.10f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSkillClick(text) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ds.sh(44.dp))
                        .padding(horizontal = ds.sw(16.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ds.sw(8.dp))
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(ds.sm(20.dp))
                    )
                    Text(
                        text = text,
                        color = Color(0xFF12192B),
                        fontSize = ds.sp(14f),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantAttachments(
    attachments: List<MediaAttachment>,
    timestamp: Long? = null,
    audioPlaybackState: AudioPlaybackState = AudioPlaybackState(),
    loadingAudioBlobRefs: Set<String> = emptySet(),
    onToggleAudioAttachmentPlayback: (MediaAttachment) -> Unit = {},
    onAttachmentOpen: (MediaAttachment, List<MediaAttachment>) -> Unit = { _, _ -> },
    onAttachmentDownload: (MediaAttachment) -> Unit = {},
) {
    val ds = LocalDesignScale.current
    val imageAttachments = attachments.filter { it.effectiveContentType()?.startsWith("image") == true }
    val audioAttachments = attachments.filter { it.effectiveContentType()?.startsWith("audio") == true }
    val docAttachments = attachments.filter {
        it.effectiveContentType()?.startsWith("image") != true &&
        it.effectiveContentType()?.startsWith("audio") != true
    }

    Surface(
        shape = RoundedCornerShape(ds.sm(22.dp)),
        color = Color.White,
        border = BorderStroke(0.5.dp, Color(0xFF1F2535).copy(alpha = 0.20f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = ds.sw(14.dp), vertical = ds.sh(12.dp)),
            verticalArrangement = Arrangement.spacedBy(ds.sh(10.dp))
        ) {
            // ── 图片 ──
            imageAttachments.forEach { att ->
                Box(
                    modifier = Modifier
                        .size(ds.sw(120.dp))
                        .clip(RoundedCornerShape(ds.sm(8.dp)))
                        .clickable { onAttachmentOpen(att, imageAttachments) },
                ) {
                    BlobImage(
                        blobRef = att.blobRef,
                        contentDescription = att.fileName,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(ds.sm(6.dp))
                            .size(ds.sm(24.dp))
                            .clip(RoundedCornerShape(ds.sm(12.dp)))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { onAttachmentDownload(att) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_download),
                            contentDescription = "下载",
                            modifier = Modifier.size(ds.sm(14.dp)),
                            tint = Color.White,
                        )
                    }
                }
            }

            // ── 音频 ──
            if (audioAttachments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(ds.sh(8.dp))) {
                    audioAttachments.forEach { att ->
                        AudioAttachmentCard(
                            attachment = att,
                            timestamp = timestamp,
                            audioPlaybackState = audioPlaybackState,
                            isLoading = att.blobRef in loadingAudioBlobRefs,
                            onPlayToggle = { onToggleAudioAttachmentPlayback(att) },
                            onClick = { onAttachmentOpen(att, audioAttachments) },
                        )
                    }
                }
            }

            // ── 文档 ──
            if (docAttachments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(ds.sh(8.dp))) {
                    docAttachments.forEach { att ->
                        AttachmentFileCard(
                            icon = Res.drawable.ic_doc,
                            fileName = att.fileName ?: "文件",
                            onClick = { onAttachmentOpen(att, listOf(att)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentFileCard(
    icon: org.jetbrains.compose.resources.DrawableResource,
    fileName: String,
    onClick: () -> Unit,
) {
    val ds = LocalDesignScale.current
    Surface(
        shape = RoundedCornerShape(ds.sm(99.dp)),
        color = Color(0xFFF5F5F7),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = ds.sw(14.dp), end = ds.sw(12.dp))
                .padding(vertical = ds.sh(10.dp)),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(ds.sm(20.dp)),
                tint = Color(0xFF1F2535),
            )
            Spacer(modifier = Modifier.width(ds.sw(10.dp)))
            Text(
                text = fileName,
                fontSize = ds.sp(14f),
                color = Color(0xFF1F2535),
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(ds.sm(20.dp)),
            )
        }
    }
}

// ─────────────── 音频附件卡片 ───────────────

private fun formatAudioTimestamp(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0) return "音频文件"
    return try {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val dt = instant.toLocalDateTime(TimeZone.of("Asia/Shanghai"))
        val y = dt.year
        val m = dt.monthNumber.toString().padStart(2, '0')
        val d = dt.dayOfMonth.toString().padStart(2, '0')
        val hh = dt.hour.toString().padStart(2, '0')
        val mm = dt.minute.toString().padStart(2, '0')
        "$y.$m.$d.  $hh:$mm"
    } catch (_: Exception) {
        "音频文件"
    }
}

@Composable
private fun AudioAttachmentCard(
    attachment: MediaAttachment,
    timestamp: Long?,
    audioPlaybackState: AudioPlaybackState,
    isLoading: Boolean,
    onPlayToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val ds = LocalDesignScale.current
    val sourceId = "chat-audio-${attachment.blobRef}"
    val isCurrentAudio = audioPlaybackState.sourceId == sourceId
    val isPlaying = isCurrentAudio && audioPlaybackState.isPlaying
    val displayText = attachment.fileName
        ?.takeIf { it.isNotBlank() }
        ?: formatAudioTimestamp(timestamp)

    Surface(
        shape = RoundedCornerShape(ds.sm(99.dp)),
        color = Color(0xFFF5F5F7),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = ds.sw(10.dp), end = ds.sw(12.dp))
                .padding(vertical = ds.sh(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Play / Pause 按钮
            Surface(
                shape = CircleShape,
                color = Color(0xFF1F2535),
                modifier = Modifier.size(ds.sm(28.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onPlayToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(ds.sm(14.dp)),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(ds.sm(14.dp)),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(ds.sw(10.dp)))

            // 日期 / 文件名
            Text(
                text = displayText,
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1F2535),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // 右箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(ds.sm(20.dp)),
            )
        }
    }
}

// ─────────────── 可折叠思考气泡 ───────────────

@Composable
private fun ThinkingBubble(
    events: List<StreamEvent>,
    isStreaming: Boolean,
    reasoningText: String?,
    streamingStatusText: String?,
    ds: com.cephalon.lucyApp.components.DesignScale,
) {
    var expanded by remember { mutableStateOf(true) }

    // 对话结束后自动折叠
    LaunchedEffect(isStreaming) {
        if (!isStreaming && events.isNotEmpty()) {
            expanded = false
        }
    }

    Surface(
        shape = RoundedCornerShape(ds.sm(12.dp)),
        color = Color(0xFFF8F9FA),
        border = BorderStroke(0.5.dp, Color(0xFFE8E8E8)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(ds.sm(12.dp))
        ) {
            // ── 标题行：状态指示 + 折叠切换 ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded },
            ) {
                if (isStreaming) {
                    ThinkingPulsingDot(ds = ds)
                    Spacer(Modifier.width(ds.sw(8.dp)))
                    ThinkingWaveText(
                        text = "Thinking",
                        fontSize = ds.sp(13f),
                        color = Color(0xFF555555),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(ds.sm(8.dp))
                            .background(Color(0xFF4CAF50), shape = androidx.compose.foundation.shape.CircleShape),
                    )
                    Spacer(Modifier.width(ds.sw(8.dp)))
                    Text(
                        text = "Done",
                        fontSize = ds.sp(13f),
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(ds.sw(6.dp)))
                Text(
                    text = if (expanded) "▾" else "▸",
                    fontSize = ds.sp(11f),
                    color = Color(0xFFAAAAAA),
                )
            }

            // ── 折叠态：显示最后一条事件摘要 ──
            if (!expanded) {
                val lastEvent = events.lastOrNull { it.isActive } ?: events.lastOrNull()
                if (lastEvent != null) {
                    Spacer(Modifier.height(ds.sh(4.dp)))
                    val summary = when (lastEvent.type) {
                        "delivered" -> "已送达"
                        "typing" -> "正在输入"
                        "reasoning" -> "思考中"
                        "tool" -> "🔧 ${lastEvent.label}"
                        "finish" -> "✓ ${lastEvent.label}"
                        else -> lastEvent.label
                    }
                    Text(
                        text = summary,
                        fontSize = ds.sp(11f),
                        color = Color(0xFF999999),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ── 展开态：完整事件时间线 ──
            if (expanded) {
                Spacer(Modifier.height(ds.sh(6.dp)))
                if (events.isNotEmpty()) {
                    EventTimeline(events = events, reasoningText = reasoningText, ds = ds)
                } else if (isStreaming) {
                    // 无事件时的等待状态
                    val fallback = streamingStatusText ?: "等待响应"
                    Text(
                        text = fallback,
                        fontSize = ds.sp(12f),
                        color = Color(0xFF999999),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingWaveText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        text.forEachIndexed { index, char ->
            val offsetY = kotlin.math.sin((phase - index * 0.4f).toDouble()).toFloat() * 2.5f
            Text(
                text = char.toString(),
                fontSize = fontSize,
                color = color,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.offset(y = offsetY.dp),
            )
        }
    }
}

@Composable
private fun ThinkingPulsingDot(
    ds: com.cephalon.lucyApp.components.DesignScale,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Box(
        modifier = Modifier
            .size(ds.sm(8.dp))
            .background(
                Color(0xFF4CAF50).copy(alpha = alpha),
                shape = androidx.compose.foundation.shape.CircleShape,
            ),
    )
}

// ─────────────── 事件时间线 ───────────────

@Composable
private fun EventTimeline(
    events: List<StreamEvent>,
    reasoningText: String?,
    ds: com.cephalon.lucyApp.components.DesignScale,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ds.sh(2.dp)),
        modifier = Modifier.padding(horizontal = ds.sw(4.dp)),
    ) {
        events.forEach { event ->
            when (event.type) {
                "tool" -> ToolEventRow(event = event, ds = ds)
                "reasoning" -> {
                    ReasoningEventRow(event = event, reasoningText = reasoningText, ds = ds)
                }
                "finish" -> FinishEventRow(event = event, ds = ds)
                else -> StatusEventRow(event = event, ds = ds)
            }
        }
    }
}

// ── ○ / ✓ 状态行 ──

@Composable
private fun StatusEventRow(
    event: StreamEvent,
    ds: com.cephalon.lucyApp.components.DesignScale,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = ds.sh(2.dp)),
    ) {
        EventCheckIcon(isActive = event.isActive, ds = ds)
        Spacer(modifier = Modifier.width(ds.sw(8.dp)))
        val dots = if (event.isActive) rememberAnimatedDots() else ""
        Text(
            text = "${event.label}$dots",
            fontSize = ds.sp(11f),
            color = if (event.isActive) Color(0xFF999999) else Color(0xFF666666),
            fontWeight = FontWeight.Normal,
        )
    }
}

// ── 工具调用 pill ──

@Composable
private fun ToolEventRow(
    event: StreamEvent,
    ds: com.cephalon.lucyApp.components.DesignScale,
) {
    val label = event.label
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = ds.sh(2.dp)),
    ) {
        EventCheckIcon(isActive = event.isActive, ds = ds)
        Spacer(modifier = Modifier.width(ds.sw(8.dp)))
        Surface(
            shape = RoundedCornerShape(ds.sm(16.dp)),
            color = Color(0xFFF5F5F5),
            border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = ds.sw(10.dp), vertical = ds.sh(4.dp)),
            ) {
                Text(text = "🔧", fontSize = ds.sp(10f))
                Spacer(modifier = Modifier.width(ds.sw(4.dp)))
                val dots = if (event.isActive) rememberAnimatedDots() else ""
                Text(
                    text = "工具调用：$label$dots",
                    fontSize = ds.sp(11f),
                    color = if (event.isActive) Color(0xFF999999) else Color(0xFF555555),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── 思考事件：可折叠 + 完整思考内容 ──

@Composable
private fun ReasoningEventRow(
    event: StreamEvent,
    reasoningText: String?,
    ds: com.cephalon.lucyApp.components.DesignScale,
) {
    var expanded by remember { mutableStateOf(event.isActive) }
    // 流式期间自动展开
    LaunchedEffect(event.isActive) { if (event.isActive) expanded = true }

    Column {
        // 标题行：○/✓ + "Thinks" + 展开/收起
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = ds.sh(2.dp)),
        ) {
            EventCheckIcon(isActive = event.isActive, ds = ds)
            Spacer(modifier = Modifier.width(ds.sw(8.dp)))
            val dots = if (event.isActive) rememberAnimatedDots() else ""
            Text(
                text = "${event.label}$dots",
                fontSize = ds.sp(11f),
                color = if (event.isActive) Color(0xFF999999) else Color(0xFF666666),
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(ds.sw(6.dp)))
            Text(
                text = if (expanded) "▾" else "▸",
                fontSize = ds.sp(12f),
                color = Color(0xFFAAAAAA),
            )
        }
        // 思考内容
        if (expanded && !reasoningText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(ds.sm(8.dp)),
                color = Color(0xFFF9F9F9),
                border = BorderStroke(0.5.dp, Color(0xFFE8E8E8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ds.sw(24.dp), top = ds.sh(2.dp), bottom = ds.sh(4.dp)),
            ) {
                Text(
                    text = reasoningText,
                    fontSize = ds.sp(12f),
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(ds.sm(10.dp)),
                    lineHeight = ds.sp(18f),
                )
            }
        }
    }
}

// ── Finish 标记 ──

@Composable
private fun FinishEventRow(
    event: StreamEvent,
    ds: com.cephalon.lucyApp.components.DesignScale,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = ds.sh(2.dp)),
    ) {
        EventCheckIcon(isActive = false, ds = ds)
        Spacer(modifier = Modifier.width(ds.sw(8.dp)))
        Text(
            text = event.label,
            fontSize = ds.sp(11f),
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── ○ / ✓ 圆圈勾选图标（Canvas 绘制，确保居中）──

@Composable
private fun EventCheckIcon(
    isActive: Boolean,
    ds: com.cephalon.lucyApp.components.DesignScale,
) {
    val iconSize = ds.sm(12.dp)
    val strokeColor = Color(0xFFCCCCCC)
    val fillColor = Color(0xFF4CAF50)
    val checkColor = Color.White

    androidx.compose.foundation.Canvas(modifier = Modifier.size(iconSize)) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        if (isActive) {
            // 未完成：空心圆
            drawCircle(
                color = strokeColor,
                radius = r - 1.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
            )
        } else {
            // 完成：绿色实心圆
            drawCircle(
                color = fillColor,
                radius = r,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
            // 居中绘制 ✓
            val path = androidx.compose.ui.graphics.Path().apply {
                val s = r * 0.45f
                moveTo(cx - s * 0.8f, cy)
                lineTo(cx - s * 0.15f, cy + s * 0.55f)
                lineTo(cx + s * 0.85f, cy - s * 0.5f)
            }
            drawPath(
                path = path,
                color = checkColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
        }
    }
}

@Composable
private fun StreamingStatusRow(
    text: String,
    ds: com.cephalon.lucyApp.components.DesignScale,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = ds.sw(4.dp), vertical = ds.sh(4.dp)),
    ) {
        val dots = rememberAnimatedDots()
        Text(
            text = "$text$dots",
            fontSize = ds.sp(14f),
            color = Color(0xFF999999),
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
private fun rememberAnimatedDots(): String {
    var dotCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = (dotCount + 1) % 4
        }
    }
    return ".".repeat(dotCount)
}

@Composable
private fun ImageAttachmentCell(
    attachment: DraftAttachment?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    if (attachment == null) {
        Spacer(modifier = modifier)
    } else {
        Card(
            shape = RoundedCornerShape(ds.sm(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B)),
            modifier = modifier
                .clickable { onClick() }
        ) {
            if (attachment.nasFileId != null) {
                BlobImage(
                    blobRef = attachment.uri,
                    contentDescription = attachment.displayName,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                PlatformImageThumbnail(
                    uri = attachment.uri,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
