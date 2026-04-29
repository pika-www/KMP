package com.cephalon.lucyApp.screens.agentmodel

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_camera
import androidios.composeapp.generated.resources.ic_composer_add
import androidios.composeapp.generated.resources.ic_composer_mic
import androidios.composeapp.generated.resources.ic_img
import androidios.composeapp.generated.resources.ic_send
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.ErrorOutline
import com.cephalon.lucyApp.components.BlobImage
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.media.AudioPlaybackState
import com.cephalon.lucyApp.media.PlatformImageThumbnail
import com.cephalon.lucyApp.sdk.MediaAttachment
import org.jetbrains.compose.resources.painterResource
import androidios.composeapp.generated.resources.ic_audio
import androidios.composeapp.generated.resources.ic_doc
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

@Composable
internal fun AgentModelComposer(
    inputText: TextFieldValue,
    onInputTextChange: (TextFieldValue) -> Unit,
    draftAttachments: List<DraftAttachment>,
    onRemoveDraftAttachment: (DraftAttachment) -> Unit,
    onImageClick: (ImagePreviewState) -> Unit,
    onFileClick: (DraftAttachment) -> Unit,
    onAudioClick: (DraftAttachment) -> Unit,
    audioPlaybackState: AudioPlaybackState = AudioPlaybackState(),
    loadingAudioBlobRefs: Set<String> = emptySet(),
    onToggleRecordingPlayback: (DraftAttachment) -> Unit,
    isRecording: Boolean,
    isVoiceBusy: Boolean,
    onVoiceStart: () -> Unit,
    attachmentsExpanded: Boolean,
    onToggleAttachments: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isStopMode: Boolean = false,
    isSendDisabled: Boolean = false,
    onSuggestionClick: (String) -> Unit,
    onInputFocusChanged: (Boolean) -> Unit = {},
    uploadStates: Map<String, AttachmentUploadState> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    val circleShape = RoundedCornerShape(ds.sm(99.dp))
    val inputLineHeight = ds.sp(16f)
    var visualLineCount by remember { mutableStateOf(1) }
    val currentLineCount = maxOf(
        inputText.text.lineSequence().count().coerceAtLeast(1),
        visualLineCount
    )
    var hasEnteredMultilineMode by remember { mutableStateOf(currentLineCount > 1) }
    LaunchedEffect(currentLineCount) {
        hasEnteredMultilineMode = when {
            inputText.text.isEmpty() -> false
            currentLineCount > 1 -> true
            else -> hasEnteredMultilineMode
        }
    }
    val isMultilineInput = hasEnteredMultilineMode
    val inputShape = if (isMultilineInput) {
        RoundedCornerShape(16.dp)
    } else {
        RoundedCornerShape(ds.sm(100.dp))
    }
    val inputScrollState = rememberScrollState()
    val density = LocalDensity.current
    val quickMenuLeftPx = with(density) { 20.dp.roundToPx() }
    var bottomActionBarTopInWindowPx by remember { mutableStateOf(0) }
    var quickMenuSize by remember { mutableStateOf(IntSize.Zero) }
    var isInputFocused by remember { mutableStateOf(false) }
    var previousInputLineCount by remember { mutableStateOf(currentLineCount) }

    LaunchedEffect(inputText.text, isInputFocused) {
        if (isInputFocused && currentLineCount > previousInputLineCount) {
            // Wait one frame for scrollState.maxValue to reflect the newly inserted line.
            delay(32)
            inputScrollState.scrollTo(inputScrollState.maxValue)
        }
        previousInputLineCount = currentLineCount
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .imePadding()
            .padding(start = ds.sw(20.dp), end = ds.sw(20.dp), top = ds.sh(8.dp), bottom = ds.sh(24.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (draftAttachments.isNotEmpty()) {
                DraftAttachmentPreviewRow(
                    attachments = draftAttachments,
                    onRemoveAttachment = onRemoveDraftAttachment,
                    onImageClick = onImageClick,
                    onFileClick = onFileClick,
                    onAudioClick = onAudioClick,
                    audioPlaybackState = audioPlaybackState,
                    loadingAudioBlobRefs = loadingAudioBlobRefs,
                    onToggleRecordingPlayback = onToggleRecordingPlayback,
                    uploadStates = uploadStates,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(ds.sh(8.dp)))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ds.sh(40.dp))
                    .onGloballyPositioned { coordinates ->
                        bottomActionBarTopInWindowPx = coordinates.boundsInWindow().top.toInt()
                    },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(ds.sw(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(ds.sm(36.dp))
                        .clip(circleShape)
                        .background(Color.Black.copy(alpha = 0.05f))
                        .border(0.5.dp, Color.Black.copy(alpha = 0.05f), circleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onToggleAttachments() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_composer_add),
                        contentDescription = "More",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(ds.sm(16.dp))
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isMultilineInput) {
                                Modifier.heightIn(min = ds.sh(40.dp))
                            } else {
                                Modifier.height(ds.sh(40.dp))
                            }
                        )
                        .shadow(
                            elevation = 30.dp,
                            shape = inputShape,
                            ambientColor = Color.Black.copy(alpha = 0.05f),
                            spotColor = Color.Black.copy(alpha = 0.05f)
                        )
                        .clip(inputShape)
                        .background(Color.Black.copy(alpha = 0.05f))
                        .border(0.5.dp, Color.Black.copy(alpha = 0.05f), inputShape)
                        .padding(
                            start = ds.sw(16.dp),
                            top = if (isMultilineInput) ds.sh(12.dp) else ds.sh(8.dp),
                            end = ds.sw(12.dp),
                            bottom = if (isMultilineInput) ds.sh(12.dp) else ds.sh(8.dp)
                        )
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        textStyle = TextStyle(
                            color = Color.Black.copy(alpha = 0.9f),
                            fontSize = ds.sp(12f),
                            fontWeight = FontWeight.Normal,
                            lineHeight = inputLineHeight
                        ),
                        maxLines = Int.MAX_VALUE,
                        cursorBrush = SolidColor(Color.Black.copy(alpha = 0.9f)),
                        enabled = !isRecording,
                        onTextLayout = { textLayoutResult ->
                            visualLineCount = textLayoutResult.lineCount.coerceAtLeast(1)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = inputLineHeight.value.dp * 4)
                            .then(
                                if (isMultilineInput) {
                                    Modifier.verticalScroll(inputScrollState)
                                } else {
                                    Modifier
                                }
                            )
                            .padding(
                                end = if (isMultilineInput) ds.sw(48.dp) else ds.sw(28.dp),
                                bottom = if (isMultilineInput) ds.sh(20.dp) else 0.dp
                            )
                            .onFocusChanged {
                                isInputFocused = it.isFocused
                                onInputFocusChanged(it.isFocused)
                            },
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isMultilineInput) {
                                            Modifier
                                        } else {
                                            Modifier.fillMaxHeight()
                                        }
                                    ),
                                contentAlignment = if (isMultilineInput) {
                                    Alignment.TopStart
                                } else {
                                    Alignment.CenterStart
                                }
                            ) {
                                if (inputText.text.isEmpty()) {
                                    Text(
                                        text = when {
                                            isRecording -> "录音中..."
                                            isVoiceBusy -> "正在转写语音..."
                                            else -> "询问脑花"
                                        },
                                        color = Color.Black.copy(alpha = 0.40f),
                                        fontSize = ds.sp(12f),
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = ds.sp(16f),
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (isMultilineInput) {
                        SendActionButton(
                            isSendDisabled = isSendDisabled,
                            isStopMode = isStopMode,
                            onSend = onSend,
                            onStop = onStop,
                            buttonSize = 24.dp,
                            iconSize = 12.dp,
                            stopIndicatorSize = 7.dp,
                            stopIndicatorCorner = 1.5.dp,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { if (!isVoiceBusy) onVoiceStart() }
                                .padding(vertical = ds.sh(4.dp)),
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
                }

                if (!isMultilineInput) {
                    SendActionButton(
                        isSendDisabled = isSendDisabled,
                        isStopMode = isStopMode,
                        onSend = onSend,
                        onStop = onStop,
                    )
                }
            }
        }
    }
    if (attachmentsExpanded) {
        val quickMenuOffset = IntOffset(
            x = quickMenuLeftPx,
            y = bottomActionBarTopInWindowPx - quickMenuSize.height
        )
        Popup(
            alignment = Alignment.TopStart,
            offset = quickMenuOffset,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
            onDismissRequest = { onToggleAttachments() }
        ) {
            AttachmentQuickMenu(
                onOpenCamera = {
                    onOpenCamera()
                    onToggleAttachments()
                },
                onOpenGallery = {
                    onOpenGallery()
                    onToggleAttachments()
                },
                onOpenFilePicker = {
                    onOpenFilePicker()
                    onToggleAttachments()
                },
                onMeasured = { quickMenuSize = it }
            )
        }
    }
}

@Composable
private fun SendActionButton(
    isSendDisabled: Boolean,
    isStopMode: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    buttonSize: androidx.compose.ui.unit.Dp = 36.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    stopIndicatorSize: androidx.compose.ui.unit.Dp = 10.dp,
    stopIndicatorCorner: androidx.compose.ui.unit.Dp = 2.dp,
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(ds.sm(99.dp)))
            .background(if (isSendDisabled) Color.Black.copy(alpha = 0.35f) else Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isSendDisabled,
            ) {
                if (isStopMode) onStop() else onSend()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isStopMode) {
            Box(
                modifier = Modifier
                    .size(stopIndicatorSize)
                    .offset(y = (-ds.sh(1.dp)))
                    .clip(RoundedCornerShape(stopIndicatorCorner))
                    .background(Color.White)
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.ic_send),
                contentDescription = "Send",
                tint = Color.Unspecified,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun AttachmentQuickMenu(
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onMeasured: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    val menuShape = RoundedCornerShape(ds.sm(28.dp))
    Column(
        modifier = modifier
            .wrapContentWidth()
            .onGloballyPositioned { onMeasured(it.size) }
            .shadow(
                elevation = 30.dp,
                shape = menuShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(menuShape)
            .background(Color.White)
            .padding(horizontal = ds.sw(20.dp), vertical = ds.sh(16.dp)),
        verticalArrangement = Arrangement.spacedBy(ds.sh(0.dp))
    ) {
        AttachmentQuickMenuItem(
            iconRes = Res.drawable.ic_camera,
            label = "摄像头",
            onClick = onOpenCamera,
        )
        AttachmentQuickMenuItem(
            iconRes = Res.drawable.ic_img,
            label = "相册",
            onClick = onOpenGallery,
        )
        AttachmentQuickMenuItem(
            iconRes = Res.drawable.ic_doc,
            label = "文档",
            onClick = onOpenFilePicker,
        )
    }
}

@Composable
private fun AttachmentQuickMenuItem(
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    onClick: () -> Unit,
) {
    val ds = LocalDesignScale.current
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .heightIn(min = ds.sh(40.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = Color.Black.copy(alpha = 0.9f),
            fontSize = ds.sp(12f),
            fontWeight = FontWeight.Normal,
            lineHeight = ds.sp(16f),
        )
    }
}

@Composable
private fun DraftAttachmentPreviewRow(
    attachments: List<DraftAttachment>,
    onRemoveAttachment: (DraftAttachment) -> Unit,
    onImageClick: (ImagePreviewState) -> Unit,
    onFileClick: (DraftAttachment) -> Unit,
    onAudioClick: (DraftAttachment) -> Unit,
    audioPlaybackState: AudioPlaybackState = AudioPlaybackState(),
    loadingAudioBlobRefs: Set<String> = emptySet(),
    onToggleRecordingPlayback: (DraftAttachment) -> Unit,
    uploadStates: Map<String, AttachmentUploadState> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val imageAttachments = attachments.filter { it.type == DraftAttachmentType.Image }
    val visualAttachments = attachments.filter {
        it.type == DraftAttachmentType.Image ||
            it.type == DraftAttachmentType.File ||
            it.type == DraftAttachmentType.Audio
    }
    val imageUris = imageAttachments.map { it.uri }

    val ds = LocalDesignScale.current
    val pillShape = RoundedCornerShape(ds.sm(99.dp))

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ds.sh(8.dp))
    ) {
        // ── 图片 + 文件 + 音频附件（同一横向行混排）──
        if (visualAttachments.isNotEmpty()) {
            val imageCardSize = ds.sm(72.dp)
            val documentCardWidth = ds.sm(180.dp)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(ds.sw(10.dp))
            ) {
                itemsIndexed(
                    items = visualAttachments,
                    key = { index, att -> "visual:${att.type}:${att.uri}:$index" }
                ) { _, attachment ->
                    val uploadState = if (attachment.nasFileId == null) uploadStates[attachment.uri] else null
                    val isFailed = uploadState is AttachmentUploadState.Failed
                    val isUploading = uploadState is AttachmentUploadState.Uploading
                    val imageIndex = imageAttachments.indexOfFirst { it.uri == attachment.uri }
                    val resolvedBlobRef = attachment.blobRef?.trim()?.takeIf { it.isNotBlank() }
                        ?: (uploadStates[attachment.uri] as? AttachmentUploadState.Success)?.blobRef
                    val isAudioLoading = attachment.type == DraftAttachmentType.Audio &&
                        resolvedBlobRef?.let { it in loadingAudioBlobRefs } == true

                    Box {
                        if (attachment.type == DraftAttachmentType.Image) {
                            Card(
                                modifier = Modifier
                                    .size(imageCardSize)
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
                                shape = RoundedCornerShape(ds.sm(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                border = if (isFailed) BorderStroke(1.5.dp, Color(0xFFE53935)) else null,
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
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
                        } else {
                            Surface(
                                modifier = Modifier
                                    .width(documentCardWidth)
                                    .height(imageCardSize)
                                    .clickable {
                                        if (attachment.type == DraftAttachmentType.Audio) {
                                            onAudioClick(attachment)
                                        } else {
                                            onFileClick(attachment)
                                        }
                                    },
                                shape = RoundedCornerShape(ds.sm(12.dp)),
                                color = Color(0xFFF5F5F7),
                                border = if (isFailed) BorderStroke(1.5.dp, Color(0xFFE53935)) else null,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = ds.sw(14.dp)),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(ds.sw(10.dp))
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (attachment.type == DraftAttachmentType.Audio) {
                                                Res.drawable.ic_audio
                                            } else {
                                                Res.drawable.ic_doc
                                            }
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(ds.sm(20.dp)),
                                        tint = Color(0xFF1F2535),
                                    )
                                    Text(
                                        text = attachment.displayName(),
                                        fontSize = ds.sp(14f),
                                        color = Color(0xFF1F2535),
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.MiddleEllipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (attachment.type == DraftAttachmentType.Audio && isAudioLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(ds.sm(16.dp)),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF1F2535),
                                        )
                                    }
                                }
                            }
                        }
                        if (isUploading || isFailed) {
                            Box(
                                modifier = Modifier
                                    .then(
                                        if (attachment.type == DraftAttachmentType.Image) {
                                            Modifier.size(imageCardSize)
                                        } else {
                                            Modifier
                                                .width(documentCardWidth)
                                                .height(imageCardSize)
                                        }
                                    )
                                    .clip(RoundedCornerShape(ds.sm(12.dp)))
                                    .background(
                                        if (isFailed) Color.Red.copy(alpha = 0.18f)
                                        else Color.Black.copy(alpha = 0.25f)
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(ds.sm(20.dp)),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Upload failed",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(ds.sm(20.dp)),
                                    )
                                }
                            }
                        }
                        DraftRemoveButton(
                            ds = ds,
                            modifier = Modifier.align(Alignment.TopEnd),
                            onClick = { onRemoveAttachment(attachment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftRemoveButton(
    ds: com.cephalon.lucyApp.components.DesignScale,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .padding(top = ds.sh(4.dp), end = ds.sw(4.dp))
            .size(ds.sm(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.60f)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(ds.sm(12.dp))
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
) {
    val ds = LocalDesignScale.current
    Surface(
        shape = RoundedCornerShape(ds.sm(18.dp)),
        color = Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, Color(0xFFE7E7E7)),
        modifier = Modifier
            .height(ds.sh(34.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = ds.sw(12.dp))) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF111111)
            )
        }
    }
}
