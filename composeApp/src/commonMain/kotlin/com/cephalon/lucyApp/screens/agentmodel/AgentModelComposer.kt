package com.cephalon.lucyApp.screens.agentmodel

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_composer_add
import androidios.composeapp.generated.resources.ic_composer_mic
import androidios.composeapp.generated.resources.ic_composer_send
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.components.BlobImage
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.media.PlatformImageThumbnail
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun AgentModelComposer(
    inputText: TextFieldValue,
    onInputTextChange: (TextFieldValue) -> Unit,
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
    val ds = LocalDesignScale.current
    val boxShape = RoundedCornerShape(ds.sm(16.dp))
    val glassBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFDFDFDF).copy(alpha = 0.10f),
            Color.White
        )
    )
    val actionBtnShape = RoundedCornerShape(30.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F7))
            .imePadding()
            .padding(start = ds.sw(12.dp), end = ds.sw(12.dp), top = ds.sh(8.dp), bottom = ds.sh(24.dp))
    ) {
        // ── 外部毛玻璃盒子 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = boxShape,
                    ambientColor = Color.Black.copy(alpha = 0.03f),
                    spotColor = Color.Black.copy(alpha = 0.05f)
                )
                .clip(boxShape)
                .background(glassBrush)
                .border(1.dp, Color.White, boxShape)
                .padding(ds.sm(12.dp))
        ) {
            if (draftAttachments.isNotEmpty()) {
                DraftAttachmentPreviewRow(
                    attachments = draftAttachments,
                    onRemoveAttachment = onRemoveDraftAttachment,
                    onImageClick = onImageClick,
                    onFileClick = onFileClick,
                    playingRecordingId = playingRecordingId,
                    onToggleRecordingPlayback = onToggleRecordingPlayback,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(ds.sh(8.dp)))
            }

            // ── 输入框（无边框）──
            BasicTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                textStyle = TextStyle(
                    color = Color(0xFF1F2535),
                    fontSize = ds.sp(15f),
                    fontWeight = FontWeight.Medium
                ),
                maxLines = Int.MAX_VALUE,
                cursorBrush = SolidColor(Color(0xFF1F2535)),
                enabled = !isRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ds.sm(3.dp)),
                decorationBox = { innerTextField ->
                    Box {
                        if (inputText.text.isEmpty()) {
                            Text(
                                text = if (isRecording) "录音中..."
                                else if (isVoiceBusy) "正在转写语音..."
                                else "Ask Anything",
                                color = Color(0xFF9A9A9A),
                                fontSize = ds.sp(15f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            // ── 底部操作栏 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：+ 和 麦克风
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ds.sw(8.dp))
                ) {
                    // + 号按钮
                    Box(
                        modifier = Modifier
                            .border(0.5.dp, Color(0xFF1F2535).copy(alpha = 0.20f), actionBtnShape)
                            .clip(actionBtnShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggleAttachments() }
                            .padding(ds.sm(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_composer_add),
                            contentDescription = "More",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(ds.sm(16.dp))
                        )
                    }
                    // 麦克风按钮
                    Box(
                        modifier = Modifier
                            .border(0.5.dp, Color(0xFF1F2535).copy(alpha = 0.20f), actionBtnShape)
                            .clip(actionBtnShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { if (!isVoiceBusy) onVoiceStart() }
                            .padding(ds.sm(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_composer_mic),
                            contentDescription = "Voice",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(ds.sm(16.dp))
                        )
                    }
                }

                // 右侧：发送按钮
                Box(
                    modifier = Modifier
                        .size(ds.sm(30.dp))
                        .border(0.5.dp, Color(0xFF1F2535), actionBtnShape)
                        .clip(actionBtnShape)
                        .background(Color(0xFF1F2535))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSend() }
                        .padding(start = ds.sw(6.dp), end = ds.sw(6.dp), top = ds.sh(7.dp), bottom = ds.sh(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_composer_send),
                        contentDescription = "Send",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(ds.sm(18.dp))
                    )
                }
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
                        .padding(top = 4.dp, end = 4.dp)
                        .size(20.dp)
                        .clickable { onRemoveAttachment(attachment) },
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.40f)
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
