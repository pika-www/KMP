package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.media.PlatformImageThumbnail

@Composable
internal fun AgentModelComposer(
    inputText: String,
    onInputChange: (String) -> Unit,
    draftAttachments: List<DraftAttachment>,
    onRemoveDraftAttachment: (DraftAttachment) -> Unit,
    onImageClick: (ImagePreviewState) -> Unit,
    onFileClick: (DraftAttachment) -> Unit,
    playingRecordingId: String?,
    onToggleRecordingPlayback: (String) -> Unit,
    isRecording: Boolean,
    isVoiceBusy: Boolean,
    onVoiceStart: () -> Unit,
    attachmentsExpanded: Boolean,
    onToggleAttachments: () -> Unit,
    onSend: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F7))
            .padding(bottom = 8.dp)
    ) {
        HorizontalDivider(color = Color(0xFFEDEDED))

//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 12.dp, vertical = 10.dp),
//            horizontalArrangement = Arrangement.spacedBy(10.dp)
//        ) {
//            SuggestionChip(text = "快速") { onSuggestionClick("快速") }
//            SuggestionChip(text = "帮我写作") { onSuggestionClick("帮我写作") }
//            SuggestionChip(text = "AI 创作") { onSuggestionClick("AI 创作") }
//            SuggestionChip(text = "拍题答疑") { onSuggestionClick("拍题答疑") }
//        }
//
//        if (draftAttachments.isNotEmpty()) {
//            DraftAttachmentPreviewRow(
//                attachments = draftAttachments,
//                onRemoveAttachment = onRemoveDraftAttachment,
//                onImageClick = onImageClick,
//                onFileClick = onFileClick,
//                playingRecordingId = playingRecordingId,
//                onToggleRecordingPlayback = onToggleRecordingPlayback,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 12.dp)
//            )
//        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleAttachments) {
                Icon(
                    imageVector = if (attachmentsExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "More",
                    tint = Color(0xFF111111)
                )
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                TextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = if (isRecording) {
                                "录音中..."
                            } else if (isVoiceBusy) {
                                "正在转写语音..."
                            } else {
                                "发消息或点击语音输入..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9A9A9A)
                        )
                    },
                    leadingIcon = {
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = if (isRecording) Color(0xFF111111) else if (isVoiceBusy) Color(0xFFB5B5B5) else Color(0xFF6B6B6B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF111111)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRecording
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = {
                if (!isVoiceBusy) {
                    onVoiceStart()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isVoiceBusy) Color(0xFFB5B5B5) else Color(0xFF111111)
                )
            }

            IconButton(onClick = onSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color(0xFF111111)
                )
            }
        }
    }
}

@Composable
private fun DraftAttachmentPreviewRow(
    attachments: List<DraftAttachment>,
    onRemoveAttachment: (DraftAttachment) -> Unit,
    onImageClick: (ImagePreviewState) -> Unit,
    onFileClick: (DraftAttachment) -> Unit,
    playingRecordingId: String?,
    onToggleRecordingPlayback: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageAttachments = attachments.filter { it.type == DraftAttachmentType.Image }
    val imageUris = imageAttachments.map { it.uri }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(
            items = attachments,
            key = { index, attachment -> "${attachment.type}:${attachment.uri}:$index" }
        ) { _, attachment ->
            Box {
                when (attachment.type) {
                    DraftAttachmentType.Image -> {
                        val imageIndex = imageAttachments.indexOfFirst { it.uri == attachment.uri }
                        Card(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable {
                                    if (imageIndex >= 0) {
                                        onImageClick(
                                            ImagePreviewState(
                                                images = imageUris,
                                                selectedIndex = imageIndex
                                            )
                                        )
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            PlatformImageThumbnail(
                                uri = attachment.uri,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    DraftAttachmentType.File -> {
                        Surface(
                            modifier = Modifier
                                .width(164.dp)
                                .height(72.dp)
                                .clickable { onFileClick(attachment) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5),
                            border = BorderStroke(1.dp, Color(0xFFE7E7E7))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color(0xFF111111)
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
                                    color = Color(0xFF111111),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    DraftAttachmentType.Audio -> {
                        Surface(
                            modifier = Modifier
                                .width(210.dp)
                                .height(72.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5),
                            border = BorderStroke(1.dp, Color(0xFFE7E7E7))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color(0xFF111111),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { onToggleRecordingPlayback(attachment.uri) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (playingRecordingId == attachment.uri) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Audio Preview",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "语音",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8A8A8A)
                                    )
                                    Text(
                                        text = uriDisplayName(attachment.uri),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF111111),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(onClick = { onToggleRecordingPlayback(attachment.uri) }) {
                                    Text(if (playingRecordingId == attachment.uri) "暂停" else "播放")
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clickable { onRemoveAttachment(attachment) },
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xB3000000)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, Color(0xFFE7E7E7)),
        modifier = Modifier
            .height(34.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF111111)
            )
        }
    }
}
