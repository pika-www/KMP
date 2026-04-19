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
import com.cephalon.lucyApp.sdk.MediaAttachment

private const val STREAMING_PLACEHOLDER_TEXT = "思考中..."

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
    onImageClick: (ImagePreviewState) -> Unit,
    onFileClick: (PickedFile) -> Unit,
    onTapMessageArea: () -> Unit,
    onSkillClick: (String) -> Unit = {},
    onAttachmentClick: (MediaAttachment) -> Unit = {},
    onCopySuccess: () -> Unit = {},
    streamingStatusText: String? = null,
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
                is ChatItem.Assistant -> "assistant_${item.messageId ?: index}"
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
                    val isThinkingPlaceholder = item.text == STREAMING_PLACEHOLDER_TEXT
                    val displayText =
                        if (isThinkingPlaceholder) {
                            streamingStatusText ?: rememberThinkingStatusText()
                        } else {
                            item.text
                        }
                    Column {
                        if (displayText.isNotBlank()) {
                            Bubble(
                                text = displayText,
                                background = Color.Transparent,
                                textColor = Color(0xFF111111),
                                alignEnd = false,
                                border = null,
                                isMarkdown = !isThinkingPlaceholder,
                                onClick = onTapMessageArea,
                                onCopySuccess = onCopySuccess,
                            )
                        }
                        if (item.attachments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(ds.sh(6.dp)))
                            AssistantAttachments(
                                attachments = item.attachments,
                                onAttachmentClick = onAttachmentClick,
                            )
                        }
                    }
                }

                is ChatItem.User -> {
                    BubbleContainer(alignEnd = true) { bubbleMaxWidth ->
                        Surface(
                            shape = RoundedCornerShape(ds.sm(99.dp)),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, Color(0xFF1F2535).copy(alpha = 0.20f)),
                            modifier = Modifier
                                .clickable { onTapMessageArea() }
                                .wrapContentWidth()
                                .widthIn(max = bubbleMaxWidth)
                        ) {
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

                is ChatItem.UserAttachments -> {
                    BubbleContainer(alignEnd = true) { bubbleMaxWidth ->
                        val imageCellSize = ((bubbleMaxWidth - 28.dp - 8.dp) / 2).coerceAtMost(132.dp)
                        Surface(
                            shape = RoundedCornerShape(ds.sm(16.dp)),
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

                                Column(verticalArrangement = Arrangement.spacedBy(ds.sh(8.dp))) {
                                    files.forEach { attachment ->
                                        Surface(
                                            shape = RoundedCornerShape(ds.sm(12.dp)),
                                            color = Color(0xFFF5F5F5),
                                            border = BorderStroke(1.dp, Color(0xFFE7E7E7)),
                                            modifier = Modifier.clickable { onFileClick(attachment.asPickedFile()) }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = ds.sw(10.dp), vertical = ds.sh(10.dp)),
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

                                    audios.forEach { attachment ->
                                        val recording = attachment.asAudioRecording()
                                        Surface(
                                            shape = RoundedCornerShape(ds.sm(12.dp)),
                                            color = Color(0xFFF5F5F5),
                                            border = BorderStroke(1.dp, Color(0xFFE7E7E7))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = ds.sw(10.dp), vertical = ds.sh(10.dp)),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(ds.sw(10.dp))
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = Color(0xFF111111),
                                                    modifier = Modifier.size(ds.sm(32.dp))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clickable { onToggleRecordingPlayback(recording) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        androidx.compose.material3.Icon(
                                                            imageVector = if (playingRecordingId == recording.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "语音",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF8A8A8A)
                                                    )
                                                    Text(
                                                        text = recording.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF111111),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                OutlinedButton(onClick = { onToggleRecordingPlayback(recording) }) {
                                                    Text(if (playingRecordingId == recording.id) "暂停" else "播放")
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
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                is ChatItem.RecordingItem -> {
                    BubbleContainer(alignEnd = false) { bubbleMaxWidth ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE7E7E7)),
                            modifier = Modifier
                                .clickable { onTapMessageArea() }
                                .wrapContentWidth()
                                .widthIn(max = bubbleMaxWidth)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = Color(0xFF111111)
                                )
                                Column(modifier = Modifier.widthIn(max = bubbleMaxWidth - 140.dp)) {
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
                                        onToggleRecordingPlayback(
                                            AudioRecording(
                                                id = item.id,
                                                name = item.name,
                                                path = item.path
                                            )
                                        )
                                    }
                                ) {
                                    Text(if (playingRecordingId == item.id) "暂停" else "播放")
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
        Res.drawable.ic_skill_knowledge to "脑花帮解答 知识库检索专属答案",
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
    onAttachmentClick: (MediaAttachment) -> Unit = {},
) {
    val ds = LocalDesignScale.current
    val imageAttachments = attachments.filter { it.effectiveContentType()?.startsWith("image") == true }
    val audioAttachments = attachments.filter { it.effectiveContentType()?.startsWith("audio") == true }
    val docAttachments = attachments.filter {
        val ct = it.effectiveContentType()
        ct == null || (!ct.startsWith("image") && !ct.startsWith("audio"))
    }

    // ── 图片附件：缩略图 + 点击下载 ──
    if (imageAttachments.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ds.sw(6.dp)),
        ) {
            imageAttachments.forEach { att ->
                Box(
                    modifier = Modifier
                        .size(ds.sw(120.dp))
                        .clip(RoundedCornerShape(ds.sm(8.dp))),
                ) {
                    BlobImage(
                        blobRef = att.blobRef,
                        contentDescription = att.fileName,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // 仅下载图标可点击
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(ds.sm(6.dp))
                            .size(ds.sm(24.dp))
                            .clip(RoundedCornerShape(ds.sm(12.dp)))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { onAttachmentClick(att) },
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
        }
    }

    // ── 音频附件：文件卡片 ──
    audioAttachments.forEach { att ->
        AttachmentFileCard(
            icon = Res.drawable.ic_audio,
            fileName = att.fileName ?: "音频文件",
            onDownloadClick = { onAttachmentClick(att) },
        )
    }

    // ── 文档附件：文件卡片 ──
    docAttachments.forEach { att ->
        AttachmentFileCard(
            icon = Res.drawable.ic_doc,
            fileName = att.fileName ?: "文件",
            onDownloadClick = { onAttachmentClick(att) },
        )
    }
}

@Composable
private fun AttachmentFileCard(
    icon: org.jetbrains.compose.resources.DrawableResource,
    fileName: String,
    onDownloadClick: () -> Unit,
) {
    val ds = LocalDesignScale.current
    Surface(
        shape = RoundedCornerShape(ds.sm(12.dp)),
        color = Color(0xFFF5F5F7),
        modifier = Modifier
            .padding(top = ds.sh(4.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sw(12.dp), vertical = ds.sh(10.dp)),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(ds.sm(20.dp)),
                tint = Color(0xFF666666),
            )
            Spacer(modifier = Modifier.width(ds.sw(8.dp)))
            Text(
                text = fileName,
                fontSize = ds.sp(14f),
                color = Color(0xFF333333),
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(ds.sw(8.dp)))
            Icon(
                painter = painterResource(Res.drawable.ic_download),
                contentDescription = "下载",
                modifier = Modifier
                    .size(ds.sm(18.dp))
                    .clickable(onClick = onDownloadClick),
                tint = Color(0xFF999999),
            )
        }
    }
}

@Composable
private fun ImageAttachmentCell(
    attachment: DraftAttachment?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachment == null) {
        Spacer(modifier = modifier)
    } else {
        Card(
            shape = RoundedCornerShape(12.dp),
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
