package com.cephalon.lucyApp.screens.localdeploy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.media.PlatformImageThumbnail

@Composable
internal fun LocalDeployTestComposer(
    inputText: String,
    onInputChange: (String) -> Unit,
    draftAttachments: List<DraftAttachment>,
    onRemoveDraftAttachment: (DraftAttachment) -> Unit,
    isRecording: Boolean,
    isCancelBySlide: Boolean,
    voiceCancelThreshold: Dp,
    onVoiceStart: () -> Unit,
    onVoiceFinish: () -> Unit,
    onVoiceCancel: () -> Unit,
    onVoiceCancelStateChange: (Boolean) -> Unit,
    attachmentsExpanded: Boolean,
    onToggleAttachments: () -> Unit,
    onSend: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnVoiceStart = rememberUpdatedState(onVoiceStart)
    val currentOnVoiceFinish = rememberUpdatedState(onVoiceFinish)
    val currentOnVoiceCancel = rememberUpdatedState(onVoiceCancel)
    val currentOnVoiceCancelStateChange = rememberUpdatedState(onVoiceCancelStateChange)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 8.dp)
    ) {
        HorizontalDivider(color = Color(0xFFEDEDED))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SuggestionChip(text = "快速") { onSuggestionClick("快速") }
            SuggestionChip(text = "帮我写作") { onSuggestionClick("帮我写作") }
            SuggestionChip(text = "AI 创作") { onSuggestionClick("AI 创作") }
            SuggestionChip(text = "拍题答疑") { onSuggestionClick("拍题答疑") }
        }

        if (draftAttachments.isNotEmpty()) {
            DraftAttachmentPreviewRow(
                attachments = draftAttachments,
                onRemoveAttachment = onRemoveDraftAttachment,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
        }

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
                                if (isCancelBySlide) "松开取消" else "松开发送，上滑取消"
                            } else {
                                "发消息或按住说话..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9A9A9A)
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .pointerInput(voiceCancelThreshold) {
                                    var voiceDragDyPx = 0f
                                    val thresholdPx = voiceCancelThreshold.toPx()
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            voiceDragDyPx = 0f
                                            currentOnVoiceCancelStateChange.value(false)
                                            currentOnVoiceStart.value()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            voiceDragDyPx += dragAmount.y
                                            currentOnVoiceCancelStateChange.value(voiceDragDyPx <= -thresholdPx)
                                        },
                                        onDragCancel = {
                                            voiceDragDyPx = 0f
                                            currentOnVoiceCancelStateChange.value(false)
                                            currentOnVoiceCancel.value()
                                        },
                                        onDragEnd = {
                                            val shouldCancel = voiceDragDyPx <= -thresholdPx
                                            voiceDragDyPx = 0f
                                            currentOnVoiceCancelStateChange.value(false)
                                            if (shouldCancel) {
                                                currentOnVoiceCancel.value()
                                            } else {
                                                currentOnVoiceFinish.value()
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = if (isRecording) Color(0xFF111111) else Color(0xFF6B6B6B),
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
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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
    modifier: Modifier = Modifier,
) {
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
                        Card(
                            modifier = Modifier.size(72.dp),
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
                                .height(72.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5),
                            border = BorderStroke(1.dp, Color(0xFFE7E7E7))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "文件",
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
