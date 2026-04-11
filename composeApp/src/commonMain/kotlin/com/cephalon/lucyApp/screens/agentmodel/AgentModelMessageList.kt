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
import androidx.compose.foundation.lazy.items
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
import com.cephalon.lucyApp.media.AudioRecording
import com.cephalon.lucyApp.media.PickedFile
import com.cephalon.lucyApp.media.PlatformImageThumbnail
import kotlinx.coroutines.delay

private const val STREAMING_PLACEHOLDER_TEXT = "思考中..."

@Composable
internal fun AgentModelMessageList(
    messages: List<ChatItem>,
    playingRecordingId: String?,
    onToggleRecordingPlayback: (AudioRecording) -> Unit,
    onImageClick: (ImagePreviewState) -> Unit,
    onFileClick: (PickedFile) -> Unit,
    onTapMessageArea: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(items = messages) { item ->
            when (item) {
                is ChatItem.Assistant -> {
                    val displayText =
                        if (item.text == STREAMING_PLACEHOLDER_TEXT) {
                            rememberThinkingStatusText()
                        } else {
                            item.text
                        }
                    Bubble(
                        text = displayText,
                        background = Color.White,
                        textColor = Color(0xFF111111),
                        alignEnd = false,
                        onClick = onTapMessageArea
                    )
                }

                is ChatItem.User -> {
                    Bubble(
                        text = item.text,
                        background = Color(0xFF111111),
                        textColor = Color.White,
                        alignEnd = true,
                        onClick = onTapMessageArea
                    )
                }

                is ChatItem.UserAttachments -> {
                    BubbleContainer(alignEnd = true) { bubbleMaxWidth ->
                        val imageCellSize = ((bubbleMaxWidth - 28.dp - 8.dp) / 2).coerceAtMost(132.dp)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                            modifier = Modifier
                                .clickable { onTapMessageArea() }
                                .wrapContentWidth()
                                .widthIn(max = bubbleMaxWidth)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val messageText = item.text
                                if (!messageText.isNullOrBlank()) {
                                    Text(
                                        text = messageText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }

                                val images = item.attachments.filter { it.type == DraftAttachmentType.Image }
                                val files = item.attachments.filter { it.type == DraftAttachmentType.File }
                                val audios = item.attachments.filter { it.type == DraftAttachmentType.Audio }

                                if (images.isNotEmpty()) {
                                    val imageUris = images.map { it.uri }
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        images.chunked(2).forEachIndexed { rowIndex, rowImages ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    files.forEach { attachment ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF2B2B2B),
                                            border = BorderStroke(1.dp, Color(0xFF3A3A3A)),
                                            modifier = Modifier.clickable { onFileClick(attachment.asPickedFile()) }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = Color.White.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = attachment.fileExtensionLabel(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Text(
                                                    text = attachment.displayName(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    audios.forEach { attachment ->
                                        val recording = attachment.asAudioRecording()
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF2B2B2B),
                                            border = BorderStroke(1.dp, Color(0xFF3A3A3A))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = Color.White.copy(alpha = 0.12f),
                                                    modifier = Modifier.size(32.dp)
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
                                                        color = Color(0xFFBBBBBB)
                                                    )
                                                    Text(
                                                        text = recording.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.White,
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
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
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
            PlatformImageThumbnail(
                uri = attachment.uri,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
